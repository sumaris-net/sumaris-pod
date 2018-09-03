package net.sumaris.core.dao.technical.schema;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.tool.hbm2ddl.ColumnMetadata;
import org.hibernate.tool.hbm2ddl.ForeignKeyMetadata;
import org.hibernate.tool.hbm2ddl.IndexMetadata;
import org.hibernate.tool.hbm2ddl.TableMetadata;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Overrides of the {@link SumarisTableMetadata} with some improvements:
 * <ul>
 * <li>Obtains number of columns via {@link #getColumnsCount()}</li>
 * <li>Obtains all columns names available via {@link #getColumnNames()}</li>
 * <li>Obtains primary key column names via {@link #getPkNames()}</li>
 * </ul>
 * <p/>
 * And others methods used to synchronize referentials:
 * <p/>
 * <ul>
 * <li>Obtains query to update a row of the table (column names order is the one introduced by method {@link #getColumnNames()}: {@link #getUpdateQuery()}</li>
 * <li>Obtains query to insert a row in the table (column names order is the one introduced by method {@link #getColumnNames()}: {@link #getInsertQuery()}</li>
 * </ul>
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public abstract class SumarisTableMetadata {

	protected static final String QUERY_SELECT_MAX_UPDATE = "SELECT max(update_date) FROM %s";

	protected static final String QUERY_INSERT = "INSERT INTO %s (%s) VALUES (%s)";

	protected static final String QUERY_UPDATE = "UPDATE %s SET %s WHERE %s";

	protected static final String QUERY_DELETE = "DELETE FROM %s WHERE %s";

	protected static final String QUERY_SELECT_PRIMARY_KEYS = "SELECT %s FROM %s";

	protected static final String QUERY_SELECT_COUNT = "SELECT count(*) FROM %s";

	protected static final String QUERY_HQL_SELECT = "from %s";

	protected final String existingPrimaryKeysQuery;

	private final String maxUpdateDateQuery;

	protected final String countQuery;

	protected final TableMetadata delegate;

	protected final Map<String, ColumnMetadata> columns;

	protected final Set<String> pkNames;

	protected final Set<String> notNullNames;

	protected final int[] pkIndexs;

	protected final String insertQuery;

	protected final String updateQuery;

	protected final String hqlSelectQuery;

	protected final boolean withUpdateDateColumn;

	protected SingleTableEntityPersister entityPersister;

	public abstract String getCountDataToUpdateQueryWithNull();

	public abstract String getDataToUpdateQueryWithNull();

	public abstract String getCountDataToUpdateQuery();

	public abstract String getDataToUpdateQuery();

	public abstract boolean useUpdateDateColumn();

	public SumarisTableMetadata(TableMetadata delegate,
								DatabaseMetaData meta,
								SingleTableEntityPersister entityPersister) {

		Preconditions.checkNotNull(delegate);
		this.delegate = delegate;
		this.entityPersister = entityPersister;

		try {
			Field field = TableMetadata.class.getDeclaredField("columns");
			field.setAccessible(true);
			this.columns = (Map) field.get(delegate);
			this.withUpdateDateColumn = columns.containsKey("update_date");
			this.pkNames = initPrimaryKeys(meta);
			Preconditions.checkNotNull(pkNames);
			this.pkIndexs = createPkIndex();
			this.notNullNames = initNotNull(this.columns);

			this.insertQuery = createInsertQuery();
			this.updateQuery = createUpdateQuery();
			this.maxUpdateDateQuery = String.format(QUERY_SELECT_MAX_UPDATE, getName());
			this.existingPrimaryKeysQuery = String.format(QUERY_SELECT_PRIMARY_KEYS, Joiner.on(',').join(pkNames), getName());
			this.countQuery = String.format(QUERY_SELECT_COUNT, getName());

			this.hqlSelectQuery = String.format(QUERY_HQL_SELECT, this.entityPersister.getEntityName());

		} catch (Exception e) {
			throw new RuntimeException("Could not init " + this, e);
		}
	}

	public Set<String> getPkNames() {
		return pkNames;
	}

	public Set<String> getNotNullNames() {
		return notNullNames;
	}

	public boolean isWithUpdateDateColumn() {
		return withUpdateDateColumn;
	}

	public int getColumnsCount() {
		return columns.size();
	}

	public SortedSet<String> getColumnNames() {
		return Sets.newTreeSet(columns.keySet());
	}

	public String getName() {
		return delegate.getName();
	}

	public ForeignKeyMetadata getForeignKeyMetadata(ForeignKey fk) {
		return delegate.getForeignKeyMetadata(fk);
	}

	public SumarisColumnMetadata getColumnMetadata(String columnName) {
		ColumnMetadata column = delegate.getColumnMetadata(columnName);
		if (column == null) {
			return null;
		}
		return new SumarisColumnMetadata(column, null);
	}

	public String getSchema() {
		return delegate.getSchema();
	}

	public String getCatalog() {
		return delegate.getCatalog();
	}

	public ForeignKeyMetadata getForeignKeyMetadata(String keyName) {
		return delegate.getForeignKeyMetadata(keyName);
	}

	public IndexMetadata getIndexMetadata(String indexName) {
		return delegate.getIndexMetadata(indexName);
	}

	public String getInsertQuery() {
		return insertQuery;
	}

	public String getInsertQuery(Set<String> columnNames) {
		return createInsertQuery(columnNames);
	}

	public String getInsertQuery(SumarisColumnMetadata[] columns) {
		Preconditions.checkNotNull(columns);
		LinkedHashSet<String> columnNames = Sets.newLinkedHashSetWithExpectedSize(columns.length + 1);

		// Retrieve identifier columns
		String[] pkColumnNames = this.entityPersister.getIdentifierColumnNames();
		columnNames.addAll(Arrays.asList(pkColumnNames));
		if (pkColumnNames.length > 1) {
			throw new DataIntegrityViolationException("Could not compute a sql insert query when more than one identifier, for table: "
					+ this.getName());
		}

		// Add given columns
		for (SumarisColumnMetadata column : columns) {
			// If column is not empty (column could be skip)
			if (column != null) {
				columnNames.add(column.getName());
			}
		}

		return createInsertQuery(columnNames);
	}

	public String getUpdateQuery() {
		return updateQuery;
	}

	public String getExistingPrimaryKeysQuery() {
		return existingPrimaryKeysQuery;
	}

	public String getMaxUpdateDateQuery() {
		return maxUpdateDateQuery;
	}

	public String getCountQuery() {
		return countQuery;
	}

	public int[] getPkIndexs() {
		return pkIndexs;
	}

	public String getSelectHQLQuery() {
		return hqlSelectQuery;
	}

	protected Set<String> initPrimaryKeys(DatabaseMetaData meta) throws SQLException {

		Set<String> result = Sets.newHashSet();
		ResultSet rs = meta.getPrimaryKeys(getCatalog(), getSchema(), getName());
		try {

			while (rs.next()) {
				result.add(rs.getString("COLUMN_NAME").toLowerCase());
			}
			rs.close();
			return ImmutableSet.copyOf(result);
		} finally {
			//closeSilency
		}
	}

	protected Set<String> initNotNull(Map<String, ColumnMetadata> columns) throws SQLException {
		Set<String> result = Sets.newHashSet();
		for (String columnName : columns.keySet()) {
			ColumnMetadata columnMetadata = columns.get(columnName);
			if ("NO".equals(columnMetadata.getNullable()) && !pkNames.contains(columnName)) {
				result.add(columnName);
			}
		}
		return ImmutableSet.copyOf(result);
	}

	protected int[] createPkIndex() {

		int[] result = new int[pkNames.size()];

		int pkI = 0;
		for (String pkName : pkNames) {
			String pkColumnName = pkName.toLowerCase();

			int i = 1;

			int index = -1;
			for (String columnName : getColumnNames()) {
				if (pkColumnName.equals(columnName)) {
					index = i;
				} else {
					i++;
				}
			}
			result[pkI++] = index;
		}

		return result;
	}

	protected String createInsertQuery() {
		return createInsertQuery(getColumnNames());
	}

	protected String createInsertQuery(Set<String> columnNames) {

		StringBuilder queryParams = new StringBuilder();
		StringBuilder valueParams = new StringBuilder();

		for (String columnName : columnNames) {
			queryParams.append(", ").append(columnName);
			valueParams.append(", ?");
		}

		String result = String.format(QUERY_INSERT,
										getName(),
										queryParams.substring(2),
										valueParams.substring(2));
		return result;
	}

	protected String createUpdateQuery() {

		StringBuilder updateParams = new StringBuilder();
		StringBuilder pkParams = new StringBuilder();

		for (String columnName : getColumnNames()) {
			updateParams.append(", ").append(columnName).append(" = ?");
		}

		for (String columnName : getPkNames()) {
			pkParams.append("AND ").append(columnName).append(" = ?");
		}

		String result = String.format(QUERY_UPDATE,
										getName(),
										updateParams.substring(2),
										pkParams.substring(4));
		return result;
	}

	public Serializable generateIdentifier(SessionImplementor session) {

		try {
			Class clazz = Class.forName(entityPersister.getName());
			Object entity = clazz.newInstance();
			Serializable id = entityPersister.getIdentifierGenerator().generate(session, entity);
			return id;
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String createDeleteQuery(String[] columnNames) {

		String whereClause = null;
		if (columnNames == null || columnNames.length == 0) {
			whereClause = "1==1";
		} else {
			StringBuilder whereClauseBuilder = new StringBuilder();
			for (String columnName : columnNames) {
				whereClauseBuilder.append("AND ").append(columnName).append(" = ?");
			}
			whereClause = whereClauseBuilder.substring(4);
		}

		String result = String.format(QUERY_DELETE,
										getName().toUpperCase(),
										whereClause);
		return result;
	}
}

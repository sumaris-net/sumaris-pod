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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sumaris.core.config.SumarisConfiguration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	protected static final String QUERY_SELECT_ALL = "SELECT %s FROM %s %s";

	protected static final String QUERY_SELECT_PRIMARY_KEYS = "SELECT %s FROM %s";

	protected static final String QUERY_SELECT_COUNT_ALL = "SELECT count(*) FROM %s %s";

	protected static final String QUERY_HQL_SELECT = "from %s";

	protected final String existingPrimaryKeysQuery;

	private final String maxUpdateDateQuery;

	protected final String countAllQuery;

	protected final Table delegate;

	protected final Map<String, SumarisColumnMetadata> columns;

	protected final Map<String, ForeignKey> foreignKeys;

	protected final Set<String> pkNames;

	protected final Set<String> notNullNames;

	protected final int[] pkIndexs;

	protected final String selectAllQuery;

	protected final String insertQuery;

	protected final String updateQuery;

	protected final String hqlSelectQuery;

	protected final boolean withUpdateDateColumn;

	protected PersistentClass persistentClass;

	protected final String sequenceName;

	protected final String sequenceNextValQuery;

	public abstract String getCountDataToUpdateQueryWithNull();

	public abstract String getDataToUpdateQueryWithNull();

	public abstract String getCountDataToUpdateQuery();

	public abstract String getDataToUpdateQuery();

	public abstract boolean useUpdateDateColumn();

	public SumarisTableMetadata(Table delegate,
								SumarisDatabaseMetadata dbMeta,
								DatabaseMetaData jdbcDbMeta,
								PersistentClass persistentClass) throws SQLException{
		Preconditions.checkNotNull(delegate);
		Preconditions.checkNotNull(dbMeta);
		Preconditions.checkNotNull(jdbcDbMeta);

		this.delegate = delegate;
		this.persistentClass = persistentClass;

		try {
			this.columns = initColumns(delegate, jdbcDbMeta);
			this.foreignKeys = initForeignKeys(delegate);
			this.withUpdateDateColumn = columns.containsKey("update_date");
			this.pkNames = initPrimaryKeys(delegate);
			Preconditions.checkNotNull(pkNames);
			this.pkIndexs = createPkIndex();
			this.notNullNames = initNotNull(this.columns);

			this.countAllQuery = createAllCountQuery();
			this.selectAllQuery = createSelectAllQuery();
			this.insertQuery = createInsertQuery();
			this.updateQuery = createUpdateQuery();
			this.maxUpdateDateQuery = String.format(QUERY_SELECT_MAX_UPDATE, getName());
			this.existingPrimaryKeysQuery = String.format(QUERY_SELECT_PRIMARY_KEYS, Joiner.on(',').join(pkNames), getName());

			this.hqlSelectQuery = persistentClass != null ? String.format(QUERY_HQL_SELECT, this.persistentClass.getEntityName()) : null;
			this.sequenceName = initSequenceName(dbMeta);
			this.sequenceNextValQuery = createSequenceNextValQuery(dbMeta.getDialect());

		} catch (Exception e) {
			throw new SQLException("Could not init metadata on table " + delegate.getName(), e);
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

	public ForeignKey getForeignKeyMetadata(Table.ForeignKeyKey fk) {
		return delegate.getForeignKeys().get(fk);
	}

	public SumarisColumnMetadata getColumnMetadata(String columnName) {

		return columns.get(columnName.toLowerCase());
	}

	public String getSchema() {
		return delegate.getSchema();
	}

	public String getCatalog() {
		return delegate.getCatalog();
	}

	public ForeignKey getForeignKeyMetadata(String keyName) {
		return foreignKeys.get(keyName);
	}

	public Index getIndexMetadata(String indexName) {
		return delegate.getIndex(indexName);
	}

	/**
	 * <p>Getter for the field <code>sequenceName</code>.</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String getSequenceName() {
		return sequenceName;
	}

	/**
	 * <p>Getter for the field <code>selectAllQuery</code>.</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	public String getSelectAllQuery() {
		return selectAllQuery;
	}

	/**
	 * <p>Getter for the field <code>insertQuery</code>.</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
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
		List<String> pkColumnNames = Lists.newArrayList();
		for(Iterator propertyIterator = this.delegate.getIdentifierValue().getColumnIterator();
			propertyIterator.hasNext(); ) {
			Column column = (Column) propertyIterator.next();
			pkColumnNames.add(column.getName());
		}

		columnNames.addAll(pkColumnNames);
		if (pkColumnNames.size() > 1) {
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

	public String getCountAllQuery() {
		return countAllQuery;
	}

	public int[] getPkIndexs() {
		return pkIndexs;
	}

	public String getSelectHQLQuery() {
		return hqlSelectQuery;
	}

	public String getSequenceNextValQuery() {
		return sequenceNextValQuery;
	}

	protected Set<String> initPrimaryKeys(Table delegate) {
		Map<String, Column> result = Maps.newLinkedHashMap();
		for(Iterator propertyIterator = delegate.getPrimaryKey().getColumnIterator();
			propertyIterator.hasNext(); ) {
			Column column = (Column) propertyIterator.next();
			result.put(column.getName(), column);
		}

		return result.keySet();
	}

//	protected Set<String> initPrimaryKeys(DatabaseMetaData meta) throws SQLException {
//
//		Set<String> result = Sets.newHashSet();
//		ResultSet rs = meta.getPrimaryKeys(getCatalog(), getSchema(), getName());
//		try {
//
//			while (rs.next()) {
//				result.add(rs.getString("COLUMN_NAME").toLowerCase());
//			}
//			rs.close();
//			return ImmutableSet.copyOf(result);
//		} finally {
//			//closeSilency
//		}
//	}

	protected Map<String, SumarisColumnMetadata> initColumns(Table delegate, DatabaseMetaData jdbcDbMeta) throws SQLException {
		Map<String, Column> columns = Maps.newLinkedHashMap();
		for(Iterator propertyIterator = delegate.getColumnIterator();
			propertyIterator.hasNext(); ) {
			Column column = (Column) propertyIterator.next();
			columns.put(column.getName().toLowerCase(), column);
		}

		Map<String, SumarisColumnMetadata> result = Maps.newLinkedHashMap();

		ResultSet rs = null;

		try {

			rs = jdbcDbMeta.getColumns(delegate.getCatalog(), delegate.getSchema(), delegate.getName().toUpperCase(), "%");

			while(rs.next()) {
				String columnName = rs.getString("COLUMN_NAME").toLowerCase();
				Column column = columns.get(columnName);
				if (column != null) {
					String defaultValue = SumarisConfiguration.getInstance().getColumnDefaultValue(getName(), columnName);
					Number position = rs.getInt("ORDINAL_POSITION");

					SumarisColumnMetadata columnMeta = new SumarisColumnMetadata(rs, column, null, defaultValue);
					result.put(columnName.toLowerCase(), columnMeta);
				}
			}


		}
		finally {
			if (rs != null) {
				rs.close();
			}

		}
		if (result.size() == 0) {
			throw new RuntimeException("Unable to load columns metadata on table " + delegate.getName());
		}

		return result;
	}

	protected Map<String, ForeignKey> initForeignKeys(Table delegate) {
		Map<String, ForeignKey> result = Maps.newLinkedHashMap();
		for(Iterator foreignKeyIterator = delegate.getForeignKeyIterator();
			foreignKeyIterator.hasNext(); ) {
			ForeignKey foreignKey = (ForeignKey) foreignKeyIterator.next();

			result.put(foreignKey.getName().toLowerCase(), foreignKey);
		}

		return result;
	}


	protected Set<String> initNotNull(Map<String, SumarisColumnMetadata> columns) {
		Set<String> result = Sets.newHashSet();
		for (String columnName : columns.keySet()) {
			SumarisColumnMetadata column = columns.get(columnName);
			if (!column.isNullable() && !pkNames.contains(columnName)) {
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

	/**
	 * <p>createAllCountQuery.</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	protected String createAllCountQuery() {
		String tableAlias = "t";
		return String.format(QUERY_SELECT_COUNT_ALL, getName(), tableAlias);
	}

	/**
	 * <p>createSelectAllQuery.</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	protected String createSelectAllQuery() {
		String tableAlias = "t";
		return String.format(QUERY_SELECT_ALL,
				createSelectParams(tableAlias),
				getName(),
				tableAlias);
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

	/**
	 * <p>createSelectParams.</p>
	 *
	 * @param tableAlias a {@link java.lang.String} object.
	 * @return a {@link java.lang.String} object.
	 */
	protected String createSelectParams(String tableAlias) {
		return createSelectParams(getColumnNames(), tableAlias);
	}

	/**
	 * <p>createSelectParams.</p>
	 *
	 * @param columnNames a {@link java.util.List} object.
	 * @param tableAlias a {@link java.lang.String} object.
	 * @return a {@link java.lang.String} object.
	 */
	protected String createSelectParams(Collection<String> columnNames,
										String tableAlias) {
		Preconditions.checkArgument(
				CollectionUtils.isNotEmpty(columnNames),
				String.format("No column found for table: %s",
						getName()));

		StringBuilder queryParams = new StringBuilder();

		for (String columnName : columnNames) {
			queryParams.append(", ");
			if (tableAlias != null) {
				queryParams.append(tableAlias).append(".");
			}
			queryParams.append(columnName);
		}

		return queryParams.substring(2);
	}

	/**
	 * <p>initSequenceName.</p>
	 *
	 * @param dbMeta a {@link fr.ifremer.common.synchro.meta.SynchroDatabaseMetadata} object.
	 * @return a {@link java.lang.String} object.
	 */
	protected String initSequenceName(SumarisDatabaseMetadata dbMeta) {
		final Set<String> availableSequences = dbMeta.getSequences();
		final String sequenceSuffix = dbMeta.getSequenceSuffix();
		final int maxSqlNameLength = dbMeta.getDialect().getMaxAliasLength();

		final String tableName = getName().toLowerCase();
		String sequenceName;

		// Compute the max size of
		final int maxLength = maxSqlNameLength - sequenceSuffix.length();
		if (maxLength > -0) {
			sequenceName = (SumarisMetadataUtils.ensureMaximumNameLength(
					tableName, maxLength) + sequenceSuffix).toLowerCase();
			if (availableSequences.contains(sequenceName)) {
				return sequenceName;
			}
		}

		// If not found (with length limit), try without length limit
		sequenceName = (tableName + sequenceSuffix).toLowerCase();
		if (availableSequences.contains(sequenceName)) {
			return sequenceName;
		}

		// sequence not found
		return null;
	}

	/**
	 * <p>createSequenceNextValQuery.</p>
	 *
	 * @param dialect a {@link org.hibernate.dialect.Dialect} object.
	 * @return a {@link java.lang.String} object.
	 */
	protected String createSequenceNextValQuery(Dialect dialect) {
		if (StringUtils.isBlank(sequenceName)) {
			return null;
		}
		return dialect.getSequenceNextValString(sequenceName);
	}
}

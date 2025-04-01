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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Metadata on a database table. Useful to request a table:
 * <p/>
 * <ul>
 * <li>Obtains query to update a row of the table (column names order is the one introduced by method {@link #getColumnNames()}: {@link #getUpdateQuery()}</li>
 * <li>Obtains query to insert a row in the table (column names order is the one introduced by method {@link #getColumnNames()}: {@link #getInsertQuery()}</li>
 * </ul>
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class SumarisTableMetadata {
	
	public static final String DEFAULT_TABLE_ALIAS = "t"; 

	protected static final String QUERY_INSERT = "INSERT INTO %s (%s) VALUES (%s)";
	protected static final String QUERY_UPDATE = "UPDATE %s SET %s WHERE %s";
	protected static final String QUERY_DELETE = "DELETE FROM %s %s WHERE %s";
	protected static final String QUERY_SELECT_ALL = "SELECT %s FROM %s %s";
	protected static final String QUERY_SELECT_PRIMARY_KEYS = "SELECT %s FROM %s";
	protected static final String QUERY_SELECT_COUNT_ALL = "SELECT count(*) FROM %s %s";
	protected static final String QUERY_SELECT_MAX = "SELECT max(%s) FROM %s";
	protected static final String QUERY_HQL_SELECT = "from %s";
	protected static final Set<String> COLUMN_NAMES_TO_ESCAPE = AnsiSqlKeywords.INSTANCE.sql2003().stream().map(String::toLowerCase).collect(Collectors.toSet());

	protected final SumarisDatabaseMetadata dbMeta;
	protected final QualifiedTableName tableName;
	protected final String tableAlias;

	protected String existingPrimaryKeysQuery;
	protected String maxUpdateDateQuery;
	protected String countAllQuery;
	protected Map<String, SumarisColumnMetadata> columns;
	protected Set<String> pkNames;
	protected Set<String> notNullNames;
	protected int[] pkIndexs;
	protected String selectAllQuery;
	protected String insertQuery;
	protected String updateQuery;
	protected boolean withUpdateDateColumn;
	protected String sequenceName;
	protected String sequenceNextValQuery;
	protected String countDataToUpdateQuery;
	protected String dataToUpdateQuery;

	protected SumarisTableMetadata(QualifiedTableName tableName,
								   SumarisDatabaseMetadata dbMeta,
								   DatabaseMetaData jdbcDbMeta) throws SQLException{
		Preconditions.checkNotNull(tableName);
		Preconditions.checkNotNull(dbMeta);
		Preconditions.checkNotNull(jdbcDbMeta);

		this.dbMeta = dbMeta;
		this.tableAlias = DEFAULT_TABLE_ALIAS;
		this.tableName = tableName;

		init(dbMeta, jdbcDbMeta);
	}

	protected void init(SumarisDatabaseMetadata dbMeta, DatabaseMetaData jdbcDbMeta) throws SQLException {
		try {
			// Retrieve some data on the table
			this.columns = initColumns(this.tableName, jdbcDbMeta);
			this.withUpdateDateColumn = this.columns.containsKey(dbMeta.getDefaultUpdateDateColumnName().toLowerCase());
			this.pkNames = initPrimaryKeys(jdbcDbMeta);
			Preconditions.checkNotNull(pkNames);
			this.pkIndexs = createPkIndex();
			this.notNullNames = initNotNull(this.columns);

			// Create basic SQL queries (select/insert/update)
			this.countAllQuery = createAllCountQuery();
			this.selectAllQuery = createSelectAllQuery();
			this.insertQuery = createInsertQuery();
			this.updateQuery = createUpdateQuery();
			this.existingPrimaryKeysQuery = String.format(QUERY_SELECT_PRIMARY_KEYS, Joiner.on(',').join(pkNames), getName());

			// Create SQL queries using the update date column (if exists on table)
			this.dataToUpdateQuery = createSelectAllToUpdateQuery(dbMeta);
			this.countDataToUpdateQuery = createCountDataToUpdateQuery(dbMeta);
			this.maxUpdateDateQuery = String.format(QUERY_SELECT_MAX, dbMeta.getDefaultUpdateDateColumnName(), getName());

			// Retrieve how to generate an identifier
			this.sequenceName = initSequenceName(dbMeta);
			this.sequenceNextValQuery = createSequenceNextValQuery(dbMeta.getDialect());

		} catch (Exception e) {
			throw new SQLException("Could not init metadata on table " + getName(), e);
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

	public Set<String> getColumnNames() {
		return columns.keySet();
	}

	public List<String> getEscapedColumnNames() {
		return getEscapedColumnNames(getColumnNames());
	}

	public List<String> getEscapedColumnNames(Collection<String> columnNames) {
		return columnNames.stream().map(this::getColumnMetadata).map(SumarisColumnMetadata::getEscapedName).toList();
	}

	public boolean isColumnNameToEscape(String columnName) {
		return COLUMN_NAMES_TO_ESCAPE.contains(columnName.toLowerCase());
	}

	public String getEscapedColumnName(String columnName) {
		if (dbMeta.getDialect() instanceof Oracle10gDialect) {
			columnName = columnName.toUpperCase();
		}
		if (isColumnNameToEscape(columnName)) {
			columnName = dbMeta.getDialect().openQuote() + columnName + dbMeta.getDialect().closeQuote();
		}
		return columnName;
	}

	public SumarisDatabaseMetadata getDbMeta() {
		return dbMeta;
	}

	public String getAlias() {
		return tableAlias;
	}

	public String getName() {
		return tableName.getTableName().getText();
	}

	public SumarisColumnMetadata getColumnMetadata(String columnName) {
		return columns.get(columnName.toLowerCase());
	}

	public boolean hasColumn(String columnName) {
		return columns.containsKey(columnName.toLowerCase());
	}

	public String getSchema() {
		return tableName.getSchemaName() != null ? tableName.getSchemaName().getText() : null;
	}

	public String getCatalog() {
		return tableName.getCatalogName() != null ? tableName.getCatalogName().getText() : null;
	}

	/**
	 * <p>Getter for the field <code>sequenceName</code>.</p>
	 *
	 * @return a {@link String} object.
	 */
	public String getSequenceName() {
		return sequenceName;
	}

	/**
	 * <p>Getter for the field <code>selectAllQuery</code>.</p>
	 *
	 * @return a {@link String} object.
	 */
	public String getSelectAllQuery() {
		return selectAllQuery;
	}

	public String getSelectQuery(Collection<String> columnNames,
								 String whereClause) {
		return getSelectQuery(false, columnNames, whereClause, null, null);
	}

	public String getSelectQuery(boolean distinct,
								 Collection<String> columnNames,
								 String whereClause,
								 String sort,
								 SortDirection direction) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(QUERY_SELECT_ALL,
				(distinct ? "DISTINCT " : "") + createSelectParams(columnNames, tableAlias),
				tableName.render(),
				tableAlias));

		// Where clause
		if (StringUtils.isNotBlank(whereClause)) {
			sb.append(" ").append(whereClause);
		}

		// Add order by
		if (StringUtils.isNotBlank(sort)) {
			String sortColumn = getColumnMetadata(sort).getEscapedName();
			sb.append(" ORDER BY ")
					.append(String.format("%s %s", SumarisMetadataUtils.getAliasedColumnName(tableAlias, sortColumn), (direction != null ? direction.name() : "")));
		}

		return sb.toString();
	}




	/**
	 * <p>Getter for the field <code>insertQuery</code>.</p>
	 *
	 * @return a {@link String} object.
	 */
	public String getInsertQuery() {
		return insertQuery;
	}

	public String getInsertQuery(Set<String> columnNames) {
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

	public String getSequenceNextValQuery() {
		return sequenceNextValQuery;
	}

	public String getDataToUpdateQuery() {
		return dataToUpdateQuery;
	}

	public String getCountDataToUpdateQuery() {
		return countDataToUpdateQuery;
	}

	public String getInsertQuery(SumarisColumnMetadata[] columns) {
		Preconditions.checkNotNull(columns);
		LinkedHashSet<String> columnNames = Sets.newLinkedHashSetWithExpectedSize(columns.length + 1);

		Set<String> pkColumnNames = getPkNames();
		if (pkColumnNames.size() > 1) {
			throw new DataIntegrityViolationException("Could not compute a sql insert query when more than one identifier, for table: "
					+ this.getName());
		}
		columnNames.addAll(pkColumnNames);

		// Add given columns
		for (SumarisColumnMetadata column : columns) {
			// If column is not empty (column could be skip)
			if (column != null) {
				columnNames.add(column.getName());
			}
		}

		return createInsertQuery(columnNames);
	}

	public String getDeleteQuery(String[] columnNames) {

		String whereClause = null;
		if (columnNames != null && columnNames.length > 0) {
			StringBuilder whereClauseBuilder = new StringBuilder();
			for (String columnName : getEscapedColumnNames(List.of(columnNames))) {
				whereClauseBuilder
						.append("AND ")
						.append(SumarisMetadataUtils.getAliasedColumnName(tableAlias, columnName))
						.append(" = ?");
			}
			whereClause = whereClauseBuilder.substring(4);
		}

		return getDeleteQuery(whereClause);
	}

	public String getDeleteQuery() {
		return getDeleteQuery((String)null);
	}

	public String getDeleteQuery(String whereClauseContent) {
		String result = String.format(QUERY_DELETE,
				tableName.render(),
				tableAlias,
				whereClauseContent == null ? "1=1" : whereClauseContent);
		return result;
	}

	/* -- protected methods -- */

	protected Set<String> initPrimaryKeys(DatabaseMetaData jdbcDbMeta) throws SQLException {

		Set<String> result = Sets.newHashSet();
		ResultSet rs = jdbcDbMeta.getPrimaryKeys(getCatalog(), getSchema(), getName());
		try {
			while (rs.next()) {
				result.add(rs.getString("COLUMN_NAME").toLowerCase());
			}
			rs.close();
			return ImmutableSet.copyOf(result);
		} finally {
			if (rs != null) {
				rs.close();
			}
		}
	}


	protected Map<String, SumarisColumnMetadata> initColumns(QualifiedTableName tableName, DatabaseMetaData jdbcDbMeta) throws SQLException {

		Map<String, SumarisColumnMetadata> result = Maps.newLinkedHashMap();

		ResultSet rs = jdbcDbMeta.getColumns(getCatalog(), getSchema(), getName(), "%");

		try {
			while (rs.next()) {
				String columnName = rs.getString("COLUMN_NAME").toLowerCase();
				String defaultValue = SumarisConfiguration.getInstance().getColumnDefaultValue(getName(), columnName);

				// Create column meta
				SumarisColumnMetadata columnMeta = new SumarisColumnMetadata(this, rs, defaultValue);
				result.put(columnName.toLowerCase(), columnMeta);
			}
		}
		finally {
			if (rs != null) {
				rs.close();
			}
		}
		if (result.size() == 0) {
			throw new RuntimeException("Unable to load columns metadata on table " + getName());
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
	 * @return a {@link String} object.
	 */
	protected String createAllCountQuery() {
		return String.format(QUERY_SELECT_COUNT_ALL, getName(), tableAlias);
	}

	/**
	 * <p>createSelectAllQuery.</p>
	 *
	 * @return a {@link String} object.
	 */
	protected String createSelectAllQuery() {
		return String.format(QUERY_SELECT_ALL,
				createSelectParams(tableAlias),
				tableName.render(),
				tableAlias);
	}

	protected String createInsertQuery() {
		return createInsertQuery(getColumnNames());
	}

	protected String createInsertQuery(Set<String> columnNames) {

		StringBuilder queryParams = new StringBuilder();
		StringBuilder valueParams = new StringBuilder();

		for (String columnName : getEscapedColumnNames(columnNames)) {
			queryParams.append(", ").append(columnName);
			valueParams.append(", ?");
		}

		String result = String.format(QUERY_INSERT,
				tableName.render(),
				queryParams.substring(2),
				valueParams.substring(2));
		return result;
	}

	protected String createUpdateQuery() {

		// Skip if no PK (if no pk - e.g. extraction tables)
		if (CollectionUtils.isEmpty(getPkNames())) return null;

		StringBuilder updateParams = new StringBuilder();
		StringBuilder pkParams = new StringBuilder();

		for (String columnName : getEscapedColumnNames()) {
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

	/**
	 * <p>createSelectParams.</p>
	 *
	 * @param tableAlias a {@link String} object.
	 * @return a {@link String} object.
	 */
	protected String createSelectParams(String tableAlias) {
		return createSelectParams(getColumnNames(), tableAlias);
	}

	/**
	 * <p>createSelectParams.</p>
	 *
	 * @param columnNames a {@link java.util.List} object.
	 * @param tableAlias a {@link String} object.
	 * @return a {@link String} object.
	 */
	protected String createSelectParams(Collection<String> columnNames,
										String tableAlias) {
		Preconditions.checkArgument(
				CollectionUtils.isNotEmpty(columnNames),
				String.format("No column found for table: %s",
						getName()));

		return String.join(", ", SumarisMetadataUtils.getAliasedColumns(tableAlias, getEscapedColumnNames(columnNames)));
	}

	/**
	 * <p>initSequenceName.</p>
	 *
	 * @param dbMeta a {@link SumarisDatabaseMetadata} object.
	 * @return a {@link String} object.
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
	 * @param dialect a {@link Dialect} object.
	 * @return a {@link String} object.
	 */
	protected String createSequenceNextValQuery(Dialect dialect) {
		if (StringUtils.isBlank(sequenceName)) {
			return null;
		}
		return dialect.getSequenceNextValString(sequenceName);
	}

	protected String createSelectAllToUpdateQuery(SumarisDatabaseMetadata dbMeta) {
		String query = String.format(QUERY_SELECT_ALL,
				createSelectParams(tableAlias),
				tableName.render(),
				tableAlias);

		// add a tripFilter on update date column
		if (isWithUpdateDateColumn()) {

			String updateDateColumn = SumarisMetadataUtils.getAliasedColumnName(tableAlias, dbMeta.getDefaultUpdateDateColumnName());
			query += String.format(" WHERE (%s IS NULL OR %s > ?)", updateDateColumn, updateDateColumn);
		}
		return query;
	}

	protected String createCountDataToUpdateQuery(SumarisDatabaseMetadata dbMeta) {
		String query = String.format(QUERY_SELECT_COUNT_ALL,
				tableName.render(),
				tableAlias);

		// add a tripFilter on update date column
		if (isWithUpdateDateColumn()) {

			String updateDateColumn = SumarisMetadataUtils.getAliasedColumnName(tableAlias, dbMeta.getDefaultUpdateDateColumnName());
			query += String.format(" WHERE (%s IS NULL OR %s > ?)", updateDateColumn, updateDateColumn);
		}
		return query;
	}

}

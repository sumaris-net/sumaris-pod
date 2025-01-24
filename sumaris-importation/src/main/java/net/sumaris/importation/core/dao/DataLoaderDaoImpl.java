/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.importation.core.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.importation.core.service.vo.DataLoadError;
import net.sumaris.importation.core.service.vo.DataLoadResult;
import net.sumaris.importation.core.util.csv.FileMessageFormatter;
import net.sumaris.importation.core.util.csv.FileReader;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.type.BooleanType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static net.sumaris.importation.core.service.vo.DataLoadError.ErrorType;

@Repository("dataLoaderDao")
@Slf4j
public class DataLoaderDaoImpl extends HibernateDaoSupport implements DataLoaderDao {

	// Additional log
	private static final Logger hibernateLog = LoggerFactory.getLogger("org.hibernate.SQL");

	public final static int MAX_LOG_ERRORS = 500;
	private final static int BATCH_ROW_COUNT = 10000;

	private final static Integer NULL_VALUE = -1;

	protected static NumberFormat numberFormat = NumberFormat.getInstance();
	protected static char decimalSeparator = '\0';
	protected static char inverseDecimalSeparator = ',';
	static {
		DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
		decimalSeparator = decimalFormatSymbols.getDecimalSeparator();
		if (decimalSeparator == ',') {
			inverseDecimalSeparator = '.';
		}
	}

	@Autowired
	protected DataSource dataSource;

	@Autowired
	protected SumarisDatabaseMetadata sumarisDatabaseMetadata;

	@Autowired
	protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	protected SQLErrorCodeSQLExceptionTranslator sqlExceptionTranslator;

	@Autowired
	protected DataSource datasource;

	private final boolean showSql;

	@Autowired
	public DataLoaderDaoImpl(EntityManager entityManager, DataSource dataSource) {
		super();
		setEntityManager(entityManager);
		this.dataSource = dataSource;
		this.sqlExceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
		this.showSql = hibernateLog.isDebugEnabled();
	}


	@Override
	public DataLoadError[] validate(FileReader reader, DatabaseTableEnum table) throws IOException {

		DataLoadResult result = new DataLoadResult();
		validate(reader, table, result);
		return result.getErrors();
	}

	@Override
	public DataLoadError[] load(FileReader fileReader, DatabaseTableEnum table) throws IOException {
		return load(fileReader, table, false);
	}

	public DataLoadError[] load(FileReader reader, DatabaseTableEnum table, boolean validate) throws IOException {
		Preconditions.checkNotNull(reader);
		DataLoadResult result = new DataLoadResult();

		// Validate file, if need
		if (validate) {
			validate(reader, table, result);

			if (!result.isSuccess()) return result.getErrors();
		}

		SumarisTableMetadata tableMetadata = sumarisDatabaseMetadata.getTable(table.name());

		// Import file :
		log.info(FileMessageFormatter.format(tableMetadata, null, reader.getCurrentLine(), "Importing file: " + reader.getFileName()));


		try {
			// Read column headers :
			String[] headers = reader.getHeaders();
			SumarisColumnMetadata[] headerColumns = getColumnMetadataFromHeaders(reader, result, tableMetadata, headers);

			if (headerColumns == null) {
				return result.getErrors();
			}

			Connection conn = DataSourceUtils.getConnection(dataSource);
			boolean isTransactional = DataSourceUtils.isConnectionTransactional(conn, dataSource);

			try {
				String insertQuery = tableMetadata.getInsertQuery(headerColumns);
				PreparedStatement insertStatement = conn.prepareStatement(insertQuery);

				String[] cols = null;
				int insertCount = 0;
				boolean rowHasErrors = false;
				while ((cols = reader.readNext()) != null) {
					int parameterIndex = 1;

					// Generate a new id (only if the previous row was not skipped)
					if (!rowHasErrors) {
						Serializable id = sumarisDatabaseMetadata.generateIdentifier(conn, tableMetadata);
						insertStatement.setObject(parameterIndex++, id);
					} else {
						parameterIndex++;
					}
					if (log.isTraceEnabled()) {
						log.trace(FileMessageFormatter.format(tableMetadata, null, reader.getCurrentLine(), "Importing line with values:"));
					}
					rowHasErrors = false;
					int colIndex = 0;
					for (SumarisColumnMetadata columnMetadata : headerColumns) {
						// If column is not skipped
						if (columnMetadata != null) {
							String cellValue = null;
							if (colIndex < cols.length) {
								cellValue = cols[colIndex];
								if (log.isTraceEnabled()) {
									log.trace("\t" + columnMetadata.getName() + "=" + cellValue);
								}
							} else {
								cellValue = columnMetadata.getDefaultValue();
							}
							boolean parameterOK = setParameterValue(parameterIndex++, insertStatement, reader, tableMetadata, columnMetadata, colIndex,
									cellValue, result);
							rowHasErrors = rowHasErrors || !parameterOK;
						}
						colIndex++;
					}

					if (rowHasErrors) {
						if (log.isDebugEnabled()) {
							log.debug( FileMessageFormatter.format(tableMetadata, null,  reader.getCurrentLine(), " Errors found -> Line skipped"));
						}
					} else {
						if (showSql) {
							hibernateLog.debug(insertQuery);
						}
						insertStatement.addBatch();
						insertCount++;

						if (insertCount > 0 && insertCount % BATCH_ROW_COUNT == 0) {
							log.debug(FileMessageFormatter.format(tableMetadata, null,  reader.getCurrentLine(), "read " + insertCount + " lines..."));
							insertStatement.executeBatch();
						}
					}
				}
				if (insertCount > 0 && insertCount % BATCH_ROW_COUNT != 0) {
					insertStatement.executeBatch();
				}
				if (log.isInfoEnabled()) {
					log.info(FileMessageFormatter.format(tableMetadata, null, reader.getCurrentLine(), "INSERT count: " + insertCount));
				}

				if (!isTransactional) conn.commit();

			} catch (BatchUpdateException bue) {
				if (!isTransactional) conn.rollback();
				bue.getNextException().printStackTrace();
				throw bue;
			} finally {
				if (!isTransactional) DataSourceUtils.releaseConnection(conn, dataSource);
				reader.close();
			}

			return result.getErrors();
		} catch (SQLException e) {
			log.error(FileMessageFormatter.format(tableMetadata, null, reader.getCurrentLine(), "Error during file importation: " + reader.getFileName()), e);
			throw sqlExceptionTranslator.translate("Importing file: " + reader.getFileName(), null, e);
		}
	}


	/* -- protected methods -- */

	protected void validate(FileReader reader, DatabaseTableEnum table, DataLoadResult result ) throws IOException {

		SumarisTableMetadata tableMetadata = sumarisDatabaseMetadata.getTable(table.name());

		if (log.isInfoEnabled()) {
			log.info(FileMessageFormatter.format(tableMetadata, null,  reader.getCurrentLine(), "Starting file validation... " + reader.getFileName()));
		}

		// Read column headers :
		String[] headers = reader.getHeaders();
		SumarisColumnMetadata[] mappedColumns = getColumnMetadataFromHeaders(reader, result, tableMetadata, headers);

		if (!result.isSuccess() || mappedColumns == null) {
			return;
		}

		// Read rows
		String[] cols;
		while ((cols = reader.readNext()) != null) {
			int colIndex = 0;

			for (String cellValue : cols) {
				SumarisColumnMetadata columnMetadata = mappedColumns[colIndex++];
				validateColumnValue(reader, result, tableMetadata, columnMetadata, colIndex, cellValue);
			}
		}
	}


	private boolean setParameterValue(int parameterIndex, PreparedStatement insertStatement, FileReader reader,
                                      SumarisTableMetadata tableMetadata,
                                      SumarisColumnMetadata columnMetadata, int columnNumber, String cellValue,
                                      DataLoadResult result) throws SQLException {
		int sqlType = columnMetadata.getTypeCode();
		try {

			if (StringUtils.isBlank(cellValue)
					|| "NULL".equals(cellValue)
					|| (sqlType == Types.NUMERIC && "na".equalsIgnoreCase(cellValue))
					|| (sqlType == Types.NUMERIC && "n/a".equalsIgnoreCase(cellValue))) {
				if (StringUtils.isNotBlank(columnMetadata.getDefaultValue())) {
					cellValue = columnMetadata.getDefaultValue();
				} else {
					// TODO BLA: manage this - ask IRL with NULL ?
					insertStatement.setNull(parameterIndex, sqlType);

					// Mandatory
					if (!columnMetadata.isNullable()) {

						addError(reader, result,
								tableMetadata, columnMetadata,
								columnNumber,
								ErrorType.ERROR,
								"NULL_VALUE",
								I18n.t("import.validation.error.NULL_VALUE",
										columnMetadata.getName() ));
						return false;
					}

					return true;
				}
			}

			// Remove spaces in numerical values
			if ((sqlType == Types.NUMERIC
					|| sqlType == Types.BIGINT
					|| sqlType == Types.INTEGER
					|| sqlType == Types.FLOAT
					|| sqlType == Types.REAL)
					&& cellValue.indexOf(' ') != -1) {
				cellValue = cellValue.replaceAll(" ", "");
			}

			// Special case a boolean value (1:true, 0:false)
			if (sqlType == BooleanType.INSTANCE.sqlType()) {
				if ("yes".equalsIgnoreCase(cellValue) || "true".equalsIgnoreCase(cellValue)) {
					cellValue = "1";
				} else if ("no".equalsIgnoreCase(cellValue) || "false".equalsIgnoreCase(cellValue)) {
					cellValue = "0";
				}
			}

			// Length
			int columnSize = getColumnSize(tableMetadata, columnMetadata);
			int decimalDigits = columnMetadata.getDecimalDigits();
			if (columnSize > 0 && cellValue.trim().length() > columnSize && (sqlType != Types.NUMERIC || decimalDigits == 0)) {
				addError(reader, result, tableMetadata, columnMetadata,
						columnNumber,
						ErrorType.ERROR,
						"TOO_LONG_VALUE",
						I18n.t("import.validation.error.TOO_LONG_VALUE",
								cellValue, columnMetadata.getName(), columnSize ));
				insertStatement.setNull(parameterIndex, sqlType);
				return false;
			}
			if (sqlType == Types.NUMERIC) {
				cellValue = StringUtils.deleteWhitespace(cellValue);
				if (decimalDigits == 0) {
					long value = Long.parseLong(cellValue);
					insertStatement.setLong(parameterIndex, value);
				} else {
					if (cellValue.indexOf(inverseDecimalSeparator) != -1) {
						cellValue = cellValue.replace(inverseDecimalSeparator, decimalSeparator);
					}
					// If value is too long (whole length)
					if (columnSize > 0 && cellValue.length() > columnSize - 1) {
						int integerLength = columnSize - decimalDigits;
						if (cellValue.indexOf(decimalSeparator) > integerLength) {
							addError(reader, result, tableMetadata, columnMetadata,
									columnNumber,
									ErrorType.ERROR,
									"TOO_LONG_VALUE_WITH_SCALE",
									I18n.t("import.validation.error.TOO_LONG_VALUE_WITH_SCALE",
											cellValue, columnMetadata.getName(), columnSize, decimalDigits ));
							insertStatement.setNull(parameterIndex, sqlType);
							return false;
						} else {
							addErrorOnce(reader, result, tableMetadata, columnMetadata,
									columnNumber,
									ErrorType.WARNING,
									"ROUND_VALUES",
									I18n.t("import.validation.error.ROUND_VALUES",
											columnMetadata.getName(), decimalDigits ));
						}
					}
					Number value = numberFormat.parse(cellValue);
					//DecimalFormat.getInstance().parse(cellValue);
					insertStatement.setObject(parameterIndex, value, sqlType);
				}
				return true;
			}

			// Double
			if (sqlType == DoubleType.INSTANCE.sqlType()) {
				if (cellValue.indexOf(',') != -1) {
					cellValue = cellValue.replace(',', '.');
				}
				double value = Double.parseDouble(cellValue);
				insertStatement.setDouble(parameterIndex, value);
				return true;
			}

			// Integer
			if (sqlType == IntegerType.INSTANCE.sqlType()) {
				int value = Integer.parseInt(cellValue);
				insertStatement.setInt(parameterIndex, value);
				return true;
			}

			// Decimal
			if (sqlType == Types.DECIMAL) {
				//int value = DecimalFormat.parseInt(cellValue);
				//insertStatement.setInt(parameterIndex, value);
				return true;
			}

			// Float
			if (sqlType == FloatType.INSTANCE.sqlType()) {
				if (cellValue.indexOf(',') != -1) {
					cellValue = cellValue.replace(',', '.');
				}
				float value = Float.parseFloat(cellValue);
				insertStatement.setFloat(parameterIndex, value);
				return true;
			}

			// Boolean
			if (sqlType == BooleanType.INSTANCE.sqlType()) {
				boolean value = false;
				if ("yes".equalsIgnoreCase(cellValue)
						|| "true".equalsIgnoreCase(cellValue)
						|| "1".equalsIgnoreCase(cellValue)) {
					value = true;
				}
				insertStatement.setBoolean(parameterIndex, value);
				return true;
			}

			insertStatement.setObject(parameterIndex++, cellValue);
			return true;
		} catch (SQLDataException sqlde) {
			addError(reader, result, tableMetadata, columnMetadata,
					columnNumber,
					ErrorType.ERROR,
					"SQL_DATA_EXCEPTION",
					I18n.t("import.validation.error.SQL_DATA_EXCEPTION",
							columnMetadata.getName(), cellValue, sqlde.getMessage(), sqlde.getNextException().getMessage()));
			insertStatement.setNull(parameterIndex, sqlType);
			return false;
		} catch (SQLException sqle) {
			addError(reader, result, tableMetadata, columnMetadata,
					columnNumber,
					ErrorType.ERROR,
					"SQL_EXCEPTION",
					I18n.t("import.validation.error.SQL_EXCEPTION",
							columnMetadata.getName(), cellValue, sqle.getMessage() ));
			insertStatement.setNull(parameterIndex, sqlType);
			return false;
		}
		catch (ParseException | NumberFormatException pe) {
			addError(reader, result, tableMetadata, columnMetadata,
					columnNumber,
					ErrorType.ERROR,
					"SQL_EXCEPTION",
					I18n.t("import.validation.error.PARSE_EXCEPTION",
							columnMetadata.getName(), cellValue, pe.getMessage() ));
			insertStatement.setNull(parameterIndex, sqlType);
			return false;
		}
	}

	protected SumarisColumnMetadata[] getColumnMetadataFromHeaders(FileReader reader,
                                                                         DataLoadResult result,
                                                                         SumarisTableMetadata tableMetadata,
                                                                         String[] headers) {
		if (headers == null || headers.length == 0) {

			addError(reader, result, tableMetadata, null,
					-1,
					ErrorType.FATAL,
					"NO_HEADER",
					I18n.t("import.validation.error.NO_HEADER", null, null));
			return null;
		}

		Set<String> notNullColumns = new HashSet<String>();
		notNullColumns.addAll(tableMetadata.getNotNullNames());

		int colIndex = 0;
		boolean hasBadHeaders = false;
		List<SumarisColumnMetadata> mappedColumns = Lists.newLinkedList();
		for (String columnName : headers) {
			SumarisColumnMetadata columnMetadata = tableMetadata.getColumnMetadata(columnName);
			if (columnMetadata != null) {
				mappedColumns.add(columnMetadata);
				notNullColumns.remove(columnName.toLowerCase());
			} else {
				// Insert null, to have the good index in array
				mappedColumns.add(null);
				if (columnName.trim().isEmpty()) {
					addError(reader, result, tableMetadata, null,
							-1,
							ErrorType.WARNING,
							"EMPTY_COLUMN_NAME",
							I18n.t("import.validation.error.EMPTY_COLUMN_NAME",
									colIndex, tableMetadata.getColumnNames().toString() ));
				} else {
					addError(reader, result, tableMetadata, null,
							-1,
							ErrorType.WARNING,
							"UNKNOWN_COLUMN_NAME",
							I18n.t("import.validation.error.UNKNOWN_COLUMN_NAME",
									columnName, tableMetadata.getColumnNames().toString() ));
				}
			}
		}

		// Check if mandatory col are presents
		if (!hasBadHeaders && notNullColumns.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (String notNullColumn : notNullColumns) {
				SumarisColumnMetadata columnMetadata = tableMetadata.getColumnMetadata(notNullColumn);
				// If a default value is present, add as header (Insertion will use default value)
				if (StringUtils.isNotEmpty(columnMetadata.getDefaultValue())) {
					mappedColumns.add(columnMetadata);
				} else {
					sb.append(", ").append(notNullColumn.toUpperCase());
					hasBadHeaders = true;
				}
			}
			if (hasBadHeaders) {
				addError(reader, result, tableMetadata, null,
						-1,
						ErrorType.FATAL,
						"MISSING_COLUMN",
						I18n.t("import.validation.error.MISSING_COLUMN",
								sb.substring(2) ));
			}
		}

		if (hasBadHeaders) {
			return null;
		}

		return mappedColumns.toArray(new SumarisColumnMetadata[mappedColumns.size()]);
	}

	protected Object validateColumnValue(FileReader reader, DataLoadResult result, SumarisTableMetadata tableMetadata, SumarisColumnMetadata columnMetadata,
										 int columnNumber, String cellValue) {
		boolean hasError = false;

		// Skipped column
		if (columnMetadata == null) {
			return NULL_VALUE;
		}

		int sqlType = columnMetadata.getTypeCode();
		if (StringUtils.isBlank(cellValue)
				|| "NULL".equals(cellValue)
				|| (sqlType == Types.NUMERIC && "na".equalsIgnoreCase(cellValue))
				|| (sqlType == Types.NUMERIC && "n/a".equalsIgnoreCase(cellValue))) {
			return NULL_VALUE;
		}

		// Default value : apply default value if need
		if (StringUtils.isBlank(cellValue) && columnMetadata.getDefaultValue() != null) {
			cellValue = columnMetadata.getDefaultValue();
		}

		// Mandatory
		if (StringUtils.isBlank(cellValue) && !columnMetadata.isNullable()) {
			addError(reader, result, tableMetadata, columnMetadata,
					columnNumber,
					ErrorType.ERROR,
					"NULL_VALUE",
					I18n.t("import.validation.error.NULL_VALUE",
							columnMetadata.getName() ));
			hasError = true;
		}

		// Length
		int columnSize = getColumnSize(tableMetadata, columnMetadata);
		int decimalDigits = columnMetadata.getDecimalDigits();
		if (cellValue != null && columnSize > 0 && cellValue.trim().length() > columnSize) {

			addError(reader, result, tableMetadata, columnMetadata,
					columnNumber,
					ErrorType.ERROR,
					"TOO_LONG_VALUE",
					I18n.t("import.validation.error.TOO_LONG_VALUE",
							cellValue, columnMetadata.getName(), columnSize ));
			hasError = true;
		}


		try {
			if (columnMetadata.getTypeCode() == DoubleType.INSTANCE.sqlType()) {
				if (cellValue.contains(",")) {
					cellValue = cellValue.replace(',', '.');
				}
				return Double.parseDouble(cellValue);
			}

			if (columnMetadata.getTypeCode() == IntegerType.INSTANCE.sqlType()) {
				return Integer.parseInt(cellValue);
			}
		}
		catch (NumberFormatException pe) {
			addError(reader, result, tableMetadata, columnMetadata,
					columnNumber,
					ErrorType.ERROR,
					"SQL_EXCEPTION",
					I18n.t("import.validation.error.PARSE_EXCEPTION",
							columnMetadata.getName(), cellValue, pe.getMessage()));
			hasError = true;
		}

		if (hasError) {
			return NULL_VALUE;
		}

		return cellValue;
	}

	public Set<List<String>> getExistingPrimaryKeys(Connection connection,
			SumarisTableMetadata table) throws SQLException {

		Set<String> pkNames = table.getPkNames();
		int pkCount = pkNames.size();
		String sql = table.getExistingPrimaryKeysQuery();

		PreparedStatement statement = connection.prepareStatement(sql);

		Set<List<String>> result = Sets.newHashSet();
		try {
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next()) {
				List<String> pk = Lists.newArrayListWithCapacity(pkCount);
				for (int i = 1; i <= pkCount; i++) {
					pk.add(String.valueOf(resultSet.getObject(i)));
				}
				result.add(pk);
			}
			return result;
		} finally {
			Daos.closeSilently(statement);
		}
	}

	protected List<String> getPk(ResultSet incomingData, int[] pkIndexs) throws SQLException {
		List<String> result = Lists.newArrayListWithCapacity(pkIndexs.length);
		for (int pkIndex : pkIndexs) {
			Object pk = incomingData.getObject(pkIndex);
			result.add(pk == null ? null : String.valueOf(pk));
		}
		return result;
	}

	protected int getColumnSize(SumarisTableMetadata tableMetadata, SumarisColumnMetadata columnMetadata) {
		StringBuilder sb = new StringBuilder();
		sb.append(tableMetadata.getName());
		sb.append('.');
		sb.append(columnMetadata.getName());
		sb.append(".length");
		try {
			String propValue = I18n.t(sb.toString(), null, Locale.getDefault());
			int overrideSize = Integer.parseInt(propValue);
			if (overrideSize != -1) {
				return overrideSize;
			}
		} catch (NoSuchMessageException | NumberFormatException e) {
			// continue
		}
		return columnMetadata.getColumnSize();

	}

	@Override
	public int removeData(DatabaseTableEnum table, String[] filteredColumns, Object[] filteredValues) {
		SumarisTableMetadata tableMetadata = sumarisDatabaseMetadata.getTable(table.name());
		Preconditions.checkNotNull(tableMetadata);

		Connection conn = DataSourceUtils.getConnection(dataSource);
		String deleteQuery = null;
		try {

			if (filteredColumns != null && filteredColumns.length > 0) {
				Preconditions.checkNotNull(filteredValues);
				Preconditions.checkArgument(filteredColumns.length == filteredValues.length);

				Set<SumarisColumnMetadata> columns = Sets.newLinkedHashSet();
				StringBuilder logBuilder = new StringBuilder();
				int i = 0;
				for (String columnName : filteredColumns) {
					SumarisColumnMetadata column = tableMetadata.getColumnMetadata(columnName);
					if (column == null) {
						throw new DataRetrievalFailureException("Unknown column name '" + columnName + "' for table " + table.name());
					}
					columns.add(column);
					if (log.isInfoEnabled()) {
						logBuilder.append(columnName).append("=").append(filteredValues[i]).append(", ");
					}
					i++;
				}

				if (log.isInfoEnabled()) {
					log.info(String.format("Removing rows on table {%s} using tripFilter {%s}", table.name(), logBuilder.substring(0, logBuilder.length() - 2)));
				}

				deleteQuery = tableMetadata.getDeleteQuery(filteredColumns);
				PreparedStatement deleteStatement = conn.prepareStatement(deleteQuery);
				int paramIndex = 1;
				for (SumarisColumnMetadata column : columns) {
					deleteStatement.setObject(paramIndex, filteredValues[paramIndex - 1], column.getTypeCode());
					paramIndex++;
				}
				return deleteStatement.executeUpdate();
			} else {
				if (log.isInfoEnabled()) {
					log.info(String.format("Removing ALL rows on table {%s}", table.name()));
				}

				deleteQuery = tableMetadata.getDeleteQuery();
				PreparedStatement deleteStatement = conn.prepareStatement(deleteQuery);
				return deleteStatement.executeUpdate();
			}
		} catch (SQLException e) {
			throw sqlExceptionTranslator.translate("Unable to delete table " + table.name(), deleteQuery, e);
		} finally {
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
	}


	protected void addError(
			FileReader reader,
			DataLoadResult result,
			SumarisTableMetadata tableMetadata, SumarisColumnMetadata colMeta,
			int columnNumber,
			ErrorType errorType,
			String errorCode, String description) {
		DataLoadError error = DataLoadError.Builder.create(reader, tableMetadata, colMeta, description)
				.setColumnNumber(columnNumber != -1 ? columnNumber : null)
				.setErrorCode(errorCode)
				.setErrorType(errorType)
				.build();

		result.addError(error);
		logError(error, result);
	}

	protected void addErrorOnce(
			FileReader reader,
			DataLoadResult result,
			SumarisTableMetadata tableMetadata, SumarisColumnMetadata colMeta,
			int columnNumber,
			ErrorType errorType,
			String errorCode, String description) {
		DataLoadError error = DataLoadError.Builder.create(reader, tableMetadata, colMeta, description)
				.setColumnNumber(columnNumber != -1 ? columnNumber : null)
				.setErrorCode(errorCode)
				.setErrorType(errorType)
				.build();

		result.addErrorOnce(error);
		logError(error, result);
	}

	protected void logError(DataLoadError error, DataLoadResult result) {
		if (result.errorCount() < MAX_LOG_ERRORS) {

			// log
			switch (error.getErrorType()) {
				case WARNING:
					log.warn(error.getDescription());
					break;
				case ERROR:
					log.error(error.getDescription());
					break;
				case FATAL:
					log.error(error.getDescription());
					break;
			}
		}
	}
}

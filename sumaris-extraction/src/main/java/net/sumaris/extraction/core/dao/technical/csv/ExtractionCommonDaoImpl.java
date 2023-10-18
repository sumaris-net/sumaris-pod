package net.sumaris.extraction.core.dao.technical.csv;

/*-
 * #%L
 * Quadrige3 Core :: Client API
 * %%
 * Copyright (C) 2017 - 2018 Ifremer
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

import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.ResultSetHelperService;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionCsvDao")
@Lazy
@Slf4j
public class ExtractionCommonDaoImpl extends ExtractionBaseDaoImpl<ExtractionContextVO, ExtractionFilterVO> implements ExtractionCommonDao {

    @Autowired
    private DataSource dataSource;

    @Override
    public void dumpQueryToCSV(File file, String query,
                               Map<String, String> aliasByColumnMap,
                               Map<String, String> dateFormatsByColumnMap,
                               Map<String, String> decimalFormatsByColumnMap,
                               Set<String> excludeColumnNames) throws IOException {

        // create output file
        Writer fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

        // write special character for encoding recognition
        fileWriter.write(UTF8_BOM);

        // create csv writer
        CSVWriter csvWriter = new CSVWriter(fileWriter, configuration.getCsvSeparator());

        // fill result set
        queryAllowEmptyResultSet(
                query,
                new CsvResultSetExtractor(csvWriter, true, aliasByColumnMap, dateFormatsByColumnMap, decimalFormatsByColumnMap, excludeColumnNames));

        // flush result set in file and close
        csvWriter.flush();
        csvWriter.close();

    }

    @Override
    public void clean(ExtractionContextVO context) {
        super.clean(context);
    }

    /* -- private methods -- */

    private void queryAllowEmptyResultSet(String query, CsvResultSetExtractor csvResultSetExtractor) {

        Connection connection = DataSourceUtils.getConnection(dataSource);
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = Daos.prepareQuery(connection, query);
            rs = statement.executeQuery();
            csvResultSetExtractor.extractData(rs);
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException(String.format("Error while executing query [%s]: %s", query, e.getMessage()), e);
        } finally {
            Daos.closeSilently(rs);
            Daos.closeSilently(statement);
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private class CsvResultSetExtractor implements ResultSetExtractor<CSVWriter> {

        private static final boolean DEFAULT_TRIM = false;
        private static final boolean DEFAULT_CSV_APPLY_QUOTES_TO_ALL = false;
        private final CSVWriter writer;
        private final boolean showColumnHeaders;
        private final CsvResultSetHelperService helperService;

        CsvResultSetExtractor(CSVWriter csvWriter, boolean showColumnHeaders,
                              Map<String, String> aliasByColumnMap,
                              Map<String, String> dateFormatsByColumnMap,
                              Map<String, String> decimalFormatsByColumnMap,
                              Set<String> excludeColumnNames) {
            writer = csvWriter;
            helperService = new CsvResultSetHelperService(aliasByColumnMap, dateFormatsByColumnMap, decimalFormatsByColumnMap, excludeColumnNames);
            writer.setResultService(helperService);
            this.showColumnHeaders = showColumnHeaders;
        }

        @Override
        public CSVWriter extractData(ResultSet rs) throws SQLException {
            try {
                writer.writeAll(rs, showColumnHeaders, DEFAULT_TRIM, DEFAULT_CSV_APPLY_QUOTES_TO_ALL);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("%s rows written", helperService.getNbRowsWritten()));
                }
            } catch (IOException e) {
                log.error(e.getLocalizedMessage());
            }
            return writer;
        }
    }

    private class CsvResultSetHelperService extends ResultSetHelperService {

        private final Map<String, String> aliasByColumnMap;
        private final Map<String, String> dateFormatsByColumnMap;
        private final Map<String, String> decimalFormatsByColumnMap;
        private final Map<String, DecimalFormat> decimalFormatsCache;
        private final Set<String> excludeColumnNames;
        private final Set<Integer> excludeColumnIndexes;
        private int nbRowsWritten = 0;

        private CsvResultSetHelperService(Map<String, String> aliasByColumnMap,
                                          Map<String, String> dateFormatsByColumnMap,
                                          Map<String, String> decimalFormatsByColumnMap,
                                          Set<String> excludeColumnNames) {
            this.aliasByColumnMap = aliasByColumnMap;
            this.dateFormatsByColumnMap = dateFormatsByColumnMap;
            this.decimalFormatsByColumnMap = decimalFormatsByColumnMap;
            this.decimalFormatsCache = new HashMap<>();
            this.excludeColumnNames = excludeColumnNames;
            this.excludeColumnIndexes = new TreeSet<>(Comparator.reverseOrder());
        }

        int getNbRowsWritten() {
            return nbRowsWritten;
        }

        @Override
        public String[] getColumnNames(ResultSet rs) throws SQLException {
            // handle column names
            ResultSetMetaData meta = rs.getMetaData();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < meta.getColumnCount(); i++) {
                String columnName = meta.getColumnLabel(i + 1);
                if (excludeColumnNames != null && excludeColumnNames.contains(columnName)) {
                    excludeColumnIndexes.add(i);
                    continue;
                }
                if (aliasByColumnMap != null && aliasByColumnMap.containsKey(columnName)) {
                    columnName = aliasByColumnMap.get(columnName);
                }
                names.add(columnName);
            }
            return names.toArray(new String[0]);
        }

        @Override
        public String[] getColumnValues(ResultSet rs, boolean trim, String dateFormatString, String timeFormatString) throws SQLException, IOException {

            nbRowsWritten++;
            String[] values = super.getColumnValues(rs, trim, dateFormatString, timeFormatString);

            for (Integer index : excludeColumnIndexes)
                values = ArrayUtils.remove(values, index);

            return values;
        }

        @Override
        protected String handleBigDecimal(ResultSet rs, int columnIndex) throws SQLException {

            // handle decimal field
            BigDecimal value = rs.getBigDecimal(columnIndex);
            if (value == null) return "";

            // Must handle this value by its float representation because of rounding
            value = new BigDecimal(String.valueOf(rs.getFloat(columnIndex)));

            // find a formatter
            String columnName = rs.getMetaData().getColumnLabel(columnIndex);
            if (decimalFormatsCache.containsKey(columnName)) {
                return decimalFormatsCache.get(columnName).format(value);
            }
            if (decimalFormatsByColumnMap != null && decimalFormatsByColumnMap.containsKey(columnName)) {
                DecimalFormat decimalFormat = new DecimalFormat(decimalFormatsByColumnMap.get(columnName));
                decimalFormatsCache.put(columnName, decimalFormat);
                return decimalFormat.format(value);
            }

            return value.toPlainString();
        }

        @Override
        protected String handleDate(ResultSet rs, int columnIndex, String dateFormatString) throws SQLException {
            // handle date column
            String columnName = rs.getMetaData().getColumnLabel(columnIndex);
            if (dateFormatsByColumnMap != null && dateFormatsByColumnMap.containsKey(columnName)) {
                return super.handleDate(rs, columnIndex, dateFormatsByColumnMap.get(columnName));
            }
            return super.handleDate(rs, columnIndex, dateFormatString);
        }

        @Override
        protected String handleTimestamp(ResultSet rs, int columnIndex, String timestampFormatString) throws SQLException {
            // handle timestamp column
            String columnName = rs.getMetaData().getColumnLabel(columnIndex);
            if (dateFormatsByColumnMap != null && dateFormatsByColumnMap.containsKey(columnName)) {
                return super.handleTimestamp(rs, columnIndex, dateFormatsByColumnMap.get(columnName));
            }
            return super.handleTimestamp(rs, columnIndex, timestampFormatString);
        }
    }
}

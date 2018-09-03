package net.sumaris.core.dao.technical;

/*-
 * #%L
 * SUMARiS :: Core
 * $Id:$
 * $HeadURL:$
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.exception.SumarisTechnicalException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.nuiton.i18n.I18n;
import org.nuiton.version.Version;
import org.nuiton.version.Versions;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.Date;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nuiton.i18n.I18n.t;

/**
 * Useful method around DAO and entities.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 3.5
 */
public class Daos {

    private final static String JDBC_URL_PREFIX = "jdbc:";
    private final static String JDBC_URL_PREFIX_HSQLDB = JDBC_URL_PREFIX + "hsqldb:";
    private final static String JDBC_URL_PREFIX_ORACLE = JDBC_URL_PREFIX + "oracle:";
    private final static String JDBC_URL_PREFIX_HSQLDB_FILE = JDBC_URL_PREFIX_HSQLDB + "file:";

    /**
     * Constant <code>DB_DIRECTORY="db"</code>
     */
    public static final String DB_DIRECTORY = "db";

    /**
     * Logger.
     */
    private static final Log log = LogFactory.getLog(Daos.class);

    private static final boolean debug = log.isDebugEnabled();

    /**
     * <p>Constructor for Daos.</p>
     */
    protected Daos() {
        // helper class does not instantiate
    }

    /**
     * Create a new hibernate configuration, with all hbm.xml files for the schema need for app
     *
     * @param jdbcUrl  a {@link String} object.
     * @param username a {@link String} object.
     * @param password a {@link String} object.
     * @param schema   a {@link String} object.
     * @param dialect  a {@link String} object.
     * @param driver   a {@link String} object.
     * @return the hibernate Configuration
     */
    public static Properties getConnectionProperties(String jdbcUrl, String username, String password, String schema, String dialect, String driver) {

        // Building a new configuration
        Properties p = new Properties();

        // Set driver
        p.setProperty(Environment.DRIVER, driver);

        // Set hibernate dialect
        p.setProperty(Environment.DIALECT, dialect);

        // To be able to retrieve connection
        p.setProperty(Environment.URL, jdbcUrl);
        p.setProperty(Environment.USER, username);
        p.setProperty(Environment.PASS, password);

        if (StringUtils.isNotBlank(schema)) {
            p.setProperty(Environment.DEFAULT_SCHEMA, schema);
        }

        // Try with synonyms enable
        p.setProperty(AvailableSettings.ENABLE_SYNONYMS, "true");

        // Pour tester avec le metadata generic (normalement plus long pour Oracle)
        // cfg.setProperty("hibernatetool.metadatadialect", "org.hibernate.cfg.rveng.dialect.JDBCMetaDataDialect");
        if (jdbcUrl.startsWith("jdbc:oracle")) {
            p.setProperty("hibernatetool.metadatadialect", "org.hibernate.cfg.rveng.dialect.OracleMetaDataDialect");
        }

        return p;
    }

    /**
     * <p>closeSilently.</p>
     *
     * @param statement a {@link Statement} object.
     */
    public static void closeSilently(Statement statement) {
        try {
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
        } catch (AbstractMethodError e) {
            try {
                statement.close();
            } catch (SQLException ignored) {
            }
            if (debug) {
                log.debug("Fix this linkage error, damned hsqlsb 1.8.0.7:(");
            }
        } catch (IllegalAccessError e) {
            if (debug) {
                log.debug("Fix this IllegalAccessError error, damned hsqlsb 1.8.0.7:(");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not close statement, but do not care", e);
            }
        }
    }

    /**
     * <p>closeSilently.</p>
     *
     * @param connection a {@link Connection} object.
     */
    public static void closeSilently(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not close connection, but do not care", e);
            }
        }
    }

    /**
     * <p>closeSilently.</p>
     *
     * @param statement a {@link ResultSet} object.
     */
    public static void closeSilently(ResultSet statement) {
        try {
            if (statement != null && !statement.isClosed()) {

                statement.close();
            }
        } catch (AbstractMethodError e) {
            try {
                statement.close();
            } catch (SQLException ignored) {
            }
            if (debug) {
                log.debug("Fix this linkage error, damned hsqlsb 1.8.0.7:(");
            }
        } catch (IllegalAccessError e) {
            if (debug) {
                log.debug("Fix this IllegalAccessError error, damned hsqlsb 1.8.0.7:(");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not close statement, but do not care", e);
            }
        }
    }

    /**
     * <p>closeSilently.</p>
     *
     * @param session a {@link Session} object.
     */
    public static void closeSilently(Session session) {
        try {
            if (session != null && session.isOpen()) {

                session.close();
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not close session, but do not care", e);
            }
        }
    }

    /**
     * <p>createConnection.</p>
     *
     * @param connectionProperties a {@link Properties} object.
     * @return a {@link Connection} object.
     * @throws SQLException if any.
     */
    public static Connection createConnection(Properties connectionProperties) throws SQLException {
        return createConnection(
                connectionProperties.getProperty(Environment.URL),
                connectionProperties.getProperty(Environment.USER),
                connectionProperties.getProperty(Environment.PASS)
        );
    }

    /**
     * <p>getUrl.</p>
     *
     * @param connectionProperties a {@link Properties} object.
     * @return a {@link String} object.
     */
    public static String getUrl(Properties connectionProperties) {
        return connectionProperties.getProperty(Environment.URL);
    }

    /**
     * <p>getUser.</p>
     *
     * @param connectionProperties a {@link Properties} object.
     * @return a {@link String} object.
     */
    public static String getUser(Properties connectionProperties) {
        return connectionProperties.getProperty(Environment.USER);
    }

    /**
     * <p>getDriver.</p>
     *
     * @param connectionProperties a {@link Properties} object.
     * @return a {@link String} object.
     */
    public static String getDriver(Properties connectionProperties) {
        return connectionProperties.getProperty(Environment.DRIVER);
    }

    /**
     * <p>createConnection.</p>
     *
     * @param jdbcUrl  a {@link String} object.
     * @param user     a {@link String} object.
     * @param password a {@link String} object.
     * @return a {@link Connection} object.
     * @throws SQLException if any.
     */
    public static Connection createConnection(String jdbcUrl,
                                              String user,
                                              String password) throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl,
                user,
                password);
        connection.setAutoCommit(false);
        return connection;
    }

    /**
     * <p>fillConnectionProperties.</p>
     *
     * @param p        a {@link Properties} object.
     * @param url      a {@link String} object.
     * @param username a {@link String} object.
     * @param password a {@link String} object.
     */
    public static void fillConnectionProperties(Properties p,
                                                String url,
                                                String username,
                                                String password) {
        p.put(Environment.URL, url);
        p.put(Environment.USER, username);
        p.put(Environment.PASS, password);
    }

    /**
     * <p>getJdbcUrl.</p>
     *
     * @param directory a {@link File} object.
     * @param dbName    a {@link String} object.
     * @return a {@link String} object.
     */
    public static String getJdbcUrl(File directory, String dbName) {
        String jdbcUrl = JDBC_URL_PREFIX_HSQLDB_FILE + directory.getAbsolutePath() + "/" + dbName;
        jdbcUrl = jdbcUrl.replaceAll("\\\\", "/");
        return jdbcUrl;
    }

    /**
     * <p>isHsqlDatabase.</p>
     *
     * @param jdbcUrl a {@link String} object.
     * @return a boolean.
     */
    public static boolean isHsqlDatabase(String jdbcUrl) {
        Preconditions.checkNotNull(jdbcUrl);
        return jdbcUrl.startsWith(JDBC_URL_PREFIX_HSQLDB);
    }

    public static boolean isHsqlDatabase(Connection conn) {
        Preconditions.checkNotNull(conn);
        try {
            String jdbcUrl = conn.getMetaData().getURL();
            return isHsqlDatabase(jdbcUrl);
        }
        catch(SQLException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    /**
     * <p>isOracleDatabase.</p>
     *
     * @param jdbcUrl a {@link String} object.
     * @return a boolean.
     */
    public static boolean isOracleDatabase(String jdbcUrl) {
        Preconditions.checkNotNull(jdbcUrl);
        return jdbcUrl.startsWith(JDBC_URL_PREFIX_ORACLE);
    }

    /**
     * <p>isFileDatabase.</p>
     *
     * @param jdbcUrl a {@link String} object.
     * @return a boolean.
     */
    public static boolean isFileDatabase(String jdbcUrl) {
        Preconditions.checkNotNull(jdbcUrl);
        return jdbcUrl.startsWith(JDBC_URL_PREFIX_HSQLDB_FILE);
    }

    /**
     * <p>isFileDatabase.</p>
     *
     * @param conn a {@link Connection} object.
     * @return a boolean.
     */
    public static boolean isFileDatabase(Connection conn) {
        Preconditions.checkNotNull(conn);
        try {
            String jdbcUrl = conn.getMetaData().getURL();
            return isFileDatabase(jdbcUrl);
        }
        catch(SQLException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    /**
     * <p>getDbDirectoryFromJdbcUrl.</p>
     *
     * @param jdbcUrl a {@link String} object.
     * @return a {@link String} object.
     */
    public static String getDbDirectoryFromJdbcUrl(String jdbcUrl) {
        Preconditions.checkNotNull(jdbcUrl);

        // HsqlDB file database
        if (jdbcUrl.startsWith(JDBC_URL_PREFIX_HSQLDB_FILE)) {
            String dbDirectory = jdbcUrl.substring(JDBC_URL_PREFIX_HSQLDB_FILE.length());

            // Remove the DB name
            int lastSlashIndex = dbDirectory.lastIndexOf('/');
            if (lastSlashIndex != -1) {
                dbDirectory = dbDirectory.substring(0, lastSlashIndex);
            }
            return dbDirectory;
        }

        return null;
    }

    /**
     * <p>setIntegrityConstraints.</p>
     *
     * @param connectionProperties       a {@link Properties} object.
     * @param enableIntegrityConstraints a boolean.
     * @throws SQLException if any.
     */
    public static void setIntegrityConstraints(Properties connectionProperties, boolean enableIntegrityConstraints) throws SQLException {
        // Execute the SQL order
        Connection connection = null;
        try {
            connection = createConnection(connectionProperties);
            setIntegrityConstraints(connection, enableIntegrityConstraints);
        } finally {
            closeSilently(connection);
        }

    }

    /**
     * <p>setIntegrityConstraints.</p>
     *
     * @param connection                 a {@link Connection} object.
     * @param enableIntegrityConstraints a boolean.
     * @throws SQLException if any.
     */
    public static void setIntegrityConstraints(Connection connection, boolean enableIntegrityConstraints) throws SQLException {
        String jdbcUrl = connection.getMetaData().getURL();

        String sql;
        // if HSQLDB
        if (isHsqlDatabase(jdbcUrl)) {
            Version hsqldbVersion = getDatabaseVersion(connection);

            // 1.8 :
            if ("1.8".equals(hsqldbVersion.toString())) {
                sql = "SET REFERENTIAL_INTEGRITY %s";
            }

            // 2.x :
            else {
                sql = "SET DATABASE REFERENTIAL INTEGRITY %s";
            }
            sql = String.format(sql, enableIntegrityConstraints ? "TRUE" : "FALSE");
            sqlUpdate(connection, sql);
        }

        /*else if (isOracleDatabase(jdbcUrl)) {
            OracleStatements.setIntegrityConstraints(connection, enableIntegrityConstraints);
        }*/

        // else: not supported operation
        else {
            throw new SumarisTechnicalException(String.format(
                    "Could not enable/disable integrity constraints on database: %s. Not implemented for this DBMS.", jdbcUrl));
        }

    }

    /**
     * Check if connection properties are valid. Try to open a SQL connection, then close it. If no error occur, the connection is valid.
     *
     * @param jdbcDriver a {@link String} object.
     * @param jdbcUrl    a {@link String} object.
     * @param user       a {@link String} object.
     * @param password   a {@link String} object.
     * @return a boolean.
     */
    public static boolean isValidConnectionProperties(
            String jdbcDriver,
            String jdbcUrl,
            String user,
            String password) {
        try {
            Class<?> driverClass = Class.forName(jdbcDriver);
            DriverManager.registerDriver((Driver) driverClass.newInstance());
        } catch (Exception e) {
            log.error("Could not load JDBC Driver: " + e.getMessage(), e);
            return false;
        }

        Connection connection = null;
        try {
            connection = createConnection(
                    jdbcUrl,
                    user,
                    password);
            return true;
        } catch (SQLException e) {
            log.error("Could not connect to database: " + e.getMessage().trim());
        } finally {
            Daos.closeSilently(connection);
        }
        return false;
    }

    /**
     * Check if connection properties are valid. Try to open a SQL connection, then close it. If no error occur, the connection is valid.
     *
     * @param connectionProperties a {@link Properties} object.
     * @return a boolean.
     */
    public static boolean isValidConnectionProperties(Properties connectionProperties) {
        return isValidConnectionProperties(
                connectionProperties.getProperty(Environment.DRIVER),
                connectionProperties.getProperty(Environment.URL),
                connectionProperties.getProperty(Environment.USER),
                connectionProperties.getProperty(Environment.PASS));
    }

    private static final double EARTH_RADIUS = 6378288.0;

    private static final MathContext MATH_CONTEXT_4_DIGIT = new MathContext(4);

    private static DecimalFormatSymbols symbols;

    private static DecimalFormat decimalFormat;

    /**
     * <p>computeDistanceInMeters.</p>
     *
     * @param startLatitude  a {@link Float} object.
     * @param startLongitude a {@link Float} object.
     * @param endLatitude    a {@link Float} object.
     * @param endLongitude   a {@link Float} object.
     * @return a int.
     */
    public static int computeDistanceInMeters(Float startLatitude,
                                              Float startLongitude,
                                              Float endLatitude,
                                              Float endLongitude) {

        double sLat = startLatitude * Math.PI / 180.0;
        double sLong = startLongitude * Math.PI / 180.0;
        double eLat = endLatitude * Math.PI / 180.0;
        double eLong = endLongitude * Math.PI / 180.0;

        Double d = EARTH_RADIUS
                * (Math.PI / 2 - Math.asin(Math.sin(eLat) * Math.sin(sLat)
                + Math.cos(eLong - sLong) * Math.cos(eLat) * Math.cos(sLat)));
        return d.intValue();
    }

    /**
     * <p>getDistanceInMiles.</p>
     *
     * @param distance a {@link Float} object.
     * @return a {@link String} object.
     */
    public static String getDistanceInMiles(Float distance) {
        String distanceText;
        if (distance != null) {
            Float distanceInMiles = distance / 1852;
            distanceText = String.format("%.3f", distanceInMiles);

        } else {
            distanceText = "";
        }
        return distanceText;
    }

    /**
     * <p>getRoundedLengthStep.</p>
     *
     * @param lengthStep a float.
     * @param aroundUp   a boolean.
     * @return a float.
     */
    public static float getRoundedLengthStep(float lengthStep, boolean aroundUp) {
        int intValue = (int) ((lengthStep + (aroundUp ? 0.001f : 0f)) * 10);
        return intValue / 10f;
    }

    /**
     * <p>getDecimalFormatSymbols.</p>
     *
     * @return a {@link DecimalFormatSymbols} object.
     */
    public static DecimalFormatSymbols getDecimalFormatSymbols() {
        if (symbols == null) {
            symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            symbols.setGroupingSeparator(' ');
        }
        return symbols;
    }

    /**
     * <p>Getter for the field <code>decimalFormat</code>.</p>
     *
     * @param minDecimal a int.
     * @param maxDecimal a int.
     * @return a {@link DecimalFormat} object.
     */
    public static DecimalFormat getDecimalFormat(int minDecimal, int maxDecimal) {
        if (decimalFormat == null) {
            decimalFormat = new DecimalFormat();
            decimalFormat.setDecimalFormatSymbols(getDecimalFormatSymbols());
            decimalFormat.setGroupingUsed(false);
        }
        decimalFormat.setMinimumFractionDigits(minDecimal);
        decimalFormat.setMaximumFractionDigits(maxDecimal);
        return decimalFormat;
    }

    /**
     * <p>getWeightStringValue.</p>
     *
     * @param weight a {@link Float} object.
     * @return a {@link String} object.
     */
    public static String getWeightStringValue(Float weight) {
        String textValue;
        if (weight != null) {
            DecimalFormat weightDecimalFormat = getDecimalFormat(1, 3);
            textValue = weightDecimalFormat.format(weight);

        } else {
            textValue = "";
        }
        return textValue;
    }

    /**
     * <p>getValueOrComputedValue.</p>
     *
     * @param value         a N object.
     * @param computedValue a N object.
     * @return a N object.
     */
    public static <N extends Number> N getValueOrComputedValue(N value, N computedValue) {
        return value == null ? computedValue : value;
    }

    /**
     * <p>getValueOrComputedValueComputed.</p>
     *
     * @param value         a N object.
     * @param computedValue a N object.
     * @return a {@link Boolean} object.
     */
    public static <N extends Number> Boolean getValueOrComputedValueComputed(N value, N computedValue) {
        Boolean result;
        if (value == null) {

            result = computedValue == null ? null : true;
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Round the given value to max 4 digits.
     *
     * @param value the float to round.
     * @return the rounded value
     * @since 1.0.1
     */
    public static float roundKiloGram(float value) {
        BigDecimal sumB = new BigDecimal(value);
        return sumB.abs(MATH_CONTEXT_4_DIGIT).floatValue();
    }

    /**
     * Compare two weights with rounding them to kilograms.
     *
     * @param v0 first weight to compare
     * @param v1 second weight to compare
     * @return 1 if v0 > v1, -1 if v0 < v1, 0 if v0 == v1
     */
    public static int compareWeights(float v0, float v1) {
        v0 = roundKiloGram(v0);
        v1 = roundKiloGram(v1);
        float delta = v0 - v1;
        int result;
        if (delta > 0.00001) {
            // v0 > v1
            result = 1;
        } else if (delta < -0.0001f) {
            // v0 < v1
            result = -1;
        } else {
            // v0 == v1
            result = 0;
        }
        return result;
    }

    /**
     * <p>isSmallerWeight.</p>
     *
     * @param v0 a float.
     * @param v1 a float.
     * @return a boolean.
     */
    public static boolean isSmallerWeight(float v0, float v1) {
        return compareWeights(v0, v1) < 0;
    }

    /**
     * <p>isGreaterWeight.</p>
     *
     * @param v0 a float.
     * @param v1 a float.
     * @return a boolean.
     */
    public static boolean isGreaterWeight(float v0, float v1) {
        return compareWeights(v0, v1) > 0;
    }

    /**
     * <p>isEqualWeight.</p>
     *
     * @param v0 a float.
     * @param v1 a float.
     * @return a boolean.
     */
    public static boolean isEqualWeight(float v0, float v1) {
        return compareWeights(v0, v1) == 0;
    }

    /**
     * <p>isNotEqualWeight.</p>
     *
     * @param v0 a float.
     * @param v1 a float.
     * @return a boolean.
     */
    public static boolean isNotEqualWeight(float v0, float v1) {
        return compareWeights(v0, v1) != 0;
    }

    /**
     * <p>convertToDouble.</p>
     *
     * @param floatValue a {@link Float} object.
     * @return a {@link Double} object.
     */
    public static Double convertToDouble(Float floatValue) {
        if (floatValue == null) {
            return null;
        }
        // TODO : trouver une meilleur solution (attention à ne pas perdre de précision)
        return Double.parseDouble(Float.toString(floatValue));
    }

    /**
     * <p>convertToFloat.</p>
     *
     * @param doubleValue a {@link Double} object.
     * @return a {@link Float} object.
     */
    public static Float convertToFloat(Double doubleValue) {
        if (doubleValue == null) {
            return null;
        }
        // TODO : trouver une meilleur solution (attention à ne pas perdre de précision)
        return Float.parseFloat(Double.toString(doubleValue));
    }

    /**
     * <p>convertToInteger.</p>
     *
     * @param value a {@link String} object.
     * @return a {@link Integer} object.
     */
    public static Integer convertToInteger(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            log.error("error when try to parse an integer", nfe);
            return null;
        }
    }

    /**
     * <p>convertToDate.</p>
     *
     * @param date a {@link Object} object.
     * @return a {@link Date} object.
     */
    public static Date convertToDate(Object date) {
        if (date instanceof Timestamp) {
            return new Date(((Timestamp) date).getTime());
        } else if (date instanceof Date) {
            return (Date) date;
        } else {
            return null;
        }
    }

    /**
     * <p>safeConvertToBoolean.</p>
     *
     * @param object       a {@link Object} object.
     * @param defaultValue a boolean.
     * @return a boolean.
     */
    public static boolean safeConvertToBoolean(Object object, boolean defaultValue) {
        if (object instanceof Boolean) {
            return ((Boolean) object);
        } else if (object instanceof Number) {
            return ((Number) object).intValue() > 0;
        } else if (object instanceof String) {
            if (StringUtils.isNumeric((String) object)) {
                return Integer.parseInt((String) object) != 0;
            }
        }
        return defaultValue;
    }

    /**
     * Convert to boolean (and return false if null)
     *
     * @param object a {@link Object} object.
     * @return a boolean.
     */
    public static boolean safeConvertToBoolean(Object object) {
        if (object instanceof Boolean) {
            return ((Boolean) object);
        } else if (object instanceof Number) {
            return ((Number) object).intValue() > 0;
        } else if (object instanceof String) {
            if (StringUtils.isNumeric((String) object)) {
                return Integer.parseInt((String) object) != 0;
            }
        }
        return false;
    }

    /**
     * Convert to Boolean (may return null)
     *
     * @param object a {@link Object} object.
     * @return a {@link Boolean} object.
     */
    public static Boolean convertToBoolean(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Boolean) {
            return ((Boolean) object);
        } else if (object instanceof Number) {
            return ((Number) object).intValue() > 0;
        } else if (object instanceof String) {
            if (StringUtils.isNumeric((String) object)) {
                return Integer.parseInt((String) object) != 0;
            }
        } else if (object instanceof Character) {
            if (StringUtils.isNumeric(object.toString())) {
                return Integer.parseInt(object.toString()) != 0;
            }
        }
        throw new SumarisTechnicalException(String.format("Unable to convert value to boolean, for class [%s]", object.getClass().getCanonicalName()));
    }

    /**
     * <p>convertToString.</p>
     *
     * @param bool a {@link Boolean} object.
     * @return a {@link String} object.
     */
    public static String convertToString(Boolean bool) {
        return bool == null ? null : (bool ? "1" : "0");
    }

    /**
     * <p>sqlUpdate.</p>
     *
     * @param dataSource a {@link DataSource} object.
     * @param sql        a {@link String} object.
     * @return a int.
     */
    public static int sqlUpdate(DataSource dataSource, String sql) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return sqlUpdate(connection, sql);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    /**
     * <p>sqlUpdate.</p>
     *
     * @param connection a {@link Connection} object.
     * @param sql        a {@link String} object.
     * @return a int.
     */
    public static int sqlUpdate(Connection connection, String sql) {
        Statement stmt;
        try {
            stmt = connection.createStatement();
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Could not open database connection", ex);
        }

        // Log using a special logger
        if (debug) {
            log.debug(sql);
        }

        try {
            return stmt.executeUpdate(sql);
        } catch (SQLException ex) {
            throw new DataIntegrityViolationException("Could not execute query: " + sql, ex);
        } finally {
            closeSilently(stmt);
        }
    }

    /**
     * <p>sqlUnique.</p>
     *
     * @param dataSource a {@link DataSource} object.
     * @param sql        a {@link String} object.
     * @return a {@link Object} object.
     */
    public static Object sqlUnique(DataSource dataSource, String sql) throws DataAccessResourceFailureException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return sqlUnique(connection, sql);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    /**
     * <p>sqlUniqueTyped.</p>
     *
     * @param dataSource a {@link DataSource} object.
     * @param sql        a {@link String} object.
     * @param <T>        a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    public static <T> T sqlUniqueTyped(DataSource dataSource, String sql) {
        return (T) sqlUnique(dataSource, sql);
    }

    /**
     * <p>sqlUnique.</p>
     *
     * @param connection a {@link Connection} object.
     * @param sql        a {@link String} object.
     * @return a {@link Object} object.
     */
    public static Object sqlUnique(Connection connection, String sql) {
        Statement stmt;
        try {
            stmt = connection.createStatement();
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Could not open database connection", ex);
        }

        // Log using a special logger
        if (debug) {
            log.debug(sql);
        }

        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                throw new DataRetrievalFailureException("Executed query return no row: " + sql);
            }
            Object result = rs.getObject(1);
            if (rs.next()) {
                throw new DataRetrievalFailureException("Executed query has more than one row: " + sql);
            }
            return result;

        } catch (SQLException ex) {
            throw new DataIntegrityViolationException("Could not execute query: " + sql, ex);
        } finally {
            closeSilently(stmt);
        }
    }

    /**
     * <p>sqlUniqueOrNull.</p>
     *
     * @param connection a {@link Connection} object.
     * @param sql        a {@link String} object.
     * @return a {@link Object} object.
     */
    public static Object sqlUniqueOrNull(Connection connection, String sql) {
        Statement stmt;
        try {
            stmt = connection.createStatement();
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Could not open database connection", ex);
        }

        // Log using a special logger
        if (debug) {
            log.debug(sql);
        }

        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                return null;
            }
            Object result = rs.getObject(1);
            if (rs.next()) {
                throw new DataRetrievalFailureException("Executed query has more than one row: " + sql);
            }
            return result;

        } catch (SQLException ex) {
            throw new DataIntegrityViolationException("Could not execute query: " + sql, ex);
        } finally {
            closeSilently(stmt);
        }
    }

    /**
     * <p>sqlUniqueTyped.</p>
     *
     * @param connection a {@link Connection} object.
     * @param sql        a {@link String} object.
     * @param <T>        a T object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    public static <T> T sqlUniqueTyped(Connection connection, String sql) {
        return (T) sqlUnique(connection, sql);
    }

    /**
     * <p>shutdownDatabase.</p>
     *
     * @param connection a {@link Connection} object.
     */
    public static void shutdownDatabase(Connection connection) {
        shutdownDatabase(connection, false);
    }

    /**
     * <p>shutdownDatabase.</p>
     *
     * @param dataSource a {@link DataSource} object.
     */
    public static void shutdownDatabase(DataSource dataSource) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            shutdownDatabase(connection);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    /**
     * <p>shutdownDatabase.</p>
     *
     * @param connection a {@link Connection} object.
     * @param compact    a boolean.
     */
    public static void shutdownDatabase(Connection connection, boolean compact) {
        try {
            String jdbcUrl = connection.getMetaData().getURL();
            if (isFileDatabase(jdbcUrl)) {
                String sql = "SHUTDOWN";
                if (compact) {
                    sql += " COMPACT";
                }
                sqlUpdate(connection, sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * <p>shutdownDatabase.</p>
     *
     * @param connectionProperties a {@link Properties} object.
     * @throws SQLException if any.
     */
    public static void shutdownDatabase(Properties connectionProperties) throws SQLException {
        Connection conn = Daos.createConnection(connectionProperties);
        try {
            shutdownDatabase(conn);
        } finally {
            closeSilently(conn);
        }
    }

    /**
     * <p>prepareQuery.</p>
     *
     * @param connection a {@link Connection} object.
     * @param sql        a {@link String} object.
     * @return a {@link PreparedStatement} object.
     * @throws SQLException if any.
     */
    public static PreparedStatement prepareQuery(Connection connection, String sql) throws SQLException {

        if (debug) {
            log.debug(String.format("Execute query: %s", sql));
        }

        return connection.prepareStatement(sql);
    }

    /**
     * <p>bindQuery.</p>
     *
     * @param connection a {@link Connection} object.
     * @param sql        a {@link String} object.
     * @param bindingMap a {@link Map} object.
     * @return a {@link PreparedStatement} object.
     * @throws SQLException if any.
     */
    public static PreparedStatement bindQuery(Connection connection, String sql, Map<String, Object> bindingMap) throws SQLException {
        StringBuilder sb = new StringBuilder();

        StringBuilder debugParams = null;
        if (debug) {
            debugParams = new StringBuilder();
        }

        List<Object> orderedBindingValues = Lists.newArrayList();
        Matcher paramMatcher = Pattern.compile(":[a-zA-Z_0-9]+").matcher(sql);
        int offset = 0;
        while (paramMatcher.find()) {
            String bindingName = sql.substring(paramMatcher.start() + 1, paramMatcher.end());
            Object bindingValue = bindingMap.get(bindingName);
            if (bindingValue == null && !bindingMap.containsKey(bindingName)) {
                log.error(t("sumaris.persistence.bindingQuery.error.log",
                        bindingName,
                        sql));
                throw new DataAccessResourceFailureException(t("sumaris.persistence.bindingQuery.error",
                        sql));
            }
            orderedBindingValues.add(bindingValue);
            sb.append(sql.substring(offset, paramMatcher.start()))
                    .append("?");
            offset = paramMatcher.end();

            if (debug) {
                debugParams.append(", ").append(bindingValue);
            }
        }
        if (offset > 0) {
            if (offset < sql.length()) {
                sb.append(sql.substring(offset));
            }
            sql = sb.toString();
        }

        if (debug) {
            log.debug(String.format("Execute query: %s", sql));
            log.debug(String.format("  with params: [%s]", debugParams.length() > 2 ? debugParams.substring(2)
                    : "no binding"));
        }

        PreparedStatement statement = connection.prepareStatement(sql);

        int index = 1;
        for (Object value : orderedBindingValues) {
            statement.setObject(index, value);
            index++;
        }

        return statement;
    }

    /**
     * <p>compactDatabase.</p>
     *
     * @param dataSource a {@link DataSource} object.
     */
    public static void compactDatabase(DataSource dataSource) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            compactDatabase(connection);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    /**
     * <p>compactDatabase.</p>
     *
     * @param connectionProperties a {@link Properties} object.
     * @throws SQLException if any.
     */
    public static void compactDatabase(Properties connectionProperties) throws SQLException {
        Connection conn = Daos.createConnection(connectionProperties);
        try {
            compactDatabase(conn);
        } finally {
            closeSilently(conn);
        }
    }

    /**
     * Will compact database (only for HsqlDB connection)<br/>
     * This method typically call a 'CHECKPOINT DEFRAG'
     *
     * @param connection a valid JDBC connection
     */
    public static void compactDatabase(Connection connection) {

        try {
            connection.setReadOnly(false);
            String jdbcUrl = connection.getMetaData().getURL();
            if (jdbcUrl.startsWith(JDBC_URL_PREFIX_HSQLDB)) {
                String sql = "CHECKPOINT DEFRAG";
                sqlUpdate(connection, sql);
            }
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException(I18n.t("sumaris.persistence.compactDatabase.error"), e);
        }
    }

    /**
     * Transform into a string (e.g. for log...) the given connection properties
     *
     * @param connectionProperties a {@link Properties} object.
     * @return a {@link String} object.
     */
    public static String getLogString(Properties connectionProperties) {
        Preconditions.checkNotNull(connectionProperties);
        StringBuilder result = new StringBuilder();

        // Driver
        String jdbcDriver = getDriver(connectionProperties);
        if (StringUtils.isNotBlank(jdbcDriver)) {
            result.append(t("sumaris.persistence.connection.driver", jdbcDriver)).append('\n');
        }

        // DB Directory (if any)
        String jdbcUrl = Daos.getUrl(connectionProperties);
        if (Daos.isFileDatabase(jdbcUrl)) {
            String dbDirectory = Daos.getDbDirectoryFromJdbcUrl(jdbcUrl);
            if (dbDirectory != null) {
                result.append(t("sumaris.persistence.connection.directory", dbDirectory)).append('\n');
            }
        }

        // URL
        result.append(t("sumaris.persistence.connection.url", getUrl(connectionProperties))).append('\n');

        // User
        result.append(t("sumaris.persistence.connection.username", getUser(connectionProperties))).append('\n');

        // Catalog
        String jdbcCatalog = connectionProperties.getProperty(Environment.DEFAULT_CATALOG);
        if (StringUtils.isNotBlank(jdbcCatalog)) {
            result.append(t("sumaris.persistence.connection.catalog", jdbcCatalog)).append('\n');
        }

        // Schema
        String jdbcSchema = connectionProperties.getProperty(Environment.DEFAULT_SCHEMA);
        if (StringUtils.isNotBlank(jdbcSchema)) {
            result.append(t("sumaris.persistence.connection.schema", jdbcSchema)).append('\n');
        }

        return result.substring(0, result.length() - 1);
    }

    /**
     * Count number of rows in a table
     *
     * @param connection a {@link Connection} object.
     * @param tableName  a {@link String} object.
     * @return a long.
     */
    public static long countTableRows(Connection connection, String tableName) {

        String sql = "SELECT COUNT(*) FROM " + tableName;
        PreparedStatement statement = null;
        ResultSet rs;
        try {
            statement = connection.prepareStatement(sql);
            rs = statement.executeQuery();
            if (rs.next()) {
                Object result = rs.getObject(1);
                if (result != null && result instanceof Number) {
                    return ((Number) result).longValue();
                }
            }
            throw new DataAccessResourceFailureException(String.format("Could not count rows for table %s, because query return no rows ! [%s]", tableName, sql));
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException(String.format("Error while counting rows of table %s: [%s]", tableName, sql), e);
        } finally {
            Daos.closeSilently(statement);
        }
    }

    /**
     * Check DB directory tree, and return directory with database files inside
     * Throw exception if error in tree, or if script and properties files are not found
     *
     * @param dbDirectory a {@link File} object.
     * @return a {@link File} object.
     */
    public static File checkAndNormalizeDbDirectory(File dbDirectory) {
        Preconditions.checkNotNull(dbDirectory);
        Preconditions.checkArgument(dbDirectory.isDirectory());

        // Collect files and directories
        Collection<File> subFilesAndDirs = FileUtils.listFilesAndDirs(dbDirectory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        subFilesAndDirs.remove(dbDirectory); // remove itself (keep children only)

        // If only one file or dir: should be directory (root directory)
        if (CollectionUtils.size(subFilesAndDirs) == 1) {
            File subFileOrDir = subFilesAndDirs.iterator().next();
            if (!subFileOrDir.isDirectory()) {
                throw new DataAccessResourceFailureException(t("sumaris.persistence.db.zip.badContent", DB_DIRECTORY));
            }

            if (!subFileOrDir.isDirectory()) {
                throw new DataAccessResourceFailureException(t("sumaris.persistence.db.zip.badContent", DB_DIRECTORY));
            }

            // If not the 'db' directory, try to use it as main directory
            if (!subFileOrDir.getName().equalsIgnoreCase(DB_DIRECTORY)) {
                dbDirectory = subFileOrDir;
                subFilesAndDirs = FileUtils.listFilesAndDirs(dbDirectory, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                subFilesAndDirs.remove(dbDirectory); // remove itself (keep children only)
            }
        }

        // Try to find a db directory
        for (File subDirOrFile : subFilesAndDirs) {
            if (subDirOrFile.isDirectory()
                    && subDirOrFile.getName().equalsIgnoreCase(DB_DIRECTORY)) {
                dbDirectory = subDirOrFile;
                break;
            }
        }

        // On db directory, try to find script and properties files
        Collection<File> dbFiles = FileUtils.listFiles(dbDirectory, TrueFileFilter.INSTANCE, null);
        boolean hasScriptFile = false;
        boolean hasPropertiesFile = false;
        String dbName = SumarisConfiguration.getInstance().getDbName();
        for (File dbFile : dbFiles) {
            if (dbFile.isFile()
                    && dbFile.getName().equalsIgnoreCase(dbName + ".script")) {
                hasScriptFile = true;
            }
            if (dbFile.isFile()
                    && dbFile.getName().equalsIgnoreCase(dbName + ".properties")) {
                hasPropertiesFile = true;
            }
        }
        if (!hasScriptFile || !hasPropertiesFile) {
            throw new DataAccessResourceFailureException(t("sumaris.persistence.db.zip.badContent", DB_DIRECTORY));
        }

        return dbDirectory;
    }

    /**
     * Set collection items. Wille reuse the instance of the collection is possible
     *
     * @param existingEntities a {@link Collection} object.
     * @param function         a {@link Function} object.
     * @param vos              an array of V objects.
     * @param <E>              a E object.
     * @param <V>              a V object.
     */
    public static <E, V> void replaceEntities(Collection<E> existingEntities, V[] vos, Function<V, E> function) {
        Preconditions.checkNotNull(existingEntities);
        Collection<E> newEntities = Beans.transformCollection(Arrays.asList(vos), function);
        existingEntities.clear();
        existingEntities.addAll(newEntities);
    }

    /**
     * Set collection items. Wille reuse the instance of the collection is possible
     *
     * @param existingEntities a {@link Collection} object.
     * @param function         a {@link Function} object.
     * @param vos              a {@link Collection} object.
     * @param <E>              a E object.
     * @param <V>              a V object.
     */
    public static <E, V> void replaceEntities(Collection<E> existingEntities, Collection<V> vos, Function<V, E> function) {
        Preconditions.checkNotNull(existingEntities);
        Collection<E> newEntities = Beans.transformCollection(vos, function);
        existingEntities.clear();
        existingEntities.addAll(newEntities);
    }


    /**
     * <p>getDatabaseCurrentTimestamp.</p>
     *
     * @param connection a {@link Connection} object.
     * @param dialect    a {@link Dialect} object.
     * @return a {@link Timestamp} object.
     * @throws SQLException if any.
     */
    public static Timestamp getDatabaseCurrentTimestamp(Connection connection, Dialect dialect) throws SQLException {
        final String sql = dialect.getCurrentTimestampSelectString();
        Object result = Daos.sqlUniqueTyped(connection, sql);
        return toTimestampFromJdbcResult(result);
    }

    /**
     * <p>getDatabaseVersion.</p>
     *
     * @param connection a {@link Connection} object.
     * @return a {@link Version} object.
     * @throws SQLException if any.
     */
    public static Version getDatabaseVersion(Connection connection) throws SQLException {
        int majorVersion = connection.getMetaData().getDatabaseMajorVersion();
        int minorVersion = connection.getMetaData().getDatabaseMinorVersion();
        return Versions.valueOf(String.format("%d.%d", majorVersion, minorVersion));
    }

    /**
     * <p>convertToBigDecimal.</p>
     *
     * @param value       a {@link Double} object.
     * @param digitNumber a {@link Integer} object.
     * @return a {@link BigDecimal} object.
     */
    public static BigDecimal convertToBigDecimal(Number value, Integer digitNumber) {

        if (value == null) {
            return null;
        }

        int digitNb = digitNumber == null ? 0 : digitNumber;
        return new BigDecimal(String.format(Locale.US, "%." + digitNb + "f", value));
    }

    public static Timestamp toTimestampFromJdbcResult(Object source) throws SQLException {
        Object result = source;
        if (!(result instanceof Timestamp)) {
            if (result instanceof Date) {
                result = new Timestamp(((Date) result).getTime());
            } else if (result instanceof OffsetDateTime) {
                result = new Timestamp(((OffsetDateTime) result).atZoneSimilarLocal(ZoneOffset.UTC).toInstant().toEpochMilli());
            } else {
                throw new SQLException("Could not get database current timestamp. Invalid result (not a timestamp ?): " + result);
            }
        }
        return (Timestamp) result;
    }

    /* -- private methods  -- */


    public static void setTimezone(Connection connection, String timezone) throws SQLException {
        Preconditions.checkNotNull(timezone);

        setTimezone(connection, TimeZone.getTimeZone(timezone));
    }

    public static void setTimezone(Connection connection, TimeZone timezone) throws SQLException {
        Preconditions.checkNotNull(connection);
        Preconditions.checkNotNull(timezone);

        if (isHsqlDatabase(connection.getMetaData().getURL())) {

            int offset = timezone.getOffset(System.currentTimeMillis());

            StringBuffer sql = new StringBuffer()
                    .append("SET TIME ZONE INTERVAL '")
                    .append(offset < 0 ? "-" : "+")
                    .append(new SimpleDateFormat("hh:mm").format(new Date(Math.abs(offset))))
                    .append("' HOUR TO MINUTE;");
            PreparedStatement ps = connection.prepareStatement(sql.toString());
            ps.execute();
            ps.close();
        }
    }

}

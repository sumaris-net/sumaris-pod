package net.sumaris.extraction.core.dao.technical;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.collect.ImmutableMap;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.core.util.Dates;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class DaosPgsqlTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("pgsql");

    @Test
    public void getSqlToDate() {

        Date date = Dates.getFirstDayOfYear(2019);
        String sql = Daos.getSqlToDate(date);
        Assert.assertNotNull(sql);
        Assert.assertEquals("TO_DATE('2019-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS')", sql);
    }

    @Test
    public void getSelectHashCodeString() throws Exception {

        SumarisConfiguration config = SumarisConfiguration.getInstance();
        DatabaseType dbType = Daos.getDatabaseType(config.getJdbcURL());
        Assume.assumeNotNull(dbType);

        // Get the string
        String hashCodeString = Daos.getSelectHashCodeString(dbType, "%s");
        Assert.assertNotNull(hashCodeString);

        try (Connection conn = Daos.createConnection(config.getConnectionProperties())) {


            // Test not empty value
            {
                final String[] expressions = new String[] {
                        "aaaaaa",
                        "test ex",
                        "abcdef",
                        "abcdefg",
                        "abcdefgh",
                        "abcdefghi",
                        "abcdefghijkl",
                        "abcdefghijklmnopqrs",
                        "abcdefghijklmnopqrstuvxyz",
                        "abcdefghijklmnopqrstuvxyz".toUpperCase(),
                        "0123456789",
                        "#{[|^@]}ễ²¡÷×¿“~´\"éç'@^\\´& ",
                };
                for (String expression: expressions) {
                    Integer result = getDaoHashCode(conn, expression);
                    Assert.assertNotNull(result);

                    // Hsqldb function is equivalent to Java String.hashCode()
                    if (dbType == DatabaseType.hsqldb) {
                        Assert.assertEquals(String.format("Invalid hashCode for expresion '%s'. Expected: %s, Actual: %s", expression,
                                expression.hashCode(), result),
                                expression.hashCode(), result.intValue());
                    }
                }
            }

            // Test empty value
            {
                Integer result = getDaoHashCode(conn, "");
                Assert.assertNotNull(result);
                Assert.assertEquals(0, result.intValue());
            }

            // Test null value
            Assert.assertNull(getDaoHashCode(conn,null));
        }
    }

    @Test
    public void sqlReplaceColumnNames(){
        String sql = "SELECT\n" +
            "  TO_DATE(S.DATE || ' ' || S.TIME, 'YYYY-MM-DD HH24:MI') AS FISHING_TIME\n" +
            "  SUM(S.FISHING_TIME) AS TRIP_COUNT_BY_FISHING_TIME\n" +
            "  S.FISHING_TIME_NOT_REPLACED\n" +
            "FROM TRIP T\n" +
            "WHERE\n" +
            "  0 < S.FISHING_TIME";
        String expectedSql = "SELECT\n" +
            "  TO_DATE(S.FISHING_DATE || ' ' || S.TIME, 'YYYY-MM-DD HH24:MI') AS FISHING_DURATION\n" +
            "  SUM(S.FISHING_DURATION) AS TRIP_COUNT_BY_FISHING_TIME\n" +
            "  S.FISHING_TIME_NOT_REPLACED\n" +
            "FROM TRIP T\n" +
            "WHERE\n" +
            "  0 < S.FISHING_DURATION";
        String actualSql = Daos.sqlReplaceColumnNames(sql, ImmutableMap.of("date", "fishing_date",
            "fishing_time", "fishing_duration"));

        Assert.assertEquals(expectedSql, actualSql);
    }


    /* -- protected method -- */

    protected Integer getDaoHashCode(Connection conn, String expression) throws SQLException {

        DatabaseType dbType = Daos.getDatabaseType(conn.getMetaData().getURL());
        Assume.assumeNotNull(dbType);

        // Get the string
        String hashCodeString = Daos.getSelectHashCodeString(dbType, "%s");
        Assert.assertNotNull(hashCodeString);

        String quotedExpression = expression == null ? "null" : "'" + expression.replaceAll("'", "''") + "'";
        String sql = "SELECT "
                + String.format(hashCodeString, quotedExpression)
                + " HASH FROM status WHERE id=0";

        try (PreparedStatement ps = Daos.prepareQuery(conn, sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                Assert.assertTrue(rs.next());
                Integer result = rs.getObject("HASH", Integer.class);
                return result;
            }
        }
    }
}

package net.sumaris.core.dao.technical.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PostgresqlStatements {

    private static final String DISABLE_ALL_CONSTRAINTS_SQL = "set session_replication_role = replica;";

    private static final String ENABLE_ALL_CONSTRAINTS_SQL = "set session_replication_role = origin;";


    public static void setIntegrityConstraints(Connection connection, boolean enableIntegrityConstraints) throws SQLException {
        // Disable
        if (!enableIntegrityConstraints) {
            execute(connection, DISABLE_ALL_CONSTRAINTS_SQL);
        }

        // Enable
        else {
            execute(connection, ENABLE_ALL_CONSTRAINTS_SQL);
        }
    }

    private static void execute(Connection connection, String procedure) throws SQLException {
        PreparedStatement stat = connection.prepareStatement(procedure);
        stat.execute();
    }
}

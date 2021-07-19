package net.sumaris.core.dao.technical.jdbc;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2021 SUMARiS Consortium
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

package net.sumaris.core.dao;

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


import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * Abstract class for unit test on services.
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DaoTestConfiguration.class})
//@TestPropertySource(locations="classpath:sumaris-core-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public abstract class AbstractDaoTest extends net.sumaris.core.test.AbstractDaoTest {

	@Autowired
	protected SumarisConfiguration config;

	@Autowired
	protected DatabaseFixtures fixtures;

	/* -- Internal method -- */

	/**
	 * Delete all existing observations and linked data
	 * 
	 */
	protected void cleanData(Properties connectionProperties) {
		log.info("Cleaning all inserted data on database...");
		Connection connection = null;
		try {
			connection = Daos.createConnection(connectionProperties);

			executeSqlFile(connection, "sumaris-core-test-cleanInsertedData.sql");
		} catch (SQLException sqle) {
			log.error("Could not clean all inserted data on database", sqle);
			Assume.assumeNoException("Could not clean all inserted data on database", sqle);
		} finally {
			Daos.closeSilently(connection);
		}
	}

	/**
	 * Delete all existing referential tables (and progra/strat, rule...)
	 * 
	 */
	protected void cleanReferential(Properties connectionProperties) {
		log.info("Cleaning all inserted referential on database...");
		Connection connection = null;
		try {
			connection = Daos.createConnection(connectionProperties);

			executeSqlFile(connection, "sumaris-core-test-cleanInsertedData.sql");
		} catch (SQLException sqle) {
			log.error("Could not clean all inserted referential on database", sqle);
			Assume.assumeNoException("Could not clean all inserted referential on database", sqle);
		} finally {
			Daos.closeSilently(connection);
		}
	}

	/**
	 * Execute a SQL file
	 * 
	 * @param connection
	 * @param classpathFile
	 * @throws SQLException
	 *             if error during execution
	 */
	private void executeSqlFile(Connection connection, String classpathFile) throws SQLException {

		List<String> sqlLines;
		try {
			InputStream is = getClass().getClassLoader().getResourceAsStream(classpathFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			sqlLines = CharStreams.readLines(br);
		} catch (IOException ioe) {
			throw new AssertionError(String.format("Unable to read sql file '%s'", classpathFile));
		}

		boolean isAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);

		for (String sql : sqlLines) {
			if (StringUtils.isNotBlank(sql) && !sql.trim().startsWith("--")) {
				Daos.sqlUpdate(connection, sql);
			}
		}

		connection.setAutoCommit(isAutoCommit);
	}

	/**
	 * Execute a SQL file, and assume the execution is OK
	 * 
	 */
	protected void assumeExecuteSqlFile(Properties connectionProperties, String classpathFile) {
		log.info(String.format("Executing SQL file [%s]", classpathFile));
		Connection connection = null;
		try {
			connection = Daos.createConnection(connectionProperties);

			executeSqlFile(connection, classpathFile);
		} catch (SQLException e) {
			log.error("Could not execute SQL from file: " + classpathFile, e);
			Assume.assumeNoException("Could not execute SQL from file: " + classpathFile, e);
		} finally {
			Daos.closeSilently(connection);
		}
	}
}

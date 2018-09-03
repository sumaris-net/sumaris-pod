package net.sumaris.core.service;


import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.test.DatabaseResource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
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
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServiceTestConfiguration.class})
@TestPropertySource(locations="classpath:sumaris-core-test.properties")
public class AbstractServiceTest {

	/** Logger. */
	private static final Log log =
			LogFactory.getLog(AbstractServiceTest.class);

	@Autowired
	protected SumarisConfiguration config;

	/* -- Internal method -- */

	protected SumarisConfiguration getConfig() {
		return config;
	}

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

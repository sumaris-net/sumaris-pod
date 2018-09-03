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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

public class LoadDatabase {

	private static final Log log = LogFactory.getLog(LoadDatabase.class);

	private static Session session;

	private static String SQL_INSERT = "INSERT INTO ${table}(${keys}) VALUES(${values})";
	private static final String TABLE_REGEX = "\\$\\{table\\}";
	private static final String KEYS_REGEX = "\\$\\{keys\\}";
	private static final String VALUES_REGEX = "\\$\\{values\\}";

	public LoadDatabase(org.hibernate.SessionFactory sessionFactory) {
		super();
		session = sessionFactory.getCurrentSession();
	}

	public void run(File csvDirectory) throws Exception {

		if (!csvDirectory.exists() || !csvDirectory.isDirectory()) {
			throw new Exception(
					"You can only supply a directory to this LoadDatabase");
		}

		log.info("Retrieving list of csv files contained within :"
				+ csvDirectory.getName());
		CsvFilter csvFilter = new CsvFilter();
		File[] csvFiles = csvDirectory.listFiles(csvFilter);
		for (File csvFile : csvFiles) {
			loadDatabaseData(csvFile);
		}
	}

	/**
	 * This method assumes that Hibernate is set up already and can be loaded
	 * with data contained in the CSV file.
	 * 
	 * @param csvFile
	 * @throws Exception
	 */

	private void loadDatabaseData(File csvFile) throws Exception {
		String filename = csvFile.getName();
		log.info("Reading contents of :" + filename);

		String databaseName = filename.substring(0, filename.indexOf("."));
		log.info("Obtained database name: " + databaseName);

		BufferedReader in = new BufferedReader(new FileReader(csvFile));

		// First line will allways be the column names
		String keys = in.readLine();
		if (keys == null || keys.length() == 0) {
			throw new Exception("No columns defined in :" + filename);
		}

		// trailing garbage plus " from column names
		keys = keys.replaceAll("\"", "");
		String insertTemplate = SQL_INSERT.replaceFirst(TABLE_REGEX, databaseName);
		insertTemplate = insertTemplate.replaceFirst(KEYS_REGEX, keys);
		String values = null;
		while ((values = in.readLine()) != null) {
			values = values.replaceAll("\"", "\\\'");
			insertIntoDatabase(insertTemplate.replaceFirst(VALUES_REGEX, values));
		}

	}

	/**
	 * Calls hibernate and inserts the datainto the database
	 * 
	 * @param statement
	 */

	private void insertIntoDatabase(String statement) {
		log.debug(statement);
		session.beginTransaction();
		session.createSQLQuery(statement).executeUpdate();
		session.flush();
		log.debug("SQL SUCCEEDED");
	}

	/**
	 * Private class to allow only CSV files to be read. Could have been in a
	 * different class but thought it would be more portable this way.
	 * 
	 * @author leighgordy
	 * 
	 */

	private class CsvFilter implements FilenameFilter {

		public CsvFilter() {
		}

		public boolean accept(File dir, String name) {

			if (name.toLowerCase().endsWith(".csv")) {
				return true;
			} else {
				return false;
			}
		}

	}
}

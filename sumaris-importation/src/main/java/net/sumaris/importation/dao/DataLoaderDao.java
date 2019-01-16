package net.sumaris.importation.dao;

import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.importation.util.csv.FileReader;
import net.sumaris.importation.service.vo.DataLoadError;


import java.io.IOException;

/**
 * @author benoit.lavenier@e-is.pro
 * 
 * DAO in charge of data import
 * 
 */
public interface DataLoaderDao {


	/**
	 * Validate a file
	 * @param fileReader the file reader
	 * @param table the table to load
	 * @return
	 * @throws IOException
	 */
	DataLoadError[] validate(FileReader fileReader, DatabaseTableEnum table) throws IOException;

	/**
	 * Import a file into the database
	 * 
	 * @param fileReader
	 *            the file reader
	 * @param table the table to load
	 * @return 
	 * @throws IOException
	 */
	DataLoadError[] load(FileReader fileReader, DatabaseTableEnum table) throws IOException;

	/**
	 * Remove a file table
	 * @param userId
	 * @param table
	 * @param country The country to remove in data (optional: null=all data)
	 * @return number of lines removed
	 */
	int removeData(DatabaseTableEnum table, String[] filteredColumns, Object[] filteredValues);

}

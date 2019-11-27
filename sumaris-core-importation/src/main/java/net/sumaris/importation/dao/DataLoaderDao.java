package net.sumaris.importation.dao;

/*-
 * #%L
 * SUMARiS:: Core Importation
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

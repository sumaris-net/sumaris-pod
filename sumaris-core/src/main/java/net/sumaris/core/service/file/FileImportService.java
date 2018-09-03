package net.sumaris.core.service.file;

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


import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.vo.file.ValidationErrorVO;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;

/**
 * @author BLA
 * 
 *    Service in charge of importing csv file into DB
 * 
 */
@Transactional
public interface FileImportService {

	/**
	 * Import a file into thedatabase
	 * 
	 * @param userId
	 *            id of connected user 
	 * @param country the country data to import (null=all countries)
	 * @return 
	 * @throws IOException
	 */
	void importFile(int userId, File inputFile, DatabaseTableEnum table, String country, boolean validate) throws IOException,
			FileValidationException;

	/**
	 * Validate file format
	 * @param userId
	 * @param inputFile
	 * @param table
	 * @return
	 * @throws IOException
	 */
	ValidationErrorVO[] validateFile(int userId, File inputFile, DatabaseTableEnum table) throws IOException;

}

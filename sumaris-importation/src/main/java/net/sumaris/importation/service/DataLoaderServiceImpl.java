package net.sumaris.importation.service;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.util.Files;
import net.sumaris.importation.dao.DataLoaderDao;
import net.sumaris.importation.exception.FileValidationException;
import net.sumaris.importation.service.vo.DataLoadError;
import net.sumaris.importation.util.csv.CSVFileReader;
import net.sumaris.importation.util.csv.FileReader;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

import static net.sumaris.importation.service.vo.DataLoadError.ErrorType;

@Service("dataLoaderService")
@Slf4j
public class DataLoaderServiceImpl implements DataLoaderService {

	@Autowired
	protected SumarisConfiguration config;

	@Autowired
	protected SumarisDatabaseMetadata databaseMetadata;

	@Autowired
	protected DataLoaderDao dao;

	@Override
	public void remove(DatabaseTableEnum table, String[] filteredColumns, Object[] filteredValues) {

		if (ArrayUtils.isNotEmpty(filteredColumns) || ArrayUtils.isNotEmpty(filteredValues)) {
			dao.removeData(table, filteredColumns, filteredValues);
		} else {
			dao.removeData(table, null, null);
		}
	}

	@Override
	public void load(File inputFile, DatabaseTableEnum table, boolean validate) throws IOException,
			FileValidationException {
		DataLoadError[] errors;
		Files.checkExists(inputFile);

		// Validate if need
		if (validate) {
			validate(inputFile, table);
		}

		// Creating a new reader
		FileReader reader = new CSVFileReader(inputFile, true);

		try {
			errors = dao.load(reader, table);

			// Stop if result KO
			if (hasErrorOrFatal(errors)) throw new FileValidationException(errors);
		}
		finally {
			reader.close();
		}
	}

	@Override
	public void validate(File inputFile, DatabaseTableEnum table) throws IOException, FileValidationException {
		Files.checkExists(inputFile);

		FileReader reader = new CSVFileReader(inputFile, true);

		try {
			DataLoadError[] errors = dao.validate(reader, table);
			// Stop if result KO
			if (hasErrorOrFatal(errors)) throw new FileValidationException(errors);
		}
		finally {
			reader.close();
		}
	}

	/* -- protected methods -- */

	protected boolean hasErrorOrFatal(DataLoadError[] validationDataLoadErrors){
		if (validationDataLoadErrors != null && validationDataLoadErrors.length > 0) {
			for (DataLoadError error : validationDataLoadErrors) {
				if (error.getErrorType() == ErrorType.ERROR
						|| error.getErrorType() == ErrorType.FATAL) {
					return true;
				}
			}
		}
		return false;
	}
}

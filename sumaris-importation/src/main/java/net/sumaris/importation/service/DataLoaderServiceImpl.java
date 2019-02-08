package net.sumaris.importation.service;

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.util.file.FileUtils;
import net.sumaris.importation.dao.DataLoaderDao;
import net.sumaris.importation.exception.FileValidationException;
import net.sumaris.importation.util.csv.CSVFileReader;
import net.sumaris.importation.util.csv.FileReader;
import static net.sumaris.importation.service.vo.DataLoadError.*;
import net.sumaris.importation.service.vo.DataLoadError;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service("dataLoaderService")
public class DataLoaderServiceImpl implements DataLoaderService {

	protected static final Logger log = LoggerFactory.getLogger(DataLoaderServiceImpl.class);

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
		FileUtils.checkExists(inputFile);

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
		FileUtils.checkExists(inputFile);

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

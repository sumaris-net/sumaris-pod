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


import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisHibernateColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.vo.ErrorType;
import net.sumaris.core.vo.file.ValidationErrorVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@Service("fileImportService")
@Slf4j
public class FileImportServiceImpl implements FileImportService {

	//@Autowired
	//protected FileImportDao fileImportDao;

	@Override
	public void importFile(int userId, File inputFile, DatabaseTableEnum table, String country, boolean validate) throws IOException,
			FileValidationException {
		Preconditions.checkNotNull(inputFile);

		if (inputFile.exists() == false) {
			throw new FileNotFoundException("File not exists: " + inputFile.getAbsolutePath());
		}

		if (StringUtils.isNotBlank(country)) {
			//fileImportDao.removeData(userId, table, new String[] { "COUNTRY_CODE_VESSEL" }, new String[] { country });
		} else {
			//fileImportDao.removeData(userId, table, null, null);
		}

		ValidationErrorVO[] errors = new ValidationErrorVO[0];
		// FIXME: call dao
		// ValidationErrorVO[] errors = fileImportDao.importFile(userId, inputFile, table, validate);

		if (errors != null && errors.length > 0) {
			boolean hasErrorOrFatal = false;
			for (ValidationErrorVO error : errors) {
				if (error.getErrorType() == ErrorType.ERROR
						|| error.getErrorType() == ErrorType.FATAL) {
					hasErrorOrFatal = true;
					break;
				}
			}
			if (hasErrorOrFatal) {
				throw new FileValidationException(errors);
			}
		}
	}

	@Override
	public ValidationErrorVO[] validateFile(int userId, File inputFile, DatabaseTableEnum table) throws IOException {

		if (inputFile.exists() == false) {
			throw new FileNotFoundException("File not exists: " + inputFile.getAbsolutePath());
		}

		return null;
		//FIXME
		//return fileImportDao.validateFile(userId, inputFile, table);
	}

	protected String getLogPrefix(SumarisTableMetadata table, SumarisHibernateColumnMetadata colMeta, int lineNumber) {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(table.getName());
		if (colMeta != null) {
			sb.append(".").append(colMeta.getName());
		}
		if (lineNumber != -1) {
			sb.append(" / ").append(lineNumber);
		}
		sb.append("] ");
		return sb.toString();

	}
}

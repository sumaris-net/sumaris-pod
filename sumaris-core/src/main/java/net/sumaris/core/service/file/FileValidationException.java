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


import net.sumaris.core.vo.file.ValidationErrorVO;

public class FileValidationException extends Exception {

	private static final long serialVersionUID = 4369710207426419207L;

	protected final ValidationErrorVO[] fileValidationErrors;

	protected final static int MAX_ERRORS_IN_DESCRIPTION = 100;

	public FileValidationException(ValidationErrorVO[] fileValidationErrors) {
		this.fileValidationErrors = fileValidationErrors;
	}

	public ValidationErrorVO[] getFileValidationErrors() {
		return fileValidationErrors;
	}

	@Override
	public String getMessage() {
		String baseMessage = "Error during file validation";
		if (fileValidationErrors == null) {
			return baseMessage;
		}

		StringBuilder sb = new StringBuilder(baseMessage);
		sb.append(":");
		int errorCount = 0;
		for (ValidationErrorVO error : fileValidationErrors) {
			if (errorCount == MAX_ERRORS_IN_DESCRIPTION) {
				sb.append("\n\t(...)");
				break;
			}
			errorCount++;
			sb.append("\n\t");
			sb.append(error.getErrorType().name());
			sb.append(" - ");
			sb.append(error.getDescription());
		}
		return sb.toString();
	}
}

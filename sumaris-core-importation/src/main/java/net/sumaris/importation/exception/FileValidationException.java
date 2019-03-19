package net.sumaris.importation.exception;

import net.sumaris.importation.service.vo.DataLoadError;

public class FileValidationException extends Exception {

	private static final long serialVersionUID = 4369710207426419207L;

	protected final DataLoadError[] fileValidationDataLoadErrors;

	protected final static int MAX_ERRORS_IN_DESCRIPTION = 100;

	public FileValidationException(DataLoadError[] fileValidationDataLoadErrors) {
		this.fileValidationDataLoadErrors = fileValidationDataLoadErrors;
	}

	public DataLoadError[] getFileValidationDataLoadErrors() {
		return fileValidationDataLoadErrors;
	}

	@Override
	public String getMessage() {
		String baseMessage = "Error during file validation";
		if (fileValidationDataLoadErrors == null) {
			return baseMessage;
		}

		StringBuilder sb = new StringBuilder(baseMessage);
		sb.append(":");
		int errorCount = 0;
		for (DataLoadError error : fileValidationDataLoadErrors) {
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

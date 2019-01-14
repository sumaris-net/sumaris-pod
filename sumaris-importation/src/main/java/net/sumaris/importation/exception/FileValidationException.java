package net.sumaris.importation.exception;

import net.sumaris.importation.vo.ValidationErrorVO;

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

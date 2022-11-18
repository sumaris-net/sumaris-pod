/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.importation.core.util.csv;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.schema.SumarisHibernateColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.util.type.SequenceIterator;
import net.sumaris.importation.core.service.vo.DataLoadError;
import net.sumaris.importation.core.service.vo.DataLoadResult;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.springframework.dao.DataIntegrityViolationException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

import static net.sumaris.importation.core.service.vo.DataLoadError.Builder;
import static net.sumaris.importation.core.service.vo.DataLoadError.ErrorType;

@Slf4j
public class CSVFileReader implements FileReader {

	public final static int MAX_LOG_ERRORS = 500;

	public final static char[] CSV_SEPARATORS = { ';', ',', '\t' };

	protected CSVReader delegate;

	protected FileInputStream fis;
	protected BOMInputStream ubis;
	protected InputStreamReader isr;
	protected BufferedReader br;

	protected DataLoadResult result;

	protected String[] headers;

	protected String[] rowBuffer;

	protected SequenceIterator lineCounter;

	protected boolean ignoreEmptyRow;

	protected String filename;

	protected char separator;

	// TODO : not used yet : used to know if a row is empty
	protected String emptyCellsRow;

	public CSVFileReader(File inputFile, boolean ignoreEmptyRow) throws IOException {
		init(inputFile, ignoreEmptyRow, true, null);
	}

	public CSVFileReader(File inputFile, boolean ignoreEmptyRow, boolean hasHeaders) throws IOException {
		init(inputFile, ignoreEmptyRow, hasHeaders, null);
	}

	public CSVFileReader(File inputFile, boolean ignoreEmptyRow, boolean hasHeaders, String encodingName)
			throws IOException {
		init(inputFile, ignoreEmptyRow, hasHeaders, encodingName);
	}

	@Override
	public String getFileName() {
		return filename;
	}

	public char getSeparator() {
		return separator;
	}

	protected void init(File inputFile, boolean ignoreEmptyRow, boolean hasHeaders, String encodingName)
			throws IOException {
		Preconditions.checkNotNull(inputFile);
		if (inputFile.exists() == false) {
			throw new FileNotFoundException("File not exists: " + inputFile.getAbsolutePath());
		}
		if (log.isDebugEnabled()) {
			log.debug("Initialize CSV Reader for file: " + inputFile.getPath());
		}

		this.ignoreEmptyRow = ignoreEmptyRow;
		this.filename = inputFile.getName();
		this.result = new DataLoadResult();
		lineCounter = new SequenceIterator();

		this.rowBuffer = null;
		this.headers = null;
		for (char separator : CSV_SEPARATORS) {

			fis = new FileInputStream(inputFile);
			ubis = new BOMInputStream(fis);
			if (encodingName != null) {
				isr = new InputStreamReader(ubis, Charset.forName(encodingName));
			} else {
				isr = new InputStreamReader(ubis);
			}
			br = new BufferedReader(isr);
			delegate = new CSVReader(br, separator, '\"', 0);

			// Read column headers :
			rowBuffer = readNext();
			if (rowBuffer.length == 1) {
				close();
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Will use detected separator: " + (separator == '\t' ? "TAB" : separator));
				}
				this.separator = separator;
				this.emptyCellsRow = StringUtils.join(new String[this.rowBuffer.length], separator);
				break;
			}
		}

		if (hasHeaders) {
			if (rowBuffer == null) {
				addError(null,
						ErrorType.FATAL,
						"NO_HEADER",
						I18n.t("import.validation.error.NO_HEADER", null, null));

				throw new DataIntegrityViolationException("No valid headers found in file: " + inputFile.getPath());
			} else {
				this.headers = rowBuffer;
				this.rowBuffer = null;
			}
		}
	}

	public String[] readNext() throws IOException {
		String[] cols = null;
		if (rowBuffer != null && rowBuffer.length > 0) {
			cols = rowBuffer;
			rowBuffer = null;
			return cols;
		}
		if (!ignoreEmptyRow) {
			// Read the next line
			cols = delegate.readNext();

			// Increment line number
			lineCounter.next();
		} else {
			do {
				// Read the next line
				cols = delegate.readNext();

				// Increment line number
				lineCounter.next();

				// If end of file, return null
				if (cols == null || cols.length == 0) {
					return null;
				}
			} while (cols.length == 1
					&& cols[0].trim().length() == 0);
		}

		// Ensure cols length is equals (or greater) than headers length 
		// to avoid IndexOutOfRangeException
		if (headers != null && cols.length != headers.length) {
			return Arrays.copyOf(cols, headers.length);
		}

		return cols;
	}

	/**
	 * Add a error only once (will log the record only the first call).
	 * The errorCode is used as an unique key
	 * @param tableMetadata
	 * @param colMeta
	 * @param columnNumber
	 * @param errorType
	 * @param errorCode
	 * @param description
	 */
	protected void addErrorOnce(
            SumarisTableMetadata tableMetadata, SumarisHibernateColumnMetadata colMeta,
            int columnNumber,
            ErrorType errorType,
            String errorCode, String description) {
		addError(createError(tableMetadata, colMeta, columnNumber, errorType, errorCode, description), true);
	}

	protected void addError(
            SumarisTableMetadata tableMetadata, SumarisHibernateColumnMetadata colMeta,
            int columnNumber,
            ErrorType errorType,
            String errorCode, String description) {

		addError(createError(tableMetadata, colMeta, columnNumber, errorType, errorCode, description),
				false);
	}

	protected void addError(DataLoadError error, boolean onlyOnce) {

		if (onlyOnce) {
			result.addErrorOnce(error);
		}
		else {
			result.addError(error);
		}

		if (result.errorCount() < MAX_LOG_ERRORS) {

			// log
			switch (error.getErrorType()) {
				case WARNING:
					log.warn(error.getDescription());
					break;
				case ERROR:
					log.error(error.getDescription());
					break;
				case FATAL:
					log.error(error.getDescription());
					break;
			}
		}
	}

	protected DataLoadError createError(
            SumarisTableMetadata tableMetadata, SumarisHibernateColumnMetadata colMeta,
            int columnNumber,
            ErrorType errorType,
            String errorCode, String description) {

		DataLoadError error = Builder.create(tableMetadata, colMeta, lineCounter.getCurrentValue(), description)
				.setColumnNumber(columnNumber != -1 ? columnNumber : null)
				.setErrorCode(errorCode)
				.setErrorType(errorType)
				.build();

		return error;
	}

	protected void addError(Integer columnIndex,
							ErrorType errorType,
							String errorCode,
							String description) {
		Preconditions.checkArgument(columnIndex == null || columnIndex.intValue() >= 0);

		DataLoadError error = new DataLoadError();

		error.setLineNumber(lineCounter.getCurrentValue());
		error.setColumnNumber(columnIndex);
		String fullDescription = null;
		if (columnIndex != null
				&& headers != null
				&& columnIndex.intValue() < headers.length) {
			error.setColumnName(headers[columnIndex]);

			fullDescription = String.format("[%s:%s - %s] %s", filename, lineCounter.getCurrentValue(), headers[columnIndex], description);
		} else {
			fullDescription = String.format("[%s:%s] %s", filename, lineCounter.getCurrentValue(), description);
		}
		error.setDescription(fullDescription);
		error.setErrorCode(errorCode);
		error.setErrorType(errorType);

		addError(error, false);
	}

	@Override
	public void close() throws IOException {
		delegate.close();
		br.close();
		isr.close();
		ubis.close();
		fis.close();

		lineCounter = new SequenceIterator();
		rowBuffer = null;
		emptyCellsRow = null;
	}

	public DataLoadResult getResult() {
		return result;
	}

	public String[] getHeaders() {
		return headers;
	}

	public boolean isIgnoreEmptyRow() {
		return ignoreEmptyRow;
	}

	public void setIgnoreEmptyRow(boolean ignoreEmptyRow) {
		this.ignoreEmptyRow = ignoreEmptyRow;
	}

	public int getCurrentLine() {
		return lineCounter.getCurrentValue();
	}


}

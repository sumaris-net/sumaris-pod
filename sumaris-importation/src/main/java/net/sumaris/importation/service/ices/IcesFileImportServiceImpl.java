package net.sumaris.importation.service.ices;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.model.SumarisTable;
import net.sumaris.importation.exception.FileValidationException;
import net.sumaris.importation.service.FileImportService;
import net.sumaris.importation.vo.ErrorType;
import net.sumaris.importation.vo.ValidationErrorVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.tool.hbm2ddl.ColumnMetadata;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@Service("rdbV1FileImportService")
public class RdbV1FileImportServiceImpl implements FileImportService {

	protected static final Log log = LogFactory.getLog(RdbV1FileImportServiceImpl.class);

	//protected List<FileColumnValueListener> listeners = Lists.newArrayList();
	//protected Map<String, FileColumnValueListener> listenerByColumnMap = Maps.newHashMap();

	//@Autowired
	//protected FileImportDao fileImportDao;

	@Override
	public void importFile(int userId, File inputFile, SumarisTable table, String country, boolean validate, boolean appendData) throws IOException,
			FileValidationException {
		Preconditions.checkNotNull(inputFile);

		if (inputFile.exists() == false) {
			throw new FileNotFoundException("File not exists: " + inputFile.getAbsolutePath());
		}

		// If not append : then remove old data
		if (!appendData) {

			if (StringUtils.isNotBlank(country)) {
				//TODO  fileImportDao.removeData(userId, table, new String[] { "COUNTRY_CODE_VESSEL" }, new String[] { country });
			} else {
				//TODO  fileImportDao.removeData(userId, table, null, null);
			}
		}

		ValidationErrorVO[] errors = null; // TODO fileImportDao.importFile(userId, inputFile, table, validate);

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
	public ValidationErrorVO[] validateFile(int userId, File inputFile, SumarisTable table) throws IOException {

		if (inputFile.exists() == false) {
			throw new FileNotFoundException("File not exists: " + inputFile.getAbsolutePath());
		}

		//TODO  return fileImportDao.validateFile(userId, inputFile, table);
		return null;
	}

	protected String getLogPrefix(SumarisTableMetadata table, ColumnMetadata colMeta, int lineNumber) {
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

//	@Override
//	public void addColumnValueListener(FileColumnValueListener listener) {
//		listeners.add(listener);
//	}
//
//	@Override
//	public void removeColumnValueListener(FileColumnValueListener listener) {
//		listeners.remove(listener);
//		if (listenerByColumnMap.containsValue(listener)) {
//			for (String key : listenerByColumnMap.keySet()) {
//				if (listener.equals(listenerByColumnMap.get(key))) {
//					listenerByColumnMap.remove(key);
//				}
//			}
//		}
//	}
//
//	@Override
//	public void addColumnValueListener(String tableName, String columnName, FileColumnValueListener listener) {
//		Preconditions.checkNotNull(tableName);
//		Preconditions.checkNotNull(columnName);
//		Preconditions.checkNotNull(listener);
//		listeners.remove(listener);
//		listenerByColumnMap.put(tableName + "." + columnName, listener);
//	}
}

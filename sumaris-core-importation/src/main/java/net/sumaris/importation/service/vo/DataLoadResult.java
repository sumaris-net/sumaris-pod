package net.sumaris.importation.service.vo;

import com.google.common.collect.Lists;

import java.util.List;

public class DataLoadResult {

    public final static int MAX_ERRORS_COUNT = 1000;

    protected List<DataLoadError> errors = Lists.newArrayList();
    protected List<String> errorsOnceList = Lists.newArrayList();

    public void addError(DataLoadError error) {
        if (errors.size() < MAX_ERRORS_COUNT) {
            errors.add(error);
        }
    }

    public void addErrorOnce(DataLoadError error) {
        if (errorsOnceList.contains(error.getErrorCode())) {
            return;
        }

        errorsOnceList.add(error.getErrorCode());
        addError(error);
    }

    public DataLoadError[] getErrors(){
        return this.errors.toArray(new DataLoadError[errors.size()]);
    }

    public int errorCount() {
        return errors.size();
    }

    public boolean isSuccess(){
        if (errors.size() > 0) {
            for (DataLoadError error : errors) {
                if (error.getErrorType() == DataLoadError.ErrorType.ERROR
                        || error.getErrorType() == DataLoadError.ErrorType.FATAL) {
                    return false;
                }
            }
        }
        return true;
    }

}

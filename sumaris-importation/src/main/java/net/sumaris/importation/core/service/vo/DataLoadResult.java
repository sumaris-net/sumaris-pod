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

package net.sumaris.importation.core.service.vo;

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

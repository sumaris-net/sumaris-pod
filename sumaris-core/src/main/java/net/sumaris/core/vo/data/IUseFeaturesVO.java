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

package net.sumaris.core.vo.data;

import net.sumaris.core.model.data.IUseFeaturesEntity;
import net.sumaris.core.model.data.IWithDataQualityEntity;
import net.sumaris.core.model.data.IWithProgramEntity;
import net.sumaris.core.model.data.IWithVesselEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;

public interface IUseFeaturesVO extends
    IDataVO<Integer>,
    IWithVesselSnapshotVO<Integer>,
    IWithProgramEntity<Integer, ProgramVO>,
    IWithDataQualityEntity<Integer, Integer>,
    IWithMeasurementValues
{

    Date getStartDate();
    void setStartDate(Date value);
    Date getEndDate();
    void setEndDate(Date value);

    Integer getVesselId();
    void setVesselId(Integer value);

    ProgramVO getProgram();
    void setProgram(ProgramVO value);

    String getComments();
    void setComments(String value);

    Date getCreationDate();
    void setCreationDate(Date value);

    Integer getRecorderDepartmentId();
    void setRecorderDepartmentId(Integer value);

    Integer getRecorderPersonId();
    void setRecorderPersonId(Integer value);

    List<DataOriginVO> getDataOrigins();
    void setDataOrigins(List<DataOriginVO> value);

    @Override
    default DepartmentVO getRecorderDepartment() {
        if (this.getRecorderDepartmentId() == null) return null;

        DepartmentVO result = new DepartmentVO();
        result.setId(this.getRecorderDepartmentId());
        return result;
    }

    @Override
    default void setRecorderDepartment(DepartmentVO recorderDepartment) {
        if (recorderDepartment == null || recorderDepartment.getId() == null) {
            this.setRecorderDepartmentId(null);
        }
        else {
            this.setRecorderDepartmentId(recorderDepartment.getId());
        }
    }
}
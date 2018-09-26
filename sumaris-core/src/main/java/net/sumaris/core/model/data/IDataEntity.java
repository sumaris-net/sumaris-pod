package net.sumaris.core.model.data;

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

import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.QualityFlag;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public interface IDataEntity<T extends Serializable> extends Serializable, IUpdateDateEntityBean<T, Date> {

    int LENGTH_COMMENTS = 2000;

    String PROPERTY_CONTROL_DATE = "controlDate";
    String PROPERTY_QUALIFICATION_DATE = "qualificationDate";
    String PROPERTY_QUALITY_FLAG = "qualityFlag";
    String PROPERTY_RECORDER_DEPARTMENT = "recorderDepartment";

    Department getRecorderDepartment();

    void setRecorderDepartment(Department recorderDepartment);

    Date getControlDate();

    void setControlDate(Date controlDate);

    Date getQualificationDate();

    void setQualificationDate(Date qualificationDate);

    QualityFlag getQualityFlag();

    void setQualityFlag(QualityFlag qualityFlag);
}

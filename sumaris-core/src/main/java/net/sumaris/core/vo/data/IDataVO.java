package net.sumaris.core.vo.data;

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
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.vo.IValueObject;
import net.sumaris.core.vo.administration.user.DepartmentVO;

import java.io.Serializable;
import java.util.Date;

public interface IDataVO<ID extends Serializable>
        extends IValueObject<ID>,
        IUpdateDateEntityBean<ID, Date>,
        IWithRecorderDepartmentEntity<ID, DepartmentVO> {


    String PROPERTY_CONTROL_DATE = "controlDate";
    String PROPERTY_QUALIFICATION_DATE = "qualificationDate";
    String PROPERTY_QUALITY_FLAG_ID = "qualityFlagId";
    String PROPERTY_QUALIFICATION_COMMENTS = "qualificationComments";


    Date getControlDate();

    void setControlDate(Date controlDate);

    Date getQualificationDate();

    void setQualificationDate(Date qualificationDate);

    Integer getQualityFlagId();

    void setQualityFlagId(Integer qualityFlagId);

    String getQualificationComments();

    void setQualificationComments(String qualificationComments);
}

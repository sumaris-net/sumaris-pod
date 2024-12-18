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

import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IUpdateDateEntity;

import java.io.Serializable;
import java.util.Date;

public interface IWithDataQualityEntity<ID extends Serializable, QF extends Serializable>
        extends IEntity<ID> {

    interface Fields extends IEntity.Fields {
        String CONTROL_DATE = "controlDate";
        String QUALIFICATION_DATE = "qualificationDate";
        String QUALITY_FLAG = "qualityFlag";
        String QUALIFICATION_COMMENTS = "qualificationComments";
    }

    Date getControlDate();

    void setControlDate(Date controlDate);

    Date getQualificationDate();

    void setQualificationDate(Date qualificationDate);

    QF getQualityFlag();

    void setQualityFlag(QF qualityFlag);

    String getQualificationComments();

    void setQualificationComments(String qualificationComments);
}

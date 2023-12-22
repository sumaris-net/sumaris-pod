package net.sumaris.core.model.data;

/*-
 * #%L
 * SUMARiS:: Core shared
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
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.referential.QualityFlag;

import java.io.Serializable;
import java.util.Date;

public interface IUseFeaturesEntity extends
    IDataEntity<Integer>,
    IWithVesselEntity<Integer, Vessel>,
    IWithProgramEntity<Integer, Program> {

    interface Fields extends IDataEntity.Fields, IWithVesselEntity.Fields, IWithProgramEntity.Fields, IWithValidationDateEntity.Fields {
        String END_DATE = "endDate";
        String START_DATE = "startDate";
        String CREATION_DATE = "creationDate";
    }

    Date getStartDate();

    void setStartDate(Date startDate);

    Date getEndDate();

    void setEndDate(Date endDate);

    Date getCreationDate();

    void setCreationDate(Date creationDate);
}

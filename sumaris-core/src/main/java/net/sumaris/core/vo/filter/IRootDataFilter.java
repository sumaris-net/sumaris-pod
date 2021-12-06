package net.sumaris.core.vo.filter;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import net.sumaris.core.model.data.DataQualityStatusEnum;

import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public interface IRootDataFilter extends IDataFilter {

    Integer getRecorderPersonId();

    void setRecorderPersonId(Integer recorderPersonId);

    Date getStartDate();

    void setStartDate(Date startDate);

    Date getEndDate();

    void setEndDate(Date endDate);

    String getProgramLabel();

    void setProgramLabel(String programLabel);

    Integer getLocationId();

    void setLocationId(Integer locationId);

    DataQualityStatusEnum[] getDataQualityStatus();

    void setDataQualityStatus(DataQualityStatusEnum[] dataQualityStatus);

    Integer[] getProgramIds();

    void setProgramIds(Integer[] programIds);
}

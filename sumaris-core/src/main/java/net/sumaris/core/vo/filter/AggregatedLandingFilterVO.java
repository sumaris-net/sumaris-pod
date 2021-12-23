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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;

import java.util.Date;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class AggregatedLandingFilterVO implements IRootDataFilter {

    private Date startDate;
    private Date endDate;
    private String programLabel;
    private Integer recorderDepartmentId;
    private Integer recorderPersonId;
    private Integer locationId;

    // Parent
    private Integer observedLocationId;
    private Integer[] programIds;

    // Quality
    private Integer[] qualityFlagIds; // NOT used
    private DataQualityStatusEnum[] dataQualityStatus; // NOT used
}

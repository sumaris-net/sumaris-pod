package net.sumaris.core.vo.filter;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.util.Beans;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class OperationFilterVO implements IDataFilter {

    public static OperationFilterVO nullToEmpty(OperationFilterVO f) {
        return f != null ? f : new OperationFilterVO();
    }

    private Integer tripId;
    private Integer recorderDepartmentId;
    private Integer vesselId;
    private Integer[] physicalGearIds;
    private String programLabel;
    private Integer[] includedIds;
    private Integer[] excludedIds;
    private Boolean excludeChildOperation;
    private Boolean hasNoChildOperation;
    private Date startDate;
    private Date endDate;
    private Integer[] gearIds;
    private String[] taxonGroupLabels;
    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;
    private Integer[] boundingBox;
    private Boolean needBatchDenormalization;

    @JsonIgnore
    public OperationFilterVO clone() {
        return Beans.clone(this, OperationFilterVO.class);
    }
}

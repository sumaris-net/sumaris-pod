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

package net.sumaris.core.vo.data.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.DataQualityStatusEnum;
import net.sumaris.core.vo.filter.IDataFilter;

/**
 * @author peck7 on 01/09/2020.
 */
@Data
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchFilterVO implements IDataFilter {

    public static BatchFilterVO nullToEmpty(BatchFilterVO f) {
        return f != null ? f : new BatchFilterVO();
    }

    private Integer operationId;
    private Integer saleId;
    private Integer recorderDepartmentId;

    // Quality
    private Integer[] qualityFlagIds;
    private DataQualityStatusEnum[] dataQualityStatus;
}

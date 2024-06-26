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

package net.sumaris.core.service.data.denormalize;

import lombok.Builder;
import lombok.Data;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.vo.technical.job.IJobResultVO;

import java.io.Serializable;

@Data
@Builder
public class DenormalizedTripResultVO implements IJobResultVO, Serializable {

    private long tripCount;
    private long operationCount;
    private long batchCount;

    private long tripErrorCount;
    private long invalidBatchCount;
    private long executionTime;

    private String message;

    private JobStatusEnum status;
}

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

package net.sumaris.core.vo.data.vessel;

import lombok.Builder;
import lombok.Data;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.vo.technical.job.IJobResultVO;

import java.util.Date;

@Data
@Builder
public class UpdateVesselSnapshotsResultVO implements IJobResultVO {

    private long inserts;
    private long updates;
    private long deletes;
    private Integer errors;

    private long vessels;

    private String message;

    private Date filterStartDate;

    private Date filterMinUpdateDate;

    private JobStatusEnum status;

    public boolean hasError() {
        return errors != null && errors > 0;
    }
}

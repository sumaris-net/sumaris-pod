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

package net.sumaris.importation.core.service.vessel.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sumaris.core.model.technical.job.JobStatusEnum;
import net.sumaris.core.vo.technical.job.IJobResultVO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiopVesselImportResultVO implements IJobResultVO {

    private Integer inserts;
    private Integer updates;
    private Integer disables;
    private Integer warnings;

    private Integer errors;

    private String message;

    private JobStatusEnum status;

    public boolean hasError() {
        return this.errors != null && this.errors > 0;
    }
}

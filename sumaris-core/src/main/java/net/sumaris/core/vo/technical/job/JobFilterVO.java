package net.sumaris.core.vo.technical.job;

/*-
 * #%L
 * Quadrige3 Core :: Model Shared
 * %%
 * Copyright (C) 2017 - 2020 Ifremer
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
import net.sumaris.core.model.technical.job.JobStatusEnum;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Date;

@Data
@FieldNameConstants
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobFilterVO implements Serializable {

    @Nonnull
    public static JobFilterVO nullToDefault(JobFilterVO filter) {
        return filter != null ? filter : JobFilterVO.builder().build();
    }

    private Integer id;
    private String issuer;
    private String issuerEmail;

    @Builder.Default
    private String[] types = null;

    @Builder.Default
    private JobStatusEnum[] status = null;

    @Builder.Default
    private Integer[] includedIds = null;
    @Builder.Default
    private Integer[] excludedIds = null;

    private Date lastUpdateDate;
    private Date startedBefore;

}

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

package net.sumaris.core.event.job;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IProgressionModel;
import net.sumaris.core.model.IValueObject;

/**
 * Job progression
 */
@Data
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@ToString
public class JobProgressionVO implements IValueObject<Integer> {

    public static JobProgressionVO fromModel(IProgressionModel progression) {
        JobProgressionVO result = new JobProgressionVO();
        result.setMessage(progression.getMessage());
        result.setCurrent(progression.getCurrent());
        result.setTotal(progression.getTotal());
        return result;
    }

    @EqualsAndHashCode.Include
    private Integer id; // = job.id
    private String name;
    private String message;
    private long current;
    private long total;

}

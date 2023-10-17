package net.sumaris.core.model.administration.programStrategy;

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

import lombok.Getter;
import lombok.NonNull;
import net.sumaris.core.model.annotation.EntityEnum;
import net.sumaris.core.model.annotation.IEntityEnum;

import java.io.Serializable;
import java.util.Arrays;

@Getter
@EntityEnum(entity = AcquisitionLevel.class,
    resolveAttributes = AcquisitionLevel.Fields.LABEL)
public enum AcquisitionLevelEnum implements IEntityEnum, Serializable {

    TRIP(1, "TRIP"),
    OPERATION(3, "OPERATION"),
    CHILD_OPERATION(20, "CHILD_OPERATION"),
    PHYSICAL_GEAR(2, "PHYSICAL_GEAR"),
    CATCH_BATCH(4, "CATCH_BATCH"),
    SORTING_BATCH(5, "SORTING_BATCH"),
    SORTING_BATCH_INDIVIDUAL(6, "SORTING_BATCH_INDIVIDUAL"),

    /**
     * @deprecated use SAMPLE instead
     */
    @Deprecated
    SURVIVAL_TEST(7, "SURVIVAL_TEST"),
    INDIVIDUAL_MONITORING(8, "INDIVIDUAL_MONITORING"),
    SAMPLE(12, "SAMPLE"),
    OBSERVED_LOCATION(10, "OBSERVED_LOCATION")
    ;

    private Integer id;
    private String label;

    AcquisitionLevelEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    public static AcquisitionLevelEnum valueOf(final int id) {
        for (AcquisitionLevelEnum v : values()) {
            if (v.id == id) return v;
        }
        throw new IllegalArgumentException("Unknown AcquisitionLevelEnum: " + id);
    }

    public static AcquisitionLevelEnum byLabel(@NonNull final String label) {
        return Arrays.stream(values())
            .filter(level -> label.equals(level.label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown AcquisitionLevelEnum: " + label));
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}

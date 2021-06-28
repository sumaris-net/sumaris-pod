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

import net.sumaris.core.dao.technical.model.annotation.EntityEnum;

@EntityEnum(entity = AcquisitionLevel.class,
        joinAttributes = AcquisitionLevel.Fields.LABEL)
public enum AcquisitionLevelEnum {

    TRIP(1, "TRIP"),
    OPERATION(3, "OPERATION"),
    CATCH_BATCH(4, "CATCH_BATCH"),
    SORTING_BATCH(5, "SORTING_BATCH"),
    SURVIVAL_TEST(7, "SURVIVAL_TEST"),
    INDIVIDUAL_MONITORING(8, "INDIVIDUAL_MONITORING");

    private Integer id;
    private String label;

    AcquisitionLevelEnum(Integer id, String label) {
      this.id = id;
      this.label = label;
    }

    public static AcquisitionLevelEnum valueOf(final int id) {
        for (AcquisitionLevelEnum v: values()) {
            if (v.id == id) return v;
        }
        throw new IllegalArgumentException("Unknown AcquisitionLevelEnum: " + id);
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}

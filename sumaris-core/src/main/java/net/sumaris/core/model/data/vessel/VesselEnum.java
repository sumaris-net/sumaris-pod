package net.sumaris.core.model.data.vessel;

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

import lombok.NonNull;
import net.sumaris.core.model.annotation.EntityEnum;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.referential.VesselType;

import java.io.Serializable;
import java.util.Arrays;

@EntityEnum(entity = Vessel.class, configAttributes = {Vessel.Fields.ID}, resolveAttributes = {Vessel.Fields.ID})
public enum VesselEnum implements Serializable {

    UNKNOWN(5, "UNK"),
    ;

    public static VesselEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown VesselEnum: " + id));
    }

    public static VesselEnum byLabel(@NonNull final String label) {
        return Arrays.stream(values())
            .filter(level -> label.equals(level.label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown VesselEnum: " + label));
    }

    private Integer id;
    private String label;

    VesselEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
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

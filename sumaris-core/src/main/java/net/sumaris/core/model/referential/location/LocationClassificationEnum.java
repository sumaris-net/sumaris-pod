package net.sumaris.core.model.referential.location;

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

import java.io.Serializable;
import java.util.Arrays;

@EntityEnum(entity = LocationClassification.class)
public enum LocationClassificationEnum implements Serializable {

    LAND(1, "LAND"),
    SEA(2, "SEA");

    public static LocationClassificationEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(classification -> classification.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown LocationClassificationEnum: " + id));
    }

    public static LocationClassificationEnum byLabel(@NonNull final String label) {
        return Arrays.stream(values())
            .filter(classification -> label.equals(classification.label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown LocationClassificationEnum: " + label));
    }

    private Integer id;
    private String label;

    LocationClassificationEnum(Integer id, String label) {
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

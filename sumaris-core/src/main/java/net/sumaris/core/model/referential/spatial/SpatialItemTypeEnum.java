package net.sumaris.core.model.referential.spatial;

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

import lombok.NonNull;
import net.sumaris.core.model.annotation.EntityEnum;
import net.sumaris.core.model.referential.ObjectType;

import java.io.Serializable;
import java.util.Arrays;

@EntityEnum(entity = SpatialItemType.class,
    configAttributes = SpatialItemType.Fields.ID,
    resolveAttributes = SpatialItemType.Fields.LABEL)
public enum SpatialItemTypeEnum implements Serializable {

    DEPTH_GRADIENT(1, "SPATIAL_DEPTH_GRADIENT"),
    NEARBY_SPECIFIC_AREA(2, "SPATIAL_NEARBY_SPECIFIC_AREA"),
    DISTANCE_TO_COAST_GRADIENT(3,"SPATIAL_DISTANCE_TO_COAST_GRADIENT"),
    TAXON_GROUP(4,"SPATIAL_TAXON_GROUP_FAO"),
    GEAR(5,"SPATIAL_GEAR_FAO"),
    METIER(8,"SPATIAL_METIER")
    ;

    public static SpatialItemTypeEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ObjectTypeEnum: " + id));
    }

    public static SpatialItemTypeEnum byLabel(@NonNull final String label) {
        return Arrays.stream(values())
            .filter(level -> label.equals(level.label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ObjectTypeEnum: " + label));
    }

    private Integer id;
    private String label;

    SpatialItemTypeEnum(Integer id, String label) {
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

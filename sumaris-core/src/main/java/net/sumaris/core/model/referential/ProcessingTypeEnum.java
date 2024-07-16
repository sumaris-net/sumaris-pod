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

package net.sumaris.core.model.referential;

import lombok.NonNull;
import net.sumaris.core.model.annotation.EntityEnum;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

@EntityEnum(entity = ProcessingType.class, resolveAttributes = {IItemReferentialEntity.Fields.LABEL})
public enum ProcessingTypeEnum implements Serializable {

    DENORMALIZE_BATCH(1, "DENORMALIZE_BATCH"),
    SUMARIS_EXTRACTION(2, "SUMARIS_EXTRACTION"),
    SIOP_VESSELS_IMPORTATION(3, "SIOP_VESSELS_IMPORTATION"),

    SIOP_ACTIVITY_CALENDARS_IMPORTATION(5, "SIOP_ACTIVITY_CALENDARS_IMPORTATION"),
    VESSEL_SNAPSHOTS_INDEXATION(4, "VESSEL_SNAPSHOTS_INDEXATION"),

    FILL_LOCATION_HIERARCHY(49, "SYS_P_FILL_LOCATION_HIERARCHY"), // ID Harmonie
    FILL_TAXON_GROUP_HIERARCHY(50, "SYS_P_FILL_TAXON_GROUP_HIERARCHY"), // ID Harmonie

    UNKNOWN(-1, "UNKNOWN");

    public static Optional<ProcessingTypeEnum> byId(final int id) {
        return Arrays.stream(values())
                .filter(enumValue -> enumValue.id == id)
                .findFirst();
    }

    public static Optional<ProcessingTypeEnum> byLabel(@NonNull final String label) {
        return Arrays.stream(values())
                .filter(level -> label.equals(level.label))
                .findFirst();
    }

    public static @NonNull Optional<ProcessingTypeEnum> byLabelOrName(@NonNull final String label) {
        Optional<ProcessingTypeEnum> result = Arrays.stream(values())
                .filter(level -> label.equals(level.label))
                .findFirst();
        if (result.isPresent()) return result;

        // Try as name
        return ProcessingTypeEnum.byName(label);
    }

    public static @NonNull Optional<ProcessingTypeEnum> byName(@NonNull final String name) {
        try {
            return Optional.of(ProcessingTypeEnum.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Integer id;
    private String label;

    ProcessingTypeEnum(Integer id, String label) {
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

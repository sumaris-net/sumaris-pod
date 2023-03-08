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

import lombok.NonNull;

import java.io.Serializable;
import java.util.Arrays;

public enum ProgramPropertyEnum implements Serializable {

    TRIP_OPERATION_ALLOW_PARENT("sumaris.trip.operation.allowParent", Boolean.class, Boolean.FALSE.toString()),
    TRIP_OPERATION_ENABLE_SAMPLE("sumaris.trip.operation.sample.enable", Boolean.class, Boolean.FALSE.toString()),

    TRIP_BATCH_TAXON_NAME_ENABLE("sumaris.trip.operation.batch.taxonName.enable", Boolean.class, Boolean.TRUE.toString()),

    TRIP_BATCH_TAXON_GROUP_ENABLE("sumaris.trip.operation.batch.taxonGroup.enable", Boolean.class, Boolean.TRUE.toString()),

    TRIP_BATCH_TAXON_GROUPS_NO_WEIGHT("sumaris.trip.operation.batch.taxonGroups.noWeight", String.class, ""),

    TRIP_BATCH_LENGTH_WEIGHT_CONVERSION_ENABLE("sumaris.trip.operation.batch.lengthWeightConversion.enable", Boolean.class, Boolean.FALSE.toString()),
    TRIP_BATCH_ROUND_WEIGHT_CONVERSION_COUNTRY_ID("sumaris.trip.operation.batch.roundWeightConversion.country.id", Integer.class, null),

    TRIP_BATCH_MEASURE_INDIVIDUAL_TAXON_NAME_ENABLE("sumaris.trip.operation.batch.individual.taxonName.enable",
            Boolean.class, Boolean.TRUE.toString()),

    PROGRAM_STRATEGY_DEPARTMENT_ENABLE("sumaris.program.strategy.department.enable", Boolean.class, Boolean.FALSE.toString()),

    TRIP_EXTRACTION_SAMPLING_METHOD("sumaris.trip.extraction.sampling.method", String.class, "Observer")

    ;

    private String key;
    private String defaultValue;

    private Class type;
    ProgramPropertyEnum(String key, Class type, String defaultValue) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public static ProgramPropertyEnum findByKey(@NonNull final String key) {
        return Arrays.stream(values()).filter(item -> key.equalsIgnoreCase(item.getKey())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ProgramPropertyEnum with key: " + key));
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }


    public Class getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}

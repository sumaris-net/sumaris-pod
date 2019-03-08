package net.sumaris.core.model.referential.pmfm;

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

import java.io.Serializable;
import java.util.Arrays;

public enum PmfmEnum implements Serializable  {

    SMALLER_MESH_GAUGE_MM(3, "SMALLER_MESH_GAUGE_MM"),
    BOTTOM_DEPTH_M(30, "BOTTOM_DEPTH_M"),
    BOTTOM_TEMP_C(32, "BOTTOM_TEMP_C"),
    GEAR_DEPTH_M(36, "GEAR_DEPTH_M"),
    DISCARD_OR_LANDING(90, "DISCARD_OR_LANDING"),

    CONVEYOR_BELT(20, "CONVEYOR_BELT"),
    NB_FISHERMEN(21, "NB_FISHERMEN"),
    NB_OPERATION(23, "NB_OPERATION"),
    NB_SAMPLING_OPERATION(24, "NB_SAMPLING_OPERATION"),
    MAIN_METIER(25, "MAIN_METIER"),
    RANDOM_SAMPLING_OPERATION(26, "RANDOM_SAMPLING_OPERATION"),

    SELECTIVITY_DEVICE(4, "SELECTIVITY_DEVICE"),
    SUBSTRATE_TYPE(31, "SUBSTRATE_TYPE"),
    SEA_STATE(33, "SEA_STATE"),
    TRIP_PROGRESS(34,"TRIP_PROGRESS"),
    SURVIVAL_SAMPLING_TYPE(35, "SURVIVAL_SAMPLING_TYPE"),

    LANDING_WEIGHT(50, "LANDING_WEIGHT"),
    SAND_STONES_WEIGHT_RANGE(51, "SAND_STONES_WEIGHT_RANGE"),
    BENTHOS_WEIGHT_RANGE(52, "BENTHOS_WEIGHT_RANGE"),
    ON_DECK_DATE_TIME(53, "ON_DECK_DATE_TIME"),
    SORTING_START_DATE_TIME(54, "SORTING_START_DATE_TIME"),
    SORTING_END_DATE_TIME(55, "SORTING_END_DATE_TIME"),

    SEX(80, "SEX"),
    TAG_ID(82, "TAG_ID"),
    LENGTH_TOTAL_CM(81, "LENGTH_TOTAL_CM"),
    IS_DEAD(94, "IS_DEAD"),
    DISCARD_REASON(95, "DISCARD_REASON")
    ;

    public static PmfmEnum valueOf(final int id) {
        return Arrays.stream(values())
                .filter(level -> level.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PmfmEnum: " + id));
    }

    private int id;
    private String label;

    PmfmEnum(int id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * Returns the database row id
     *
     * @return int the id
     */
    public int getId()
    {
        return this.id;
    }

    public String getLabel()
    {
        return this.label;
    }
}

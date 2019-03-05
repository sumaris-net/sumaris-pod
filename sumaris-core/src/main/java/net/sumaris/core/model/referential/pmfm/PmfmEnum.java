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
    SURVIVAL_SAMPLING_TYPE(35, "SURVIVAL_SAMPLING_TYPE")
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

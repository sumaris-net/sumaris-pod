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
import net.sumaris.core.dao.technical.model.annotation.EntityEnum;

import java.io.Serializable;
import java.util.Arrays;

@EntityEnum(entity = LocationLevel.class)
public enum LocationLevelEnum implements Serializable {

    COUNTRY(1, "Country"),
    HARBOUR(2, "Port"),
    AUCTION(3, "Auction"),
    RECTANGLE_ICES(4, "ICES_RECTANGLE"),
    RECTANGLE_GFCM(5, "GFCM_RECTANGLE"),
    SQUARE_10(6, "SQUARE_10"), // 10' x 10'
    SQUARE_3(7, "SQUARE_3"), // 3' x 3'

    AREA_FAO(101, "FAO_AREA"),  // Zone FAO
    SUB_AREA_ICES(110, "ICES_SUB_AREA"), // Sous-zone CIEM (=ICES)
    DIVISION_ICES(111, "ICES_DIVISION"), // Division CIEM (=ICES)
    SUB_DIVISION_ICES(112, "ICES_SUB_DIVISION"), // Sous-Division CIEM (=ICES)
    SUB_AREA_GFCM(140, "GFCM_SUB_AREA"), // Sous-zone CGPM (=GFCM)
    DIVISION_GFCM(141, "GFCM_DIVISION"), // Division CGPM (=GFCM)
    SUB_DIVISION_GFCM(142, "GFCM_SUB_DIVISION") // Sous-Division CGPM (=GFCM)
    ;

    public static LocationLevelEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown LocationLevelEnum: " + id));
    }

    public static LocationLevelEnum byLabel(@NonNull final String label) {
        return Arrays.stream(values())
            .filter(level -> label.equals(level.label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown LocationLevelEnum: " + label));
    }

    private Integer id;
    private String label;

    LocationLevelEnum(Integer id, String label) {
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

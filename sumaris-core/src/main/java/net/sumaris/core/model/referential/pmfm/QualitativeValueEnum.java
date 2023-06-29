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

import net.sumaris.core.model.annotation.EntityEnum;
import net.sumaris.core.model.annotation.IEntityEnum;

import java.io.Serializable;
import java.util.Arrays;

@EntityEnum(entity = QualitativeValue.class, resolveAttributes = {QualitativeValue.Fields.LABEL, QualitativeValue.Fields.ID})
public enum QualitativeValueEnum implements Serializable, IEntityEnum {

    SORTING_BULK(390, "VRAC"), // Adagio => 311
    SORTING_NON_BULK(391, "H-VRAC"), // Adagio => 310
    SORTING_UNSORTED(392, "NONE"), // Adagio => 2146
    DRESSING_WHOLE(381, "WHL"), // Entier - Adagio => 139
    DRESSING_GUTTED(381, "GUT"), // Eviscéré - Adagio => 120
    PRESERVATION_FRESH(332, "FRE"),
    SIZE_CATEGORY_NONE(435, "UNS"),

    /*PACKAGING*/
    WEIGHT(399, "WEI"),
    UNIT(395, "UNI"),
    DOZEN(396, "DOZ"),
    HUNDRED(397, "HUN"),
    PIECES(398, "PCS"),

    // LANDING_OR_DISCARD
    LANDING(190, "LAN"),
    DISCARD(191, "DIS"),

    // SEX
    SEX_UNSEXED(188, "NS"), // Adagio => 302
    ;

    public static QualitativeValueEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown QualitativeValueEnum: " + id));
    }

    private Integer id;
    private String label;

    QualitativeValueEnum(Integer id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * Returns the database row id
     *
     * @return int the id
     */
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}

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

public enum QualitativeValueEnum implements Serializable  {

    SORTING_BULK(20, "SORTING_BULK"),
    DRESSING_WHOLE(74, "DRESSING_WHOLE"),
    PRESERVATION_FRESH(76, "PRESERVATION_FRESH"),
    SIZE_CATEGORY_NONE(298, "SIZE_CATEGORY_NONE"),

    /*PACKAGING*/
    WEIGHT(317, "WEI"),
    VOLUME(318, "VOL"),
    UNIT(319, "UNI"),
    DOZEN(320, "DOZ"),
    HUNDRED(321, "HUN"),
    PIECES(423, "PCS"),

    ;

    public static QualitativeValueEnum valueOf(final int id) {
        return Arrays.stream(values())
                .filter(level -> level.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown QualitativeValueEnum: " + id));
    }

    private int id;
    private String label;

    QualitativeValueEnum(int id, String label) {
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

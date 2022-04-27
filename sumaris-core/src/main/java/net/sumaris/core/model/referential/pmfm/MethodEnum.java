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

import net.sumaris.core.dao.technical.model.annotation.EntityEnum;

import java.io.Serializable;
import java.util.Arrays;

@EntityEnum(entity = Method.class)
public enum MethodEnum implements Serializable {

    UNKNOWN(0, "UNK"),
    AUCTION(261, "AUCTION"), // ID from SIH-Harmonie
    MEASURED_BY_OBSERVER(1, "MEASURED_BY_OBSERVER"),
    OBSERVED_BY_OBSERVER(2, "OBSERVED_BY_OBSERVER"),
    ESTIMATED_BY_OBSERVER(3, "ESTIMATED_BY_OBSERVER"),
    CALCULATED(4, "CALCULATED"),
    OBSERVED_BY_FISHERMAN(5, "OBSERVED_BY_FISHERMAN"),
    DECLARED_BY_FISHERMAN(6, "DECLARED_BY_FISHERMAN"),

    CALCULATED_WEIGHT_LENGTH(47, "CALCULATED_WEIGHT_LENGTH"),
    CALCULATED_WEIGHT_LENGTH_SUM(283, "CALCULATED_WEIGHT_LENGTH_SUM"),
    ;

    public static MethodEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(methodEnum -> methodEnum.id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown MethodEnum: " + id));
    }

    private Integer id;
    private String label;

    MethodEnum(Integer id, String label) {
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

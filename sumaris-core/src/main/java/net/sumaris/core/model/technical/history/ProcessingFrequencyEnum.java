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

package net.sumaris.core.model.technical.history;

import net.sumaris.core.dao.technical.model.annotation.EntityEnum;
import net.sumaris.core.model.referential.StatusEnum;

import java.io.Serializable;
import java.util.Arrays;


@EntityEnum(entity = ProcessingFrequency.class)
public enum ProcessingFrequencyEnum implements Serializable {

    NEVER(0, "NEVER"),
    MANUALLY(1, "MANUALLY"),
    DAILY(2, "DAILY"),
    WEEKLY(3, "WEEKLY"),
    MONTHLY(4, "MONTHLY");

    public static ProcessingFrequencyEnum valueOf(final int id) {
        return Arrays.stream(values())
            .filter(enumValue -> enumValue.id == id)
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown ProcessingFrequencyEnum: " + id));
    }

    private int id;
    private String label;

    ProcessingFrequencyEnum(int id, String label) {
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

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel()
    {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
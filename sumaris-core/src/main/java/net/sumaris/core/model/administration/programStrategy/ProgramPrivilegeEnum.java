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

package net.sumaris.core.model.administration.programStrategy;

import java.io.Serializable;

public enum ProgramPrivilegeEnum implements Serializable {


    MANAGER(1),
    OBSERVER(2),
    VIEWER(3),
    VALIDATOR(4),
    QUALIFIER(5);

    private int id;

    ProgramPrivilegeEnum(int id) {
        this.id = id;
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


    public static ProgramPrivilegeEnum valueOf(final int id) {
        for (ProgramPrivilegeEnum v: values()) {
            if (v.id == id) return v;
        }
        throw new IllegalArgumentException("Unknown ProgramPrivilegeEnum: " + id);
    }
}

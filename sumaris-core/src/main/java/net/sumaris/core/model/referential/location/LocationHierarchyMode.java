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

package net.sumaris.core.model.referential.location;

import java.io.Serializable;

/**
 * Enumeration representing different modes of location hierarchy resolution.
 */
public enum LocationHierarchyMode implements Serializable {

    /**
     * Enumeration constant representing no resolution
     */
    NONE,

    /**
     * Enumeration constant representing a resolution approach
     * from the bottom to the top.
     */
    BOTTOM_UP,

    /**
     * Enumeration constant representing a resolution approach
     * from the top to the bottom.
     */
    TOP_DOWN,

    /**
     * Enumeration constant representing a resolution approach that
     * combines both bottom-up and top-down strategies.
     */
    BOTH;
}

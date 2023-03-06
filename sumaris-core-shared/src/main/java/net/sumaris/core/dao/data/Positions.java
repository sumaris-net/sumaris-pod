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

package net.sumaris.core.dao.data;

public abstract class Positions {

    protected Positions() {
        // Helper class
    }

    public static boolean isNotNullAndValid(IPosition position) {

        if (position == null || position.getLatitude() == null || position.getLongitude() == null) return false;

        // Invalid lat/lon
        if (position.getLatitude() < -90 || position.getLatitude() > 90
            || position.getLongitude() < -180 || position.getLongitude() > 180) {
            return false;
        }

        // OK: valid
        return true;
    }

    public static boolean isNullOrInvalid(IPosition position) {
        return !isNotNullAndValid(position);
    }
}

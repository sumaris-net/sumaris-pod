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

package net.sumaris.core.util.conversion;

import lombok.NonNull;

public abstract class UnitConversions {
    protected UnitConversions() {
        // helper class
    }

    public static double weightToKgConversion(@NonNull String unitSymbol) {
        return switch (unitSymbol) {
            case "t" -> 1000;
            case "kg" -> 1;
            case "g" -> 1d/1000;
            case "mg" -> 1d/1000/1000;
            default -> throw new IllegalStateException("Unexpected value: " + unitSymbol);
        };
    }

    public static double lengthToMeterConversion(@NonNull String unitSymbol) {
        return switch (unitSymbol) {
            case "km" -> 1000;
            case "m" -> 1;
            case "dm" -> 1d/10;
            case "cm" -> 1d/100;
            case "mm" -> 1d/1000;
            case "μm" -> 1d/1000/1000;
            default -> throw new IllegalStateException("Unexpected value: " + unitSymbol);
        };
    }
}

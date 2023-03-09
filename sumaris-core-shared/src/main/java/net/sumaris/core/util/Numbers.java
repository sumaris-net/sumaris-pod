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

package net.sumaris.core.util;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Numbers {

    protected Numbers() {
        // Helper class
    }

    public static String format(Number value, int maximumFractionDigits) {
        DecimalFormat formatter = new DecimalFormat();
        formatter.setMaximumFractionDigits(maximumFractionDigits);
        formatter.setDecimalSeparatorAlwaysShown(false);
        formatter.setGroupingUsed(false);
        formatter.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.FRANCE));
        return formatter.format(value);
    }

    public static String format(Number value) {
        DecimalFormat formatter = new DecimalFormat();
        formatter.setDecimalSeparatorAlwaysShown(false);
        formatter.setGroupingUsed(false);
        formatter.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.FRANCE));
        return formatter.format(value);
    }

    public static BigDecimal firstNotNullAsBigDecimal(Number... values) {
        for (Number v: values) {
            if (v != null) {
                if (v instanceof BigDecimal) return (BigDecimal)v;
                return new BigDecimal(v.doubleValue());
            }
        }
        return null;
    }

    public static Double asDouble(@Nullable BigDecimal value) {
        if (value != null) return value.doubleValue();
        return null;
    }

    public static double doubleValue(@Nullable BigDecimal value, double defaultValue) {
        if (value != null) return value.doubleValue();
        return defaultValue;
    }

    public static Double round(BigDecimal value, int scale) {
        if (value != null) return value
            .divide(new BigDecimal(1d), scale, RoundingMode.HALF_UP)
            .doubleValue();
        return null;
    }
}

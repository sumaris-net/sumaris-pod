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
import java.lang.reflect.Array;
import java.util.List;
import java.util.stream.Stream;

public class ArrayUtils extends org.apache.commons.lang3.ArrayUtils {

    protected ArrayUtils() {
        super();
    }

    @Nullable
    @SuppressWarnings("varargs")
    public static Integer[] concat(@Nullable Integer value, @Nullable Integer[] values) {
        return ArrayUtils.concat(value, values, Integer[].class);
    }

    @Nullable
    @SuppressWarnings("varargs")
    public static <T> T[] concat(@Nullable T value, @Nullable T[] values, Class<T[]> type) {
        if (value == null) {
            return values;
        }
        else if (org.apache.commons.lang3.ArrayUtils.isNotEmpty(values)) {
            if (org.apache.commons.lang3.ArrayUtils.contains(values, value)) return values;
            return org.apache.commons.lang3.ArrayUtils.add(values, value);
        }
        else {
            T[] result = type.cast(Array.newInstance(type.getComponentType(), 1));
            result[0] = value;
            return result;
        }
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <T> List<T> asList(T... values) {
        return java.util.Arrays.asList(values);
    }

    public static <T> Stream<T> stream(T[] array) {
        return java.util.Arrays.stream(array);
    }
}

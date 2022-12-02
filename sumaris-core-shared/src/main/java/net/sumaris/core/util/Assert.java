package net.sumaris.core.util;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2022 SUMARiS Consortium
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;

/**
 * @author peck7 on 14/03/2019.
 */
public class Assert extends org.springframework.util.Assert {

    public static void notNull(Object object) {
        notNull(object, "this argument is required; it must not be null");
    }

    public static void isNull(Object object) {
        isNull(object, "this argument must be null");
    }

    public static void notEmpty(Collection<?> collection) {
        notEmpty(collection, "this collection must not be empty: it must contain at least 1 element");
    }

    public static void isTrue(boolean expression) {
        isTrue(expression, "this expression must be true");
    }

    public static void nonNullElement(Collection<?> collection) {
        if (collection != null && collection.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("this collection must not contain any null elements");
        }
    }

    public static void notEmpty(Object[] array) {
        notEmpty(array, "this array must not be empty: it must contain at least 1 element");
    }

    public static void notBlank(String string) {
        notBlank(string, "this string must not be blank");
    }

    public static void notBlank(String string, String message) {
        isTrue(StringUtils.isNotBlank(string), message);
    }

    public static void equals(Object o1, Object o2) {
        isTrue(Objects.equals(o1, o2), "both objects must be equals");
    }

    public static void equals(Object o1, Object o2, String message) {
        isTrue(Objects.equals(o1, o2), message);
    }

    public static void size(Object object, int size) {
        isTrue(CollectionUtils.size(object) == size, String.format("this object must have %d elements", size));
    }

}

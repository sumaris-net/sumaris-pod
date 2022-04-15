package net.sumaris.core.util;

/*-
 * #%L
 * SUMARiS:: Core shared
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class StreamUtils extends org.springframework.util.StreamUtils {

    /**
     * <p>concat streams.</p>
     *
     * @param list a {@link Stream} object.
     * @param <E>  a E object.
     * @return a {@link Stream} object.
     */
    public static <E> Stream<E> concat(Stream<E>... streams) {
        return Arrays.stream(streams).flatMap(s -> s);
    }

    /**
     * <p>concat collections.</p>
     *
     * @param list a {@link Stream} object.
     * @param <E>  a E object.
     * @return a {@link Stream} object.
     */
    public static <E> Stream<E> concat(Collection<E>... lists) {
        return Arrays.stream(lists).flatMap(StreamUtils::getStream);
    }

    /**
     * <p>getList.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E>  a E object.
     * @return a {@link List} object.
     */
    public static <E> Stream<E> getStream(Collection<E> list) {
        if (list == null) {
            return Stream.empty();
        }
        return list.stream();
    }

    public static <E> Stream<E> getStream(E[] array) {
        if (array == null) {
            return Stream.empty();
        }
        return Arrays.stream(array);
    }
}

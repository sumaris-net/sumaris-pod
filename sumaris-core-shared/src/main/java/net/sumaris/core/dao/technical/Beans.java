package net.sumaris.core.dao.technical;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
 * $Id:$
 * $HeadURL:$
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

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import net.sumaris.core.dao.technical.model.IEntityBean;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.shared.exception.ErrorCodes;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * helper class for beans (split by property, make sure list exists, ...)
 * Created by blavenie on 13/10/15.
 */
public class Beans {

    /**
     * <p>getList.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> List<E> getList(Collection<E> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        } else if (list instanceof List<?>){
            return (List<E>) list;
        } else {
            return Lists.newArrayList(list);
        }
    }

    /**
     * <p>getListWithoutNull.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> List<E> getListWithoutNull(Collection<E> list) {
        List<E> result = getList(list);
        result.removeAll(Collections.singleton((E) null));
        return result;
    }

    /**
     * <p>getSet.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link Set} object.
     */
    public static <E> Set<E> getSet(Collection<E> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Sets.newHashSet();
        } else {
            return Sets.newHashSet(list);
        }
    }

    /**
     * <p>getSetWithoutNull.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link Set} object.
     */
    public static <E> Set<E> getSetWithoutNull(Collection<E> list) {
        Set<E> result = getSet(list);
        result.removeAll(Collections.singleton((E) null));
        return result;
    }

    /**
     * <p>getMap.</p>
     *
     * @param map a {@link Map} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <K, V> Map<K, V> getMap(Map<K, V> map) {
        if (MapUtils.isEmpty(map)) {
            return Maps.newHashMap();
        } else {
            return Maps.newHashMap(map);
        }
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param propertyName a {@link String} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <K, V> Map<K, V> splitByProperty(Iterable<V> list, String propertyName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        return getMap(Maps.uniqueIndex(list, input -> getProperty(input, propertyName)));
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <K extends Serializable, V extends IEntityBean<K>> Map<K, V> splitById(Iterable<V> list) {
        return getMap(Maps.uniqueIndex(list, IEntityBean::getId));
    }

    /**
     * <p>collectProperties.</p>
     *
     * @param collection a {@link Collection} object.
     * @param propertyName a {@link String} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link List} object.
     */
    public static <K, V> List<K> collectProperties(Collection<V> collection, String propertyName) {
        if (CollectionUtils.isEmpty(collection)) return new ArrayList<>();
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        return collection.stream().map((Function<V, K>) v -> getProperty(v, propertyName)).collect(Collectors.toList());

    }

    private static <K, V> Function<V, K> newPropertyFunction(final String propertyName) {
        return input -> getProperty(input, propertyName);
    }

    /**
     * <p>getProperty.</p>
     *
     * @param object       a K object.
     * @param propertyName a {@link String} object.
     * @param <K>          a K object.
     * @param <V>          a V object.
     * @return a V object.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> V getProperty(K object, String propertyName) {
        try {
            return (V) PropertyUtils.getProperty(object, propertyName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new SumarisTechnicalException( String.format("Could not get property %1s on object of type %2s", propertyName, object.getClass().getName()), e);
        }
    }

    /**
     * <p>setProperty.</p>
     *
     * @param object       a K object.
     * @param propertyName a {@link String} object.
     * @param value        a V object.
     * @param <K>          a K object.
     * @param <V>          a V object.
     */
    public static <K, V> void setProperty(K object, String propertyName, V value) {
        try {
            PropertyUtils.setProperty(object, propertyName, value);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new SumarisTechnicalException( String.format("Could not set property %1s not found on object of type %2s", propertyName, object.getClass().getName()), e);
        }
    }

    public static Integer[] asIntegerArray(Collection<Integer> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return values.toArray(new Integer[values.size()]);
    }

    public static String[] asStringArray(Collection<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return values.toArray(new String[values.size()]);
    }

    public static String[] asStringArray(String value, String delimiter) {
        if (StringUtils.isBlank(value)) return new String[0];
        StringTokenizer tokenizer = new StringTokenizer(value, delimiter);
        String[] values = new String[tokenizer.countTokens()];
        int i=0;
        while (tokenizer.hasMoreTokens()) {
            values[i] = tokenizer.nextToken();
            i++;
        }
        return values;
    }

    public static <E> List<E> filterCollection(Collection<E> collection, Predicate<E> predicate) {
        return collection.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <O, E> List<O> transformCollection(Collection<? extends E> collection, Function<E, O> funtion) {
        return collection.stream().map(funtion).collect(Collectors.toList());
    }

    public static <T> Comparator<T> naturalComparator(final String sortAttribute, final SortDirection sortDirection) {
        if (sortAttribute == null) {
            return naturalComparator("id", sortDirection);
        }

        final Comparator<String> propertyComparator = ComparatorUtils.naturalComparator();

        if (SortDirection.ASC.equals(sortDirection)) {
            return (o1, o2) -> propertyComparator.compare(
                        getProperty(o1, sortAttribute),
                        getProperty(o2, sortAttribute)
                );
        }
        else {
            return (o1, o2) -> propertyComparator.compare(
                    getProperty(o2, sortAttribute),
                    getProperty(o1, sortAttribute)
            );
        }
    }

    //public static Map<String, String[]> cacheCopyPropertiesIgnored;
    public static Map<Class<?>, Map<Class<?>, String[]>> cacheCopyPropertiesIgnored = Maps.newConcurrentMap();

    /**
     * Usefull method that ignore complex type, as list
     * @param source
     * @param target
     */
    public static <S, T> void copyProperties(S source, T target) {

        Map<Class<?>, String[]> cache = cacheCopyPropertiesIgnored.get(source.getClass());
        if (cache == null) {
            cache = Maps.newConcurrentMap();
            cacheCopyPropertiesIgnored.put(source.getClass(), cache);
        }
        String[] cachedIgnoreProperties = cache.get(target.getClass());

        // Fill the cache
        if (cachedIgnoreProperties == null) {

            PropertyDescriptor[] targetDescriptors = BeanUtils.getPropertyDescriptors(target.getClass());
            Map<String, PropertyDescriptor> targetProperties = Maps.uniqueIndex(ImmutableList.copyOf(targetDescriptors), PropertyDescriptor::getName);

            List<String> ignorePropertiesList = Stream.of(BeanUtils.getPropertyDescriptors(source.getClass()))
                    // Keep invalid properties
                    .filter(pd -> {
                        PropertyDescriptor targetDescriptor = targetProperties.get(pd.getName());
                        return targetDescriptor == null
                                || !targetDescriptor.getPropertyType().isAssignableFrom(pd.getPropertyType())
                                || Collection.class.isAssignableFrom(pd.getPropertyType())
                                || targetDescriptor.getWriteMethod() == null;
                    })
                    .map(PropertyDescriptor::getName)
                    .collect(Collectors.toList());
            cachedIgnoreProperties = ignorePropertiesList.toArray(new String[ignorePropertiesList.size()]);

            // Add to cache
            cache.put(target.getClass(), cachedIgnoreProperties);
        }

        BeanUtils.copyProperties(source, target, cachedIgnoreProperties);
    }
}

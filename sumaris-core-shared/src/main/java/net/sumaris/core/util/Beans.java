package net.sumaris.core.util;

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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IEntity;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * helper class for beans (split by property, make sure list exists, ...)
 * Created by blavenie on 13/10/15.
 */
@Slf4j
public class Beans {

    protected Beans() {
        // helper class does not instantiate
    }

    public static <C> C newInstance(Class<C> objectClass) {
        Preconditions.checkNotNull(objectClass, "Cant create an instance of 'null'");
        try {
            return objectClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    public static <C> @Nonnull C nullToEmpty(@Nullable C object, Class<C> objectClass)  {
        return object != null ? object : newInstance(objectClass);
    }

    /**
     * <p>getList.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E>  a E object.
     * @return a {@link List} object.
     */
    public static <E> List<E> getList(Collection<E> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        } else if (list instanceof List<?>) {
            return (List<E>) list;
        } else {
            return Lists.newArrayList(list);
        }
    }

    public static <E> List<E> getList(E[] values) {
     return values !=null ? Lists.newArrayList(values) : Lists.newArrayList();
    }

    /**
     * <p>getList.</p>
     *
     * @param iterable a {@link Collection} object.
     * @param <E>  a E object.
     * @return a {@link List} object.
     */
    public static <E> List<E> getList(Iterable<E> iterable) {
        if (iterable == null) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(iterable);
    }

    @SafeVarargs
    public static <E> Collection<E> intersection(Collection<E> ...lists) {
        Collection<E> result = null;
        for (Collection<E> item: lists) {
            result = result == null ? item : CollectionUtils.intersection(item, result);
        }

        return result;
    }

    @SafeVarargs
    public static <E> Collection<E> intersectionSkipEmpty(Collection<E> ...lists) {
        Collection<E> result = null;
        for (Collection<E> item: lists) {
            // Skip is empty
            if (CollectionUtils.isNotEmpty(item)) {
                result = result == null ? item // First not empty list
                    : CollectionUtils.intersection(item, result);
            }
        }

        return result;
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
        return ArrayUtils.stream(array);
    }

    public static <E> Stream<E> getStream(Iterable<E> iterable) {
        if (iterable == null) {
            return Stream.empty();
        }
        return StreamSupport.stream(
                iterable.spliterator(),
                false
        );

    }

    /**
     * <p>getListWithoutNull.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E>  a E object.
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
     * @param <E>  a E object.
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
     * @param <E>  a E object.
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

    public static <T> T safeGet(List<T> list, int index) {
        if (list == null)
            return null;
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    public static <T> boolean equals(Collection<T> a, Collection<T> b) {
        return CollectionUtils.size(a) == CollectionUtils.size(b)
            && (a == null || CollectionUtils.isEqualCollection(a, b));
    }


    /**
     * <p>splitByProperty.</p>
     *
     * @param list         a {@link Iterable} object.
     * @param propertyName a {@link String} object.
     * @param <K>          a K object.
     * @param <V>          a V object.
     * @return a {@link Map} object.
     */
    public static <K, V> Map<K, V> splitByProperty(Iterable<V> list, String propertyName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        if (list == null) return new HashMap<>();
        return getMap(Maps.uniqueIndex(list, input -> getProperty(input, propertyName)));
    }

    /**
     * <p>splitByNotUniqueProperty.</p>
     *
     * @param list         a {@link Iterable} object.
     * @param propertyName a {@link String} object.
     * @param <K>          a K object.
     * @param <V>          a V object.
     * @return a {@link Map} object.
     */
    public static <K, V> ListMultimap<K, V> splitByNotUniqueProperty(Iterable<V> list, String propertyName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        if (list == null) return ArrayListMultimap.create();
        return Multimaps.index(list, input -> getProperty(input, propertyName));
    }

    /**
     * <p>splitByNotUniqueProperty.</p>
     *
     * @param list         a {@link Iterable} object.
     * @param propertyName a {@link String} object.
     * @param defaultKey   the default key, when property if null
     * @param <K>          a K object.
     * @param <V>          a V object.
     * @return a {@link Map} object.
     */
    public static <K, V> ListMultimap<K, V> splitByNotUniqueProperty(Iterable<V> list, String propertyName, @NonNull K defaultKey) {
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        if (list == null) return ArrayListMultimap.create();
        return Multimaps.index(list, input -> {
            K key = getProperty(input, propertyName);
            if (key == null) return defaultKey;
            return key;
        });
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <V>  a V object.
     * @return a {@link Map} object.
     */
    public static <V> Multimap<Integer, V> splitByNotUniqueHashcode(Iterable<V> list) {
        return list != null ? Multimaps.index(list, Object::hashCode) : ArrayListMultimap.create();
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <K>  a K object.
     * @param <V>  a V object.
     * @return a {@link Map} object.
     */
    public static <K extends Serializable, V extends IEntity<K>> Map<K, V> splitById(Iterable<V> list) {
        return list != null ? getMap(Maps.uniqueIndex(list, IEntity::getId)) : new HashMap<>();
    }

    /**
     * <p>collectIds.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <K>  a K object.
     * @param <V>  a V object.
     * @return a {@link Map} object.
     */
    public static <K extends Serializable, V extends IEntity<K>> List<K> collectIds(Collection<V> entities) {
        return Beans.getStream(entities).map(IEntity::getId).collect(Collectors.toList());
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param entities list of entities.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    @SafeVarargs
    public static <K extends Serializable, V extends IEntity<K>> List<K> collectIds(V... entities) {
        return Beans.getStream(entities).map(IEntity::getId).collect(Collectors.toList());
    }

    /**
     * <p>collectIdsAsSetcollectIdsAsSet.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <K>  a K object.
     * @param <V>  a V object.
     * @return a {@link Map} object.
     */
    public static <K extends Serializable, V extends IEntity<K>> Set<K> collectIdsAsSet(Collection<V> entities) {
        return Beans.getStream(entities).map(IEntity::getId).collect(Collectors.toSet());
    }

    /**
     * <p>collectIdsAsSet.</p>
     *
     * @param entities list of entities.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    @SafeVarargs
    public static <K extends Serializable, V extends IEntity<K>> Set<K> collectIdsAsSet(V... entities) {
        return Beans.getStream(entities).map(IEntity::getId).collect(Collectors.toSet());
    }

    /**
     * Removes duplicated entities from a collection, preserving the first occurrence of each entity based on its ID.
     *
     * @param entities the collection of entities to be processed
     * @return a list of entities with duplicates removed, or null if the input collection is null
     */
    public static <K extends Serializable, V extends IEntity<K>> List<V> removeDuplicatesById(Collection<V> entities) {
        if (entities == null) return null;
        return getList(getStream(entities)
            .collect(Collectors.toMap(IEntity::getId, Function.identity(),
                (existing, replacement) -> existing, // Keep first occurrence
                LinkedHashMap::new // Keep existing order
            ))
            .values());
    }

    /**
     * <p>collectProperties.</p>
     *
     * @param collection   a {@link Collection} object.
     * @param propertyName a {@link String} object.
     * @param <K>          a K object.
     * @param <V>          a V object.
     * @return a {@link List} object.
     */
    public static <K, V> List<K> collectProperties(Collection<V> collection, String propertyName) {
        if (CollectionUtils.isEmpty(collection)) return new ArrayList<>();
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        return collection.stream().map((Function<V, K>) v -> getProperty(v, propertyName)).collect(Collectors.toList());
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
    public static <K, V> Set<K> collectDistinctProperties(Collection<V> collection, String propertyName) {
        if (CollectionUtils.isEmpty(collection)) return new HashSet<>();
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        return collection.stream().map((Function<V, K>) v -> getProperty(v, propertyName)).collect(Collectors.toSet());
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
            throw new SumarisTechnicalException(String.format("Could not find property %1s on object of type %2s", propertyName, object.getClass().getName()), e);
        }
    }

    public static <K, V> V getPrivateProperty(K object, String propertyName) {
        try {
            return (V) PropertyUtils.getProperty(object, propertyName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            try {
                Field field = object.getClass().getDeclaredField(propertyName);
                field.setAccessible(true);
                return (V)field.get(object);
            } catch (Exception other) {
                throw new SumarisTechnicalException( String.format("Could not find property %1s on object of type %2s", propertyName, object.getClass().getName()), e);

            }
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
            throw new SumarisTechnicalException(String.format("Could not set property %1s not found on object of type %2s", propertyName, object.getClass().getName()), e);
        }
    }

    /**
     * Retrieves a property collector for a specified target class and key.
     *
     * @param <T> the type of the referential entity
     * @param targetClass the class of the target referential entity
     * @param key the key representing the property to collect
     * @return a Collector for the specified property of the target class
     */
    public static <T> Collector<T, ?, ?> getPropertyCollector(Class<?> targetClass, String key) {
        try {
            Field field = targetClass.getDeclaredField(key);
            Class<?> fieldType = field.getType();

            if (Set.class.isAssignableFrom(fieldType)) {
                return Collectors.toSet();
            } else if (List.class.isAssignableFrom(fieldType)) {
                return Collectors.toList();
            } else {
                throw new IllegalArgumentException("Unsupported field type: " + fieldType.getName());
            }
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field with key " + key + " not found in class " + targetClass.getName(), e);
        }
    }

    public static Integer[] asIntegerArray(Iterable<? extends Integer> values) {
        if (values == null) {
            return new Integer[0];
        }
        return Iterables.toArray(values, Integer.class);
    }

    public static String[] asStringArray(Collection<? extends String> values) {
        if (values == null) {
            return new String[0];
        }
        return Iterables.toArray(values, String.class);
    }

    public static String[] asStringArray(String value, String delimiter) {
        if (StringUtils.isBlank(value)) return new String[0];
        StringTokenizer tokenizer = new StringTokenizer(value, delimiter);
        String[] values = new String[tokenizer.countTokens()];
        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            values[i] = tokenizer.nextToken();
            i++;
        }
        return values;
    }

    public static <E> List<E> filterCollection(Collection<E> collection, Predicate<E> predicate) {
        return getList(collection).stream().filter(predicate).collect(Collectors.toList());
    }

    public static <K, V> Map<K, V> filterMap(Map<K, V> map, Predicate<K> predicate) {
        return getMap(map).entrySet()
            .stream()
            .filter(entry -> predicate.test(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <O, E> List<O> transformCollection(Collection<? extends E> collection, Function<E, O> function) {
        return Beans.getStream(collection).map(function).toList();
    }

    public static <K, V> Map<K, V> mergeMap(Map<K, V> map1, Map<K, V> map2) {
        if (MapUtils.isEmpty(map1)) return map2;
        if (MapUtils.isEmpty(map2)) return map1;
        return Stream.concat(map1.entrySet().stream(), map2.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static <T> Comparator<T> naturalComparator(final String sortAttribute, final SortDirection sortDirection) {
        if (sortAttribute == null) {
            return Comparator.comparingInt((value) -> getProperty(value, "id"));
        }

        final Comparator<String> propertyComparator = ComparatorUtils.naturalComparator();

        if (SortDirection.ASC.equals(sortDirection)) {
            return (o1, o2) -> {
                String p1 = getProperty(o1, sortAttribute);
                String p2 = getProperty(o2, sortAttribute);
                if (p1 == null && p2 == null) return 0;
                else if (p2 == null) return 1;
                else if (p1 == null) return -1;
                return propertyComparator.compare(p1, p2);
            };
        } else {
            return (o1, o2) -> {
                String p1 = getProperty(o1, sortAttribute);
                String p2 = getProperty(o2, sortAttribute);
                if (p1 == null && p2 == null) return 0;
                else if (p2 == null) return -1;
                else if (p1 == null) return 1;
                return propertyComparator.compare(p2, p1);
            };
        }
    }

    public static <T> Comparator<T> unsortedComparator() {
        return (o1, o2) -> 0;
    }

    public static <T, R extends T> R clone(T source, Class<R> resultClass) {
        R target = newInstance(resultClass);
        copyProperties(source, target);
        return target;
    }

    public static <T, R extends T> R clone(T source, Class<R> resultClass, String... excludedPropertyNames) {
        R target = newInstance(resultClass);
        copyProperties(source, target, excludedPropertyNames);
        return target;
    }


    public static Map<Class<?>, Map<Class<?>, String[]>> cacheCopyPropertiesIgnored = Maps.newConcurrentMap();

    /**
     * Usefull method that ignore complex type, as list
     *
     * @param source
     * @param target
     */
    public static <S, T> void copyProperties(S source, T target) {
        copyProperties(source, target, new String[0]);
    }

    /**
     * Usefull method that ignore complex type, as list
     *
     * @param source
     * @param target
     */
    public static <S, T> void copyProperties(S source, T target, String... exceptProperties) {

        Map<Class<?>, String[]> cache = cacheCopyPropertiesIgnored.computeIfAbsent(source.getClass(), k -> Maps.newConcurrentMap());
        String[] ignoredProperties = cache.get(target.getClass());

        // Fill the cache
        if (ignoredProperties == null) {

            PropertyDescriptor[] targetDescriptors = BeanUtils.getPropertyDescriptors(target.getClass());
            Map<String, PropertyDescriptor> targetProperties = Maps.uniqueIndex(ImmutableList.copyOf(targetDescriptors), PropertyDescriptor::getName);

            ignoredProperties = Stream.of(BeanUtils.getPropertyDescriptors(source.getClass()))
                // Keep invalid properties
                .filter(pd -> {
                    PropertyDescriptor targetDescriptor = targetProperties.get(pd.getName());
                    boolean ignored = targetDescriptor == null
                        || !targetDescriptor.getPropertyType().isAssignableFrom(pd.getPropertyType())
                        || Collection.class.isAssignableFrom(pd.getPropertyType()) // Ignore List, Collection, etc
                        || targetDescriptor.getWriteMethod() == null;
                    return ignored;
                })
                .map(PropertyDescriptor::getName)
                .toArray(String[]::new);

            // Add to cache
            cache.put(target.getClass(), ignoredProperties);
        }

        BeanUtils.copyProperties(source, target, org.apache.commons.lang3.ArrayUtils.addAll(ignoredProperties, exceptProperties));
    }

    public static boolean beanIsEmpty(Object bean, String... ignoredAttributes) {
        if (bean == null)
            return true;

        return ArrayUtils.stream(bean.getClass().getDeclaredFields())
            .filter(field -> !org.apache.commons.lang3.ArrayUtils.contains(ignoredAttributes, field.getName()))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .allMatch(field -> ObjectUtils.isEmpty((Object) getProperty(bean, field.getName())));
    }

    /**
     * Compute equality of 2 Map
     * should return true if :
     * - both map is exact same object
     * - both are null
     * - both have same size and each entry set of first map are also present in the second
     *
     * @param map1 first map
     * @param map2 second map
     * @return true if they are equal
     */
    public static <K, V> boolean mapsAreEquals(Map<K, V> map1, Map<K, V> map2) {
        if (map1 == map2)
            return true;
        if (map1 == null || map2 == null || map1.size() != map2.size())
            return false;
        if (map1 instanceof IdentityHashMap || map2 instanceof IdentityHashMap)
            throw new IllegalArgumentException("Cannot compare IdentityHashMap's");
        return map1.entrySet().stream()
            .allMatch(e -> e.getValue().equals(map2.get(e.getKey())));
    }

    public static <E> int lastIndexOf(List<E> list, Predicate<E> predicate) {
        for (ListIterator<E> iter = list.listIterator(list.size()); iter.hasPrevious(); )
            if (predicate.test(iter.previous()))
                return iter.nextIndex();
        return -1;
    }

    public static Integer hashCode(Collection<?> beans) {
        return getStream(beans)
            .map(item -> item.hashCode())
            .reduce((h1, h2) -> h1 * h2)
            .orElse(null);
    }
}

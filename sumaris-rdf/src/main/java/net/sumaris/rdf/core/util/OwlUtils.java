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

package net.sumaris.rdf.core.util;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.rdf.core.model.ModelURIs;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public abstract class OwlUtils {

    public static String ADAGIO_PREFIX = "http://www.e-is.pro/2019/03/adagio/";
    public static ZoneId ZONE_ID = ZoneId.systemDefault();
    public static String DATE_ISO_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_ISO_TIMESTAMP);
    public static SimpleDateFormat DATE_ISO_FORMAT = new SimpleDateFormat(DATE_ISO_TIMESTAMP);

    public static Method getterOfField(Class t, String field) {
        return findGetterOfField(t, field)
            .orElseThrow(() -> new SumarisTechnicalException(String.format("Class %s has no getter for %s", t.getSimpleName(), field)));
    }

    public static Optional<Method> findGetterOfField(Class t, String field) {
        try {
            Method res = t.getMethod("get" + field.substring(0, 1).toUpperCase() + field.substring(1));
            return Optional.of(res);
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }

    }

    public static Optional<Method> setterOfField(Resource schema, Class t, String field, RdfOwlConversionContext context) {
        try {
            Optional<Field> f = fieldOf(schema, t, field, context);
            if (f.isPresent()) {
                String setterName = "set" + f.get().getName().substring(0, 1).toUpperCase() + f.get().getName().substring(1);
                //log.info("setterName " + setterName);
                Method met = t.getMethod(setterName, f.get().getType());
                return Optional.of(met);
            }

        } catch (NoSuchMethodException e) {
            log.warn("NoSuchMethodException setterOfField " + field);
        } catch (NullPointerException e) {
            log.warn("NullPointerException setterOfField " + field);
        }
        return Optional.empty();
    }

    public static Map<Class, Resource> JAVA2XSD_TYPE_MAP = ImmutableMap.<Class, Resource>builder()
        .put(Date.class, XSD.date)
        .put(LocalDateTime.class, XSD.dateTime)
        .put(Timestamp.class, XSD.dateTimeStamp)
        .put(Integer.class, XSD.integer)
        .put(Short.class, XSD.xshort)
        .put(Long.class, XSD.xlong)
        .put(Double.class, XSD.xdouble)
        .put(Float.class, XSD.xfloat)
        .put(Boolean.class, XSD.xboolean)
        .put(long.class, XSD.xlong)
        .put(int.class, XSD.integer)
        .put(float.class, XSD.xfloat)
        .put(double.class, XSD.xdouble)
        .put(short.class, XSD.xshort)
        .put(boolean.class, XSD.xboolean)
        .put(String.class, XSD.xstring)
        .put(void.class, RDFS.Literal)
        .build();

    public static List<Class> ACCEPTED_LIST_CLASS = Arrays.asList(List.class, ArrayList.class, Set.class);


    public static Optional<Field> fieldOf(Resource schema, Class t, String name, RdfOwlConversionContext context) {
        try {

            Class ret = context.URI_2_CLASS.get(t.getSimpleName());
            if (ret == null) {
                log.error("error fieldOf " + classToURI(schema, t) + " " + name);
                return Optional.empty();
            } else {
                return Optional.of(ret.getDeclaredField(name));

            }
        } catch (NoSuchFieldException e) {
            log.error("error fieldOf " + t.getSimpleName() + " " + name + " - " + e.getMessage());
        }
        return null;
    }


    public static String classToURI(Resource schema, Class type) {
        return ModelURIs.getTypeUri(schema, type);
    }

    public static String classToURI(String schemaUri, Class type) {
        return ModelURIs.getTypeUri(schemaUri, type);
    }

    public static String beanToURI(Resource schema, Class type) {
        return ModelURIs.getDataUri(schema, type);
    }

    public static String beanToURI(String schemaUri, Class type) {
        return ModelURIs.getDataUri(schemaUri, type);
    }

    public static boolean isJavaType(Type type) {
        return JAVA2XSD_TYPE_MAP.keySet().stream().anyMatch(type::equals);
    }

    public static boolean isJavaType(Method getter) {
        return isJavaType(getter.getGenericReturnType());
    }

    public static boolean isJavaType(Field field) {
        return isJavaType(field.getType());
    }

    public static boolean isDateType(Field field) {
        return Date.class.isAssignableFrom(field.getType()) || Timestamp.class.isAssignableFrom(field.getType());
    }

    /**
     * check the getter and its corresponding field's annotations
     *
     * @param met the getter method to test
     * @return true if it is a technical id to exclude from the model
     */
    public static boolean isId(Method met) {
        return "getId".equals(met.getName())
            && Stream.concat(getFieldAnnotations(getOptionalFieldByGetter(met)), Stream.of(met.getAnnotations()))
            .anyMatch(annot -> annot instanceof Id || annot instanceof org.springframework.data.annotation.Id);
    }

    /**
     * check the getter and its corresponding field's annotations
     *
     * @param field the field to test
     * @return true if it is a technical id to exclude from the model
     */
    public static boolean isId(Field field) {
        return "id".equals(field.getName())
            && Arrays.stream(field.getAnnotations())
            .anyMatch(annot -> annot instanceof Id || annot instanceof org.springframework.data.annotation.Id);
    }

    public static boolean isManyToOne(Method met) {
        return getFieldAnnotations(getOptionalFieldByGetter(met)).anyMatch(annot -> annot instanceof ManyToOne) // check the corresponding field's annotations
            ||
            Stream.of(met.getAnnotations()).anyMatch(annot -> annot instanceof ManyToOne)  // check the method's annotations
            ;
    }

    public static Stream<Annotation> getFieldAnnotations(Optional<Field> field) {
        return field.map(field1 -> Arrays.stream(field1.getAnnotations())).orElseGet(Stream::empty);
    }

    public static boolean isGetter(Method met) {
        return met.getName().startsWith("get") // only getters
            && !"getBytes".equals(met.getName()) // ignore ugly
            && met.getParameterCount() == 0 // ignore getters that are not getters
            && getOptionalFieldByGetter(met).isPresent()
            ;
    }

    public static Class cleanProxyClass(Class clazz) {
        int index = clazz.getCanonicalName().indexOf("$HibernateProxy");
        if (index != -1) {
            String entityClassName = clazz.getCanonicalName().substring(0, index);
            try {
                return Class.forName(entityClassName);
            } catch (ClassNotFoundException e) {
            }
        }

        return clazz;
    }

    public static Method findIdGetter(Class clazz) {
        for (Field f : clazz.getDeclaredFields())
            for (Annotation an : f.getDeclaredAnnotations())
                if (an instanceof Id)
                    return getterOfField(clazz, f.getName());

        return null;
    }

    public static boolean isSetter(Method met) {
        return met.getName().startsWith("set");
    }

    public static Field getFieldOrNullByGetter(Method getter) {
        String fieldName = getter.getName().substring(3, 4).toLowerCase() + getter.getName().substring(4);
        try {
            return getter.getDeclaringClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null; // this is never going to happen right ?
        }
    }


    public static Optional<Field> getOptionalFieldByGetter(Method getter) {

        String fieldName = getter.getName().substring(3, 4).toLowerCase() + getter.getName().substring(4);
        //log.info("searching field : " + fieldName);
        try {
            return Optional.of(getter.getDeclaringClass().getDeclaredField(fieldName));
        } catch (Exception e) {
            //log.error("field not found : " + fieldName + " for class " + getter.getDeclaringClass() + "  " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Resource getStdType(Field f) {
        return JAVA2XSD_TYPE_MAP.getOrDefault(f.getType(), RDFS.Literal);
    }

    public static Resource getStdType(Type type) {
        return JAVA2XSD_TYPE_MAP.getOrDefault(type, RDFS.Literal);
    }


    // =============== List handling ===============


    public static boolean isListType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;// This would be Class<List>, say
            Type raw = parameterized.getRawType();

            return (ACCEPTED_LIST_CLASS.stream() // add set LinkedList... if you wish
                .anyMatch(x -> x.getCanonicalName().equals(raw.getTypeName())));
        }

        return false;

    }

    public static Type getListParametrizedType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;// This would be Class<List>, say
            Type raw = parameterized.getRawType();
            Type own = parameterized.getOwnerType();
            Type[] typeArgs = parameterized.getActualTypeArguments();

            if (ACCEPTED_LIST_CLASS.stream()
                .anyMatch(x -> x.getCanonicalName().equals(raw.getTypeName()))) {
                return typeArgs[0];
            }
        }
        return null;
    }


    // =============== Define relation  ===============

    public static void createOneToMany(OntModel ontoModel, OntClass ontoClass, OntProperty prop, Resource resource) {
        OntClass minCardinalityRestriction = ontoModel.createMinCardinalityRestriction(null, prop, 1);
        ontoClass.addSuperClass(minCardinalityRestriction);
    }

    public static void createZeroToMany(OntModel ontoModel, OntClass ontoClass, OntProperty prop, Resource resource) {
        OntClass minCardinalityRestriction = ontoModel.createMinCardinalityRestriction(null, prop, 0);
        ontoClass.addSuperClass(minCardinalityRestriction);
    }

    public static void createZeroToOne(OntModel ontoModel, OntClass ontoClass1, OntProperty prop, OntClass ontoClass2) {
        OntClass maxCardinalityRestriction = ontoModel.createMaxCardinalityRestriction(null, prop, 1);
        ontoClass1.addSuperClass(maxCardinalityRestriction);
    }

    public static void createOneToOne(OntModel ontoModel, OntClass ontoClass1, OntProperty prop, OntClass ontoClass2) {
        OntClass maxCardinalityRestriction = ontoModel.createMaxCardinalityRestriction(null, prop, 1);
        ontoClass1.addSuperClass(maxCardinalityRestriction);
    }


    /**
     * Serialize model in requested format
     *
     * @param model  input model
     * @param format output format if null then output to RDF/XML
     * @return a string representation of the model
     */
    public static String toString(Model model, String format) {

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            if (format == null) {
                model.write(os);
            } else {
                model.write(os, format);
            }
            os.flush();
            os.close();
            return new String(os.toByteArray(), "UTF8");
        } catch (IOException e) {
            log.error("doWrite ", e);
        }
        return "there was an error writing the model ";
    }



    public static LocalDate convertToLocalDateViaMilisecond(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
            .atZone(ZONE_ID)
            .toLocalDate();
    }

    public static LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
            .atZone(ZONE_ID)
            .toLocalDate();
    }

    public static Date convertToDateViaInstant(LocalDateTime dateToConvert) {
        return Date
            .from(dateToConvert.atZone(ZONE_ID)
                .toInstant());
    }


}

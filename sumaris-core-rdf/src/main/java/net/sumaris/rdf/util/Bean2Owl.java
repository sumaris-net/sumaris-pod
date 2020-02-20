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

package net.sumaris.rdf.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;
import static net.sumaris.rdf.util.OwlUtils.*;

public class Bean2Owl {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(Bean2Owl.class);


    private String modelPrefix;
    private boolean debug;
    private Map<Class, Object> getIdMethodByClass = Maps.newHashMap();

    public Bean2Owl(String modelPrefix) {
        this.modelPrefix = modelPrefix;
        this.debug = log.isDebugEnabled();
    }

    protected String getModelUriPrefix() {
        return modelPrefix;
    }

    /**
     * Returns and adds OntClass to the model
     *
     * @param ontology
     * @param clazz
     * @return
     */
    public OntClass classToOwl(OntModel ontology, Class clazz, Map<OntClass, List<OntClass>> mutuallyDisjoint, boolean addInterface) {

        Resource schema = ontology.listSubjectsWithProperty(RDF.type, OWL.Ontology).nextResource();

        if (debug) log.debug(String.format("Converting class {%s} to ontology...", clazz.getSimpleName()));

        try {

            OntClass aClass = ontology.createClass(classToURI(schema, clazz));
            aClass.setIsDefinedBy(schema);

            String label = clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
            aClass.addLabel(label, "en");

            String entityName = clazz.getName();
            if (!"".equals(entityName))
                aClass.addComment(entityName, "en");

            if (mutuallyDisjoint != null && addInterface) {
                Stream.of(clazz.getGenericInterfaces())
                        .filter(interfaze -> !ParameterizedType.class.equals(interfaze))
                        .forEach(interfaze -> {
                            if (debug) log.debug(String.format("%s %s %s", interfaze, Class.class, interfaze.getClass()));
                            OntClass r = typeToUri(schema, interfaze);
                            if (!mutuallyDisjoint.containsKey(r)) {
                                mutuallyDisjoint.put(r, new ArrayList<>());
                            }
                            mutuallyDisjoint.get(r).add(aClass);

                            aClass.addSuperClass(r);
                        });
            }


            Stream.of(clazz.getGenericSuperclass())
                    .filter(c -> c != null && !Object.class.equals(c))
                    .forEach(i -> {
                                OntClass ont = typeToUri(schema, i);
                                aClass.addSuperClass(ont);
                            }
                    );

            Stream.of(clazz.getMethods())
                    .filter(OwlUtils::isGetter)
                    .map(OwlUtils::getFieldOfGetteR)
                    .forEach(field -> {
                        //LOG.info("processing Field : " + field + " - type?" + isJavaType(field) + " - list?" + isListType(field.getGenericType()));
                        String fieldName = classToURI(schema, clazz) + "#" + field.getName();

                        if (isJavaType(field)) {
                            Resource type = getStdType(field);

                            OntProperty stdType = ontology.createDatatypeProperty(fieldName, true);

                            stdType.setDomain(aClass.asResource());
                            stdType.setRange(type);
                            stdType.setIsDefinedBy(schema);

                            //link.addRDFType(ontology.createResource(type));
                            stdType.addLabel(field.getName(), "en");
                            //LOG.info("Simple property of type " + type + " for " + fieldName + "\n" + link);*

                        } else if (field.getDeclaringClass().isArray()) {

                        } else if (isListType(field.getGenericType())) {
                            Type contained = getListType(field.getGenericType());
                            OntProperty list = null;
                            Resource resou = null;
                            if (debug) log.debug(String.format("List property %s %s", fieldName, contained.getTypeName()));

                            if (isJavaType(contained)) {
                                list = ontology.createDatatypeProperty(fieldName, true);
                                resou = getStdType(contained);
                            } else {
                                list = ontology.createObjectProperty(fieldName, false);
                                resou = typeToUri(schema, contained);
                            }

                            list.addRange(resou);
                            list.addDomain(aClass.asResource());
                            list.setIsDefinedBy(schema);
                            list.addLabel(field.getName(), "en");

                            createZeroToMany(ontology, aClass, list, resou);

                        } else {

                            // var type = ontology.createObjectProperty();
                            OntProperty bean = ontology.createObjectProperty(fieldName, true);
                            bean.addDomain(aClass.asResource());
                            bean.addRange(typeToUri(schema, field.getType()));
                            bean.setIsDefinedBy(schema);
                            bean.addLabel(field.getName(), "en");
                            // LOG.info("Default Object property x " + link);

                        }
                    });
            return aClass;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    protected OntClass typeToUri(Resource schema, Type t) {

        OntModel model = ((OntModel) schema.getModel());

        String uri = schema + t.getTypeName();
        if (t instanceof ParameterizedType) {
            uri = uri.substring(0, uri.indexOf("<"));
        }

        uri = uri.substring(uri.lastIndexOf(".") + 1);

        OntClass ont = model.getOntClass(uri);

        if (ont == null) {

            String name = t.getTypeName();
            name = name.substring(name.lastIndexOf(".") + 1);

            ont = model.createClass(schema + name);
        }

        ont.setIsDefinedBy(schema);
        ont.addComment(t.getTypeName(), "en");

        return ont;

    }

    protected OntClass interfaceToOwl(OntModel model, Type type) {

        String name = type.getTypeName();
        name = name.substring(name.lastIndexOf(".") + 1);

        return model.createClass(getModelUriPrefix() + name);
    }

    public Resource bean2Owl(OntModel model,
                             String modelUri,
                             Object obj, int depth,
                             List<Method> includes,
                             List<Method> excludes) {
        Preconditions.checkNotNull(obj);

        Class clazz = obj.getClass();
        if (debug) log.debug(String.format("Converting object {%s} to ontology...", clazz.getSimpleName()));

        // try using the ID field if exists to represent the node
        String individualURI;
        try {
            Method m = findGetterAnnotatedID(clazz);
            individualURI = beanToURI(modelUri,  clazz) + "/" + m.invoke(obj);
        } catch (Exception e) {
            if (debug) log.error(String.format("Cannot get ID on class {%s}", clazz.getSimpleName()));
            return null;
        }

        String classSchemaURI = classToURI(modelUri, clazz);
        Resource ontClass = model.getResource(classSchemaURI);
        if (ontClass == null) ontClass = model.createResource(classSchemaURI);
        Resource individual = model.createIndividual(individualURI, ontClass);
        if (depth < 0) {
            if (debug) log.warn("Max depth reached!");
            return individual;
        }

        // Handle Methods
        Stream.of(clazz.getMethods())
                .filter(OwlUtils::isGetter)
                .filter(getter -> {
                    boolean exclude = excludes.stream().anyMatch(x -> x.equals(getter)) || isManyToOne(getter);
                    boolean include = includes.contains(getter);
                    if (debug) log.debug(String.format(" filtering %s {include: %s, exclude: %s}", getter, include, exclude));
                    return (!exclude || include);
                })
                .forEach(getter -> {
                    if (debug) log.debug("processing method " + getter.getDeclaringClass().getSimpleName()+"."+ getter.getName()+" "+getter.getGenericReturnType());

                    try {
                        Object propertyValue = getter.invoke(obj);
                        if (propertyValue == null) return; // Stop here

                        Property propertyResource = model.createProperty(classSchemaURI, "#" + getter.getName().replace("get", ""));

                        if (isId(getter)) {
                            individual.addProperty(propertyResource, propertyValue + "");
                        } else if ("getClass".equals(getter.getName())) {
                            individual.addProperty(RDF.type, propertyResource);
                        } else if (propertyValue.getClass().getCanonicalName().contains("$")) {
                            // Inner class: Skip
                        } else if (!isJavaType(getter)) {
                            if (debug) {
                                log.debug(" recurse for " + getter.getName());
                                log.debug(" not java generic, recurse on node..." + propertyValue);
                            }

                            Resource propertyValueResource = bean2Owl(model, modelUri, propertyValue, (depth - 1), includes, excludes);
                            if (propertyValueResource != null) individual.addProperty(propertyResource, propertyValueResource);

                        } else if (getter.getGenericReturnType() instanceof ParameterizedType) {

                            Resource anonId = model.createResource(new AnonId("params" + new Random().nextInt(1000000)));
                            Optional<Resource> listNode = fillDataList(model, modelUri, getter.getGenericReturnType(), propertyValue, propertyResource, anonId, depth - 1,
                                    includes,
                                    excludes);
                            if (listNode.isPresent()) {
                                if (debug) log.debug(" --and res  : " + listNode.get().getURI());
                                individual.addProperty(propertyResource, listNode.get());
                            }
                        } else {
                            if (getter.getName().toLowerCase().contains("date")) {
                                individual.addProperty(propertyResource, SIMPLE_DATE_FORMAT.format((Date) propertyValue));

                            } else {
                                individual.addProperty(propertyResource, propertyValue + "");
                            }
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }

                });

        return individual;
    }


    protected Method findGetterAnnotatedID(Class clazz) {
        for (Field f : clazz.getDeclaredFields())
            for (Annotation an : f.getDeclaredAnnotations())
                if (an instanceof Id)
                    return getterOfField(clazz, f.getName());

        return null;
    }

    protected Optional<Resource> fillDataList(OntModel model,
                                              String modelUri,
                                              Type type,
                                              Object listObject,
                                              Property prop,
                                              Resource fieldId,
                                              int depth,
                                              List<Method> includes,
                                              List<Method> excludes) {

        if (isListType(type)) {

            // Create a list containing the subjects of the role assignments in one go
            List<RDFNode> nodes = new ArrayList<>();
            List<? extends Object> asList = getList((List<? extends Object>) listObject, Object.class);

            if (asList.isEmpty()) {
                if (debug) log.debug(" - empty list, ignoring ");
                return Optional.empty();
            }
            for (Object x : asList) {
                Resource listItem = bean2Owl(model, modelUri, x, (depth - 1), includes, excludes);
                nodes.add(listItem);
            }

            RDFList list = model.createList(nodes.toArray(new RDFNode[nodes.size()]));

            if (debug) log.debug(String.format("  - rdflist {size: %s}: %s", list.size(), list));

            fieldId.addProperty(prop, list);

            return Optional.of(list);
        }

        return Optional.empty();
    }

    /**
     * Performs a forced cast.
     * Returns null if the collection type does not match the items in the list.
     *
     * @param data     The list to cast.
     * @param listType The type of list to cast to.
     */
    protected <T> List<? super T> getList(List<?> data, Class<T> listType) {
        if (data != null && !data.isEmpty() && listType.isInstance(data.iterator().next().getClass())) {
            List<T> foo = (List<T>) data;
            return foo;
        }
        return (List<T>) data;
    }


}

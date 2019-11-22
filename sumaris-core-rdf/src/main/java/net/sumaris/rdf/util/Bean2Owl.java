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

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
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
    private static Logger LOG = LoggerFactory.getLogger(Bean2Owl.class);

    private String modelPrefix;

    public Bean2Owl(String modelPrefix) {
        this.modelPrefix = modelPrefix;
    }

    protected String getModelPrefix() {
        return modelPrefix;
    }

    /**
     * Returns and adds OntClass to the model
     *
     * @param ontology
     * @param clazz
     * @return
     */
    public OntClass classToOwl(OntModel ontology, Class clazz, Map<OntClass, List<OntClass>> mutuallyDisjoint, boolean addInterface, boolean addMethods) {

        Resource schema = ontology.listSubjectsWithProperty(RDF.type, OWL.Ontology).nextResource();

        try {
            //OntResource r = m.createOntResource(SCHEMA_URL+ ent.getName().replaceAll("\\.", "/"));
            //OntProperty pred = m.createOntProperty(SCHEMA_URL+ ent.getName().replaceAll("\\.", "/"));
            //String classBase = ent.getName().replaceAll("\\.", "/");
//                        LOG.info("Building Ont of " + classBase);

            OntClass aClass = ontology.createClass(classToURI(schema, clazz));
            aClass.setIsDefinedBy(schema);

            //aClass.addSameAs(ontology.createResource(classToURI(schema, ent)));

            String label = clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1);
            //aClass.addLabel(label + "e", "fr");
            aClass.addLabel(label, "en");

            String entityName = clazz.getName();
            if (!"".equals(entityName))
                aClass.addComment(entityName, "en");


            if (mutuallyDisjoint != null && addInterface) {
                Stream.of(clazz.getGenericInterfaces())
                        .filter(interfaze -> !ParameterizedType.class.equals(interfaze))
                        .forEach(interfaze -> {
                            // LOG.info(interfaze+" "+Class.class + " " +  interfaze.getClass() );
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


            if (addMethods) {
                Stream.of(clazz.getMethods())
                        .filter(m -> !isSetter(m))
                        .filter(m -> !isGetter(m))
                        .filter(m -> Stream.of("getBytes", "hashCode", "getClass", "toString", "equals", "wait", "notify", "notifyAll").noneMatch(s -> s.equals(m.getName())))

                        .forEach(met -> {

                            String name = classToURI(schema, clazz) + "#" + met.getName();
                            OntProperty function = ontology.createObjectProperty(name, true);
                            function.addDomain(aClass.asResource());
                            if (isJavaType(met.getReturnType())) {
                                function.addRange(getStdType(met.getReturnType()));
                            } else {
                                OntClass o = typeToUri(schema, met.getReturnType());
                                if (o.getURI().endsWith("void"))
                                    function.addRange(RDFS.Literal);
                                else
                                    function.addRange(o);
                            }
                            function.setIsDefinedBy(schema);
                            function.addLabel(met.getName(), "en");
                        });
            }


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
                            //LOG.info("List property " + fieldName + " " + contained.getTypeName());

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
                            list.addLabel("list" + field.getName(), "en");

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
            LOG.error(e.getMessage(), e);
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
        //ont.addLabel(t.getTypeName(), "en");

        return ont;

    }

    protected OntClass interfaceToOwl(OntModel model, Type type) {

        String name = type.getTypeName();
        name = name.substring(name.lastIndexOf(".") + 1);

        return model.createClass(getModelPrefix() + name);
    }

    public Resource bean2Owl(OntModel model, Object obj, int depth,
                             List<Method> includes,
                             List<Method> excludes) {
        Resource schema = model.listSubjectsWithProperty(RDF.type, OWL.Ontology).nextResource();

        if (obj == null) {
            LOG.error("bean2Owl received a null object as parameter");
            return null;
        }
        String classURI = classToURI(schema, obj.getClass());
        OntClass ontClazz = model.getOntClass(classURI);
        if (ontClazz == null) {
            LOG.warn("ontClazz " + ontClazz + ", not found in model, mak" +
                    "ing one at " + classURI);
            ontClazz = classToOwl(model, obj.getClass(), null, true, true);
        }

        // try using the ID field if exists to represent the node
        String individualURI;
        try {
            Method m = findGetterAnnotatedID(obj.getClass());
            individualURI = classURI + "#" + m.invoke(obj);
            //LOG.info("Created objectIdentifier " + individualURI);
        } catch (Exception e) {
            individualURI = "";
            LOG.error(e.getClass().getName() + " bean2Owl " + classURI + " - ");
        }
        Resource individual = ontClazz.createIndividual(individualURI); // model.createResource(node);
        if (depth < 0) {
            LOG.error("Max depth reached " + depth);
            return individual;
        } else {
            // LOG.warn("depth reached " + depth);

        }


        // Handle Methods
        Stream.of(obj.getClass().getMethods())
                .filter(OwlUtils::isGetter)
                .filter(met -> excludes.stream().noneMatch(x -> x.equals(met)))
                .filter(met -> {
                    //LOG.info(" filtering on " + met +"  " +  WHITELIST.contains(met) + " "+ !isManyToOne(met) ) ;

                    // LOG.info(" -- " + WHITELIST.size() + "  " + BLACKLIST.size() + "  " + URI_2_CLASS.size());

                    return (!isManyToOne(met) || includes.contains(met));
                })
                .forEach(met -> {

                    //LOG.info("processing method " + met.getDeclaringClass().getSimpleName()+"."+ met.getName()+" "+met.getGenericReturnType());
                    try {
                        Object invoked = met.invoke(obj);
                        if (invoked == null) {
                            //LOG.warn("invoked function null "+ met.getName() + " skipping... " );
                            return;
                        }
                        Property pred = model.createProperty(classURI, "#" + met.getName().replace("get", ""));

                        if (isId(met)) {
                            individual.addProperty(pred, invoked + "");
                        } else if ("getClass".equals(met.getName())) {
                            individual.addProperty(RDF.type, pred);
                        } else if (invoked.getClass().getCanonicalName().contains("$")) {
                            //skip inner classes. mostly handles generated code issues
                        } else if (!isJavaType(met)) {
                            //LOG.warn("recurse for " + met.getName());
                            //LOG.info("not java generic, recurse on node..." + invoked);
                            Resource recurse = bean2Owl(model, invoked, (depth - 1), includes, excludes);
                            if (recurse != null)
                                individual.addProperty(pred, recurse);
                        } else if (met.getGenericReturnType() instanceof ParameterizedType) {

                            Resource anonId = model.createResource(new AnonId("params" + new Random().nextInt(1000000)));
                            //individual.addProperty( pred, anonId);

                            Optional<Resource> listNode = fillDataList(model, met.getGenericReturnType(), invoked, pred, anonId, depth - 1,
                                    includes,
                                    excludes);
                            if (listNode.isPresent()) {
                                LOG.info(" --and res  : " + listNode.get().getURI());
                                individual.addProperty(pred, listNode.get());
                            }
                            //
                        } else {
                            if (met.getName().toLowerCase().contains("date")) {
                                //String d = DATE_TIME_FORMATTER.format(((Date) invoked).toInstant());
                                individual.addProperty(pred, SIMPLE_DATE_FORMAT.format((Date) invoked));

                            } else {
                                individual.addProperty(pred, invoked + "");
                            }
                        }

                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
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

    protected Optional<Resource> fillDataList(OntModel model, Type type, Object listObject, Property prop, Resource fieldId, int depth,
                                            List<Method> includes,
                                            List<Method> excludes) {

        if (isListType(type)) {

// Create a list containing the subjects of the role assignments in one go

            List<RDFNode> nodes = new ArrayList<>();
            List<? extends Object> asList = castListSafe((List<? extends Object>) listObject, Object.class);

            if (asList.isEmpty()) {
                LOG.warn(" - empty list, ignoring ");
                return Optional.empty();
            }
            for (Object x : asList) {
                Resource listItem = bean2Owl(model, x, (depth - 1), includes, excludes);
                nodes.add(listItem);

            }

            RDFList list = model.createList(nodes.toArray(new RDFNode[nodes.size()]));

            LOG.info("  - rdflist " + list.size() + " : " + list);
//var tmp = model.createProperty("sdfsdfsdf"+new Random().nextInt(10000000));
            fieldId.addProperty(prop, list);
            //              fieldId.addProperty(tmp ,model.createList(list));
            return Optional.of(list);

        }

//        }
        return Optional.empty();
    }

    /**
     * Performs a forced cast.
     * Returns null if the collection type does not match the items in the list.
     *
     * @param data     The list to cast.
     * @param listType The type of list to cast to.
     */
    protected <T> List<? super T> castListSafe(List<?> data, Class<T> listType) {
        List<T> retval = null;
        //This test could be skipped if you trust the callers, but it wouldn't be safe then.
        if (data != null && !data.isEmpty() && listType.isInstance(data.iterator().next().getClass())) {
            LOG.info("  - castListSafe passed check ");

            @SuppressWarnings("unchecked")//It's OK, we know List<T> contains the expected type.
                    List<T> foo = (List<T>) data;
            return foo;
        }

        LOG.info("  - castListSafe failed check  forcing it though");
        return (List<T>) data;
    }


}

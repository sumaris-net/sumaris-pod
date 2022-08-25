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

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.referential.IWithUriEntity;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.core.dao.OntologyEntitiesDao;
import net.sumaris.rdf.core.model.ModelURIs;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class Bean2Owl {

    private OntologyEntitiesDao dao;
    private boolean debug;

    public Bean2Owl(OntologyEntitiesDao dao) {
        this.dao = dao;
        this.debug = log.isDebugEnabled();
    }

    public OntClass classToOwl(OntModel ontology, Class clazz, Multimap<OntClass, OntClass> mutuallyDisjoint, boolean withInterfaces) {
        Resource schema = ontology.listSubjectsWithProperty(RDF.type, OWL.Ontology).nextResource();
        return classToOwl(ontology, OwlUtils.classToURI(schema, clazz), clazz, mutuallyDisjoint, withInterfaces);
    }
    /**
     * // Create the ontology class, from a java class
     *
     * @param ontology
     * @param clazz
     * @return
     */
    public OntClass classToOwl(OntModel ontology, String classUri, Class clazz, Multimap<OntClass, OntClass> mutuallyDisjoint, boolean withInterfaces) {

        Resource schema = ontology.listSubjectsWithProperty(RDF.type, OWL.Ontology).nextResource();

        if (debug) log.debug(String.format("Converting class {%s} to ontology...", clazz.getSimpleName()));

        try {
            OntClass aClass = ontology.createClass(classUri);
            aClass.setIsDefinedBy(schema);
            aClass.addLabel(clazz.getSimpleName(), "en");
            aClass.addComment(clazz.getName(), "en"); // Class name into comment

            if (mutuallyDisjoint != null && withInterfaces) {
                Stream.of(clazz.getGenericInterfaces())
                    .filter(interfaze -> !ParameterizedType.class.equals(interfaze))
                    .forEach(interfaze -> {
                        if (debug) log.debug(String.format("%s %s %s", interfaze, Class.class, interfaze.getClass()));
                        OntClass interfaceUri = typeToUri(schema, interfaze);
                        if (!mutuallyDisjoint.containsEntry(interfaceUri, aClass)) {
                            mutuallyDisjoint.put(interfaceUri, aClass);
                        }

                        aClass.addSuperClass(interfaceUri);
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
                .map(OwlUtils::getFieldOrNullByGetter)
                .forEach(field -> {
                    String propertyUri = classUri + "#" + field.getName();

                    // Simple java type
                    if (OwlUtils.isJavaType(field)) {
                        Resource range = OwlUtils.getStdType(field);

                        OntProperty property = ontology.createDatatypeProperty(propertyUri, true);

                        property.setDomain(aClass.asResource());
                        property.setRange(range);
                        property.setIsDefinedBy(schema);

                        property.addLabel(field.getName(), "en");

                    } else if (field.getDeclaringClass().isArray()) {
                        log.warn("Property {} is an array: NOT implemented yet. Skip", propertyUri);
                    }

                    // List
                    else if (OwlUtils.isListType(field.getGenericType())) {
                        Type listParametrizedType = OwlUtils.getListParametrizedType(field.getGenericType());
                        if (debug) log.debug(String.format("List property %s %s", propertyUri, listParametrizedType.getTypeName()));

                        OntProperty listProperty;
                        Resource range;
                        if (OwlUtils.isJavaType(listParametrizedType)) {
                            listProperty = ontology.createDatatypeProperty(propertyUri, true);

                            // TODO: use Bag for Set ?
                            range = OwlUtils.getStdType(listParametrizedType);
                        } else {
                            listProperty = ontology.createObjectProperty(propertyUri, false);
                            range = typeToUri(schema, listParametrizedType);
                        }

                        listProperty.addRange(range);
                        listProperty.addDomain(aClass.asResource());
                        listProperty.setIsDefinedBy(schema);
                        listProperty.addLabel(field.getName(), "en");

                        // TODO: add a description ? comment ?
                        // list.addComment(, "en");

                        OwlUtils.createZeroToMany(ontology, aClass, listProperty, range);

                    }

                    // Another entity
                    else {

                        if (debug) log.debug(String.format("Entity property %s %s", propertyUri, field.getType().getSimpleName()));

                        // var type = ontology.createObjectProperty();
                        OntProperty property = ontology.createObjectProperty(propertyUri, true);
                        property.addDomain(aClass.asResource());
                        property.addRange(typeToUri(schema, field.getType()));
                        property.setIsDefinedBy(schema);
                        property.addLabel(field.getName(), "en");
                        // LOG.info("Default Object property x " + link);

                        // TODO: add a description ? comment ?
                        // bean.addComment(, "en");

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

        String typeUri = dao.findTypeUri(t)
            .orElseGet(() -> ModelURIs.getTypeUri(schema, t));

        OntClass ont = model.getOntClass(typeUri);

        if (ont == null) {
            ont = model.createClass(typeUri);
        }
        ont.setIsDefinedBy(schema);
        ont.addComment(t.getTypeName(), "en");

/*        if (typeUri.startsWith(schema.getURI())) {
        }
        else {

            String schemaUri = typeUri.substring(0, )
            ont.setIsDefinedBy(schema);
            ont.addComment(t.getTypeName(), "en");

        }
*/
        return ont;

    }

    public Resource bean2Owl(Model model,
                             String schemaUri,
                             Object obj, int depth,
                             List<Method> includes,
                             List<Method> excludes) {
        return bean2Owl(model, schemaUri, obj, depth, includes, excludes, obj.getClass());
    }

    public Resource bean2Owl(Model model,
                             String schemaUri,
                             Object obj, int depth,
                             List<Method> includes,
                             List<Method> excludes,
                             Class<?> clazz) {
        Preconditions.checkNotNull(obj);
        Preconditions.checkNotNull(clazz);

        // Remove proxy internal class, if any
        clazz = OwlUtils.cleanProxyClass(clazz);

        if (debug) log.debug(String.format("Converting object {%s} to ontology...", clazz.getSimpleName()));

        String individualUri = null;

        // Try to get entity URI
        if (obj instanceof IWithUriEntity) {
            individualUri = ((IWithUriEntity)obj).getUri();
        }

        // Or try using the ID field if exists to represent the node
        if (StringUtils.isBlank(individualUri)) {
            try {
                Method getIdMethod = OwlUtils.findIdGetter(clazz);
                individualUri = OwlUtils.beanToURI(schemaUri, clazz) + "/" + getIdMethod.invoke(obj);
            } catch (Exception e) {
                if (debug) log.error(String.format("Cannot find ID on class {%s}", clazz.getSimpleName()));
                return null;
            }
        }

        if (depth < 0) {
            if (debug) log.warn("Max depth reached!");
            return model.getResource(individualUri);
        }

        String classUri = OwlUtils.classToURI(schemaUri, clazz);
        Resource ontClass = model.getResource(classUri);
        if (ontClass == null) ontClass = model.createResource(classUri);
        Resource individual = model.createResource(individualUri, ontClass);

        // Handle Methods
        Stream.of(clazz.getMethods())
            .filter(OwlUtils::isGetter)
            .filter(getter -> {
                boolean exclude = excludes.stream().anyMatch(x -> x.equals(getter)) || OwlUtils.isManyToOne(getter);
                boolean include = includes.contains(getter);
                if (debug) log.debug(String.format(" filtering %s {include: %s, exclude: %s}", getter, include, exclude));
                return (!exclude || include);
            })
            .forEach(getter -> {
                Field field = OwlUtils.getFieldOrNullByGetter(getter);
                if (debug) log.debug("processing method " + field.getDeclaringClass().getSimpleName()+"."+ field.getName()+" "+field.getGenericType());

                try {
                    Object propertyValue = getter.invoke(obj);
                    if (propertyValue == null) return; // Stop here

                    Property propertyResource = model.createProperty(classUri, "#" + field.getName());

                    if (OwlUtils.isId(field)) {
                        individual.addProperty(propertyResource, propertyValue + "");
                    } else if ("class".equals(field.getName())) {
                        individual.addProperty(RDF.type, propertyResource);
                    } else if (field.getType().getCanonicalName().contains("$")) {
                        // Inner class: Skip
                        if (debug) {
                            log.debug(" skip inner class " + field.getName());
                        }
                    } else if (!OwlUtils.isJavaType(field)) {
                        if (debug) {
                            log.debug(" recurse for " + field.getName());
                            log.debug(" not java generic, recurse on node..." + propertyValue);
                        }

                        Resource propertyValueResource = bean2Owl(model, schemaUri, propertyValue, (depth - 1), includes, excludes, field.getType());
                        if (propertyValueResource != null) individual.addProperty(propertyResource, propertyValueResource);

                    } else if (field.getGenericType() instanceof ParameterizedType) {

                        Resource anonId = model.createResource(new AnonId("params" + new Random().nextInt(1000000)));
                        Optional<Resource> listNode = fillDataList(model, schemaUri, field.getGenericType(), propertyValue, propertyResource, anonId, depth - 1,
                            includes,
                            excludes);
                        if (listNode.isPresent()) {
                            if (debug) log.debug(" --and res  : " + listNode.get().getURI());
                            individual.addProperty(propertyResource, listNode.get());
                        }
                    } else if (OwlUtils.isDateType(field)) {

                        individual.addProperty(propertyResource, OwlUtils.DATE_ISO_FORMAT.format((Date) propertyValue), XSDDatatype.XSDdateTime);

                    } else {
                        individual.addProperty(propertyResource, propertyValue + "");
                    }

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

            });

        return individual;
    }




    protected Optional<Resource> fillDataList(Model model,
                                              String baseUri,
                                              Type type,
                                              Object listObject,
                                              Property prop,
                                              Resource fieldId,
                                              int depth,
                                              List<Method> includes,
                                              List<Method> excludes) {

        if (OwlUtils.isListType(type)) {

            // Create a list containing the subjects of the role assignments in one go
            List<RDFNode> nodes = new ArrayList<>();
            List<? extends Object> asList = getList((List<? extends Object>) listObject, Object.class);

            if (asList.isEmpty()) {
                if (debug) log.debug(" - empty list, ignoring ");
                return Optional.empty();
            }
            for (Object x : asList) {
                Resource listItem = bean2Owl(model, baseUri, x, (depth - 1), includes, excludes);
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

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import javax.persistence.EntityManager;
import javax.persistence.Id;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.sumaris.rdf.core.util.OwlUtils.isJavaType;
import static net.sumaris.rdf.core.util.OwlUtils.setterOfField;

@Slf4j
public abstract class Owl2Bean {

    private EntityManager entityManager;

    private String modelPrefix;

    public Owl2Bean(EntityManager entityManager, String modelPrefix) {
        this.entityManager = entityManager;
        this.modelPrefix = modelPrefix;
    }

    protected String getModelUriPrefix() {
        return modelPrefix;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    public Optional<Class> ontToJavaClass(OntClass ontClass, RdfOwlConversionContext context) {
        String uri = ontClass.getURI();
        if (uri != null) {
            if (uri.contains("#")) {
                uri = uri.substring(0, uri.indexOf("#"));
                log.warn(" tail '#'  " + uri);
            }
            if (uri.contains("<")) {
                uri = uri.substring(0, uri.indexOf("<"));
                log.warn(" tail <parametrized> " + uri);
            }
        }

        if (uri == null) {
            log.error(" uri null for OntClass " + ontClass);
            return Optional.empty();
        }


        String cName = uri.substring(uri.lastIndexOf("/") + 1);
        Class clazz = context.URI_2_CLASS.get(cName);

        if (clazz == null) {
            log.warn(" clazz not mapped for class " + cName);
            return Optional.empty();
        }

        if (clazz.isInterface()) {
            log.warn(" corresponding Type is interface, skip instances " + clazz);
            return Optional.empty();
        }
        return Optional.of(clazz);
    }

    protected boolean classEquals(Class c, Class<?> d) {
        return Objects.equals(d.getTypeName(), c.getTypeName());
    }

    protected abstract List getCacheTL();
    protected abstract List getCacheStatus();

    protected Object getTranslatedReference(RDFNode val, Class<?> setterParam, Object obj, RdfOwlConversionContext context) {

        String identifier = val.toString();
        String ontClass = null;
        if (identifier.contains("#")) {
            ontClass = identifier.substring(0, identifier.indexOf("#"));
            identifier = identifier.substring(identifier.indexOf("#") + 1);
        }
        if (setterParam == TaxonomicLevel.class) {

            for (Object ctl : getCacheTL()) {
                String lab = ((TaxonomicLevel) ctl).getLabel();
                if (identifier.endsWith(lab)) {
                    return ctl;
                }
            }


            // if none were cached, create a new TaxonomicLevel
            TaxonomicLevel tl = (TaxonomicLevel) context.URI_2_OBJ_REF.getOrDefault(val.toString(), new TaxonomicLevel());
            tl.setLabel(identifier);
            tl.setCreationDate(new Date());
            tl.setName("");
            tl.setRankOrder(1);
            tl.setStatus((Status) getCacheStatus().get(0));

            getEntityManager().persist(tl);
            log.warn("getEntityManager().persist(  TaxonomicLevel ) " + tl);

            //B2O_ARBITRARY_MAPPER.find(ontClass).apply( val.as(OntResource.class));

            return context.URI_2_OBJ_REF.putIfAbsent(val.toString(), tl);
        }


        // protected case, try to fetch reference (@Id) as Integer or String
        log.warn("getTranslatedReference " + identifier + " - " + val + " - " + obj);
        Object ref;
        try {
            Integer asInt = Integer.parseInt(identifier);
            ref = getEntityManager().getReference(setterParam, asInt);
        } catch (NumberFormatException e) {
            ref = getEntityManager().getReference(setterParam, identifier);
        }
        return ref;
    }

    protected String attributeOf(String pred) {
        String fName = pred.substring(pred.indexOf("#") + 1);
        fName = fName.substring(0, 1).toLowerCase() + fName.substring(1);
        return fName;
    }

    protected void fillObjectWithStdAttribute(Method setter, Object obj, RDFNode val) {
        String value = val.isURIResource() ? val.toString().substring(val.toString().lastIndexOf("#") + 1) : val.toString();
        Class<?> setterParam = setter.getParameterTypes()[0];
        try {
            if (classEquals(setterParam, String.class)) {
                setter.invoke(obj, val.asLiteral().getString());
            }
            if (classEquals(setterParam, Long.class) || classEquals(setterParam, long.class)) {
                setter.invoke(obj, val.asLiteral().getLong());
            } else if (classEquals(setterParam, Integer.class) || classEquals(setterParam, int.class)) {
                setter.invoke(obj, Integer.parseInt(value));
            } else if (classEquals(setterParam, Date.class)) {
                setter.invoke(obj, OwlUtils.DATE_ISO_FORMAT.parse(val.asLiteral().getString()));
            } else if (classEquals(setterParam, Boolean.class) || classEquals(setterParam, boolean.class)) {
                setter.invoke(obj, val.asLiteral().getBoolean());
            }
        } catch (Exception e) {
            log.warn("fillObjectWithStdAttribute could not reconstruct attribute "
                    + setter.getDeclaringClass().getSimpleName() + "." + setter.getName() + "(" + setterParam.getSimpleName() + ") for val " + val, e);
        }
    }

    public Optional<Object> owl2Bean(Resource ont, OntResource ontResource, Class clazz, RdfOwlConversionContext context) {
        log.info("processing ont Instance " + ontResource + " - " +
                ontResource
                        .asIndividual()
                        .listProperties().toList().size());

        try {
            Object obj = clazz.newInstance();

            ontResource
                    .asIndividual()
                    .listProperties()
                    .toList()
                    .forEach(stmt -> {
                        String pred = stmt.getPredicate().getURI();
                        RDFNode val = stmt.getObject();
                        if ((pred.startsWith(getModelUriPrefix()) || pred.startsWith(OwlUtils.ADAGIO_PREFIX)) && pred.contains("#")) {
                            String fName = attributeOf(pred);
                            try {

                                Optional<Method> setter;
                                if ("setId".equals(fName)) {
                                    setter = findSetterAnnotatedID(ont, clazz, context);
                                } else {
                                    setter = setterOfField(ont, clazz, fName, context);
                                }

                                if (setter.isPresent()) {
                                    Class<?> setterParam = setter.get().getParameterTypes()[0];

                                    if (log.isTraceEnabled()) log.trace("Trying to insert  " + fName + " => " + val + " using method ??");

                                    if (isJavaType(setterParam)) {
                                        fillObjectWithStdAttribute(setter.get(), obj, val);
                                    } else {
                                        //FIXME if entity  is different we shouldn't use the invoked method
                                        setter.get().invoke(obj, getTranslatedReference(val, setterParam, obj, context));
                                    }
                                }

                            } catch (Exception e) {
                                log.error(String.format("%s on field %s => %s using class %s using method %s %s",
                                        e.getClass().getSimpleName(), fName, val, clazz, setterOfField(ont, clazz, fName, context), e.getMessage()), e);
                            }

                            //values.put(fName, safeCastRDFNode(val, fName, clazz));
                        }

                    });
            if (obj instanceof TaxonName) {
                TaxonName tn = (TaxonName) obj;
               // tn.setName("tn");
                tn.setReferenceTaxon(null);
                getEntityManager().merge(tn);
            }
            //getEntityManager().merge(obj);
            log.info("  - created object " + ontResource + " - " + " of class " + ontResource.getClass() + "  - ");
            return Optional.of(obj);
        } catch (Exception e) {
            log.error(" processing individual " + ontResource + " - " + clazz, e);
        }
        return Optional.empty();
    }


    protected Optional<Method> findSetterAnnotatedID(Resource ont, Class clazz, RdfOwlConversionContext context) {
        for (Field f : clazz.getDeclaredFields())
            for (Annotation an : f.getDeclaredAnnotations())
                if (an instanceof Id) {
                    return setterOfField(ont, clazz, f.getName(), context);

                }
        return Optional.empty();
    }

}

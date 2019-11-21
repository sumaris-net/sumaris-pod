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

package net.sumaris.rdf.service;


import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.rdf.util.Owl2Bean;
import net.sumaris.rdf.util.OwlTransformContext;
import net.sumaris.rdf.util.OwlUtils;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfxml.xmlinput.JenaReader;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;


@Service("rdfSynchroService")
public class RdfSynchroService extends RdfModelExportServiceImpl{

    public static Logger log = LoggerFactory.getLogger(RdfSynchroService.class);

    public static String BASE_SYNCHRONIZE_MODEL_PACKAGE = "net.sumaris.core.model.referential";


    protected Owl2Bean owlConverter;
    protected List tl = new ArrayList();
    protected List statuses= new ArrayList();

    @PostConstruct
    protected void afterPropertiesSet() {
        initConfig();

        owlConverter = new Owl2Bean(this.entityManager, getModelPrefix()) {
            @Override
            protected List getCacheStatus() {
                return RdfSynchroService.this.getCacheStatus();
            }

            @Override
            protected List getCacheTL() {
                return RdfSynchroService.this.getCacheTL();
            }
        };
    }

    @Transactional
    public OntModel overwriteFromRemote(String url, String ontIRI) {

        OwlTransformContext context = createContext();

        long start = System.nanoTime();

        OntModel model = ModelFactory.createOntologyModel();
        new JenaReader().read(model, url);
        log.info("Found " + model.size() + " triples remotely, reconstructing model now " + OwlUtils.delta(start));

        List<? extends Object> recomposed = objectsFromOnt(model, context);
        log.info("Mapped ont to list of " + recomposed.size() + " objects, Making it OntClass again " + OwlUtils.delta(start));


        Stream<Class> classes = Stream.of();

        OntModel m2 = generateOntologyFromClasses(ontIRI, classes, RdfModelExportOptions.builder()
                .withInterfaces(true)
                .build());

        recomposed.forEach(r -> beanConverter.bean2Owl(m2, r, 2, propertyIncludes, propertyExcludes));

        log.info("Recomposed list of " + recomposed.size() + " objects is " + m2.size() + " triples.  " + OwlUtils.delta(start) + " - " + (100.0 * m2.size() / model.size()) + "%");
        log.info("Recomposed list of " + recomposed.size() + " objects is " + m2.size() + " triples.  " + OwlUtils.delta(start) + " - " + (100.0 * m2.size() / model.size()) + "%");

        if (model.size() == m2.size()) {
            log.info(" QUANTITATIVE SUCCESS   ");
            if (model.isIsomorphicWith(m2)) {
                log.info(" ISOMORPHIC SUCCESS " + OwlUtils.delta(start));

            }
        }
//        recomposed.forEach(obj -> {
//            try {
//                if(obj instanceof IItemReferentialEntity)
//                    ((IItemReferentialEntity)obj).setId(null);
//                getEntityManager().persist(obj);
//            } catch (Exception e) {
//                LOG.warn("didnt save "+obj+"  "+ e.getMessage());
//            }
//        });

        entityManager.flush();

        return m2;
    }

    protected List<Object> objectsFromOnt(OntModel m, OwlTransformContext context) {

        Resource schema = m.listSubjectsWithProperty(RDF.type, OWL.Ontology).nextResource();

        List<Object> ret = new ArrayList<>();

        for (OntClass ontClass : m.listClasses().toList()) {
            log.info("objectsFromOnt " + ontClass + " " + ontClass.listInstances().toList().size());
            owlConverter.ontToJavaClass(ontClass, context).ifPresent(clazz -> {
                for (OntResource ontResource : ontClass.listInstances().toList()) {
                    log.info("  ontResource " +ontResource);

                    Function<OntResource, Object> f = context.B2O_ARBITRARY_MAPPER.get(ontClass.getURI());
                    if (f != null) {
                        ret.add(f.apply(ontResource));
                    } else {
                        owlConverter.owl2Bean(schema, ontResource, clazz, context).ifPresent(ret::add);
                    }
                }
            });
        }
        return ret;
    }

    protected OwlTransformContext createContext() {
        OwlTransformContext context = new OwlTransformContext();


        new Reflections(BASE_SYNCHRONIZE_MODEL_PACKAGE, new SubTypesScanner(false))
                .getSubTypesOf(Object.class)
                .forEach(c -> context.URI_2_CLASS.put(c.getSimpleName(), c));

        context.URI_2_CLASS.put("definedURI", TaxonomicLevel.class);

        context.O2B_ARBITRARY_MAPPER.put("uri", obj -> {

            return (OntResource) null;
        });

        context.B2O_ARBITRARY_MAPPER.put(OwlUtils.ADAGIO_PREFIX + "TaxonomicLevel", ontResource -> {

            String clName = OwlUtils.ADAGIO_PREFIX + TaxonomicLevel.class.getTypeName();
            TaxonomicLevel tl = (TaxonomicLevel) context.URI_2_OBJ_REF.get(ontResource.getURI());

            try {
                // first try to get it from cache
                Property propCode = ontResource.getModel().getProperty(clName + "#Code");
                String label = ontResource.asIndividual().getPropertyValue(propCode).toString();


                for (Object ctl : getCacheTL()) {
                    if (((TaxonomicLevel) ctl).getLabel().equals(label)) {
                        return context.URI_2_OBJ_REF.putIfAbsent(ontResource.getURI(), ctl);
                    }
                }


                // not in cache, create a new object
                Property name = ontResource.getModel().getProperty(clName + "#Name");
                tl.setName(ontResource
                        .asIndividual()
                        .getProperty(name)
                        .getObject()
                        .asLiteral()
                        .getString());

                Property cd = ontResource.getModel().getProperty(clName + "#CreationDate");
                LocalDateTime ld = LocalDateTime.parse(ontResource.asIndividual().getPropertyValue(cd).asLiteral().getString(), OwlUtils.DATE_TIME_FORMATTER);

                tl.setCreationDate(owlConverter.convertToDateViaInstant(ld));

                Property order = ontResource.getModel().getProperty(clName + "#RankOrder");
                tl.setRankOrder(ontResource.asIndividual().getPropertyValue(order).asLiteral().getInt());


                tl.setStatus(entityManager.getReference(Status.class, 1));
                tl.setLabel(label);

                entityManager.persist(tl);
                return tl;

            } catch (Exception e) {
                log.error("Arbitrary Mapper error " + ontResource + " - " + tl, e);
            }

            return tl;
        });

        context.B2O_ARBITRARY_MAPPER.put(OwlUtils.ADAGIO_PREFIX + "Status", ontResource -> {
            String clName = OwlUtils.ADAGIO_PREFIX + Status.class.getTypeName();
            Status st = new Status();

            try {

                // first try to get it from cache
                Property propCode = ontResource.getModel().getProperty(clName + "#Code");
                Integer id = Integer.parseInt(ontResource.asIndividual().getPropertyValue(propCode).toString());

                int max = -1;
                for (Object ctl : getCacheStatus()) {
                    if (((Status) ctl).getId().equals(id)) {
                        return ctl;
                    } else {
                        max = Math.max(max, ((Status) ctl).getId());
                    }
                }

                Property name = ontResource.getModel().getProperty(clName + "#Name");
                st.setLabel(ontResource
                        .asIndividual()
                        .getProperty(name)
                        .getObject()
                        .asLiteral()
                        .getString());

                Property cd = ontResource.getModel().getProperty(clName + "#UpdateDate");

                LocalDateTime ld = LocalDateTime.parse(ontResource.asIndividual().getPropertyValue(cd).asLiteral().getString(), OwlUtils.DATE_TIME_FORMATTER);

                st.setUpdateDate(owlConverter.convertToDateViaInstant(ld));

                st.setId(max + 1);

                st.setName("DEFAULT GENERATED VALUE");
            } catch (Exception e) {
                log.error("Arbitrary Mapper error " + ontResource + " - " + st, e);
            }

            return st;
        });

        return context;
    }

    protected List getCacheStatus() {
        if (statuses == null || statuses.isEmpty())
            statuses.addAll(entityManager
                    .createQuery("from Status")
                    .getResultList());
        return statuses;
    }


    protected List getCacheTL() {

        if (tl == null || tl.isEmpty())
            tl.addAll(entityManager
                    .createQuery("from TaxonomicLevel")
                    .getResultList());
        return tl;
    }
}

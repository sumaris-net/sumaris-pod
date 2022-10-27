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

package net.sumaris.rdf.core.service.data.remote;


import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.util.Dates;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.dao.OntologyEntitiesDao;
import net.sumaris.rdf.core.model.ModelEntities;
import net.sumaris.rdf.core.model.ModelVocabulary;
import net.sumaris.rdf.core.service.schema.RdfSchemaFetchOptions;
import net.sumaris.rdf.core.service.schema.RdfSchemaService;
import net.sumaris.rdf.core.util.Bean2Owl;
import net.sumaris.rdf.core.util.Owl2Bean;
import net.sumaris.rdf.core.util.OwlUtils;
import net.sumaris.rdf.core.util.RdfOwlConversionContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfxml.xmlinput.JenaReader;
import org.apache.jena.shared.JenaException;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


@Service
@ConditionalOnBean({RdfConfiguration.class})
@Slf4j
public class RdfIndividualRemoteServiceImpl implements RdfIndividualRemoteService {

    protected Owl2Bean owlConverter;
    protected List tl = new ArrayList();
    protected List statuses = new ArrayList();

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected RdfConfiguration config;

    @Autowired
    protected OntologyEntitiesDao ontologyEntitiesDao;

    @Autowired
    private RdfSchemaService schemaService;

    protected Bean2Owl beanConverter;

    @PostConstruct
    protected void afterPropertiesSet() {

        beanConverter = new Bean2Owl(ontologyEntitiesDao);

        owlConverter = new Owl2Bean(config, this.entityManager) {
            @Override
            protected List getCacheStatus() {
                return RdfIndividualRemoteServiceImpl.this.getCacheStatus();
            }

            @Override
            protected List getCacheTL() {
                return RdfIndividualRemoteServiceImpl.this.getCacheTL();
            }
        };
    }

    @Override
    public OntModel getRemoteModel(java.lang.String url) {

        log.info(java.lang.String.format("Reading ontology model at {%s}...", url));
        long start = System.currentTimeMillis();
        OntModel model = ModelFactory.createOntologyModel();

        try {
            new JenaReader().read(model, url);
            log.info(java.lang.String.format("Model successfully read %s. %s triples found.", Dates.elapsedTime(start), model.size()));
        } catch (JenaException e) {
            throw new SumarisTechnicalException(java.lang.String.format("Error while reading ontology model at {%s}: %s", url, e.getMessage()), e);
        }

        return model;
    }

    @Override
    public Model importFromRemote(java.lang.String remoteUrl,
                                  java.lang.String remoteOntUri,
                                  String vocab,
                                  java.lang.String baseTargetPackage) {

        // Reading remote model
        OntModel sourceModel = getRemoteModel(remoteUrl);

        // Recomposed object, from the remote model
        RdfOwlConversionContext context = createImportContext(vocab.toLowerCase(), baseTargetPackage);

        long start = System.currentTimeMillis();
        List<? extends Object> recomposed = objectsFromOnt(sourceModel, context);
        if (CollectionUtils.isEmpty(recomposed)) {
            log.info("Remote model has no instance ! Make sure the remote URL is valid");
            return null;
        }
        log.info(java.lang.String.format("Mapped ont to list of %s objects, Making it OntClass again %s", recomposed.size(), Dates.elapsedTime(start)));

        Model targetModel = schemaService.getOntology(RdfSchemaFetchOptions.builder()
            .vocabulary(vocab)
            .withInterfaces(true)
            .build());
        java.lang.String modelUri = schemaService.getNamespace();

        recomposed.forEach(r -> beanConverter.bean2Owl(targetModel, modelUri, r, 2, ModelEntities.propertyIncludes, ModelEntities.propertyExcludes));

        float successPct = Math.round((targetModel.size() / sourceModel.size()) * 10000) / 100f;
        log.info(java.lang.String.format("Recomposed list of %s objects from %s triples. %s - %s%%", recomposed.size(), targetModel.size(), Dates.elapsedTime(start), successPct));

        if (sourceModel.size() == targetModel.size()) {
            log.info(" QUANTITATIVE SUCCESS   ");
            if (sourceModel.isIsomorphicWith(targetModel)) {
                log.info(" ISOMORPHIC SUCCESS " + Dates.elapsedTime(start));

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

        //entityManager.flush();

        return targetModel;
    }

    /* -- Protected functions -- */

    protected List<Object> objectsFromOnt(OntModel m, RdfOwlConversionContext context) {

        Resource schema = m.listSubjectsWithProperty(RDF.type, OWL.Ontology).nextResource();

        List<Object> ret = new ArrayList<>();

        m.listClasses().toList().forEach(ontClass -> {

            if (log.isTraceEnabled())
                log.trace(java.lang.String.format("Deserialize %s objects from %s ", ontClass, CollectionUtils.size(ontClass.listInstances().toList())));

            owlConverter.ontToJavaClass(ontClass, context)
                .ifPresent(clazz -> {
                    ontClass.listInstances().toList().forEach(ontResource -> {
                        if (log.isTraceEnabled()) log.trace("  ontResource " + ontResource);

                        Function<OntResource, Object> f = context.B2O_ARBITRARY_MAPPER.get(ontClass.getURI());
                        if (f != null) {
                            ret.add(f.apply(ontResource));
                        } else {
                            owlConverter.owl2Bean(schema, ontResource, clazz, context).ifPresent(ret::add);
                        }
                    });
                });
        });
        return ret;
    }

    protected RdfOwlConversionContext createImportContext(java.lang.String domain, java.lang.String basePackage) {
        RdfOwlConversionContext context = new RdfOwlConversionContext();


        new Reflections(basePackage, new SubTypesScanner(false))
            .getSubTypesOf(Object.class)
            .forEach(c -> context.URI_2_CLASS.put(c.getSimpleName(), c));

        context.URI_2_CLASS.put("definedURI", TaxonomicLevel.class);

        context.O2B_ARBITRARY_MAPPER.put("uri", obj -> {

            return (OntResource) null;
        });

        context.B2O_ARBITRARY_MAPPER.put(OwlUtils.ADAGIO_PREFIX + "TaxonomicLevel", ontResource -> {

            java.lang.String classUri = OwlUtils.ADAGIO_PREFIX + TaxonomicLevel.class.getTypeName();
            TaxonomicLevel tl = (TaxonomicLevel) context.URI_2_OBJ_REF.get(ontResource.getURI());

            try {
                // first try to find it from cache
                Property propCode = ontResource.getModel().getProperty(classUri + "#code");
                java.lang.String label = ontResource.asIndividual().getPropertyValue(propCode).toString();


                for (Object ctl : getCacheTL()) {
                    if (((TaxonomicLevel) ctl).getLabel().equals(label)) {
                        return context.URI_2_OBJ_REF.putIfAbsent(ontResource.getURI(), ctl);
                    }
                }


                // not in cache, create a new object
                Property name = ontResource.getModel().getProperty(classUri + "#name");
                tl.setName(ontResource
                    .asIndividual()
                    .getProperty(name)
                    .getObject()
                    .asLiteral()
                    .getString());

                Property cd = ontResource.getModel().getProperty(classUri + "#creationDate");
                LocalDateTime ld = LocalDateTime.parse(ontResource.asIndividual().getPropertyValue(cd).asLiteral().getString(), OwlUtils.DATE_TIME_FORMATTER);

                tl.setCreationDate(OwlUtils.convertToDateViaInstant(ld));

                Property order = ontResource.getModel().getProperty(classUri + "#rankOrder");
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
            java.lang.String clName = OwlUtils.ADAGIO_PREFIX + Status.class.getTypeName();
            Status st = new Status();

            try {

                // first try to find it from cache
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

                st.setUpdateDate(OwlUtils.convertToDateViaInstant(ld));

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

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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.vo.IValueObject;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.config.RdfConfigurationOption;
import net.sumaris.rdf.dao.RdfModelDao;
import net.sumaris.rdf.dao.cache.RdfCacheConfiguration;
import net.sumaris.rdf.util.Bean2Owl;
import net.sumaris.rdf.util.OwlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("rdfModelExportService")
public class RdfModelExportServiceImpl implements RdfModelExportService {

    private static final Logger log = LoggerFactory.getLogger(RdfModelExportServiceImpl.class);

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected RdfConfiguration config;

    @Autowired
    protected RdfModelDao modelDao;

    @Autowired
    protected RdfCacheConfiguration cacheConfiguration;

    @javax.annotation.Resource(name = "rdfModelExportService")
    protected RdfModelExportService self; // Use to call method with cache

    protected String modelPrefix;

    protected List<Method> propertyIncludes = Lists.newArrayList();

    protected List<Method> propertyExcludes = Lists.newArrayList();

    protected Bean2Owl beanConverter;

    @PostConstruct
    protected void afterPropertiesSet() {
        beanConverter = new Bean2Owl(getModelPrefix());

        // Init interface map
        initConfig();

    }

    @Override
    public OntModel generateOntology(String modelSuffix, @Nullable RdfModelExportOptions options) {
        Preconditions.checkArgument(StringUtils.isNotBlank(modelSuffix));

        options = options != null ? options : RdfModelExportOptions.builder().build();


        String uri = this.getModelPrefix() + modelSuffix;
        if (options != null) log.info("model=" + uri +" hash= " + options.hashCode() + " options=" + options.toString());

        if (options.getType() != null) {
            return generateOntologyFromType(uri,
                    options.getType(),
                    options);

        }
        else if (options.getAnnotatedType() != null) {
            return generateOntologyFromAnnotatedType(uri,
                    options.getAnnotatedType(),
                    options);

        }
        else if (CollectionUtils.isNotEmpty(options.getPackages())){
            return generateOntologyFromPackages(uri,
                    options.getPackages(),
                    options);
        }

        else {
            // Set options defaults, by model Suffix
            switch (modelSuffix) {
                case "data":
                    options.setAnnotatedType(Entity.class);
                    options.setPackages(Lists.newArrayList("net.sumaris.core.model.data"));
                    break;
                case "referential":
                    options.setAnnotatedType(Entity.class);
                    options.setPackages(ImmutableList.of(
                            "net.sumaris.core.model.administration",
                            "net.sumaris.core.model.referential"
                    ));
                    break;
                case "social":
                    options.setAnnotatedType(Entity.class);
                    options.setPackages(ImmutableList.of(
                            "net.sumaris.core.model.administration.user",
                            "net.sumaris.core.model.social"
                    ));
                    break;
                case "technical":
                    options.setAnnotatedType(Entity.class);
                    options.setPackages(ImmutableList.of(
                            "net.sumaris.core.model.file",
                            "net.sumaris.core.model.technical"
                    ));
                    break;
                case "vo":
                    options.setType(IValueObject.class);
                    options.setPackages(Lists.newArrayList("net.sumaris.core.vo"));
                    break;
                default:
                    throw new IllegalArgumentException("Missing options.packages or options.annotatedType");
            }

            // Loop (throw cache), with defaults options
            return self.generateOntology(modelSuffix, options);
        }
    }

    @Override
    public Model generateReferentialItems(String entityName, RdfModelExportOptions options) {

        String uri = this.getModelPrefix() +  "entities/"; // + entityName;
        //String uri = this.getModelPrefix() + entityName;
        log.info(String.format("Generating referential items {%s}...", uri));

        OntModel model = createOntologyModel(uri);
        Map<OntClass, List<OntClass>> mutuallyDisjoint = options.isWithDisjoints() ? new HashMap<>() : null;
        modelDao.streamAll(entityName, IUpdateDateEntityBean.class)
                .forEach(entity -> beanConverter.bean2Owl(model, entity, 2, propertyIncludes, propertyExcludes));

        withDisjoints(mutuallyDisjoint);

        return model;
    }

    /* -- protected methods -- */

    protected void initConfig() {

        try {
            propertyIncludes.addAll(Arrays.asList(
                    OwlUtils.getterOfField(TaxonName.class, TaxonName.Fields.TAXONOMIC_LEVEL),
                    OwlUtils.getterOfField(TaxonName.class, TaxonName.Fields.REFERENCE_TAXON),
                    OwlUtils.getterOfField(TaxonName.class, TaxonName.Fields.STATUS),
                    OwlUtils.getterOfField(Location.class, Location.Fields.STATUS),
                    OwlUtils.getterOfField(Location.class, Location.Fields.LOCATION_LEVEL),
                    OwlUtils.getterOfField(PmfmStrategy.class, PmfmStrategy.Fields.STRATEGY),
                    OwlUtils.getterOfField(PmfmStrategy.class, PmfmStrategy.Fields.ACQUISITION_LEVEL)
            ));

            propertyExcludes.addAll(Arrays.asList(
                    OwlUtils.getterOfField(Gear.class, Gear.Fields.STRATEGIES),
                    OwlUtils.getterOfField(Gear.class, Gear.Fields.CHILDREN),
                    OwlUtils.getterOfField(ReferenceTaxon.class, ReferenceTaxon.Fields.PARENT_TAXON_GROUPS),
                    OwlUtils.getterOfField(ReferenceTaxon.class, ReferenceTaxon.Fields.STRATEGIES),
                    OwlUtils.getterOfField(ReferenceTaxon.class, ReferenceTaxon.Fields.TAXON_NAMES)
            ));

        } catch (Exception e) {
            log.error("Error while getting a property of a class", e);
        }
    }

    protected OntModel createOntologyModel(String uri) {

        uri += (uri.endsWith("/") || uri.endsWith("#")) ? "" : "/";

        OntModel ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        ontology.setNsPrefix("this", uri);
        ontology.setNsPrefix("foaf", FOAF.getURI());
        ontology.setNsPrefix("purl", DC_11.getURI()); // http://purl.org/dc/elements/1.1/
        ontology.setStrictMode(true);

        String modelLanguage = config.getModelDefaultLanguage();

        Resource schema = ontology.createResource(uri)
                .addProperty(RDF.type, OWL.Ontology.asResource())
                .addProperty(OWL2.versionInfo, "1.1.0")
                .addProperty(OWL2.versionIRI, uri)
                // Dublin Core
                .addProperty(DC.language, modelLanguage)
                .addProperty(DC.description, config.getModelDescription(), modelLanguage)
                .addProperty(DC.title, config.getModelTitle(), modelLanguage)
                .addProperty(DC.date, config.getModelDate(), modelLanguage)
                .addProperty(DC.rights, config.getModelLicense())
                // RDFS
                .addProperty(RDFS.label, config.getModelLabel(), modelLanguage)
                .addProperty(RDFS.comment, config.getModelComment(), modelLanguage);

        // Add authors
        Iterable<String> authors = Splitter.on(',').omitEmptyStrings().trimResults().split(config.getModelAuthors());
        authors.forEach(author -> schema.addProperty(DC.creator, author));

        return ontology;

    }

    protected OntModel generateOntologyFromPackages(String uri, List<String> packages, RdfModelExportOptions options) {

        log.info(String.format("Generating {%s} on packages {%s}", uri, Joiner.on(',').join(packages)));

        OntModel model = createOntologyModel(uri);
        Map<OntClass, List<OntClass>> mutuallyDisjoint = options.isWithDisjoints() ? new HashMap<>() : null;

        Reflections reflections = new Reflections(packages, new SubTypesScanner(false));
        reflections.getSubTypesOf(Object.class).stream()
                .forEach(ent  -> beanConverter.classToOwl(model, ent, mutuallyDisjoint, options.isWithInterfaces(), options.isWithMethods()));

        withDisjoints(mutuallyDisjoint);
        return model;

    }

    protected OntModel generateOntologyFromAnnotatedType(String uri, Class<? extends Annotation> annotatedType, RdfModelExportOptions options) {

        OntModel model = createOntologyModel(uri);
        Map<OntClass, List<OntClass>> mutuallyDisjoint = options.isWithDisjoints() ? new HashMap<>() : null;

        if (CollectionUtils.isNotEmpty(options.getPackages())) {
            log.info(String.format("Generating {%s} on annotated type {%s} and packages {%s}", uri, annotatedType.getSimpleName(), options.getPackages()));
            Reflections reflections = new Reflections(options.getPackages(), new SubTypesScanner(false), new TypeAnnotationsScanner());
            reflections.getTypesAnnotatedWith(annotatedType).stream()
                    .forEach(ent  -> beanConverter.classToOwl(model, ent, mutuallyDisjoint, options.isWithInterfaces(), options.isWithMethods()));

        }
        else{
            log.info(String.format("Generating {%s} on annotated type {%s}", uri, annotatedType.getSimpleName()));
            Reflections.collect().getTypesAnnotatedWith(annotatedType).stream()
                    .forEach(ent -> beanConverter.classToOwl(model, ent, mutuallyDisjoint, options.isWithInterfaces(), options.isWithMethods()));
        }
        withDisjoints(mutuallyDisjoint);
        return model;

    }


    protected OntModel generateOntologyFromType(String uri, Class<? extends Object> type, RdfModelExportOptions options) {

        OntModel model = createOntologyModel(uri);
        Map<OntClass, List<OntClass>> mutuallyDisjoint = options.isWithDisjoints() ? new HashMap<>() : null;

        if (CollectionUtils.isNotEmpty(options.getPackages())) {
            log.info(String.format("Generating {%s} on type {%s} and packages {%s}", uri, type.getSimpleName(), options.getPackages()));
            Reflections reflections = new Reflections(options.getPackages(), new SubTypesScanner(false), new TypeAnnotationsScanner());
            reflections.getSubTypesOf(type).stream()
                    .forEach(ent  -> beanConverter.classToOwl(model, ent, mutuallyDisjoint, options.isWithInterfaces(), options.isWithMethods()));

        }
        else{
            log.info(String.format("Generating {%s} on type {%s}", uri, type.getSimpleName()));
            Reflections.collect().getSubTypesOf(type).stream()
                    .forEach(ent -> beanConverter.classToOwl(model, ent, mutuallyDisjoint, options.isWithInterfaces(), options.isWithMethods()));
        }
        withDisjoints(mutuallyDisjoint);
        return model;

    }

    public OntModel generateOntologyFromClasses(String uri, Stream<Class> classes, RdfModelExportOptions options) {
        OntModel model = createOntologyModel(uri);

        Map<OntClass, List<OntClass>> mutuallyDisjoint = options.isWithDisjoints() ? new HashMap<>() : null;

        for (Class<?> ent : classes.collect(Collectors.toList())) {
            beanConverter.classToOwl(model, ent, mutuallyDisjoint, options.isWithInterfaces(), options.isWithMethods());
        }
        withDisjoints(mutuallyDisjoint);
        return model;

    }

    protected String getModelPrefix() {

        // Init property, if not init yet
        if (this.modelPrefix == null) {
            String modelPrefix = config.getModelPrefix();
            Preconditions.checkNotNull(modelPrefix, String.format("Missing configuration option {%s}", RdfConfigurationOption.RDF_MODEL_PREFIX.getKey()));
            if (modelPrefix.lastIndexOf('/') != modelPrefix.length() - 1) {
                modelPrefix += "/";
            }
            this.modelPrefix = modelPrefix;
        }

        return this.modelPrefix;
    }

    protected void withDisjoints(Map<OntClass, List<OntClass>> mutuallyDisjoint) {

        if (mutuallyDisjoint != null && !mutuallyDisjoint.isEmpty()) {
            log.info("setting disjoints " + mutuallyDisjoint.size());
            // add mutually disjoint classes
            mutuallyDisjoint.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1) // having more than one child
                    .forEach(e -> {
                        List<OntClass> list = e.getValue();
                        for (int i = 0; i < list.size(); i++) {
                            OntClass r1 = list.get(i);
                            for (int j = i + 1; j < list.size(); j++) {
                                OntClass r2 = list.get(j);
                                //LOG.info("setting disjoint " + i + " " + j + " " + r1 + " " + r2);
                                r1.addDisjointWith(r2);
                            }
                        }
                    });
        }

    }
}
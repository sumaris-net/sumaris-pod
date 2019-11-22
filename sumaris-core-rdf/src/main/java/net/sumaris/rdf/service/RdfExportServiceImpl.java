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

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.PmfmStrategy;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.vo.IValueObject;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.dao.RdfModelDao;
import net.sumaris.rdf.dao.cache.RdfCacheConfiguration;
import net.sumaris.rdf.model.ModelEntities;
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
import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service("rdfModelExportService")
public class RdfExportServiceImpl implements RdfExportService {

    private static final Logger log = LoggerFactory.getLogger(RdfExportServiceImpl.class);

    @Autowired
    protected RdfConfiguration config;

    @Autowired
    protected RdfModelDao modelDao;

    @Autowired
    protected RdfCacheConfiguration cacheConfiguration;

    @javax.annotation.Resource(name = "rdfModelExportService")
    protected RdfExportService self; // Use to call method with cache

    protected Bean2Owl beanConverter;

    @PostConstruct
    protected void afterPropertiesSet() {
        beanConverter = new Bean2Owl(config.getModelPrefix());
    }

    @Override
    public OntModel getOntModelWithClasses(String domain, @Nullable RdfExportOptions options) {
        Preconditions.checkArgument(StringUtils.isNotBlank(domain));

        // When no options, init option for the domain, then loop (throw cache)
        if (options == null) {
            return self.getOntModelWithClasses(domain, createOptions(domain));
        }

        int originalHash = options.hashCode();

        // Make sure to fix options (set packages, type, ...)
        fixOptions(domain, options);

        // Options changed (=fixed): redirect to force cache used
        boolean optionsChanged = (originalHash != options.hashCode());
        if (optionsChanged) {
            if (log.isDebugEnabled()) log.debug("Ontology export options was fixed! Will use: " + options.toString());
            return self.getOntModelWithClasses(domain, options);
        }

        return generateOntModel(options);
    }

    @Override
    public Model getOntModelWithInstances(String domain, RdfExportOptions options) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(options);
        Preconditions.checkNotNull(options.getClassname());

        // Make sure to fix options (set packages, type, ...)
        fixOptions(domain, options);

        String uri = this.getModelDomainUri(options.getDomain()) + options.getClassname() + "/";
        log.info(String.format("Generating ontology {%s} (with instances)...", uri));

        OntModel model = createOntModel(uri);
        Map<OntClass, List<OntClass>> mutuallyDisjoint = options.isWithDisjoints() ? new HashMap<>() : null;
        modelDao.streamAll(options.getClassname(), IUpdateDateEntityBean.class)
                .forEach(entity -> beanConverter.bean2Owl(model, entity, 2, ModelEntities.propertyIncludes, ModelEntities.propertyExcludes));

        withDisjoints(mutuallyDisjoint);

        return model;
    }

    /* -- protected methods -- */

    protected RdfExportOptions createOptions(String domain) {
        RdfExportOptions options = RdfExportOptions.builder().build();
        fixOptions(domain, options);
        return options;
    }


    protected RdfExportOptions fixOptions(String domain, RdfExportOptions options) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(options);

        switch(domain) {
            case "data":
                options.setDomain("data");
                options.setAnnotatedType(Entity.class);
                options.setPackages(Lists.newArrayList("net.sumaris.core.model.data"));
                break;
            case "referential":
            case "referentials":
                options.setDomain("referential");
                options.setAnnotatedType(Entity.class);
                options.setPackages(ImmutableList.of(
                        "net.sumaris.core.model.administration",
                        "net.sumaris.core.model.referential"
                ));
                break;
            case "social":
                options.setDomain("social");
                options.setAnnotatedType(Entity.class);
                options.setPackages(ImmutableList.of(
                        "net.sumaris.core.model.administration.user",
                        "net.sumaris.core.model.social"
                ));
                break;
            case "technical":
                options.setDomain("technical");
                options.setAnnotatedType(Entity.class);
                options.setPackages(ImmutableList.of(
                        "net.sumaris.core.model.file",
                        "net.sumaris.core.model.technical"
                ));
                break;
            case "vo":
                options.setDomain("vo");
                options.setType(IValueObject.class);
                options.setPackages(Lists.newArrayList("net.sumaris.core.vo"));
                break;
            default:
                throw new SumarisTechnicalException(String.format("Unknown ontology {%s}", domain));
        }
        return options;
    }

    @Override
    public OntModel createOntModel(String uri) {
        Preconditions.checkNotNull(uri);

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

    protected OntModel generateOntModel(RdfExportOptions options) {
        Preconditions.checkNotNull(options);
        Preconditions.checkNotNull(options.getDomain());

        String uri = getModelDomainUri(options.getDomain());
        OntModel model = createOntModel(uri);
        Map<OntClass, List<OntClass>> mutuallyDisjoint = options.isWithDisjoints() ? Maps.newHashMap() : null;
        getClassesAsStream(options).forEach(clazz ->
                beanConverter.classToOwl(model, clazz, mutuallyDisjoint, options.isWithInterfaces(), options.isWithMethods()));
        return model;
    }

    protected String getModelDomainUri(String domain) {
        Preconditions.checkNotNull(domain);
        return config.getModelPrefix() + domain + "/";
    }

    protected Stream<Class<?>> getClassesAsStream(RdfExportOptions options) {

        Reflections reflections;
        Stream<Class<?>> result;

        // Define class scanner
        Scanner[] scanners = null;
        if (options.getAnnotatedType() != null) {
            scanners = new Scanner[] { new SubTypesScanner(false), new TypeAnnotationsScanner() };
        }

        // Collect by package
        if (CollectionUtils.isNotEmpty(options.getPackages())) {
            if (scanners != null) {
                reflections = new Reflections(options.getPackages(), scanners);
            }
            else {
                reflections = new Reflections(options.getPackages());
            }
        }
        // Or collect all
        else {
            reflections = Reflections.collect();
        }


        // get by type
        if (options.getType() != null) {
            result = reflections.getSubTypesOf(options.getType()).stream();
        }

        // Get by annotated type
        else if (options.getAnnotatedType() != null) {
            result = reflections.getTypesAnnotatedWith(options.getAnnotatedType()).stream();
        }

        // Get all classes
        else {
            result = reflections.getSubTypesOf(Object.class).stream();
        }

        // Filter by class name
        final String className = options.getClassname();
        if (StringUtils.isNotBlank(className)) {
            result = result.filter(clazz -> className.equalsIgnoreCase(clazz.getSimpleName()));
        }

        return result;
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
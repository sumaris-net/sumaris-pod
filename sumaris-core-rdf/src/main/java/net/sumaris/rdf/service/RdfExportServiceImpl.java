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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.vo.IValueObject;
import net.sumaris.rdf.config.RdfConfiguration;
import net.sumaris.rdf.config.RdfConfigurationOption;
import net.sumaris.rdf.dao.RdfModelDao;
import net.sumaris.rdf.dao.cache.RdfCacheConfiguration;
import net.sumaris.rdf.model.ModelDomain;
import net.sumaris.rdf.model.ModelEntities;
import net.sumaris.rdf.util.Bean2Owl;
import net.sumaris.rdf.model.ModelType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.*;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service("rdfExportService")
@ConditionalOnBean({RdfConfiguration.class})
public class RdfExportServiceImpl implements RdfExportService {

    private static final Logger log = LoggerFactory.getLogger(RdfExportServiceImpl.class);

    @Autowired
    protected RdfConfiguration config;

    @Autowired
    protected RdfModelDao modelDao;

    @Autowired
    protected RdfCacheConfiguration cacheConfiguration;

    @javax.annotation.Resource(name = "rdfExportService")
    protected RdfExportService self; // Use to call method with cache

    protected Bean2Owl beanConverter;

    @PostConstruct
    protected void afterPropertiesSet() {

        beanConverter = new Bean2Owl(config.getModelUriPrefix());

        // Check schema URI validity
        String ns = getModelNamespace();
        String uri = getModelSchemaUri();
        try {
            new URI(uri);
        }
        catch(URISyntaxException e) {
            throw new BeanInitializationException(String.format("Bad RDF model URI {%s}. Please fix the option '%s' in the configuration.", config.getModelUriPrefix(), RdfConfigurationOption.RDF_MODEL_URI_PREFIX.getKey(), e.getMessage()));
        }
        try {
            createOntModel(ns, uri);
        }
        catch(PrefixMapping.IllegalPrefixException e) {
            throw new BeanInitializationException(String.format("Bad RDF model namespace {%s}. Please fix the option '%s' in the configuration.", config.getModelUriPrefix(), RdfConfigurationOption.RDF_MODEL_NS.getKey(), e.getMessage()));
        }
    }

    @Override
    public OntModel getOntologyModel(@Nullable RdfExportOptions options) {

        // When no options, init option for the domain, then loop (throw cache)
        if (options == null) {
            return self.getOntologyModel(createOptions());
        }

        int cacheKey = options.hashCode();

        // Make sure to fix options (set packages, ...)
        fillOptions(options);
        int fixedCacheKey = options.hashCode();

        // Cache key changed by applyDomainToOptions() => loop using self, to force cache used
        boolean optionsChanged = (cacheKey != fixedCacheKey);
        if (optionsChanged) {
            if (log.isDebugEnabled())
                log.debug("Ontology export options was fixed! Will use: " + options.toString());
            return self.getOntologyModel(options);
        }

        return getOntologyNoCache(options);
    }

    @Override
    public Model getDataModel(@Nullable RdfExportOptions options) {

        // Make sure to fix options (set packages, ...)
        fillOptions(options);

        // Create base model
        OntModel model = self.getOntologyModel(options);;
        String modelUri = getModelSchemaUri();

        boolean hasClassName = StringUtils.isNotBlank(options.getClassName());

        if (options.getDomain() == ModelDomain.DATA && !hasClassName) {
            throw new IllegalArgumentException("Unable to export data without a class name!");
        }

        final int entityGraphDepth =  options.getDomain() == ModelDomain.DATA ? 3 : 0; // TODO: check if enought

        // When having classname and id
        if (hasClassName && StringUtils.isNotBlank(options.getId())) {
            // Get the bean
            IUpdateDateEntityBean entity = modelDao.getById(options.getDomain(), options.getClassName(), IUpdateDateEntityBean.class, options.getId());
            // Convert into model
            beanConverter.bean2Owl(model, modelUri, entity, entityGraphDepth, ModelEntities.propertyIncludes, ModelEntities.propertyExcludes);
        }
        else {
            getClassesAsStream(options)
                .flatMap(clazz -> modelDao.streamAll(options.getDomain(), clazz.getSimpleName(), IEntity.class))
                .forEach(entity -> beanConverter.bean2Owl(model, modelUri, entity, entityGraphDepth, ModelEntities.propertyIncludes, ModelEntities.propertyExcludes));
        }

        return model;
    }

    /* -- protected methods -- */

    protected RdfExportOptions createOptions() {
        RdfExportOptions options = RdfExportOptions.builder()
                .domain(ModelDomain.REFERENTIAL)
                .build();
        fillOptions(options);
        return options;
    }


    protected RdfExportOptions fillOptions(RdfExportOptions options) {
        Preconditions.checkNotNull(options);

        ModelDomain domain = options.getDomain();
        if (domain == null) {
            if (StringUtils.isNotBlank(options.getClassName())) {
                domain = modelDao.getDomainByClassName(options.getClassName());
            }
            else {
                domain = ModelDomain.REFERENTIAL; // default
            }
            options.setDomain(domain);
        }

        switch(domain) {
            case DATA:
                options.setAnnotatedType(Entity.class);
                options.setPackages(Lists.newArrayList("net.sumaris.core.model.data"));
                break;
            case REFERENTIAL:
                options.setAnnotatedType(Entity.class);
                options.setPackages(ImmutableList.of(
                        "net.sumaris.core.model.administration",
                        "net.sumaris.core.model.referential"
                ));
                break;
            case SOCIAL:
                options.setAnnotatedType(Entity.class);
                options.setPackages(ImmutableList.of(
                        "net.sumaris.core.model.administration.user",
                        "net.sumaris.core.model.social"
                ));
                break;
            case TECHNICAL:
                options.setAnnotatedType(Entity.class);
                options.setPackages(ImmutableList.of(
                        "net.sumaris.core.model.file",
                        "net.sumaris.core.model.technical"
                ));
                break;
            case VO:
                options.setType(IValueObject.class);
                options.setPackages(Lists.newArrayList("net.sumaris.core.vo"));
                break;
            default:
                throw new SumarisTechnicalException(String.format("Unknown ontology {%s}", domain));
        }
        return options;
    }


    protected OntModel createBaseOntModel() {
        return createBaseOntModel(getModelNamespace(), getModelSchemaUri());
    }

    protected OntModel createBaseOntModel(String namespace, String uri) {
        Preconditions.checkNotNull(namespace);
        Preconditions.checkNotNull(uri);

        OntModel ontology = ModelFactory.createOntologyModel();
        ontology.setNsPrefix(namespace, uri);
        ontology.setNsPrefix("dc", DC_11.getURI()); // http://purl.org/dc/elements/1.1/
        ontology.setStrictMode(true);

        return ontology;
    }

    protected OntModel createOntModel() {
        String uri = getModelSchemaUri();
        String ns = getModelNamespace();
        return createOntModel(ns, uri);
    }

    protected OntModel createOntModel(String namespace, String uri) {
        Preconditions.checkNotNull(uri);

        log.info(String.format("Generating ontology {%s}...", uri));

        OntModel ontology = createBaseOntModel(namespace, uri);

        // Add ontology metadata
        String modelLanguage = config.getModelDefaultLanguage();
        Resource schema =  ontology.createResource(uri)
                .addProperty(RDF.type, OWL.Ontology.asResource())
                .addProperty(OWL2.versionInfo, config.getModelVersion())
                .addProperty(OWL2.versionIRI, uri)
                // Dublin Core
                .addProperty(DC.language, modelLanguage)
                .addProperty(DC.description, config.getModelDescription(), modelLanguage)
                .addProperty(DC.title, config.getModelTitle(), modelLanguage)
                .addProperty(DC.date, config.getModelDate(), modelLanguage)
                .addProperty(DC.rights, config.getModelLicense(), modelLanguage)
                // RDFS
                .addProperty(RDFS.label, config.getModelLabel(), modelLanguage)
                .addProperty(RDFS.comment, config.getModelComment(), modelLanguage);

        // Add authors
        Iterable<String> authors = Splitter.on(',').omitEmptyStrings().trimResults().split(config.getModelAuthors());
        authors.forEach(author -> schema.addProperty(DC.creator, author));

        return ontology;

    }

    protected OntModel getOntologyNoCache(RdfExportOptions options) {
        Preconditions.checkNotNull(options);
        Preconditions.checkNotNull(options.getDomain());

        OntModel model = createOntModel();

        Map<OntClass, List<OntClass>> mutuallyDisjoint = options.isWithDisjoints() ? Maps.newHashMap() : null;
        getClassesAsStream(options).forEach(clazz -> {
            beanConverter.classToOwl(model, clazz, mutuallyDisjoint, options.isWithInterfaces());
        });

        if (options.isWithDisjoints()) withDisjoints(mutuallyDisjoint);

        return model;
    }

    @Override
    public String getModelSchemaUri() {
        String uri = config.getModelUriPrefix() + ModelType.SCHEMA.name().toLowerCase() + "/";
        // TODO: append version ?

        // model should ends with '/'
        if (uri.endsWith("#")) {
            uri = uri.substring(0, uri.length() -1);
        }
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        return uri;
    }

    protected String getModelDataUri() {
        String uri = config.getModelUriPrefix() + ModelType.DATA.name().toLowerCase() + "/";

        // model should ends with '/'
        if (uri.endsWith("#")) {
            uri = uri.substring(0, uri.length() -1);
        }
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        return uri;
    }

    protected String getModelNamespace() {
        String namespace = config.getModelNamespace();
        return StringUtils.isNotBlank(namespace) ? namespace : "this";
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

        // Filter by class names
        final Set<String> classNames = (options.getClassName() != null) ?
                modelDao.getClassNamesByRootClass(options.getDomain(), options.getClassName()) :
                (options.getDomain() != null) ? modelDao.getClassNamesByDomain(options.getDomain()) : null;
        if (CollectionUtils.isNotEmpty(classNames)) {
            return result.filter(clazz -> classNames.contains(clazz.getSimpleName()) || classNames.contains(clazz.getSimpleName().toLowerCase()));
        }

        return result;
    }

    protected void withDisjoints(Map<OntClass, List<OntClass>> mutuallyDisjoint) {

        if (mutuallyDisjoint != null && !mutuallyDisjoint.isEmpty()) {
            boolean debug = log.isDebugEnabled();
            if (debug) log.debug("setting disjoints " + mutuallyDisjoint.size());
            // add mutually disjoint classes
            mutuallyDisjoint.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1) // having more than one child
                    .forEach(e -> {
                        List<OntClass> list = e.getValue();
                        for (int i = 0; i < list.size(); i++) {
                            OntClass r1 = list.get(i);
                            for (int j = i + 1; j < list.size(); j++) {
                                OntClass r2 = list.get(j);
                                if (debug) log.debug("setting disjoint " + i + " " + j + " " + r1 + " " + r2);
                                r1.addDisjointWith(r2);
                            }
                        }
                    });
        }

    }
}
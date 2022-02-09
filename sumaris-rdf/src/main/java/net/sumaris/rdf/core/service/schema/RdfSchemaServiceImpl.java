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

package net.sumaris.rdf.core.service.schema;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.rdf.core.config.RdfCacheConfiguration;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.config.RdfConfigurationOption;
import net.sumaris.rdf.core.dao.EntitiesDao;
import net.sumaris.rdf.core.model.IModelVisitor;
import net.sumaris.rdf.core.model.ModelType;
import net.sumaris.rdf.core.model.ModelVocabulary;
import net.sumaris.rdf.core.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.core.util.Bean2Owl;
import net.sumaris.rdf.core.util.ModelUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.*;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("rdfSchemaService")
@ConditionalOnBean({RdfConfiguration.class})
@Slf4j
public class RdfSchemaServiceImpl implements RdfSchemaService {

    @Autowired
    protected RdfConfiguration config;

    @Autowired
    protected EntitiesDao modelDao;

    @Autowired
    protected RdfCacheConfiguration cacheConfiguration;

    protected Bean2Owl beanConverter;

    private boolean debug;

    protected List<IModelVisitor<Model, RdfSchemaFetchOptions>> modelVisitors = Lists.newCopyOnWriteArrayList();

    @PostConstruct
    protected void init() {

        debug = log.isDebugEnabled();

        beanConverter = new Bean2Owl(config.getModelBaseUri());

        // Check schema URI validity
        {
            String prefix = getPrefix();
            String ns = getNamespace();
            try {
                new URI(ns); // validate namespace
            } catch (URISyntaxException e) {
                throw new BeanInitializationException(String.format("Bad RDF schema namespace {%s}. Please fix the option '%s' in the configuration.", config.getModelBaseUri(), RdfConfigurationOption.RDF_MODEL_BASE_URI.getKey(), e.getMessage()));
            }
            try {
                ModelUtils.createOntologyModel(prefix, ns, ReasoningLevel.NONE); // validate prefix
            } catch (PrefixMapping.IllegalPrefixException e) {
                throw new BeanInitializationException(String.format("Bad RDF schema prefix {%s}. Please fix the option '%s' in the configuration.", config.getModelPrefix(), RdfConfigurationOption.RDF_MODEL_PREFIX.getKey(), e.getMessage()));
            }
        }
    }

    @Override
    public void register(IModelVisitor<Model, RdfSchemaFetchOptions> visitor) {
        if (!modelVisitors.contains(modelVisitors)) modelVisitors.add(visitor);
    }

    @Override
    public Model getOntology(ModelVocabulary voc) {
        return getOntology(createOptions(voc));
    }

    @Override
    @Cacheable(cacheNames = RdfCacheConfiguration.Names.ONTOLOGY, key="#options.hashCode()", condition = " #options != null", unless = "#result == null")
    public Model getOntology(RdfSchemaFetchOptions options) {
        Preconditions.checkNotNull(options);

        int cacheKey = options.hashCode();

        // Make sure to fix options (set packages, ...)
        fillOptions(options);
        int fixedCacheKey = options.hashCode();

        // Cache key changed by applyDomainToOptions() => loop using self, to force cache used
        boolean optionsChanged = (cacheKey != fixedCacheKey);
        if (optionsChanged) {
            if (debug) log.debug("Ontology export options was fixed! Will use: " + options.toString());
            return getOntology(options);
        }

        // Run export
        return getSchemaOntologyNoCache(options);
    }

    /* -- protected methods -- */

    protected RdfSchemaFetchOptions createOptions(ModelVocabulary voc) {
        RdfSchemaFetchOptions options = RdfSchemaFetchOptions.builder()
                .domain(voc)
                .build();
        fillOptions(options);
        return options;
    }

    protected RdfSchemaFetchOptions fillOptions(RdfSchemaFetchOptions options) {
        Preconditions.checkNotNull(options);

        ModelVocabulary domain = options.getDomain();
        if (domain == null) {
            if (StringUtils.isNotBlank(options.getClassName())) {
                domain = modelDao.getDomainByClassName(options.getClassName());
            }
            else {
                domain = ModelVocabulary.REFERENTIAL; // default
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

    protected Resource createSchemaResource(OntModel ontology, String namespace) {

        // Add ontology metadata
        String modelLanguage = config.getModelDefaultLanguage();
        Resource schema =  ontology.createResource(namespace)
                .addProperty(RDF.type, OWL.Ontology.asResource())
                .addProperty(OWL2.versionInfo, config.getModelVersion())
                .addProperty(OWL2.versionIRI, namespace)
                // Dublin Core
                .addProperty(DC.language, modelLanguage)
                .addProperty(DC.description, config.getModelDescription(), modelLanguage)
                .addProperty(DC.title, config.getModelTitle(), modelLanguage)
                .addProperty(DC.date, config.getModelDate(), modelLanguage)
                .addProperty(DC.rights, config.getModelLicense(), modelLanguage)
                .addProperty(DC.publisher, config.getModelPublisher(), modelLanguage)
                // RDFS
                .addProperty(RDFS.label, config.getModelLabel(), modelLanguage)
                .addProperty(RDFS.comment, config.getModelComment(), modelLanguage);

        // Add authors
        Iterable<String> authors = Splitter.on(',').omitEmptyStrings().trimResults().split(config.getModelAuthors());
        authors.forEach(author -> schema.addProperty(DC.creator, author));

        return schema;
    }

    protected Model getSchemaOntologyNoCache(RdfSchemaFetchOptions options) {
        Preconditions.checkNotNull(options);
        Preconditions.checkNotNull(options.getDomain());

        String prefix = getPrefix();
        String namespace = getNamespace();

        if (log.isDebugEnabled() && StringUtils.isBlank(options.getClassName())) {
            log.info("Generating {} ontology {{}}...", options.getDomain().name().toLowerCase(), namespace);
        }
        OntModel model = ModelUtils.createOntologyModel(prefix, namespace, options.getReasoningLevel());
        createSchemaResource(model, namespace);

        // Filter visitors, then notify them
        List<IModelVisitor> modelVisitors = getModelVisitors(model, prefix, namespace, options);

        // Notify visitors
        modelVisitors.forEach(visitor -> visitor.visitModel(model, prefix, namespace));

        Multimap<OntClass, OntClass> mutuallyDisjoint = options.isWithDisjoints() ? HashMultimap.create() : null;
        getClassesAsStream(options).forEach(clazz -> {
            // Create the ontology class, from tha java class
            OntClass ontClass = beanConverter.classToOwl(model, clazz, mutuallyDisjoint, options.isWithInterfaces());

            // Notify visitors (e.g. for equivalences)
            modelVisitors.forEach(visitor -> visitor.visitClass(model, ontClass, clazz));
        });

        if (options.isWithDisjoints()) withDisjoints(mutuallyDisjoint);

        return model;
    }

    @Override
    public String getPrefix() {
        String namespace = config.getModelPrefix();
        return StringUtils.isNotBlank(namespace) ? namespace : "this";
    }

    @Override
    public String getNamespace() {
        String uri = config.getModelBaseUri() + ModelType.SCHEMA.name().toLowerCase() + "/";
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

    protected Stream<Class<?>> getClassesAsStream(RdfSchemaFetchOptions options) {

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


        // find by type
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

    protected void withDisjoints(Multimap<OntClass, OntClass> mutuallyDisjoint) {

        if (mutuallyDisjoint != null && !mutuallyDisjoint.isEmpty()) {
            if (debug) log.debug("Adding {} disjoints to model...", mutuallyDisjoint.size());

            // add mutually disjoint classes
            mutuallyDisjoint.keys().stream()
                    .forEach(clazz -> {
                        Collection<OntClass> subClassesCollection = mutuallyDisjoint.get(clazz);
                        if (subClassesCollection.size() > 1) {
                            OntClass[] subClasses = subClassesCollection.toArray(new OntClass[subClassesCollection.size()]);
                            for (int i = 0; i < subClasses.length; i++) {
                                for (int j = i + 1; j < subClasses.length; j++) {
                                    if (debug) log.trace("Adding disjoint between {{}} and {{}}", subClasses[i], subClasses[j]);
                                    subClasses[i].addDisjointWith(subClasses[j]);
                                }
                            }
                        }

                    });
        }

    }

    public List<IModelVisitor> getModelVisitors(Model model, String ns, String schemaUri, RdfSchemaFetchOptions options) {
        return modelVisitors.stream().filter(visitor -> visitor.accept(model, ns, schemaUri, options)).collect(Collectors.toList());
    }
}
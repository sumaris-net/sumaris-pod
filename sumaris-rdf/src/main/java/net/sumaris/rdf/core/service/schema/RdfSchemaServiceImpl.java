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
import com.google.common.collect.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.ModelVocabularyEnum;
import net.sumaris.rdf.core.cache.RdfCacheConfiguration;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.config.RdfConfigurationOption;
import net.sumaris.rdf.core.dao.OntologyEntitiesDao;
import net.sumaris.rdf.core.model.IModelVisitor;
import net.sumaris.rdf.core.model.ModelType;
import net.sumaris.rdf.core.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.core.service.IRdfFetchOptions;
import net.sumaris.rdf.core.service.data.RdfIndividualFetchOptions;
import net.sumaris.rdf.core.util.Bean2Owl;
import net.sumaris.rdf.core.util.ModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.*;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("rdfSchemaService")
@ConditionalOnBean({RdfConfiguration.class})
@Slf4j
public class RdfSchemaServiceImpl implements RdfSchemaService {

    @javax.annotation.Resource
    protected RdfConfiguration rdfConfiguration;

    @javax.annotation.Resource
    protected OntologyEntitiesDao ontologyEntitiesDao;

    protected Bean2Owl beanConverter;

    private boolean debug;
    private String modelBaseUri;
    private String modelPrefix;

    protected List<IModelVisitor<Model, RdfSchemaFetchOptions>> modelVisitors = Lists.newCopyOnWriteArrayList();

    @PostConstruct
    protected void init() {
        debug = log.isDebugEnabled();
        beanConverter = new Bean2Owl(ontologyEntitiesDao);
        loadPrefixAndBaseUri();
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
        // Clean options cache
        rdfConfiguration.cleanCache();
        // Recompute prefix and base uri
        loadPrefixAndBaseUri();
    }

    @Override
    public void register(IModelVisitor<Model, RdfSchemaFetchOptions> visitor) {
        if (!modelVisitors.contains(modelVisitors)) modelVisitors.add(visitor);
    }

    @Override
    public Model getOntology(String vocabulary) {
        return getOntology(createOptions(vocabulary));
    }

    @Override
    @Cacheable(cacheNames = RdfCacheConfiguration.Names.ONTOLOGY,
        key="#options.hashCode()",
        condition = "#options != null",
        unless = "#result == null")
    public Model getOntology(RdfSchemaFetchOptions options) {
        Preconditions.checkNotNull(options);

        int cacheKey = options.hashCode();

        // Make sure to fix options (set packages, ...)
        fillOptions(options);
        int fixedCacheKey = options.hashCode();

        // Cache key changed by applyDomainToOptions() => loop using self, to force cache used
        boolean optionsChanged = (cacheKey != fixedCacheKey);
        if (optionsChanged) {
            if (debug) log.debug("Ontology export options was fixed! Will use: " + options);
            return getOntology(options);
        }

        // Run export
        return getSchemaOntologyNoCache(options);
    }

    @Override
    public Set<String> getAllVocabularies() {
        return ontologyEntitiesDao.getAllVocabularies();
    }

    @Override
    public Model getAllOntologies() {
        Model model = ModelFactory.createDefaultModel();

        // Add each vocab ont model
        getAllVocabularies()
            .stream().map(this::getOntology)
            .forEach(model::add);

        return model;
    }

    /* -- protected methods -- */

    /**
     * Check schema URI validity
     */
    protected void loadPrefixAndBaseUri() {

        String prefix = rdfConfiguration.getModelPrefix();
        modelPrefix = StringUtils.isNotBlank(prefix) ? prefix : "this";
        this.modelBaseUri = rdfConfiguration.getModelBaseUri();

        // Validate namespace, as an URI
        String uri = getNamespace();
        try {
            new URI(uri);
        } catch (URISyntaxException e) {
            throw new BeanInitializationException(String.format("Bad RDF schema namespace {%s}. Please fix the option '%s' in the configuration.", modelBaseUri, RdfConfigurationOption.RDF_MODEL_BASE_URI.getKey(), e.getMessage()));
        }

        // Validate prefix, for a new ontology creation
        try {
            ModelUtils.createOntologyModel(prefix, uri, ReasoningLevel.NONE);
        } catch (PrefixMapping.IllegalPrefixException e) {
            throw new BeanInitializationException(String.format("Bad RDF schema prefix {%s}. Please fix the option '%s' in the configuration.", prefix, RdfConfigurationOption.RDF_MODEL_PREFIX.getKey(), e.getMessage()));
        }
    }

    protected RdfSchemaFetchOptions createOptions(String vocabulary) {
        RdfSchemaFetchOptions options = RdfSchemaFetchOptions.builder()
            .vocabulary(vocabulary)
            .build();
        fillOptions(options);
        return options;
    }

    protected RdfSchemaFetchOptions fillOptions(RdfSchemaFetchOptions options) {
        Preconditions.checkNotNull(options);

        String vocab = options.getVocabulary();
        if (StringUtils.isBlank(vocab)) {
            if (StringUtils.isNotBlank(options.getClassName())) {
                vocab = ontologyEntitiesDao.getVocabularyByClassName(options.getClassName());
            }
            else {
                vocab = ModelVocabularyEnum.DEFAULT.name();
            }
            options.setVocabulary(vocab);
        }

        return options;
    }

    protected Resource createSchemaResource(OntModel ontology, String namespace) {

        // Add ontology metadata
        String modelLanguage = rdfConfiguration.getModelDefaultLanguage();
        Resource schema =  ontology.createResource(namespace)
            .addProperty(RDF.type, OWL.Ontology.asResource())
            .addProperty(OWL2.versionInfo, rdfConfiguration.getModelVersion())
            .addProperty(OWL2.versionIRI, namespace)
            // Dublin Core
            .addProperty(DC.language, modelLanguage)
            .addProperty(DC.description, rdfConfiguration.getModelDescription(), modelLanguage)
            .addProperty(DC.title, rdfConfiguration.getModelTitle(), modelLanguage)
            .addProperty(DC.date, rdfConfiguration.getModelDate(), modelLanguage)
            .addProperty(DC.rights, rdfConfiguration.getModelLicense(), modelLanguage)
            .addProperty(DC.publisher, rdfConfiguration.getModelPublisher(), modelLanguage)
            // RDFS
            .addProperty(RDFS.label, rdfConfiguration.getModelLabel(), modelLanguage)
            .addProperty(RDFS.comment, rdfConfiguration.getModelComment(), modelLanguage);

        if ("en".equalsIgnoreCase(modelLanguage)) {
            schema.addProperty(RDFS.comment, rdfConfiguration.getModelCommentFr(), "fr");
        }
        else if ("fr".equalsIgnoreCase(modelLanguage)) {
            schema.addProperty(RDFS.comment, rdfConfiguration.getModelCommentEn(), "en");
        }

        // Add authors
        Iterable<String> authors = Splitter.on(',').omitEmptyStrings().trimResults().split(rdfConfiguration.getModelAuthors());
        authors.forEach(author -> schema.addProperty(DC.creator, author));

        return schema;
    }

    protected Model getSchemaOntologyNoCache(@NonNull RdfSchemaFetchOptions options) {
        Preconditions.checkNotNull(options.getVocabulary());

        String prefix = options.getVocabulary();
        String namespace = getNamespace(options.getVocabulary(), options.getVersion());

        if (log.isDebugEnabled() && StringUtils.isBlank(options.getClassName())) {
            log.debug("Generating ontology {{}}...", namespace);
        }
        OntModel model = ModelUtils.createOntologyModel(prefix, namespace, options.getReasoningLevel());

        //if (StringUtils.isBlank(options.getClassName())) {
        createSchemaResource(model, namespace);
        //}

        // Filter visitors, then notify them
        List<IModelVisitor> modelVisitors = getModelVisitors(model, prefix, namespace, options);

        // Notify visitors
        modelVisitors.forEach(visitor -> visitor.visitModel(model, prefix, namespace));

        Multimap<OntClass, OntClass> mutuallyDisjoint = options.isWithDisjoints() ? HashMultimap.create() : null;
        getAllClassNames(options)
            .forEach(ontClassName -> {
                try {
                    Class<?> entityClass = ontologyEntitiesDao.getTypeByVocabularyAndClassName(options.getVocabulary(), ontClassName);

                    // Create the ontology class, from tha java class
                    OntClass ontClass = beanConverter.classToOwl(model, entityClass, mutuallyDisjoint, options.isWithInterfaces());

                    // Notify visitors (e.g. for equivalences)
                    modelVisitors.forEach(visitor -> visitor.visitClass(model, ontClass, entityClass));
                }
                catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            });

        if (options.isWithDisjoints()) withDisjoints(mutuallyDisjoint);

        return model;
    }

    @Override
    public String getPrefix() {
        if (modelPrefix == null) loadPrefixAndBaseUri();
        return modelPrefix;
    }

    @Override
    public String getNamespace() {
        return getNamespace(null, null);
    }

    @Override
    public String getNamespace(@NonNull String vocabulary) {
        return getNamespace(vocabulary, null);
    }

    public String getNamespace(String vocabulary, String version) {
        String uri = modelBaseUri + ModelType.SCHEMA.name().toLowerCase() + "/";

        if (uri.endsWith("#")) uri = uri.substring(0, uri.length() -1); // Remove last '#'
        if (!uri.endsWith("/")) uri += "/"; // Add trailing slash

        // Add vocabulary
        if (StringUtils.isNotBlank(vocabulary)) {
            uri += vocabulary;
            if (!uri.endsWith("/")) uri += "/"; // Add trailing slash

            // Add version
            if (StringUtils.isNotBlank(version)) {
                uri += version;
                if (!uri.endsWith("/")) uri += "/"; // Add trailing slash
            }
        }

        return uri;
    }

    @Override
    @Cacheable(cacheNames = RdfCacheConfiguration.Names.TYPES_FOR_INDIVIDUALS, key="#options.hashCode()", condition = "#options != null", unless = "#result == null")
    public Set<Class<?>> getAllTypes(@NonNull RdfIndividualFetchOptions options) {

        if (StringUtils.isNotBlank(options.getClassName())) {
            Class<?> type = ontologyEntitiesDao.getTypeByVocabularyAndClassName(options.getVocabulary(), options.getClassName());
            return ImmutableSet.of(type);
        }

        return ontologyEntitiesDao.getAllTypesByVocabulary(options.getVocabulary());
    }

    @Override
    @Cacheable(cacheNames = RdfCacheConfiguration.Names.CLASS_FOR_INDIVIDUALS, key="#options.hashCode()", condition = "#options != null", unless = "#result == null")
    public Set<String> getAllClassNames(IRdfFetchOptions options) {
        Set<String> allClassNames = ontologyEntitiesDao.getAllClassNamesByVocabulary(options.getVocabulary());

        if (StringUtils.isNotBlank(options.getClassName())) {
            return allClassNames.stream()
                .filter(ontClassName -> ontClassName != null && ontClassName.equalsIgnoreCase(options.getClassName()))
                .collect(Collectors.toSet());
        }

        return allClassNames;
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

    public List<IModelVisitor> getModelVisitors(Model model, String prefix, String namespace, RdfSchemaFetchOptions options) {
        return modelVisitors.stream().filter(visitor -> visitor.accept(model, prefix, namespace, options)).collect(Collectors.toList());
    }
}
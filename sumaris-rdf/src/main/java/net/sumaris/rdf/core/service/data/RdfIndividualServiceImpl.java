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

package net.sumaris.rdf.core.service.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.ModelVocabularyEnum;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.dao.OntologyEntitiesDao;
import net.sumaris.rdf.core.model.IModelVisitor;
import net.sumaris.rdf.core.model.ModelEntities;
import net.sumaris.rdf.core.model.ModelVocabulary;
import net.sumaris.rdf.core.model.reasoner.ReasoningLevel;
import net.sumaris.rdf.core.service.schema.RdfSchemaFetchOptions;
import net.sumaris.rdf.core.service.schema.RdfSchemaService;
import net.sumaris.rdf.core.util.Bean2Owl;
import net.sumaris.rdf.core.util.ModelUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.Entity;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("rdfIndividualService")
@ConditionalOnBean({RdfConfiguration.class})
@Slf4j
public class RdfIndividualServiceImpl implements RdfIndividualService {

    protected final RdfConfiguration configuration;

    protected final OntologyEntitiesDao ontologyEntitiesDao;

    protected final RdfSchemaService schemaService;

    protected Bean2Owl beanConverter;

    protected int defaultPageSize;
    protected int maxPageSize;

    protected List<IModelVisitor<Model, RdfIndividualFetchOptions>> modelVisitors = Lists.newCopyOnWriteArrayList();

    public RdfIndividualServiceImpl(RdfConfiguration configuration,
                                    OntologyEntitiesDao ontologyEntitiesDao,
                                    RdfSchemaService schemaService) {
        this.configuration = configuration;
        this.ontologyEntitiesDao = ontologyEntitiesDao;
        this.schemaService = schemaService;
    }

    @PostConstruct
    protected void init() {
        beanConverter = new Bean2Owl(ontologyEntitiesDao);
        defaultPageSize = configuration.getDefaultPageSize();
        maxPageSize = configuration.getMaxPageSize();
    }

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    protected void onConfigurationReady(ConfigurationEvent event) {
        defaultPageSize = configuration.getDefaultPageSize();
        maxPageSize = configuration.getMaxPageSize();
    }

    @Override
    public void register(IModelVisitor<Model, RdfIndividualFetchOptions> visitor) {
        if (!modelVisitors.contains(modelVisitors)) modelVisitors.add(visitor);
    }

    @Override
    public Model getIndividuals(@NonNull RdfIndividualFetchOptions options) {

        // Make sure to fix options (set packages, ...)
        fillOptions(options);

        // Create base model
        String prefix = schemaService.getPrefix();
        String namespace = schemaService.getNamespace(options.getVocabulary(), configuration.getModelVersion());

        OntModel schema = null;
        if (options.getReasoningLevel() != ReasoningLevel.NONE) {
            schema = (OntModel)schemaService.getOntology(RdfSchemaFetchOptions.builder()
                .vocabulary(options.getVocabulary())
                .className(options.getClassName())
                .reasoningLevel(options.getReasoningLevel())
                .build());
        }
        OntModel model = ModelUtils.createOntologyModel(prefix, namespace, options.getReasoningLevel(), schema);

        boolean hasClassName = StringUtils.isNotBlank(options.getClassName());

        // TODO ajouter une vérification d'autorisation d'export
        //if (options.getVocabulary() == ModelVocabularyEnum.DATA && !hasClassName) {
        //    throw new IllegalArgumentException("Unable to export data without a class name!");
        //}

        // TODO: check if enougth for data
        final int entityGraphDepth = 0; // options.getVocabulary() == ModelVocabularyEnum.DATA ? 3 : 0;

        // Filter visitors, then notify them
        List<IModelVisitor> modelVisitors = getModelVisitors(model, prefix, namespace, options);

        // Notify visitors
        modelVisitors.forEach(visitor -> visitor.visitModel(model, prefix, namespace));

        // When having classname and id
        if (hasClassName && StringUtils.isNotBlank(options.getId())) {
            // Get the bean
            IEntity entity = ontologyEntitiesDao.getByLabel(options.getVocabulary(), options.getClassName(), IEntity.class, options.getId());

            // Convert to individual model
            Resource individual = beanConverter.bean2Owl(model, namespace, entity, entityGraphDepth, ModelEntities.propertyIncludes, ModelEntities.propertyExcludes);

            // Notify visitors
            if (individual != null) {
                modelVisitors.forEach(visitor -> visitor.visitIndividual(model, individual, entity.getClass()));
            }
        }
        else {
            schemaService.getAllClassNames(options)
                .forEach(ontClassName -> {
                    try {

                        ontologyEntitiesDao.streamAll(options.getVocabulary(), ontClassName, options.getPage())
                            .forEach(entity -> {
                                // Create the resource
                                Resource individual = beanConverter.bean2Owl(model, namespace, entity, entityGraphDepth, ModelEntities.propertyIncludes, ModelEntities.propertyExcludes);

                                // Notify visitors
                                modelVisitors.forEach(visitor -> visitor.visitIndividual(model, individual, entity.getClass()));
                            });
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
        }


        return model;
    }

    /* -- protected methods -- */


    protected RdfIndividualFetchOptions fillOptions(@NonNull RdfIndividualFetchOptions options) {

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

        // Reasoning level
        if (options.getReasoningLevel() == null) {
            options.setReasoningLevel(ReasoningLevel.NONE);
        }

        // Page
        if (options.getPage() == null) {
            options.setPage(Page.builder()
                .offset(0)
                .size(defaultPageSize)
                .build());
        }
        else if (options.getPage().getSize() > maxPageSize) {
            throw new DataRetrievalFailureException("Size must be <= " + maxPageSize);
        }

        return options;
    }

    public void onIndividualCreated(Model model, Resource individual, Class clazz) {
        modelVisitors.forEach(visitor -> visitor.visitIndividual(model, individual, clazz));
    }

    public List<IModelVisitor> getModelVisitors(Model model, String ns, String schemaUri, RdfIndividualFetchOptions options) {
        return modelVisitors.stream().filter(visitor -> visitor.accept(model, ns, schemaUri, options)).collect(Collectors.toList());
    }

}
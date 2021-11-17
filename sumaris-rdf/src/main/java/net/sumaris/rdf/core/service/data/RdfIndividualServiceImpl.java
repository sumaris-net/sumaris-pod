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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.rdf.core.config.RdfCacheConfiguration;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.dao.EntitiesDao;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

    @Autowired
    protected RdfConfiguration config;

    @Autowired
    protected EntitiesDao modelDao;

    @Autowired
    protected RdfCacheConfiguration cacheConfiguration;

    @Autowired
    protected RdfSchemaService schemaService;

    protected Bean2Owl beanConverter;

    protected int defaultPageSize;
    protected int maxPageSize;

    protected List<IModelVisitor<Model, RdfIndividualFetchOptions>> modelVisitors = Lists.newCopyOnWriteArrayList();

    @PostConstruct
    protected void init() {

        beanConverter = new Bean2Owl(config.getModelBaseUri());

        defaultPageSize = config.getDefaultPageSize();
        maxPageSize = config.getMaxPageSize();
    }

    @Override
    public void register(IModelVisitor<Model, RdfIndividualFetchOptions> visitor) {
        if (!modelVisitors.contains(modelVisitors)) modelVisitors.add(visitor);
    }

    @Override
    public Model getIndividuals(@Nullable RdfIndividualFetchOptions options) {

        // Make sure to fix options (set packages, ...)
        fillOptions(options);

        // Create base model
        String prefix = schemaService.getPrefix();
        String namespace = schemaService.getNamespace();

        OntModel schema = null;
        if (options.getReasoningLevel() != ReasoningLevel.NONE) {
            schema = (OntModel)schemaService.getOntology(RdfSchemaFetchOptions.builder()
                    .domain(options.getDomain())
                    .className(options.getClassName())
                    .reasoningLevel(options.getReasoningLevel())
                    .build());
        }
        OntModel model = ModelUtils.createOntologyModel(prefix, namespace, options.getReasoningLevel(), schema);

        boolean hasClassName = StringUtils.isNotBlank(options.getClassName());

        if (options.getDomain() == ModelVocabulary.DATA && !hasClassName) {
            throw new IllegalArgumentException("Unable to export data without a class name!");
        }

        final int entityGraphDepth =  options.getDomain() == ModelVocabulary.DATA ? 3 : 0; // TODO: check if enougth for data

        // Filter visitors, then notify them
        List<IModelVisitor> modelVisitors = getModelVisitors(model, prefix, namespace, options);

        // Notify visitors
        modelVisitors.forEach(visitor -> visitor.visitModel(model, prefix, namespace));

        // When having classname and id
        if (hasClassName && StringUtils.isNotBlank(options.getId())) {
            // Get the bean
            IEntity entity = modelDao.getById(options.getDomain(), options.getClassName(), IEntity.class, options.getId());

            // Convert into model
            Resource individual = beanConverter.bean2Owl(model, namespace, entity, entityGraphDepth, ModelEntities.propertyIncludes, ModelEntities.propertyExcludes);

            // Notify visitors
            if (individual != null) {
                modelVisitors.forEach(visitor -> visitor.visitIndividual(model, individual, entity.getClass()));
            }
        }
        else {
            getClassesAsStream(options)
                .forEach(clazz -> modelDao.streamAll(options.getDomain(), clazz.getSimpleName(), IEntity.class, options.getPage())
                    .forEach(entity -> {
                        // Create the resource
                        Resource individual = beanConverter.bean2Owl(model, namespace, entity, entityGraphDepth, ModelEntities.propertyIncludes, ModelEntities.propertyExcludes);

                        // Notify visitors
                        modelVisitors.forEach(visitor -> visitor.visitIndividual(model, individual, clazz));
                    }));
        }


        return model;
    }

    /* -- protected methods -- */


    protected RdfIndividualFetchOptions fillOptions(RdfIndividualFetchOptions options) {
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

        // Annotation and package
        switch (domain) {
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


    // TODO: move this into SchemaService, with a cache !!
    protected Stream<Class<?>> getClassesAsStream(RdfIndividualFetchOptions options) {

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

    public void onInidividualCreated(Model model, Resource individual, Class clazz) {
        modelVisitors.forEach(visitor -> visitor.visitIndividual(model, individual, clazz));
    }


    public List<IModelVisitor> getModelVisitors(Model model, String ns, String schemaUri, RdfIndividualFetchOptions options) {
        return modelVisitors.stream().filter(visitor -> visitor.accept(model, ns, schemaUri, options)).collect(Collectors.toList());
    }

}
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

package net.sumaris.rdf.core.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.annotation.OntologyEntities;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.ModelVocabularyEnum;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.core.config.RdfConfiguration;
import net.sumaris.rdf.core.model.ModelType;
import net.sumaris.rdf.core.model.ModelURIs;
import org.hibernate.query.internal.QueryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

@Repository("entitiesDao")
@Lazy
@Slf4j
public class OntologyEntitiesDaoImpl extends HibernateDaoSupport implements OntologyEntitiesDao {

    public static final String DEFAULT_QUERY = "from %s t";
    public static final String ORDER_BY_CLAUSE = " ORDER BY t.%s %s";

    protected Collection<OntologyEntities.Definition> definitions;
    protected Map<String, OntologyEntities.Definition> cacheByType = Maps.newHashMap();

    protected SetMultimap<String, String> vocabulariesByClassname = Multimaps.newSetMultimap(Maps.newHashMap(), HashSet::new);
    protected SetMultimap<String, Class<?>> typesByVocabulary = Multimaps.newSetMultimap(Maps.newHashMap(), HashSet::new);
    protected SetMultimap<String, String> classNamesByVocabulary = Multimaps.newSetMultimap(Maps.newHashMap(), HashSet::new);
    protected Map<String, Class<?>> typesByVocabularyAndName = Maps.newHashMap();
    protected Map<String, String> queriesByVocabularyAndName = Maps.newHashMap();
    protected Map<String, String> namedQueriesByVocabularyAndName = Maps.newHashMap();

    protected Set<String> vocabularies = Sets.newHashSet();

    @Autowired
    protected ReferentialDao referentialDao;

    @Autowired
    protected RdfConfiguration rdfConfiguration;

    @Value("${rdf.data.pageSize.default:1000}")
    protected int defaultPageSize;

    @PostConstruct
    protected void init() {

        // Load ontology definitions
        definitions =  OntologyEntities.getOntologyEntityDefs(rdfConfiguration.getDelegate(),
            ModelVocabularyEnum.DEFAULT.getLabel(),
            rdfConfiguration.getModelVersion());

        // Fill all caches
        definitions.forEach(def -> {

            // Check duplicate ont class
            String typeName = def.getType().getTypeName();
            if (cacheByType.containsKey(typeName)) {
                throw new SumarisTechnicalException(String.format("Too many @OntologyEntity() on class %s", typeName));
            }

            log.debug("Adding RDF mapping from {{}} to <{}:{}>", typeName, def.getVocabulary(), def.getName());
            cacheByType.put(typeName, def);

            String key = getOntClassKey(def.getVocabulary(), def.getName());
            typesByVocabularyAndName.put(key, def.getType());

            classNamesByVocabulary.put(def.getVocabulary(), def.getName());
            vocabulariesByClassname.put(def.getName(), def.getVocabulary());
            typesByVocabulary.put(def.getVocabulary(), def.getType());
            if (!vocabularies.contains(def.getVocabulary())) vocabularies.add(def.getVocabulary());

            // Query
            if (def.getNamedQuery() != null) {
                namedQueriesByVocabularyAndName.put(key, def.getNamedQuery());
            }
            else if (def.getQuery() != null) {
                queriesByVocabularyAndName.put(key, def.getQuery());
            }

        });
    }

    @Override
    public <T> T getById(String vocabulary, String ontClassName, Class<T> aClass, Serializable id) {

        // Try to convert ID into Integer
        try {
            id = Integer.valueOf(id.toString());
        }
        catch (NumberFormatException t) {/*continue*/}

        return getByProperty(vocabulary, ontClassName, aClass, "id", id);
    }

    @Override
    public <T> T getByLabel(String vocabulary, String ontClassName, Class<T> aClass, String label) {
        return getByProperty(vocabulary, ontClassName, aClass, "label", label);
    }

    @Override
    public <T> T getByProperty(String vocabulary, String ontClassName, Class<T> aClass, String propertyName, Object propertyValue) {

        String hql = getSelectHqlQuery(vocabulary, ontClassName, null, null);

        // Add where clause, on id
        hql += " where t." + propertyName + "=:" + propertyName;

        TypedQuery<T> typedQuery = getEntityManager().createQuery(hql, aClass)
            .setParameter(propertyName, propertyValue)
            .setMaxResults(1);

        return typedQuery.getSingleResult();
    }

    @Override
    public <T> Stream<T> streamAll(String vocabulary, String ontClassName) {

        return streamAll(vocabulary, ontClassName, Page.builder()
                .offset(0)
                .size(defaultPageSize)
                .build());
    }

    @Override
    public <T> Stream<T> streamAll(@NonNull String vocabulary, @NonNull String ontClassName, @NonNull Page page) {

        String key = getOntClassKey(vocabulary, ontClassName);
        Class<T> entityClass = (Class<T>)typesByVocabularyAndName.get(key);

        String hql = getSelectHqlQuery(vocabulary, ontClassName, page.getSortBy(), page.getSortDirection());
        TypedQuery<T> typedQuery = getEntityManager().createQuery(hql, entityClass)
                .setFirstResult((int)page.getOffset())
                .setMaxResults(page.getSize());

        // When using fetch join, stream are not supported, so use a list
        boolean queryHasFetchJoins = hql.indexOf(" fetch ") != -1;
        if (queryHasFetchJoins) {
            return typedQuery.getResultList().stream();
        }

        return typedQuery.getResultStream();
    }

    @Override
    public Set<String> getAllClassNamesByVocabulary(@NonNull String vocabulary) {
        return classNamesByVocabulary.get(vocabulary);
    }

    @Override
    public Set<Class<?>> getAllTypesByVocabulary(@NonNull String vocabulary) {
        return typesByVocabulary.get(vocabulary);
    }

    @Override
    public Class<?> getTypeByVocabularyAndClassName(@NonNull String vocabulary, @NonNull String ontClassName) {

        String key = getOntClassKey(vocabulary, ontClassName);
        return typesByVocabularyAndName.get(key);
    }

    @Override
    public String getVocabularyByClassName(String ontClassName) {
        Preconditions.checkNotNull(ontClassName);
        return vocabulariesByClassname.get(ontClassName)
            .stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Class {%s} not found in ontologies", ontClassName)));
    }

    @Override
    public Optional<String> findTypeUri(Type type) {
        OntologyEntities.Definition definition = cacheByType.get(type.getTypeName());
        if (definition == null) return Optional.empty();

        String schemaUri = rdfConfiguration.getModelVocabularyUri(ModelType.SCHEMA, definition.getVocabulary(), definition.getVersion());
        String uri = ModelURIs.getTypeUri(schemaUri, definition.getName());
        return Optional.of(uri);
    }

    @Override
    public Set<String> getAllVocabularies() {
        return vocabularies;
    }

    /* -- protected methods -- */

    protected String getSelectHqlQuery(@NonNull String vocab, @NonNull String className, String sortAttribute, SortDirection sortDirection) {
        String key = getOntClassKey(vocab, className);
        String namedQuery = queriesByVocabularyAndName.get(key);

        String query;
        if (namedQuery != null) {
            query = getEntityManager().createNamedQuery(namedQuery).unwrap(QueryImpl.class).getQueryString();
        }
        else {
            query = queriesByVocabularyAndName.get(key);
        }

        if (StringUtils.isBlank(query)) {

            // Generate a basic "SELECT * FROM ..." query
            Class<?> entityClass = typesByVocabularyAndName.get(key);

            // Check exists
            if (entityClass == null) throw new IllegalArgumentException(String.format("Vocabulary {%s} has no class named {%s}. ", vocab.toLowerCase(), className));

            query = String.format(DEFAULT_QUERY, entityClass.getSimpleName());
        }

        // Add sort by
        if (StringUtils.isNotBlank(sortAttribute)) {
            query += String.format(ORDER_BY_CLAUSE, sortAttribute, sortDirection != null ? sortDirection.name() : "ASC");
        }

        return query;
    }

    /* Protected methods */

    protected String getOntClassKey(String vocabulary, String ontClassName) {
        return vocabulary + "~" + ontClassName;
    }
}

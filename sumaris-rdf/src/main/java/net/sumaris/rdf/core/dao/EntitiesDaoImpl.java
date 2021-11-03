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
import com.google.common.collect.*;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.core.model.ModelVocabulary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Repository("entitiesDao")
@Lazy
@Slf4j
public class EntitiesDaoImpl extends HibernateDaoSupport implements EntitiesDao {

    public static final String ORDER_BY_CLAUSE = " ORDER BY t.%s %s";

    protected Map<String, String> referentialQueriesByName = Maps.newHashMap();
    protected Map<String, String> dataQueriesByName = Maps.newHashMap();

    protected Multimap<String, String> referentialClassNamesByRootClass = ArrayListMultimap.create();
    protected Multimap<String, String> dataClassNamesByRootClass = ArrayListMultimap.create();

    @Autowired
    protected ReferentialDao referentialDao;

    @Value("${rdf.data.pageSize.default:1000}")
    protected int defaultPageSize;

    @Override
    public <T> T getById(ModelVocabulary domain, String className, Class<T> aClass, Serializable id) {
        String hql = getSelectHqlQuery(domain, className, null, null);

        // Add where clause, on id
        hql += " where t.id=:id";

        try {
            id = Integer.valueOf(id.toString());
        }
        catch (NumberFormatException t) {/*continue*/}


        TypedQuery<T> typedQuery = getEntityManager().createQuery(hql, aClass)
                .setParameter("id", id)
                .setMaxResults(1);

        return typedQuery.getSingleResult();
    }

    @Override
    public <T> Stream<T> streamAll(ModelVocabulary domain, String className, Class<T> aClass) {

        return streamAll(domain, className, aClass,  Page.builder()
                .offset(0)
                .size(defaultPageSize)
                .build());
    }

    @Override
    public <T> Stream<T> streamAll(ModelVocabulary domain, String className, Class<T> aClass, Page page) {
        Preconditions.checkNotNull(page);

        String hql = getSelectHqlQuery(domain, className, page.getSortBy(), page.getSortDirection());
        TypedQuery<T> typedQuery = getEntityManager().createQuery(hql, aClass)
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
    public Set<String> getClassNamesByDomain(ModelVocabulary domain) {
        Preconditions.checkNotNull(domain);
        Multimap<String, String> classNamesByRootClass;
        switch (domain) {
            case REFERENTIAL:
                classNamesByRootClass = referentialClassNamesByRootClass;
                break;
            case DATA:
                classNamesByRootClass = dataClassNamesByRootClass;
                break;
            default:
                throw new IllegalArgumentException(String.format("Model domain {%s} not found", domain.name().toLowerCase()));
        }
        return ImmutableSet.copyOf(classNamesByRootClass.values());
    }

    @Override
    public Set<String> getClassNamesByRootClass(@Nullable ModelVocabulary domain, String className) {
        Preconditions.checkNotNull(className);
        domain = domain != null ? domain : getDomainByClassName(className);
        switch (domain) {
            case REFERENTIAL:
                return ImmutableSet.copyOf(referentialClassNamesByRootClass.get(className.toLowerCase()));
            case DATA:
                return ImmutableSet.copyOf(dataClassNamesByRootClass.get(className.toLowerCase()));
            default:
                throw new IllegalArgumentException(String.format("Model class {%s} not found", className.toLowerCase()));
        }
    }

    @Override
    public ModelVocabulary getDomainByClassName(String className) {
        Preconditions.checkNotNull(className);
        if (referentialClassNamesByRootClass.containsKey(className.toLowerCase())) {
            return ModelVocabulary.REFERENTIAL;
        }
        else if (dataClassNamesByRootClass.containsKey(className.toLowerCase())) {
            return ModelVocabulary.DATA;
        }
        throw new IllegalArgumentException(String.format("Class {%s} not exists in ontology", className));
    }

    /* -- protected methods -- */

    protected String getSelectHqlQuery(ModelVocabulary vocabulary, String className, String sortAttribute, SortDirection sortDirection) {
        Preconditions.checkNotNull(vocabulary);
        Preconditions.checkNotNull(className);
        String hqlQuery = null;
        switch (vocabulary) {
            case REFERENTIAL:
                hqlQuery = referentialQueriesByName.get(className.toLowerCase());
                break;
            case DATA:
                hqlQuery = dataQueriesByName.get(className.toLowerCase());
                break;
        }

        if (StringUtils.isBlank(hqlQuery)) {
            throw new IllegalArgumentException(String.format("Vocabulary {%s} has no class named {%s}", vocabulary.name().toLowerCase(), className));
        }

        // Add sort by
        if (StringUtils.isNotBlank(sortAttribute)) {
            hqlQuery += String.format(ORDER_BY_CLAUSE, sortAttribute, sortDirection != null ? sortDirection.name() : "ASC");
        }

        return hqlQuery;
    }

    @PostConstruct
    protected void fillNamedQueries() {
        // Taxon
        referentialQueriesByName.put("taxon", "select t from TaxonName as t" +
                " left join fetch t.taxonomicLevel as tl" +
                " join fetch t.referenceTaxon as rt" +
                " join fetch t.status st");
        referentialClassNamesByRootClass.putAll("taxon", ImmutableList.of(
                TaxonName.class.getSimpleName(),
                TaxonomicLevel.class.getSimpleName(),
                ReferenceTaxon.class.getSimpleName(),
                Status.class.getSimpleName()
                ));

        referentialQueriesByName.put("transcription", "select t from TranscribingItem t" +
                " join fetch t.type tit " +
                " join fetch t.status st");
        referentialQueriesByName.put("location", "select t from Location t " +
                " join fetch t.locationLevel ll " +
                " join fetch t.validityStatus vs " +
                " join fetch t.status st");
        referentialQueriesByName.put("gear", "select t from Gear t " +
                " join fetch t.gearClassification gl " +
                " join fetch t.strategies s " +
                " join fetch t.status st");


        referentialQueriesByName.put("referencetaxon", "from ReferenceTaxon t");
        referentialQueriesByName.put("status", "from Status t");
        referentialClassNamesByRootClass.put("status", Status.class.getSimpleName());

        referentialClassNamesByRootClass.put("person", Person.class.getSimpleName());

        // Add missing query, from referential entity names
        referentialDao.getAllTypes().forEach(type -> {
            String entityName = type.getId();
            referentialQueriesByName.putIfAbsent(entityName.toLowerCase(), "from " + entityName + " t");
            if (!referentialClassNamesByRootClass.containsKey(entityName.toLowerCase())) {
                referentialClassNamesByRootClass.putAll(entityName.toLowerCase(), ImmutableList.of(entityName, Status.class.getSimpleName()));
            }
        });

    }


}

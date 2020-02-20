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

package net.sumaris.rdf.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import net.sumaris.core.dao.referential.ReferentialDaoImpl;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonName;
import net.sumaris.core.model.referential.taxon.TaxonomicLevel;
import net.sumaris.core.util.StringUtils;
import net.sumaris.rdf.model.ModelDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("rdfModelDao")
@Lazy
public class RdfModelDaoImpl extends HibernateDaoSupport implements RdfModelDao {

    private static final Logger log = LoggerFactory.getLogger(RdfModelDaoImpl.class);

    protected Map<String, String> referentialQueriesByName = Maps.newHashMap();
    protected Map<String, String> dataQueriesByName = Maps.newHashMap();

    protected Multimap<String, String> referentialClassNamesByRootClass = ArrayListMultimap.create();
    protected Multimap<String, String> dataClassNamesByRootClass = ArrayListMultimap.create();

    @Autowired
    protected ReferentialDaoImpl referentialDao;

    @Override
    public <T> T getById(ModelDomain domain, String className, Class<T> aClass, Serializable id) {
        String hql = getSelectHqlQuery(domain, className);

        // When using fetch join, stream are not supported, so use a list
        boolean queryHasSelect = hql.indexOf("select ") == 0;
        if (queryHasSelect) {
            int index = hql.substring(7).indexOf(" ");
            String alias = hql.substring(7, 7 + index);
            hql += String.format(" where %s.id=:id", alias);
        }
        else {
            hql += " where id=:id";
        }

        TypedQuery<T> typedQuery = entityManager.createQuery(hql, aClass)
                .setParameter("id", id)
                .setMaxResults(1);

        return typedQuery.getSingleResult();
    }

    @Override
    public <T> Stream<T> streamAll(ModelDomain domain, String className, Class<T> aClass) {

        String hql = getSelectHqlQuery(domain, className);
        TypedQuery<T> typedQuery = entityManager.createQuery(hql, aClass)
                .setMaxResults(20000);

        // When using fetch join, stream are not supported, so use a list
        boolean queryHasFetchJoins = hql.indexOf(" fetch ") != -1;
        if (queryHasFetchJoins) {
            return typedQuery.getResultList().stream();
        }

        return typedQuery.getResultStream();
    }

    @Override
    public Set<String> getClassNamesByDomain(ModelDomain domain) {
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
    public Set<String> getClassNamesByRootClass(@Nullable ModelDomain domain, String className) {
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
    public ModelDomain getDomainByClassName(String className) {
        Preconditions.checkNotNull(className);
        if (referentialClassNamesByRootClass.containsKey(className.toLowerCase())) {
            return ModelDomain.REFERENTIAL;
        }
        else if (dataClassNamesByRootClass.containsKey(className.toLowerCase())) {
            return ModelDomain.DATA;
        }
        throw new IllegalArgumentException(String.format("Class {%s} not exists in ontology", className));
    }

    /* -- protected methods -- */

    protected String getSelectHqlQuery(ModelDomain domain, String className) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(className);
        String hqlQuery = null;
        switch (domain) {
            case REFERENTIAL:
                hqlQuery = referentialQueriesByName.get(className.toLowerCase());
                break;
            case DATA:
                hqlQuery = dataQueriesByName.get(className.toLowerCase());
                break;
        }

        if (StringUtils.isBlank(hqlQuery)) {
            throw new IllegalArgumentException(String.format("%s with name %s not exists", domain, className));
        }
        return hqlQuery;
    }

    @PostConstruct
    protected void fillNamedQueries() {
        // Taxon
        referentialQueriesByName.put("taxon", "select tn from TaxonName as tn" +
                " left join fetch tn.taxonomicLevel as tl" +
                " join fetch tn.referenceTaxon as rt" +
                " join fetch tn.status st");
        referentialClassNamesByRootClass.putAll("taxon", ImmutableList.of(
                TaxonName.class.getSimpleName(),
                TaxonomicLevel.class.getSimpleName(),
                ReferenceTaxon.class.getSimpleName(),
                Status.class.getSimpleName()
                ));

        referentialQueriesByName.put("transcription", "select ti from TranscribingItem ti" +
                " join fetch ti.type tit " +
                " join fetch ti.status st");
        referentialQueriesByName.put("location", "select l from Location l " +
                " join fetch l.locationLevel ll " +
                " join fetch l.validityStatus vs " +
                " join fetch l.status st");
        referentialQueriesByName.put("gear", "select g from Gear g " +
                " join fetch g.gearClassification gl " +
                " join fetch g.strategies s " +
                " join fetch g.status st");


        referentialQueriesByName.put("referencetaxon", "from ReferenceTaxon");
        referentialQueriesByName.put("status", "from Status");

        // Add missing query, from referential entity names
        referentialDao.getAllTypes().forEach(type -> {
            String entityName = type.getId();
            referentialQueriesByName.putIfAbsent(entityName.toLowerCase(), "from " + entityName);
            if (!referentialClassNamesByRootClass.containsKey(entityName.toLowerCase())) {
                referentialClassNamesByRootClass.putAll(entityName.toLowerCase(), ImmutableList.of(entityName, Status.class.getSimpleName()));
            }
        });


    }


}

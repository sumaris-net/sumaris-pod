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

import com.google.common.collect.Maps;
import net.sumaris.core.dao.referential.ReferentialDaoImpl;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Repository("rdfModelDao")
public class RdfModelDaoImpl extends HibernateDaoSupport implements RdfModelDao {

    private Map<String, String> selectQueriesByName = Maps.newHashMap();

    @Autowired
    protected ReferentialDaoImpl referentialDao;

    @PostConstruct
    protected void afterPropertiesSet() {
        this.fillNamedQueries();
    }

    @Override
    public <T> Stream<T> streamAll(String entityName, Class<T> aClass) {

        String hql = getSelectHqlQuery(entityName);
        TypedQuery<T> typedQuery = entityManager.createQuery(hql, aClass)
                .setMaxResults(20000);
        boolean queryHasFetchJoins = hql.indexOf(" fetch ") != -1;

        // When using fetch join, stream are not supported, so use a list
        if (queryHasFetchJoins) {
            return typedQuery.getResultList().stream();
        }

        return typedQuery.getResultStream();
    }

    @Override
    public <T> List<T> loadAll(String entityName, Class<T> aClass) {

        String hql = getSelectHqlQuery(entityName);
        return entityManager.createQuery(hql, aClass)
                .setMaxResults(20000)
                .getResultList();
    }

    /* -- protected methods -- */

    protected String getSelectHqlQuery(String entityName) {
        String hqlQuery = selectQueriesByName.get(entityName.toLowerCase());
        if (StringUtils.isBlank(hqlQuery)) {
            throw new IllegalArgumentException(String.format("Referential with name %s not exists", entityName));
        }
        return hqlQuery;
    }

    protected void fillNamedQueries() {
        selectQueriesByName.put("taxon", "select tn from TaxonName as tn" +
                " left join fetch tn.taxonomicLevel as tl" +
                " join fetch tn.referenceTaxon as rt" +
                " join fetch tn.status st" +
                " where tn.updateDate > '2015-01-01 23:59:50'");
        selectQueriesByName.put("transcription", "select ti from TranscribingItem ti" +
                " join fetch ti.type tit " +
                " join fetch ti.status st");
        selectQueriesByName.put("location", "select l from Location l " +
                " join fetch l.locationLevel ll " +
                " join fetch l.validityStatus vs " +
                " join fetch l.status st");
        selectQueriesByName.put("gear", "select g from Gear g " +
                " join fetch g.gearClassification gl " +
                " join fetch g.strategies s " +
                " join fetch g.status st");


        // Add missing query, from referential entity names
        referentialDao.getAllTypes().forEach(type -> {
            String entityName = type.getId();
            selectQueriesByName.putIfAbsent(entityName.toLowerCase(), "from " + entityName);
        });

    }
}

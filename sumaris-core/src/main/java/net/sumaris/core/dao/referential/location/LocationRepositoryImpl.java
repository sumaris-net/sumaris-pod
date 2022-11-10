package net.sumaris.core.dao.referential.location;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import com.google.common.collect.*;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationAssociation;
import net.sumaris.core.model.referential.location.LocationHierarchy;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import org.apache.commons.collections4.MapUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author peck7 on 18/08/2020.
 */
@Slf4j
public class LocationRepositoryImpl
    extends ReferentialRepositoryImpl<Integer, Location, LocationVO, LocationFilterVO, ReferentialFetchOptions>
    implements LocationSpecifications {

    public LocationRepositoryImpl(EntityManager entityManager) {
        super(Location.class, LocationVO.class, entityManager);
    }

    @Override
    @Cacheable(cacheNames = CacheConfiguration.Names.LOCATION_BY_ID, unless = "#result==null")
    public LocationVO get(Integer id) {
        return super.get(id);
    }

    @Override
    protected Specification<Location> toSpecification(LocationFilterVO filter, ReferentialFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(inLevelIds(Location.class, filter.getLevelIds()))
            .and(hasAncestors(filter.getAncestorIds()))
            ;
    }

    @Override
    public boolean hasAssociation(int childLocationId, int parentLocationId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<LocationAssociation> root = query.from(LocationAssociation.class);

        ParameterExpression<Integer> childIdParam = builder.parameter(Integer.class);
        ParameterExpression<Integer> parentIdParam = builder.parameter(Integer.class);

        query.select(builder.count(root))
            .where(
                builder.and(
                    builder.equal(root.get(LocationAssociation.Fields.CHILD_LOCATION).get(IEntity.Fields.ID), childIdParam),
                    builder.equal(root.get(LocationAssociation.Fields.PARENT_LOCATION).get(IEntity.Fields.ID), parentIdParam)
                )
            );

        return getEntityManager().createQuery(query)
            .setParameter(childIdParam, childLocationId)
            .setParameter(parentIdParam, parentLocationId)
            .getSingleResult() > 0;
    }

    @Override
    public void addAssociation(int childLocationId, int parentLocationId, double childSurfaceRatio) {
        LocationAssociation entity = new LocationAssociation();
        entity.setChildLocation(getReference(Location.class, childLocationId));
        entity.setParentLocation(getReference(Location.class, parentLocationId));
        entity.setChildSurfaceRatio(childSurfaceRatio);

        // Update update_dt
        Date newUpdateDate = getDatabaseCurrentDate();
        entity.setUpdateDate(newUpdateDate);

        getEntityManager().persist(entity);
    }

    @Override
    public void updateLocationHierarchy() {
        String jdbcUrl = getConfig().getJdbcURL();

        // If running on Hsqldb, or Postgrsql: run java implementation
        if (Daos.isHsqlDatabase(jdbcUrl) || Daos.isPostgresqlDatabase(jdbcUrl)) {
            doUpdateLocationHierarchy();
        }

        // If Oracle, call PL/SQL procédure
        else if (Daos.isOracleDatabase(jdbcUrl)) {
            getEntityManager().createStoredProcedureQuery("P_FILL_LOCATION_HIERARCHY")
                    .execute();
        }
    }

    @Override
    public long count(LocationFilterVO filter) {
        return super.count(filter);
    }

    /* -- protected functions -- */

    protected void doUpdateLocationHierarchy() {
        long startTime = System.currentTimeMillis();
        log.info("Adding missing LocationHierarchy... (Java implementation)");

        EntityManager em = this.getEntityManager();
        Timestamp updateDate = getDatabaseCurrentTimestamp();
        Map<Integer, Set<Integer>> allParentsByChild = loadLocationHierarchyMap();
        int counter = 0;
        int iterationCounter = 0;
        boolean stop = false;
        while (!stop) {

            // Get location associations
            TypedQuery<Object[]> query = em.createQuery("select distinct " +
                "l.id, parent.id " +
                "from Location l, " +
                "LocationAssociation la, " +
                "Location parent " +
                "where l.id=la.childLocation.id and parent.id=la.parentLocation.id", Object[].class);

            int count = insertMissingLocationHierarchies(query.getResultStream(),
                allParentsByChild,
                updateDate);
            stop = count == 0;
            counter += count;
            iterationCounter++;
        }

        log.info("Adding missing LocationHierarchy [OK] {} - ({} inserts - {} iterations)",
            Dates.elapsedTime(startTime),
            counter, iterationCounter);
    }

    protected int insertMissingLocationHierarchies(Stream<Object[]> locationAssociations,
                                                   Map<Integer, Set<Integer>> existingParentsByChild,
                                                   Timestamp updateDate) {
        final EntityManager em = getEntityManager();
        final Map<Integer, Set<Integer>> newLinks = Maps.newHashMap();

        // First pass, on direct associations
        locationAssociations
            .forEach(row -> {
                Integer childId = (Integer) row[0];
                Integer parentId = (Integer) row[1];

                Set<Integer> newParents = Beans.getSet(newLinks.get(childId));
                Set<Integer> existingParents = Beans.getSet(existingParentsByChild.get(childId));

                // Add link to himself
                if (!existingParents.contains(childId)) {
                    newParents.add(childId);
                }

                // Add link to parent
                if (!existingParents.contains(parentId)) {
                    newParents.add(parentId);
                }

                // Add all parent's parents
                Beans.getStream(existingParentsByChild.get(parentId))
                    .filter(parentIdOfParent -> !existingParents.contains(parentIdOfParent))
                    .forEach(newParents::add);

                if (!newParents.isEmpty()) {
                    newLinks.put(childId, newParents);

                    existingParents.addAll(newParents);
                    existingParentsByChild.put(childId, existingParents);
                }
            });

        if (MapUtils.isEmpty(newLinks)) return 0;

        int counter = 0;
        for (Integer childId : newLinks.keySet()) {
            Location child = getReference(Location.class, childId);
            for (Integer parentId : newLinks.get(childId)) {
                Location parent = getReference(Location.class, parentId);
                LocationHierarchy lh = new LocationHierarchy();
                lh.setChildLocation(child);
                lh.setParentLocation(parent);
                lh.setUpdateDate(updateDate);

                lh.setChildSurfaceRatio(1d); // TODO: better computation
                lh.setIsMainAssociation(Boolean.TRUE); // TODO, review this
                em.persist(lh);
                counter++;
            }
        }

        em.flush();
        em.clear();

        return counter;
    }

    protected Map<Integer, Set<Integer>> loadLocationHierarchyMap() {
        TypedQuery<Object[]> query = this.getEntityManager().createQuery("select distinct lh.childLocation.id, lh.parentLocation.id from LocationHierarchy lh", Object[].class);
        return query.getResultStream()
            .collect(Collectors.toMap(
                row -> (Integer)row[0], // key
                row -> Sets.newHashSet((Integer)row[1]), // value
                (o1, o2) -> {
                    o1.addAll(o2);
                    return o1;
                },
                HashMap<Integer, Set<Integer>>::new));
    }
}

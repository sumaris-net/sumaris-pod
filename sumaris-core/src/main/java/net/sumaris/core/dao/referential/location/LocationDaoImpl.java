package net.sumaris.core.dao.referential.location;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.ValidityStatus;
import net.sumaris.core.model.referential.ValidityStatusEnum;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationAssociation;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.referential.LocationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Repository("locationDao")
public class LocationDaoImpl extends HibernateDaoSupport implements LocationDao {

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(LocationDaoImpl.class);

    @Override
    public LocationVO findByLabel(final String label) {

        try {
            return getByLabel(label);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public LocationVO getByLabel(final String label) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Location> query = builder.createQuery(Location.class);
        Root<Location> root = query.from(Location.class);

        ParameterExpression<String> labelParam = builder.parameter(String.class);

        query.select(root)
                .where(builder.equal(root.get(Location.Fields.LABEL), labelParam));

        TypedQuery<Location> q = getEntityManager().createQuery(query)
                .setParameter(labelParam, label);
        return toLocationVO(q.getSingleResult());
    }

    @Override
    public List<LocationVO> getByLocationLevel(int locationLevelId) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Location> query = builder.createQuery(Location.class);
        Root<Location> root = query.from(Location.class);

        ParameterExpression<Integer> levelIdParam = builder.parameter(Integer.class);

        query.select(root)
                .where(builder.equal(root.get(Location.Fields.LOCATION_LEVEL).get(LocationLevel.Fields.ID), levelIdParam));

        TypedQuery<Location> q = getEntityManager().createQuery(query)
                .setParameter(levelIdParam, locationLevelId);
        return q.getResultStream().map(this::toLocationVO).collect(Collectors.toList());
    }

    @Override
    public Location create(Location location) {
        Preconditions.checkNotNull(location);
        Preconditions.checkNotNull(location.getLabel());
        Preconditions.checkNotNull(location.getName());

        // Default status
        if (location.getStatus() == null) {
            location.setStatus(load(Status.class, StatusEnum.ENABLE.getId()));
        }

        // Default validity status
        if (location.getValidityStatus() == null) {
            location.setValidityStatus(load(ValidityStatus.class, ValidityStatusEnum.VALID.getId()));
        }

        getEntityManager().persist(location);

        return location;
    }

    @Override
    public Location update(Location location) {
        Preconditions.checkNotNull(location);
        Preconditions.checkNotNull(location.getId());
        Preconditions.checkNotNull(location.getLabel());
        Preconditions.checkNotNull(location.getName());

        // Default status
        if (location.getStatus() == null) {
            location.setStatus(load(Status.class, StatusEnum.ENABLE.getId()));
        }

        // Default validity status
        if (location.getValidityStatus() == null) {
            location.setValidityStatus(load(ValidityStatus.class, ValidityStatusEnum.VALID.getId()));
        }

        return getEntityManager().merge(location);
    }

    @Override
    public LocationVO toLocationVO(Location source) {
        if (source == null) return null;

        LocationVO target = new LocationVO();

        Beans.copyProperties(source, target);

        if (source.getLocationLevel() != null) {
            target.setLevelId(source.getLocationLevel().getId());
        }

        if (source.getStatus() != null) {
            target.setStatusId(source.getStatus().getId());
        }

        if (source.getValidityStatus() != null) {
            target.setValidityStatusId(source.getValidityStatus().getId());
        }

        return target;
    }

    @Override
    public void updateLocationHierarchy() {

        // If running on HSQLDB: skip (no stored procedure define)
        if (Daos.isHsqlDatabase(config.getJdbcURL())) {
            log.warn("Skipping location hierarchy (Stored procedure P_FILL_LOCATION_HIERARCHY not exists)");
            return;
        }

        Query q = getEntityManager().createNamedQuery("fillLocationHierarchy");
        q.getResultList();
    }

    @Override
    public boolean hasAssociation(int childLocationId, int parentLocationId) {
        EntityManager em = getEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();
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

        return em.createQuery(query)
                .setParameter(childIdParam, childLocationId)
                .setParameter(parentIdParam, parentLocationId)
                .getSingleResult() > 0;
    }

    @Override
    public void addAssociation(int childLocationId, int parentLocationId, double childSurfaceRatio) {
        LocationAssociation entity = new LocationAssociation();
        entity.setChildLocation(load(Location.class, childLocationId));
        entity.setParentLocation(load(Location.class, parentLocationId));
        entity.setChildSurfaceRatio(childSurfaceRatio);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        getEntityManager().persist(entity);
    }

    @Override
    public Location get(int id) {
        return load(Location.class, id);
    }

    /* -- protected methods -- */

}

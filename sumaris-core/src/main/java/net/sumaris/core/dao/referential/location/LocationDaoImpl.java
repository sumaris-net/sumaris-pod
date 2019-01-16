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
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusId;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.vo.referential.LocationVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.stream.Collectors;

@Repository("locationDao")
public class LocationDaoImpl extends HibernateDaoSupport implements LocationDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(LocationDaoImpl.class);

    @Override
    public LocationVO findByLabel(final String label) {

        try {
            return getByLabel(label);
        }
        catch(Exception e) {
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
                .where(builder.equal(root.get(Location.PROPERTY_LABEL), labelParam));

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
                .where(builder.equal(root.get(Location.PROPERTY_LOCATION_LEVEL).get(LocationLevel.PROPERTY_ID), levelIdParam));

        TypedQuery<Location> q = getEntityManager().createQuery(query)
                .setParameter(levelIdParam, locationLevelId);
        return q.getResultList().stream().map(this::toLocationVO).collect(Collectors.toList());
    }

    @Override
    public Location create(Location location) {
        Preconditions.checkNotNull(location);
        Preconditions.checkNotNull(location.getId());
        Preconditions.checkNotNull(location.getLabel());
        Preconditions.checkNotNull(location.getName());

        // Default value
        if (location.getStatus() == null) {
            location.setStatus(load(Status.class, StatusId.ENABLE.getId()));
        }

        getEntityManager().persist(location);

        return location;
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

        return target;
    }

    @Override
    public void updateLocationHierarchy() {

        Query q = getEntityManager().createNamedQuery("fillLocationHierarchy");
        q.getResultList();
    }

    /* -- protected methods -- */

}

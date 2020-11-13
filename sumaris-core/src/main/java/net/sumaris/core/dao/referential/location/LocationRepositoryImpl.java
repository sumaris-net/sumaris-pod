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

import net.sumaris.core.dao.referential.ReferentialRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationAssociation;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;

/**
 * @author peck7 on 18/08/2020.
 */
public class LocationRepositoryImpl
    extends ReferentialRepositoryImpl<Location, LocationVO, ReferentialFilterVO, ReferentialFetchOptions>
    implements LocationSpecifications {

    private static final Logger log = LoggerFactory.getLogger(LocationRepositoryImpl.class);

    public LocationRepositoryImpl(EntityManager entityManager) {
        super(Location.class, LocationVO.class, entityManager);
    }

    @Override
    protected Specification<Location> toSpecification(ReferentialFilterVO filter, ReferentialFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(inLevelIds(Location.Fields.LOCATION_LEVEL, filter));
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
        entity.setChildLocation(load(Location.class, childLocationId));
        entity.setParentLocation(load(Location.class, parentLocationId));
        entity.setChildSurfaceRatio(childSurfaceRatio);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        getEntityManager().persist(entity);
    }

    @Override
    public void updateLocationHierarchy() {
        String jdbcUrl = getConfig().getJdbcURL();
        // If running on HSQLDB: skip (no stored procedure define)
        if (Daos.isHsqlDatabase(jdbcUrl)) {
            log.warn("Skipping location hierarchy (Stored procedure P_FILL_LOCATION_HIERARCHY not exists). TODO: add Java implementation ?");

            // TODO: add Java implementation

            return;
        }

        // If Oracle, call PL/SQL procédure
        if (Daos.isOracleDatabase(jdbcUrl)) {
            getEntityManager().createStoredProcedureQuery("P_FILL_LOCATION_HIERARCHY")
                    .execute();
            return;
        }
    }
}

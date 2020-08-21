package net.sumaris.core.dao.referential.location;

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
    implements LocationRepositoryExtend {

    private static final Logger log = LoggerFactory.getLogger(LocationRepositoryImpl.class);

    public LocationRepositoryImpl(EntityManager entityManager) {
        super(Location.class, LocationVO.class, entityManager);
    }

    @Override
    protected Specification<Location> toSpecification(ReferentialFilterVO filter) {
        return super.toSpecification(filter)
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
        // If running on HSQLDB: skip (no stored procedure define)
        if (Daos.isHsqlDatabase(config.getJdbcURL())) {
            log.warn("Skipping location hierarchy (Stored procedure P_FILL_LOCATION_HIERARCHY not exists)");
            return;
        }

        //noinspection JpaQueryApiInspection (only in Oracle env)
        Query q = getEntityManager().createNamedQuery("fillLocationHierarchy");
        q.getResultList();

    }
}

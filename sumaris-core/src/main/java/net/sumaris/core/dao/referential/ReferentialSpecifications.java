package net.sumaris.core.dao.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import lombok.NonNull;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationHierarchy;
import net.sumaris.core.model.referential.spatial.SpatialItem;
import net.sumaris.core.model.referential.spatial.SpatialItem2Location;
import net.sumaris.core.model.referential.spatial.SpatialItemType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

public interface ReferentialSpecifications<ID extends Serializable, E extends IReferentialWithStatusEntity<ID>>
    extends IEntityWithStatusSpecifications<ID, E>,
        ISearchTextSpecifications<ID, E> {

    String LEVEL_LABEL_PARAMETER = "levelLabel";

    default Specification<E> hasId(Integer id) {
        if (id == null) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<Integer> idParam = cb.parameter(Integer.class, IEntity.Fields.ID);
            return cb.equal(root.get(IEntity.Fields.ID), idParam);
        }).addBind(IEntity.Fields.ID, id);
    }

    default Specification<E> hasLabel(String label) {
        if (label == null) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<String> labelParam = cb.parameter(String.class, IItemReferentialEntity.Fields.LABEL);
            return cb.equal(cb.upper(root.get(IItemReferentialEntity.Fields.LABEL)), labelParam);
        }).addBind(IItemReferentialEntity.Fields.LABEL, label.toUpperCase());
    }

    default Specification<E> hasName(String name) {
        if (name == null) return null;
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<String> labelParam = cb.parameter(String.class, IItemReferentialEntity.Fields.NAME);
            return cb.equal(cb.upper(root.get(IItemReferentialEntity.Fields.NAME)), labelParam);
        }).addBind(IItemReferentialEntity.Fields.NAME, name.toUpperCase());
    }

    default Specification<E> inLevelIds(Class<E> entityClass, Integer... levelIds) {
        if (ArrayUtils.isEmpty(levelIds)) return null;
        return ReferentialEntities.getLevelPropertyNameByClass(entityClass).map(p -> inJoinPropertyIds(p, levelIds))
            .orElse(null);
    }

    default Specification<E> inJoinPropertyIds(String joinPropertyName, Integer... ids) {
        // If empty: skip to avoid an unused join
        if (ArrayUtils.isEmpty(ids)) return null;

        final String parameterName = PROPERTY_PARAMETER_PREFIX + StringUtils.capitalize(joinPropertyName.replaceAll("[.]", "_"));
        return BindableSpecification.<E>where((root, query, cb) -> {
            ParameterExpression<Collection> parameter = cb.parameter(Collection.class, parameterName);
            return cb.in(
                    Daos.composeJoin(root, joinPropertyName, JoinType.INNER).get(IEntity.Fields.ID)
                )
                .value(parameter);
        })
        .addBind(parameterName, Arrays.asList(ids));
    }

    default Specification<E> inLevelLabels(Class<E> entityClass, String[] levelLabels) {
        // If empty: skip to avoid an unused join
        if (ArrayUtils.isEmpty(levelLabels)) return null;

        return ReferentialEntities.getLevelPropertyNameByClass(entityClass).map(levelPropertyName ->
                    BindableSpecification.<E>where((root, query, cb) -> {
                        ParameterExpression<Collection> levelParam = cb.parameter(Collection.class, LEVEL_LABEL_PARAMETER);
                        return cb.in(root.join(levelPropertyName, JoinType.INNER).get(IItemReferentialEntity.Fields.LABEL)).value(levelParam);
                    }).addBind(LEVEL_LABEL_PARAMETER, Arrays.asList(levelLabels))
            )
        .orElse(null);
    }

    default Specification<E> inSearchJoinLevelIds(String searchJoin, Integer... joinLevelIds) {
        if (StringUtils.isBlank(searchJoin) || ArrayUtils.isEmpty(joinLevelIds)) return null;

        // Try to get the entity class, from the filter 'searchJoin' attribute
        Class<? extends IReferentialEntity> joinEntityClass;
        try {
            joinEntityClass = ReferentialEntities.getEntityClass(StringUtils.capitalize(searchJoin));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Cannot filter on levelId, when searchJoin in on '%s'", searchJoin), e);
        }

        return ReferentialEntities.getLevelPropertyNameByClass(joinEntityClass)
            .map(levelPath -> StringUtils.doting(StringUtils.uncapitalize(searchJoin), levelPath)) // Create the full path
            .map(fullLevelPath -> inJoinPropertyIds(fullLevelPath, joinLevelIds))
            .orElse(null);
    }

    default Specification<E> inLocationIds(@NonNull Integer spatialItemTypeId, Integer... ids) {
        // If empty: skip to avoid an unused join
        if (ArrayUtils.isEmpty(ids)) return null;

        String locationIdsParameterName =  ReferentialFilterVO.Fields.LOCATION_IDS;
        String spatialItemTypeIdParameterName =  SpatialItem.Fields.SPATIAL_ITEM_TYPE;

        return BindableSpecification.<E>where((root, query, cb) -> {
                ParameterExpression<Integer> spatialItemTypeIdParameter = cb.parameter(Integer.class, spatialItemTypeIdParameterName);
                ParameterExpression<Collection> locationIdsParameter = cb.parameter(Collection.class, locationIdsParameterName);

                Root<SpatialItem> si = Daos.getRoot(query, SpatialItem.class);
                Join<SpatialItem, SpatialItemType> sit = Daos.composeJoin(si, SpatialItem.Fields.SPATIAL_ITEM_TYPE, JoinType.INNER);
                if (sit.getOn() == null) {
                    sit.on(cb.equal(sit.get(IEntity.Fields.ID), spatialItemTypeIdParameter));
                }
                ListJoin<SpatialItem, SpatialItem2Location> si2l = Daos.composeJoinList(si, SpatialItem.Fields.LOCATIONS, JoinType.INNER);

                Root<LocationHierarchy> lh = Daos.getRoot(query, LocationHierarchy.class);

                return cb.and(
                    cb.equal(si.get(SpatialItem.Fields.OBJECT_ID), root.get(IEntity.Fields.ID)),

                    // LH.PARENT_LOCATION_FK = SI2L.LOCATION_FK
                    cb.equal(lh.get(LocationHierarchy.Fields.PARENT_LOCATION), si2l.get(SpatialItem2Location.Fields.LOCATION)),

                    // AND LH.CHILD_LOCATION_FK IN (:locationIdsParameter)
                    cb.in(Daos.composePath(lh, StringUtils.doting(LocationHierarchy.Fields.CHILD_LOCATION, Location.Fields.ID))).value(locationIdsParameter)
                );
            })
            .addBind(spatialItemTypeIdParameterName, spatialItemTypeId)
            .addBind(locationIdsParameterName, Arrays.asList(ids))
            ;
    }

    default boolean shouldQueryDistinct(String joinProperty) {
        return true;
    }
}

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

package net.sumaris.core.dao.referential.conversion;

import net.sumaris.core.dao.referential.IEntityWithStatusSpecifications;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.referential.conversion.WeightLengthConversion;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationHierarchy;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.model.referential.location.LocationLevels;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.util.*;

public interface WeightLengthConversionSpecifications
    extends IEntityWithStatusSpecifications<WeightLengthConversion> {

    String MONTH_PARAMETER = "month";
    String RECTANGLE_LABELS_PARAMETER = "rectangleLabels";
    String RECTANGLE_LEVEL_IDS_PARAMETER = "rectangleLevelIds";

    default Specification<WeightLengthConversion> hasReferenceTaxonIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.REFERENCE_TAXON, ids);
    }

    default Specification<WeightLengthConversion> hasLocationIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.LOCATION, ids);
    }

    default Specification<WeightLengthConversion> hasRectangleLabels(String... rectangleLabels) {
        if (ArrayUtils.isEmpty(rectangleLabels)) return null;

        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> labelsParam = cb.parameter(Collection.class, RECTANGLE_LABELS_PARAMETER);
            ParameterExpression<Collection> levelIdsParam = cb.parameter(Collection.class, RECTANGLE_LEVEL_IDS_PARAMETER);

            Join<WeightLengthConversion, Location> locationJoin = Daos.composeJoin(root, WeightLengthConversion.Fields.LOCATION, JoinType.INNER);
            Root<LocationHierarchy> lh = query.from(LocationHierarchy.class);
            Root<Location> rectangleLocation =  query.from(Location.class);

            return cb.and(
                // LH.PARENT_LOCATION_FK = <ROOT>.LOCATION_FK
                cb.equal(lh.get(LocationHierarchy.Fields.PARENT_LOCATION), locationJoin),

                // AND CHILD_LOCATION.LOCATION_LEVEL_FK in -:locationLevelIds)
                cb.equal(lh.get(LocationHierarchy.Fields.CHILD_LOCATION), rectangleLocation),

                // Rectngal location levels
                Daos.composePath(rectangleLocation, StringUtils.doting(Location.Fields.LOCATION_LEVEL, LocationLevel.Fields.ID))
                    .in(levelIdsParam),

                // AND CHILD_LOCATION.LABEL in (:locationLabels)
                rectangleLocation.get(Location.Fields.LABEL).in(labelsParam)
            );
        })
        .addBind(RECTANGLE_LABELS_PARAMETER, Arrays.asList(rectangleLabels))
        .addBind(RECTANGLE_LEVEL_IDS_PARAMETER, Arrays.asList(LocationLevels.getStatisticalRectangleLevelIds()))
        ;
    }


    default Specification<WeightLengthConversion> hasSexIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.SEX, ids);
    }

    default Specification<WeightLengthConversion> hasLengthParameterIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.LENGTH_PARAMETER, ids);
    }

    default Specification<WeightLengthConversion> hasLengthUnitIds(Integer... ids) {
        return hasJoinIds(WeightLengthConversion.Fields.LENGTH_UNIT, ids);
    }

    default Specification<WeightLengthConversion> atDate(Date aDate) {
        if (aDate == null) return null;
        Calendar aCalendar = Calendar.getInstance();
        aCalendar.setTime(aDate);
        Integer month = aCalendar.get(Calendar.MONTH);
        return BindableSpecification.where((root, query, builder) -> {

            ParameterExpression<Integer> monthParam = builder.parameter(Integer.class, MONTH_PARAMETER);
            return builder.and(
                builder.lessThanOrEqualTo(root.get(WeightLengthConversion.Fields.START_MONTH), monthParam),
                builder.greaterThanOrEqualTo(root.get(WeightLengthConversion.Fields.END_MONTH), monthParam)
            );
        }).addBind(MONTH_PARAMETER, month);
    }

    List<WeightLengthConversionVO> findAll(WeightLengthConversionFilterVO filter, Page page, WeightLengthConversionFetchOptions fetchOptions);

    long count(WeightLengthConversionFilterVO filter);
}

package net.sumaris.core.dao.data.vessel;

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

import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.data.VesselUseFeatures;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.VesselUseFeaturesVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface VesselUseFeaturesSpecifications extends UseFeaturesSpecifications<VesselUseFeatures> {
    String LOCATION_IDS_PARAM = "locationIds";

    default Specification<VesselUseFeatures> hasLocationId(Integer locationId) {
        if (locationId == null) return null;
        return hasLocationIds(new Integer[]{locationId});
    }

    default Specification<VesselUseFeatures> hasLocationIds(Integer[] locationIds) {
        if (ArrayUtils.isEmpty(locationIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            // TODO use location hierarchy ?
            ParameterExpression<Collection> param = cb.parameter(Collection.class, LOCATION_IDS_PARAM);
            return cb.in(root.get(VesselUseFeatures.Fields.BASE_PORT_LOCATION).get(IEntity.Fields.ID)).value(param);
        }).addBind(LOCATION_IDS_PARAM, Arrays.asList(locationIds));
    }

    default Specification<VesselUseFeatures> hasIsActive(Integer isActive) {
        if (isActive == null) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Integer> param = cb.parameter(Integer.class, VesselUseFeatures.Fields.IS_ACTIVE);
            return cb.equal(root.get(VesselUseFeatures.Fields.IS_ACTIVE), param);
        }).addBind(VesselUseFeatures.Fields.IS_ACTIVE, isActive);
    }

    default Specification<VesselUseFeatures> hasActivityCalendarId(Integer activityCalendarId) {
        return hasParentId(activityCalendarId, StringUtils.doting(VesselUseFeatures.Fields.ACTIVITY_CALENDAR, IEntity.Fields.ID));
    }

    default Specification<VesselUseFeatures> hasDailyActivityCalendarId(Integer activityCalendarId) {
        return hasParentId(activityCalendarId, StringUtils.doting(VesselUseFeatures.Fields.DAILY_ACTIVITY_CALENDAR, IEntity.Fields.ID));
    }

    @Transactional()
    List<VesselUseFeaturesVO> saveAllByActivityCalendarId(int parentId, final List<VesselUseFeaturesVO> sources);

    @Transactional()
    List<VesselUseFeaturesVO> saveAllByDailyActivityCalendarId(int parentId, final List<VesselUseFeaturesVO> sources);
}

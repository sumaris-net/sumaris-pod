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
import net.sumaris.core.model.data.GearUseFeatures;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.GearUseFeaturesVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.ParameterExpression;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface GearUseFeaturesSpecifications
    extends UseFeaturesSpecifications<GearUseFeatures>
{
    String METIER_IDS_PARAM = "metierIds";
    String GEAR_IDS_PARAM = "gearIds";

    default Specification<GearUseFeatures> hasMetierId(Integer metierId) {
        if (metierId == null) return null;
        return hasMetierIds(new Integer[]{metierId});
    }

    default Specification<GearUseFeatures> hasMetierIds(Integer[] metierIds) {
        if (ArrayUtils.isEmpty(metierIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, METIER_IDS_PARAM);
            return root.get(GearUseFeatures.Fields.METIER).get(IEntity.Fields.ID).in(param);
        }).addBind(METIER_IDS_PARAM, Arrays.asList(metierIds));
    }

    default Specification<GearUseFeatures> hasGearId(Integer gearId) {
        if (gearId == null) return null;
        return hasGearIds(new Integer[]{gearId});
    }

    default Specification<GearUseFeatures> hasGearIds(Integer[] gearIds) {
        if (ArrayUtils.isEmpty(gearIds)) return null;
        return BindableSpecification.where((root, query, cb) -> {
            ParameterExpression<Collection> param = cb.parameter(Collection.class, GEAR_IDS_PARAM);
            return root.get(GearUseFeatures.Fields.GEAR).get(IEntity.Fields.ID).in(param);
        }).addBind(GEAR_IDS_PARAM, Arrays.asList(gearIds));
    }

    default Specification<GearUseFeatures> hasActivityCalendarId(Integer activityCalendarId) {
        return hasParentId(activityCalendarId, StringUtils.doting(GearUseFeatures.Fields.ACTIVITY_CALENDAR, IEntity.Fields.ID));
    }

    default Specification<GearUseFeatures> hasDailyActivityCalendarId(Integer activityCalendarId) {
        return hasParentId(activityCalendarId, StringUtils.doting(GearUseFeatures.Fields.DAILY_ACTIVITY_CALENDAR, IEntity.Fields.ID));
    }

    @Transactional()
    List<GearUseFeaturesVO> saveAllByActivityCalendarId(int parentId, final List<GearUseFeaturesVO> sources);

    @Transactional()
    List<GearUseFeaturesVO> saveAllByDailyActivityCalendarId(int parentId, final List<GearUseFeaturesVO> sources);
}

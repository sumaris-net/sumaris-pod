package net.sumaris.core.dao.data.activity;

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

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.*;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.activity.DailyActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.DailyActivityCalendarVO;
import net.sumaris.core.vo.filter.DailyActivityCalendarFilterVO;
import org.hibernate.jpa.QueryHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.List;

@Slf4j
public class DailyActivityCalendarRepositoryImpl
        extends RootDataRepositoryImpl<DailyActivityCalendar, DailyActivityCalendarVO, DailyActivityCalendarFilterVO, DailyActivityCalendarFetchOptions>
        implements DailyActivityCalendarSpecifications {

    private boolean enableVesselRegistrationNaturalOrder;

    @Autowired
    public DailyActivityCalendarRepositoryImpl(EntityManager entityManager,
                                               GenericConversionService conversionService) {
        super(DailyActivityCalendar.class, DailyActivityCalendarVO.class, entityManager);
        conversionService.addConverter(DailyActivityCalendar.class, DailyActivityCalendarVO.class, this::toVO);
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableVesselRegistrationNaturalOrder = configuration.enableVesselRegistrationCodeNaturalOrder();
    }

    @Override
    public Specification<DailyActivityCalendar> toSpecification(DailyActivityCalendarFilterVO filter, DailyActivityCalendarFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(id(filter.getDailyActivityCalendarId(), Integer.class))
            .and(excludedIds(filter.getExcludedIds()))
            .and(includedIds(filter.getIncludedIds()))
            .and(hasObservedLocationId(filter.getObservedLocationId()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(hasLocationId(filter.getLocationId()))
            .and(hasLocationIds(filter.getLocationIds()))
            .and(hasVesselIds(concat(filter.getVesselId(), filter.getVesselIds())))
            .and(inQualityFlagIds(filter.getQualityFlagIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            ;
    }

    @Override
    public void toVO(DailyActivityCalendar source, DailyActivityCalendarVO target, DailyActivityCalendarFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Observed location
        if (source.getObservedLocation() != null) {
            target.setObservedLocationId(source.getObservedLocation().getId());
        }
    }

    @Override
    public void toEntity(DailyActivityCalendarVO source, DailyActivityCalendar target, boolean copyIfNull) {

        super.toEntity(source, target, copyIfNull);

        // Observed location
        Integer observedLocationId = source.getObservedLocationId() != null ? source.getObservedLocationId() : (source.getObservedLocation() != null ? source.getObservedLocation().getId() : null);
        if (copyIfNull || (observedLocationId != null)) {
            if (observedLocationId == null) {
                target.setObservedLocation(null);
            } else {
                target.setObservedLocation(getReference(ObservedLocation.class, observedLocationId));
            }
        }
    }

    /* -- protected functions -- */

    @Override
    protected String toEntityProperty(@NonNull String property) {
        if (DailyActivityCalendar.Fields.VESSEL.equalsIgnoreCase(property) || property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)) {
            return StringUtils.doting(DailyActivityCalendar.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE);
        }
        if (property.endsWith(VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)) {
            return StringUtils.doting(DailyActivityCalendar.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);
        }
        if (property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
            return StringUtils.doting(DailyActivityCalendar.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.EXTERIOR_MARKING);
        }
        if (property.endsWith(VesselFeatures.Fields.NAME)) {
            return StringUtils.doting(DailyActivityCalendar.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME);
        }
        return super.toEntityProperty(property);
    }

    @Override
    protected List<Expression<?>> toSortExpressions(CriteriaQuery<?> query, Root<? extends DailyActivityCalendar> root, CriteriaBuilder cb, String property) {

        Expression<?> expression = null;

        // Add left join on vessel registration period (VRP)
        if (property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
            || property.endsWith(VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)) {

            ListJoin<Vessel, VesselRegistrationPeriod> vrp = composeVrpJoin(root, cb);
            expression = vrp.get(property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)
                ? VesselRegistrationPeriod.Fields.REGISTRATION_CODE
                : VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);
            // Natural sort
            if (enableVesselRegistrationNaturalOrder) {
                expression = Daos.naturalSort(cb, expression);
            }
        }

        // Add left join on vessel features (VF)
        if (property.endsWith(VesselFeatures.Fields.NAME)
            || property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
            ListJoin<Vessel, VesselFeatures> vf = composeVfJoin(root, cb);
            expression = vf.get(property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)
                ? VesselFeatures.Fields.EXTERIOR_MARKING
                : VesselFeatures.Fields.NAME);

            // Natural sort on exterior marking
            if (enableVesselRegistrationNaturalOrder && property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
                expression = Daos.naturalSort(cb, expression);
            }
        }

        return (expression != null) ? ImmutableList.of(expression) : super.toSortExpressions(query, root, cb, property);
    }

    @Override
    protected void configureQuery(TypedQuery<DailyActivityCalendar> query, @Nullable DailyActivityCalendarFetchOptions fetchOptions) {
        super.configureQuery(query, fetchOptions);

        if (fetchOptions == null || fetchOptions.isWithProgram()) {
            // Prepare load graph
            EntityManager em = getEntityManager();
            EntityGraph<?> entityGraph = em.getEntityGraph(DailyActivityCalendar.GRAPH_PROGRAM);
            if (fetchOptions == null || fetchOptions.isWithRecorderPerson())
                entityGraph.addSubgraph(DailyActivityCalendar.Fields.RECORDER_PERSON);
            if (fetchOptions == null || fetchOptions.isWithRecorderDepartment())
                entityGraph.addSubgraph(DailyActivityCalendar.Fields.RECORDER_DEPARTMENT);

            query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
        }
    }
}

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
import net.sumaris.core.dao.data.ImageAttachmentRepository;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.data.ActivityCalendar;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.data.VesselFeatures;
import net.sumaris.core.model.data.VesselRegistrationPeriod;
import net.sumaris.core.model.referential.ObjectTypeEnum;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.data.ImageAttachmentFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.data.activity.ActivityCalendarFetchOptions;
import net.sumaris.core.vo.data.activity.ActivityCalendarVO;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.core.vo.filter.ImageAttachmentFilterVO;
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
public class ActivityCalendarRepositoryImpl
        extends RootDataRepositoryImpl<ActivityCalendar, ActivityCalendarVO, ActivityCalendarFilterVO, ActivityCalendarFetchOptions>
        implements ActivityCalendarSpecifications {

    @Autowired
    private ImageAttachmentRepository imageAttachmentRepository;

    private boolean enableVesselRegistrationNaturalOrder;

    private boolean enableImageAttachments;

    @Autowired
    public ActivityCalendarRepositoryImpl(EntityManager entityManager,
                                          GenericConversionService conversionService) {
        super(ActivityCalendar.class, ActivityCalendarVO.class, entityManager);
        conversionService.addConverter(ActivityCalendar.class, ActivityCalendarVO.class, this::toVO);
    }

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.enableVesselRegistrationNaturalOrder = configuration.enableVesselRegistrationCodeNaturalOrder();
        this.enableImageAttachments = configuration.enableDataImages();
    }

    @Override
    public Specification<ActivityCalendar> toSpecification(ActivityCalendarFilterVO filter, ActivityCalendarFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(id(filter.getActivityCalendarId(), Integer.class))
            .and(excludedIds(filter.getExcludedIds()))
            .and(includedIds(filter.getIncludedIds()))
            .and(atYear(filter.getYear()))
            .and(betweenDate(filter.getStartDate(), filter.getEndDate()))
            .and(hasVesselTypeIds(concat(filter.getVesselTypeId(), filter.getVesselTypeIds())))
            .and(hasVesselIds(concat(filter.getVesselId(), filter.getVesselIds())))
            .and(hasRegistrationLocationIds(concat(filter.getRegistrationLocationId(), filter.getRegistrationLocationIds())))
            .and(hasBasePortLocationIds(filter.getBasePortLocationIds()))
            .and(inQualityFlagIds(filter.getQualityFlagIds()))
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            .and(hasDirectSurveyInvestigation(filter.getDirectSurveyInvestigation()))
            .and(hasEconomicSurvey(filter.getEconomicSurvey()))
            .and(hasObserverPersonIds(filter.getObserverPersonIds()))
            ;
    }

    @Override
    public void toVO(ActivityCalendar source, ActivityCalendarVO target, ActivityCalendarFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        Integer activityCalendarId = source.getId();

        // Fetch images
        if (this.enableImageAttachments && fetchOptions != null && fetchOptions.isWithImages() && activityCalendarId != null) {
            List<ImageAttachmentVO> images = imageAttachmentRepository.findAll(ImageAttachmentFilterVO.builder()
                    .objectId(activityCalendarId)
                    .objectTypeId(ObjectTypeEnum.ACTIVITY_CALENDAR.getId())
                    .build(), ImageAttachmentFetchOptions.MINIMAL);
            target.setImages(images);
        }


    }

    @Override
    public void toEntity(ActivityCalendarVO source, ActivityCalendar target, boolean copyIfNull) {

        super.toEntity(source, target, copyIfNull);

    }

    /* -- protected functions -- */

    @Override
    protected String toEntityProperty(@NonNull String property) {
        if (ActivityCalendar.Fields.VESSEL.equalsIgnoreCase(property) || property.endsWith(VesselRegistrationPeriod.Fields.REGISTRATION_CODE)) {
            return StringUtils.doting(ActivityCalendar.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.REGISTRATION_CODE);
        }
        if (property.endsWith(VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE)) {
            return StringUtils.doting(ActivityCalendar.Fields.VESSEL, Vessel.Fields.VESSEL_REGISTRATION_PERIODS, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE);
        }
        if (property.endsWith(VesselFeatures.Fields.EXTERIOR_MARKING)) {
            return StringUtils.doting(ActivityCalendar.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.EXTERIOR_MARKING);
        }
        if (property.endsWith(VesselFeatures.Fields.NAME)) {
            return StringUtils.doting(ActivityCalendar.Fields.VESSEL, Vessel.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME);
        }
        return super.toEntityProperty(property);
    }

    @Override
    protected List<Expression<?>> toSortExpressions(CriteriaQuery<?> query, Root<ActivityCalendar> root, CriteriaBuilder cb, String property) {

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
    protected void configureQuery(TypedQuery<ActivityCalendar> query, @Nullable ActivityCalendarFetchOptions fetchOptions) {
        super.configureQuery(query, fetchOptions);

        if (fetchOptions == null || fetchOptions.isWithProgram()) {
            // Prepare load graph
            EntityManager em = getEntityManager();
            EntityGraph<?> entityGraph = em.getEntityGraph(ActivityCalendar.GRAPH_PROGRAM);
            if (fetchOptions == null || fetchOptions.isWithRecorderPerson())
                entityGraph.addSubgraph(ActivityCalendar.Fields.RECORDER_PERSON);
            if (fetchOptions == null || fetchOptions.isWithRecorderDepartment())
                entityGraph.addSubgraph(ActivityCalendar.Fields.RECORDER_DEPARTMENT);

            query.setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
        }
    }
}

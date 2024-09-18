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

package net.sumaris.server.http.graphql.data;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.service.data.vessel.VesselService;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.util.ArrayUtils;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.data.vessel.VesselOwnerVO;
import net.sumaris.core.vo.filter.IRootDataFilter;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLHelper;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.administration.DataAccessControlService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@GraphQLApi
@Transactional
@RequiredArgsConstructor
public class VesselGraphQLService {
    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(VesselGraphQLService.class);

    private final SumarisServerConfiguration configuration;

    private final VesselService vesselService;

    private final VesselSnapshotService vesselSnapshotService;

    private final AuthService authService;

    private Integer[] vesselTypeIds;

    @PostConstruct
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        this.vesselTypeIds = configuration.getDataVesselTypeIds().toArray(Integer[]::new);
    }

    /* -- Vessel -- */

    @GraphQLQuery(name = "vessels", description = "Search in vessels")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselVO> findAllVessels(@GraphQLArgument(name = "filter") VesselFilterVO filter,
                                         @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                         @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                         @GraphQLArgument(name = "sortBy") String sort,
                                         @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                         @GraphQLEnvironment ResolutionEnvironment env) {

        // Map sortAttribute into model property
        if (StringUtils.isNotBlank(sort)) {
            sort = sort.replaceFirst(VesselVO.Fields.VESSEL_REGISTRATION_PERIOD + "\\.", Vessel.Fields.VESSEL_REGISTRATION_PERIODS + ".");
            sort = sort.replaceFirst(VesselVO.Fields.STATUS_ID, StringUtils.doting(Vessel.Fields.STATUS, Status.Fields.ID));
        }

        filter = fillVesselFilterDefaults(filter, true);

        return vesselService.findAll(filter,
                Page.builder()
                    .offset(offset)
                    .size(size)
                    .sortBy(sort)
                    .sortDirection(SortDirection.fromString(direction))
                    .build(),
            getVesselFetchOptions(GraphQLUtils.fields(env))
        );
    }

    @GraphQLQuery(name = "vesselSnapshots", description = "Search in vessel snapshots")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselSnapshotVO> findAllVesselSnapshots(@GraphQLArgument(name = "filter") VesselFilterVO filter,
                                                         @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                         @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                         @GraphQLArgument(name = "sortBy", defaultValue = VesselSnapshotVO.Fields.EXTERIOR_MARKING) String sort,
                                                         @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                                         @GraphQLEnvironment ResolutionEnvironment env
    ) {
        // Add restriction to filter (e.g. program=SIH)
        // and fill (or fix) dates
        filter = fillVesselFilterDefaults(filter, true);

        // Compute fetch options
        VesselFetchOptions fetchOptions = getSnapshotFetchOptions(GraphQLUtils.fields(env));

        return vesselSnapshotService.findAll(filter, offset, size, sort, SortDirection.fromString(direction), fetchOptions);
    }

    @GraphQLQuery(name = "vesselSnapshotsCount", description = "Get total vessel snapshots count")
    @Transactional(readOnly = true)
    @IsUser
    public Long countVesselSnapshots(@GraphQLArgument(name = "filter") VesselFilterVO filter) {
        // Add restriction to filter (e.g. program=SIH)
        // and fill (or fix) dates
        filter = fillVesselFilterDefaults(filter, true);

        return vesselSnapshotService.countByFilter(filter);
    }

    @GraphQLQuery(name = "vesselsCount", description = "Get total vessels count")
    @Transactional(readOnly = true)
    @IsUser
    public long countVessels(@GraphQLArgument(name = "filter") VesselFilterVO filter) {
        // Add restriction to filter (e.g. program=SIH)
        // and fill (or fix) dates
        filter = fillVesselFilterDefaults(filter, true);

        return vesselService.countByFilter(filter);
    }

    @GraphQLQuery(name = "vessel", description = "Get a vessel")
    @Transactional(readOnly = true)
    @IsUser
    public VesselVO getVesselById(@GraphQLArgument(name = "id") Integer id,
                                  @GraphQLArgument(name = "vesselId", description = "@deprecated Use 'id'") Integer vesselId // /!\ Deprecated !
    ) {
        if (id == null && vesselId != null) {
            id = vesselId;
            GraphQLHelper.logDeprecatedUse(authService,"vessel(vesselId)", "1.11.0");
        }
        return vesselService.get(id);
    }

    @GraphQLQuery(name = "vesselFeaturesHistory", description = "Get vessel features history")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselFeaturesVO> getVesselFeatures(
        @GraphQLArgument(name = "vesselId") Integer vesselId,
        @GraphQLArgument(name = "filter") VesselFilterVO filter,
        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
        @GraphQLArgument(name = "sortBy", defaultValue = VesselFeaturesVO.Fields.START_DATE) String sort,
        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
        @GraphQLEnvironment ResolutionEnvironment env) {

        filter = fillVesselFilterDefaults(filter, false);

        if (filter.getVesselId() == null) {
            filter.setVesselId(vesselId);
        }

        return vesselService.findFeaturesByFilter(filter,
            Page.create(offset, size, sort, SortDirection.fromString(direction)),
            getFeaturesFetchOptions(GraphQLUtils.fields(env)));
    }

    @GraphQLQuery(name = "vesselFeaturesHistoryCount", description = "Get vessel features history count")
    @Transactional(readOnly = true)
    @IsUser
    public Long countVesselFeatures(@GraphQLArgument(name = "vesselId") Integer vesselId,
                                        @GraphQLArgument(name = "filter") VesselFilterVO filter) {
        filter = fillVesselFilterDefaults(filter, false);

        if (filter.getVesselId() == null) {
            filter.setVesselId(vesselId);
        }

        return vesselService.countFeaturesByFilter(filter);
    }


    @GraphQLQuery(name = "vesselFeaturesHistoryCount", description = "Get vessel features history count")
    @Transactional(readOnly = true)
    @IsUser
    public Long countFeaturesByVesselId(@GraphQLArgument(name = "vesselId") Integer vesselId,
                                                   @GraphQLArgument(name = "filter") VesselFilterVO filter) {
        vesselId = vesselId != null ? vesselId : (filter != null ? filter.getVesselId() : null);
        Preconditions.checkNotNull(vesselId);
        return vesselService.countFeaturesByVesselId(vesselId);
    }

    @GraphQLQuery(name = "vesselRegistrationHistory", description = "Get vessel registration history")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselRegistrationPeriodVO> getRegistrationPeriods(@GraphQLArgument(name = "vesselId") Integer vesselId,
                                                                 @GraphQLArgument(name = "filter") VesselFilterVO filter,
                                                                 @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                                 @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                                 @GraphQLArgument(name = "sortBy", defaultValue = VesselRegistrationPeriodVO.Fields.START_DATE) String sort,
                                                                 @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        filter = fillVesselFilterDefaults(filter, false);
        if (filter.getVesselId() == null) {
            filter.setVesselId(vesselId);
        }

        return vesselService.findRegistrationPeriodsByFilter(filter, Page.create(offset, size, sort, SortDirection.fromString(direction)));
    }

    @GraphQLQuery(name = "vesselRegistrationHistoryCount", description = "Get vessel registration history count")
    @Transactional(readOnly = true)
    @IsUser
    public Long countVesselRegistrationPeriods(@GraphQLArgument(name = "vesselId") Integer vesselId,
                                               @GraphQLArgument(name = "filter") VesselFilterVO filter) {
        filter = fillVesselFilterDefaults(filter, false);

        if (filter.getVesselId() == null) {
            filter.setVesselId(vesselId);
        }

        return vesselService.countRegistrationPeriodsByFilter(filter);
    }


    @GraphQLQuery(name = "vesselOwnerHistory", description = "Get vessel owner history")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselOwnerPeriodVO> getVesselOwnerPeriods(
            @GraphQLArgument(name = "vesselId") Integer vesselId,
            @GraphQLArgument(name = "filter") VesselFilterVO filter,
            @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
            @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
            @GraphQLArgument(name = "sortBy", defaultValue = VesselOwnerPeriodVO.Fields.START_DATE) String sort,
            @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
            @GraphQLEnvironment ResolutionEnvironment env) {
        filter = fillVesselFilterDefaults(filter, false);

        if (filter.getVesselId() == null) {
            filter.setVesselId(vesselId);
        }

        return vesselService.findOwnerPeriodsByFilter(filter,
                Page.create(offset, size, sort, SortDirection.fromString(direction)));
    }

    @GraphQLQuery(name = "vesselOwnerHistoryCount", description = "Get total vessel owner history count")
    @Transactional(readOnly = true)
    @IsUser
    public Long countVesselOwnerPeriods(@GraphQLArgument(name = "vesselId") Integer vesselId,
                                        @GraphQLArgument(name = "filter") VesselFilterVO filter) {
        filter = fillVesselFilterDefaults(filter, false);

        if (filter.getVesselId() == null) {
            filter.setVesselId(vesselId);
        }

        return vesselService.countOwnerPeriodsByFilter(filter);
    }

    @GraphQLQuery(name = "vesselOwner", description = "Get a vesselOwner")
    @Transactional(readOnly = true)
    @IsUser
    public VesselOwnerVO getVesselOwnerById(@GraphQLArgument(name = "id") Integer id) {
        return vesselService.getVesselOwner(id);
    }

    @GraphQLMutation(name = "saveVessel", description = "Create or update a vessel")
    @IsUser
    public VesselVO saveVessel(@GraphQLNonNull @GraphQLArgument(name = "vessel") VesselVO vessel) {
        return vesselService.save(vessel);
    }

    @GraphQLMutation(name = "saveVessels", description = "Create or update many vessels")
    @IsUser
    public List<VesselVO> saveVessels(@GraphQLNonNull @GraphQLArgument(name = "vessels") List<VesselVO> vessels) {
        return vesselService.save(vessels);
    }

    @GraphQLMutation(name = "deleteVessel", description = "Delete a vessel (by vessel features id)")
    @IsUser
    public void deleteVessel(@GraphQLNonNull @GraphQLArgument(name = "id") int id) {
        vesselService.delete(id);
    }

    @GraphQLMutation(name = "deleteVessels", description = "Delete many vessels (by vessel features ids)")
    @IsUser
    public void deleteVessels(@GraphQLNonNull @GraphQLArgument(name = "ids") List<Integer> ids) {
        vesselService.delete(ids);
    }

    @GraphQLMutation(name = "replaceVessels", description = "Replace temporary vessels")
    @IsUser
    public void replaceVessel(
        @GraphQLNonNull @GraphQLArgument(name = "temporaryVesselIds") List<Integer> temporaryVesselIds,
        @GraphQLNonNull @GraphQLArgument(name = "validVesselId") int validVesselId
        ) {
        vesselService.replaceTemporaryVessel(temporaryVesselIds, validVesselId);
    }

    public <T extends IWithVesselSnapshotEntity<?, VesselSnapshotVO>> void fillVesselSnapshot(T bean, Set<String> fields) {
        // Fetch (if need)
        Integer vesselId = bean.getVesselId() != null ? bean.getVesselId() : (bean.getVesselSnapshot() != null ? bean.getVesselSnapshot().getVesselId() : null);
        if (vesselId != null && hasVesselFeaturesField(fields) && VesselSnapshotVO.isEmpty(bean.getVesselSnapshot(), VesselSnapshotVO.Fields.VESSEL_ID)) {
            bean.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(vesselId, Dates.resetTime(bean.getVesselDateTime())));
        }
    }

    public <T extends IWithVesselSnapshotEntity<?, VesselSnapshotVO>> void fillVesselSnapshot(List<T> beans, Set<String> fields) {
        // Add vessel if need
        if (hasVesselFeaturesField(fields)) {
            beans.parallelStream().forEach(bean -> {
                Integer vesselId = bean.getVesselId() != null ? bean.getVesselId() : (bean.getVesselSnapshot() != null ? bean.getVesselSnapshot().getVesselId() : null);
                if (vesselId != null && VesselSnapshotVO.isEmpty(bean.getVesselSnapshot(), VesselSnapshotVO.Fields.VESSEL_ID)) {
                    bean.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(vesselId, Dates.resetTime(bean.getVesselDateTime())));
                }
            });
        }
    }

    /* -- protected methods -- */

    protected boolean hasVesselFeaturesField(Set<String> fields) {
        return fields.contains(StringUtils.slashing(TripVO.Fields.VESSEL_SNAPSHOT, VesselSnapshotVO.Fields.EXTERIOR_MARKING))
                || fields.contains(StringUtils.slashing(TripVO.Fields.VESSEL_SNAPSHOT, VesselSnapshotVO.Fields.NAME));
    }

    protected VesselFetchOptions getVesselFetchOptions(Set<String> fields) {
        return VesselFetchOptions.builder()
            .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
            .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
            .withVesselFeatures(
                fields.contains(StringUtils.slashing(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.NAME))
                || fields.contains(StringUtils.slashing(VesselVO.Fields.VESSEL_FEATURES, VesselFeatures.Fields.EXTERIOR_MARKING))
            )
            .withVesselRegistrationPeriod(
                fields.contains(StringUtils.slashing(VesselVO.Fields.VESSEL_REGISTRATION_PERIOD, VesselRegistrationPeriod.Fields.REGISTRATION_CODE))
                || fields.contains(StringUtils.slashing(VesselVO.Fields.VESSEL_REGISTRATION_PERIOD, VesselRegistrationPeriod.Fields.INT_REGISTRATION_CODE))
            )
            .build();
    }


    protected VesselFetchOptions getSnapshotFetchOptions(Set<String> fields) {
        return VesselFetchOptions.builder()
            .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
            .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
            .withVesselFeatures(
                fields.contains(VesselSnapshotVO.Fields.NAME)
                    || fields.contains(VesselSnapshotVO.Fields.EXTERIOR_MARKING))
            .withVesselRegistrationPeriod(
                fields.contains(VesselSnapshotVO.Fields.REGISTRATION_CODE)
                || fields.contains(VesselSnapshotVO.Fields.INT_REGISTRATION_CODE))
            .withBasePortLocation(
                fields.contains(StringUtils.slashing(VesselSnapshotVO.Fields.BASE_PORT_LOCATION, ReferentialVO.Fields.LABEL))
            )
            .build();
    }

    protected DataFetchOptions getFeaturesFetchOptions(Set<String> fields) {
        return DataFetchOptions.builder()
            .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
            .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
            .build();
    }

    /**
     * If need, restrict vessel program (to SIH), and dates (to today)
     */
    protected VesselFilterVO fillVesselFilterDefaults(VesselFilterVO filter, boolean forceDate) {
        filter = VesselFilterVO.nullToEmpty(filter);

        boolean isAdmin = authService.isAdmin();

        // Filter on SIH program, when empty or not an admin
        // (an admin can access to any other programs)
        if (StringUtils.isBlank(filter.getProgramLabel()) || !isAdmin) {
            filter.setProgramLabel(ProgramEnum.SIH.getLabel());
        }

        // Filter on vesselTypeIds (if configured)
        if (ArrayUtils.isNotEmpty(vesselTypeIds)) {
            Integer[] userVesselTypesIds = ArrayUtils.concat(filter.getVesselTypeId(), filter.getVesselTypeIds());

            // Limit if empty or not an admin
            // (an admin can access to any other vessel types)
            if (ArrayUtils.isEmpty(userVesselTypesIds) || !isAdmin) {
                Integer[] intersection = ArrayUtils.intersectionSkipEmpty(vesselTypeIds, userVesselTypesIds);
                if (intersection != null && ArrayUtils.isEmpty(intersection)) {
                    filter.setVesselTypeIds(DataAccessControlService.NO_ACCESS_FAKE_IDS);
                }
                else {
                    filter.setVesselTypeIds(intersection);
                }
                filter.setVesselTypeId(null);
            }
        }

        // If expected a date: use today (at 0h0min)
        if (filter.getStartDate() == null && filter.getEndDate() == null) {
            if (forceDate) {
                filter.setDate(Dates.resetTime(new Date()));
            }
        }

        // Reset hour in date (0h0min - to limit cache key changes)
        else {
            if (filter.getStartDate() != null) {
                filter.setStartDate(Dates.resetTime(filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                filter.setEndDate(Dates.resetTime(filter.getEndDate()));
            }
        }

        return filter;
    }

    /**
     * Restrict to self data and/or department data
     * @param filter
     */
    protected <F extends IRootDataFilter> F fillRootDataFilter(F filter, Class<F> filterClass) {
        try {
            filter = filter != null ? filter : filterClass.getConstructor().newInstance();
        }
        catch (Exception e) {
            log.error("Cannot create filter instance: {}", e.getMessage(), e);
        }

        // Restrict to self data and/or department data
        PersonVO user = authService.getAuthenticatedUser().orElse(null);
        if (user != null) {
            if (!canUserAccessNotSelfData()) {
                // Limit data access to self data
                filter.setRecorderPersonId(user.getId());
            }
            else {
                Integer depId = user.getDepartment().getId();
                if (!canDepartmentAccessNotSelfData(depId)) {
                    // Limit data access to user's department
                    filter.setRecorderDepartmentId(depId);
                }
            }
        } else {
            filter.setRecorderPersonId(-999); // Hide all. Should never occur
        }

        return filter;
    }

    protected boolean canUserAccessNotSelfData() {
        String minRole = configuration.getAccessNotSelfDataMinRole();
        return StringUtils.isBlank(minRole) || authService.hasAuthority(minRole);
    }

    protected boolean canDepartmentAccessNotSelfData(@NonNull Integer actualDepartmentId) {
        List<Integer> expectedDepartmentIds = configuration.getAccessNotSelfDataDepartmentIds();
        return CollectionUtils.isEmpty(expectedDepartmentIds) || expectedDepartmentIds.contains(actualDepartmentId);
    }

}

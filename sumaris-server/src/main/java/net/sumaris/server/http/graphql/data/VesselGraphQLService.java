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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.service.data.vessel.VesselService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.vessel.VesselFetchOptions;
import net.sumaris.core.vo.filter.IRootDataFilter;
import net.sumaris.core.vo.filter.VesselFeaturesFilterVO;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.core.vo.filter.VesselRegistrationFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.graphql.GraphQLHelper;
import net.sumaris.server.http.graphql.GraphQLUtils;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsUser;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@GraphQLApi
@Transactional
public class VesselGraphQLService {
    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(VesselGraphQLService.class);

    @Autowired
    private SumarisServerConfiguration configuration;

    @Autowired
    private VesselService vesselService;

    @Autowired
    private AuthService authService;

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

        filter = fillVesselFilterDefaults(filter);

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
        filter = fillVesselFilterDefaults(filter);

        // Compute fetch options
        VesselFetchOptions fetchOptions = getSnapshotFetchOptions(GraphQLUtils.fields(env));

        return vesselService.findAllSnapshots(
                filter,
                Page.builder()
                    .offset(offset)
                    .size(size)
                    .sortBy(sort)
                    .sortDirection(SortDirection.fromString(direction))
                    .build(),
                fetchOptions
            );
    }

    @GraphQLQuery(name = "vesselSnapshotsCount", description = "Get total vessel snapshots count")
    @Transactional(readOnly = true)
    @IsUser
    public long countVesselSnapshots(@GraphQLArgument(name = "filter") VesselFilterVO filter) {
        // Add restriction to filter (e.g. program=SIH)
        // and fill (or fix) dates
        filter = fillVesselFilterDefaults(filter);

        return vesselService.countSnapshotsByFilter(filter);
    }

    @GraphQLQuery(name = "vesselsCount", description = "Get total vessels count")
    @Transactional(readOnly = true)
    @IsUser
    public long countVessels(@GraphQLArgument(name = "filter") VesselFilterVO filter) {
        // Add restriction to filter (e.g. program=SIH)
        // and fill (or fix) dates
        filter = fillVesselFilterDefaults(filter);

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
    public List<VesselFeaturesVO> getFeaturesByVesselId(
        @GraphQLArgument(name = "vesselId") Integer vesselId,
        @GraphQLArgument(name = "filter") VesselFeaturesFilterVO filter,
        @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
        @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
        @GraphQLArgument(name = "sortBy", defaultValue = VesselFeaturesVO.Fields.START_DATE) String sort,
        @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
        @GraphQLEnvironment ResolutionEnvironment env) {

        vesselId = vesselId != null ? vesselId : (filter != null ? filter.getVesselId() : null);
        Preconditions.checkNotNull(vesselId);
        return vesselService.getFeaturesByVesselId(vesselId,
            Pageables.create(offset, size, sort, SortDirection.fromString(direction)),
            //offset, size, sort, SortDirection.fromString(direction),
            getFeaturesFetchOptions(GraphQLUtils.fields(env)))
            .getContent();
    }

    @GraphQLQuery(name = "vesselRegistrationHistory", description = "Get vessel registration history")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselRegistrationPeriodVO> getRegistrationPeriodsByVesselId(@GraphQLArgument(name = "vesselId") Integer vesselId,
                                                                         @GraphQLArgument(name = "filter") VesselRegistrationFilterVO filter,
                                                                         @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                                         @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                                         @GraphQLArgument(name = "sortBy", defaultValue = VesselRegistrationPeriodVO.Fields.START_DATE) String sort,
                                                                         @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        vesselId = vesselId != null ? vesselId : (filter != null ? filter.getVesselId() : null);
        Preconditions.checkNotNull(vesselId);
        return vesselService.getRegistrationPeriodsByVesselId(vesselId,
            Pageables.create(offset, size, sort, SortDirection.fromString(direction))
            //offset, size, sort, SortDirection.fromString(direction)
            )
            .getContent();
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

    public <T extends IWithVesselSnapshotEntity<?, VesselSnapshotVO>> void fillVesselSnapshot(T bean, Set<String> fields) {
        // Add vessel if need
        VesselSnapshotVO result = bean.getVesselSnapshot();

        // No ID: cannot fetch
        if (result == null || result.getId() == null) return;

        // Fetch (if need)
        if (result.getName() == null && hasVesselFeaturesField(fields)) {
            result = vesselService.getSnapshotByIdAndDate(bean.getVesselSnapshot().getId(), Dates.resetTime(bean.getVesselDateTime()));
            bean.setVesselSnapshot(result);
        }
    }

    public <T extends IWithVesselSnapshotEntity<?, VesselSnapshotVO>> void fillVesselSnapshot(List<T> beans, Set<String> fields) {
        // Add vessel if need
        if (hasVesselFeaturesField(fields)) {
            beans.forEach(bean -> {
                if (bean.getVesselSnapshot() != null && bean.getVesselSnapshot().getId() != null && bean.getVesselSnapshot().getName() == null) {
                    bean.setVesselSnapshot(vesselService.getSnapshotByIdAndDate(bean.getVesselSnapshot().getId(), Dates.resetTime(bean.getVesselDateTime())));
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
     * @param filter
     */
    protected VesselFilterVO fillVesselFilterDefaults(VesselFilterVO filter) {
        filter = VesselFilterVO.nullToEmpty(filter);

        // Filter on SIH program, when empty or not an admin
        if (StringUtils.isBlank(filter.getProgramLabel()) || !authService.isAdmin()) {
            filter.setProgramLabel(ProgramEnum.SIH.getLabel());
        }

        // If expected a date: use today (at 0h0min)
        if (filter.getStartDate() == null && filter.getEndDate() == null) {
            filter.setDate(Dates.resetTime(new Date()));
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
            filter = filter != null ? filter : filterClass.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
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

package net.sumaris.server.http.graphql.data;

/*-
 * #%L
 * SUMARiS:: Server
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

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.*;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.NonNull;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.model.data.*;
import net.sumaris.core.service.data.*;
import net.sumaris.core.service.data.vessel.VesselService2;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.*;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.http.GraphQLUtils;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsUser;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class VesselGraphQLService {
    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(VesselGraphQLService.class);

    @Autowired
    private SumarisServerConfiguration config;

    @Autowired
    private VesselService vesselService;

    @Autowired
    private VesselService2 vesselService2;

    @Autowired
    private AuthService authService;

    /* -- Vessel -- */

    @GraphQLQuery(name = "vesselSnapshots", description = "Search in vessel snapshots")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselSnapshotVO> findVesselSnapshotsByFilter(@GraphQLArgument(name = "filter") VesselFilterVO filter,
                                                              @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                              @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                              @GraphQLArgument(name = "sortBy", defaultValue = VesselSnapshotVO.Fields.EXTERIOR_MARKING) String sort,
                                                              @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction
    ) {
        return vesselService.findSnapshotByFilter(
            restrictVesselFilter(filter),
            offset, size, sort,
            SortDirection.fromString(direction));
    }

    
    @GraphQLQuery(name = "vessels", description = "Search in vessels")
    //@Transactional(readOnly = true)
    //@IsUser
    public List<VesselVO> findVesselByFilter(@GraphQLArgument(name = "filter") VesselFilterVO filter,
                                             @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                             @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                             @GraphQLArgument(name = "sortBy") String sort,
                                             @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction,
                                             @GraphQLEnvironment ResolutionEnvironment env
    ) {
        return vesselService.findVesselsByFilter(
            filter,
            offset, size, sort,
            SortDirection.fromString(direction));/*
        return vesselService2.findAll(
            restrictVesselFilter(filter),
            Page.builder()
                .offset(offset)
                .size(size)
                .sortBy(sort)
                .sortDirection(SortDirection.fromString(direction))
                .build(),
            getFetchOptions(GraphQLUtils.fields(env))
        );*/
    }

    @GraphQLQuery(name = "vesselsCount2", description = "Get total vessels count")
    public long countVessels2() {
        return 2l;
    }

    @GraphQLQuery(name = "vesselsCount", description = "Get total vessels count")
    @Transactional(readOnly = true)
    @IsUser
    public long countVessels(@GraphQLArgument(name = "filter") VesselFilterVO filter) {
        return vesselService2.countByFilter(restrictVesselFilter(filter));
    }

    @GraphQLQuery(name = "vessel", description = "Get a vessel")
    @Transactional(readOnly = true)
    @IsUser
    public VesselVO getVesselById(@GraphQLArgument(name = "id") Integer id,
                                  @GraphQLArgument(name = "vesselId") Integer vesselId // /!\ Deprecated !
    ) {
        if (id == null && vesselId != null) {
            id = vesselId;
            logDeprecatedUse("vessel(vesselId)", "1.11.0");
        }
        return vesselService2.get(id);
    }

    @GraphQLQuery(name = "vesselFeaturesHistory", description = "Get vessel features history")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselFeaturesVO> getVesselFeaturesHistory(
        @GraphQLArgument(name = "vesselId") Integer vesselId,
        @GraphQLArgument(name = "filter") VesselFeaturesFilterVO filter,
                                                           @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                           @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                           @GraphQLArgument(name = "sortBy", defaultValue = VesselFeaturesVO.Fields.START_DATE) String sort,
                                                           @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        vesselId = vesselId != null ? vesselId : (filter != null ? filter.getVesselId() : null);
        Preconditions.checkNotNull(vesselId);
        return vesselService.getFeaturesByVesselId(vesselId, offset, size, sort, SortDirection.fromString(direction));
    }

    @GraphQLQuery(name = "vesselRegistrationHistory", description = "Get vessel registration history")
    @Transactional(readOnly = true)
    @IsUser
    public List<VesselRegistrationVO> getVesselRegistrationHistory(@GraphQLArgument(name = "vesselId") Integer vesselId,
                                                                   @GraphQLArgument(name = "filter") VesselRegistrationFilterVO filter,
                                                                   @GraphQLArgument(name = "offset", defaultValue = "0") Integer offset,
                                                                   @GraphQLArgument(name = "size", defaultValue = "1000") Integer size,
                                                                   @GraphQLArgument(name = "sortBy", defaultValue = VesselRegistrationVO.Fields.START_DATE) String sort,
                                                                   @GraphQLArgument(name = "sortDirection", defaultValue = "asc") String direction) {
        vesselId = vesselId != null ? vesselId : (filter != null ? filter.getVesselId() : null);
        Preconditions.checkNotNull(vesselId);
        return vesselService.getRegistrationsByVesselId(vesselId, offset, size, sort, SortDirection.fromString(direction));
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
                    bean.setVesselSnapshot(vesselService.getSnapshotByIdAndDate(bean.getVesselSnapshot().getId(), bean.getVesselDateTime()));
                }
            });
        }
    }

    /* -- protected methods -- */

    protected boolean hasVesselFeaturesField(Set<String> fields) {
        return fields.contains(StringUtils.slashing(TripVO.Fields.VESSEL_SNAPSHOT, VesselSnapshotVO.Fields.EXTERIOR_MARKING))
                || fields.contains(StringUtils.slashing(TripVO.Fields.VESSEL_SNAPSHOT, VesselSnapshotVO.Fields.NAME));
    }

    protected DataFetchOptions getFetchOptions(Set<String> fields) {
        return DataFetchOptions.builder()
                .withRecorderDepartment(fields.contains(StringUtils.slashing(IWithRecorderDepartmentEntity.Fields.RECORDER_DEPARTMENT, IEntity.Fields.ID)))
                .withRecorderPerson(fields.contains(StringUtils.slashing(IWithRecorderPersonEntity.Fields.RECORDER_PERSON, IEntity.Fields.ID)))
                .build();
    }

    /**
     * Restrict to vessel, depending of user access rights
     * @param filter
     */
    protected VesselFilterVO restrictVesselFilter(VesselFilterVO filter) {
        // Filter on SIH program, when not an admin
        if (!authService.isAdmin()) {
            filter = VesselFilterVO.nullToEmpty(filter);
            filter.setProgramLabel(ProgramEnum.SIH.getLabel());
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
        String minRole = config.getAccessNotSelfDataMinRole();
        return StringUtils.isBlank(minRole) || authService.hasAuthority(minRole);
    }

    protected boolean canDepartmentAccessNotSelfData(@NonNull Integer actualDepartmentId) {
        List<Integer> expectedDepartmentIds = config.getAccessNotSelfDataDepartmentIds();
        return CollectionUtils.isEmpty(expectedDepartmentIds) || expectedDepartmentIds.contains(actualDepartmentId);
    }

    /**
     * Check user is admin
     */
    protected void checkIsAdmin(String message) {
        if (!authService.isAdmin()) throw new AccessDeniedException(message != null ? message : "Forbidden");
    }

    protected void logDeprecatedUse(String functionName, String appVersion) {
        Integer userId = authService.getAuthenticatedUser().map(PersonVO::getId).orElse(null);
        log.warn("User {id: {}} used service {{}} that is deprecated since {appVersion: {}}.", userId, functionName, appVersion);

    }
}

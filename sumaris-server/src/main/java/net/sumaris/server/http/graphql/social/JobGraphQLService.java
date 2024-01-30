package net.sumaris.server.http.graphql.social;

/*-
 * #%L
 * Quadrige3 Core :: Server
 * %%
 * Copyright (C) 2017 - 2022 Ifremer
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

import io.leangen.graphql.annotations.*;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.event.job.JobProgressionVO;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.referential.ProcessingTypeEnum;
import net.sumaris.core.model.social.SystemRecipientEnum;
import net.sumaris.core.model.social.UserEvent;
import net.sumaris.core.model.technical.history.ProcessingHistory;
import net.sumaris.core.model.technical.job.JobTypeEnum;
import net.sumaris.core.service.data.vessel.VesselSnapshotJob;
import net.sumaris.core.service.referential.LocationHierarchyJob;
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.service.technical.JobService;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.social.UserEventVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.security.ISecurityContext;
import net.sumaris.server.service.technical.EntityWatchService;
import org.apache.commons.collections4.MapUtils;
import org.nuiton.i18n.I18n;
import org.reactivestreams.Publisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@GraphQLApi
@IsUser
@ConditionalOnBean({JobExecutionService.class, JobService.class})
@RequiredArgsConstructor
@Slf4j
public class JobGraphQLService {
    final static int DEFAULT_PAGE_SIZE = 100;
    final static int MAX_PAGE_SIZE = 1000;
    private final JobService jobService;
    private final JobExecutionService jobExecutionService;
    private final ISecurityContext<PersonVO> securityContext;
    private final EntityWatchService entityWatchService;

    private final AuthService authService;

    private final VesselSnapshotJob vesselSnapshotJob;

    private final LocationHierarchyJob locationHierarchyJob;

    @GraphQLQuery(name = "jobs", description = "Search in jobs")
    @IsUser
    public List<JobVO> findAll(@GraphQLArgument(name = "filter") JobFilterVO filter,
                               @GraphQLArgument(name = "page") Page page) {
        // Default page size
        if (page == null) {
            page = Page.builder()
                .offset(0)
                .size(DEFAULT_PAGE_SIZE)
                .sortBy(UserEventVO.Fields.UPDATE_DATE)
                .sortDirection(SortDirection.DESC)
                .build();
        }

        // Limit to 1000 items
        if (page.getSize() > MAX_PAGE_SIZE) {
            page.setSize(MAX_PAGE_SIZE);
        }

        return jobService.findAll(filter, page);
    }

    @GraphQLQuery(name = "job", description = "Get a job")
    @IsUser
    public JobVO getJob(@GraphQLArgument(name = "id") int id) {
        return jobService.findById(id).orElse(null);
    }

    @GraphQLSubscription(name = "updateJobs", description = "Subscribe to changes on jobs")
    @IsUser
    public Publisher<List<JobVO>> updateJobs(
        @GraphQLArgument(name = "filter") JobFilterVO filter,
        @GraphQLArgument(name = "interval") Integer interval
    ) {

        filter = JobFilterVO.nullToDefault(filter);

        // Is user is NOT an admin, or if no issuer: force issuer to the authenticated user
        if (!securityContext.isAdmin() || filter.getIssuer() == null) {
            PersonVO user = securityContext.getAuthenticatedUser()
                .orElseThrow(() -> new AccessDeniedException("Forbidden"));
            filter.setIssuer(user.getPubkey());
        }

        log.info("Checking jobs for issuer {} by every {} sec", filter.getIssuer(), interval);

        JobFilterVO finalFilter = filter;
        return entityWatchService.watchEntities(ProcessingHistory.class,
                Observables.distinctUntilChanged(() -> {
                    log.debug("Checking jobs for {} ...", finalFilter.getIssuer());
                    // find next 10 events
                    List<JobVO> list = jobService.findAll(
                        finalFilter,
                        Pageables.create(0L, 10, Sort.Direction.DESC, UserEvent.Fields.UPDATE_DATE)
                    ).getContent();

                    // get max event date of current result and set next filter
                    list.stream().map(JobVO::getUpdateDate).max(Date::compareTo).ifPresent(finalFilter::setLastUpdateDate);

                    if (list.isEmpty()) return Optional.empty();
                    return Optional.of(list);
                }),
                interval,
                false)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLSubscription(name = "updateJobProgression", description = "Subscribe to changes on job progression")
    @IsUser
    public Publisher<JobProgressionVO> updateJobProgression(
        @GraphQLNonNull @GraphQLArgument(name = "id") final int id,
        @GraphQLArgument(name = "interval") Integer interval
    ) {

        log.info("Listening job progression for Job#{}", id);
        return jobExecutionService.watchJobProgression(id)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLMutation(name = "cancelJob", description = "Cancel a running Job")
    @IsUser
    public JobVO cancelJob(
        @GraphQLNonNull @GraphQLArgument(name = "id") final int id
    ) {

        JobVO job = this.jobService.get(id);

        PersonVO user = this.authService.getAuthenticatedUser().orElseThrow(UnauthorizedException::new);
        if (SystemRecipientEnum.SYSTEM.getLabel().equalsIgnoreCase(job.getIssuer())) {
            // Only admin can stop a SYSTEM job
            if (!this.authService.isAdmin()) {
                log.warn("User #{} cannot cancel the SYSTEM job #{} - User is not admin", user.getId());
                throw new UnauthorizedException();
            }
        }
        else {
            // Make sure user is the job issuer
            if (!job.getIssuer().equalsIgnoreCase(user.getPubkey()) && !job.getIssuer().equalsIgnoreCase(user.getEmail())) {
                log.warn("User #{} cannot cancel the job #{} - no same issuer", user.getId(), job.getIssuer());
                throw new UnauthorizedException();
            }
        }

        String message = I18n.t("sumaris.job.cancel.message", String.format("%s %s", user.getLastName(), user.getFirstName()));

        return this.jobExecutionService.cancel(job, message);
    }

    @GraphQLQuery(name = "jobTypes", description = "Get all job types")
    @IsAdmin
    public String[] getAllJobTypes() {
        return Arrays.stream(ProcessingTypeEnum.values())
            .filter(type -> type.getId() >= 0 && !ProcessingTypeEnum.UNKNOWN.equals(type))
            .map(ProcessingTypeEnum::getLabel)
            .toArray(String[]::new);
    }

    @GraphQLMutation(name = "runJob", description = "Run a job")
    @IsAdmin
    public JobVO runJob(
        @GraphQLNonNull @GraphQLArgument(name = "type") final String type,
        @GraphQLArgument(name = "issuer", description = "job issuer", defaultValue = JobVO.SYSTEM_ISSUER) String issuer,
        @GraphQLArgument(name = "params", description = "job parameters", defaultValue = GraphQLArgument.NULL) final Map<String, Object> params
    ) {
        JobTypeEnum jobType = JobTypeEnum.valueOf(type);

        // Job issuer
        String userPubkey = this.authService.getAuthenticatedUser()
            .map(PersonVO::getPubkey)
            .orElseThrow(UnauthorizedException::new);
        if (StringUtils.isBlank(issuer)) {
            issuer = userPubkey;
        }
        else if (!issuer.equals(userPubkey) && !issuer.equals(JobVO.SYSTEM_ISSUER)) {
            throw new IllegalArgumentException(String.format("Invalid job issuer: '%s'", issuer));
        }

        // Vessel snapshot indexation
        if (jobType == JobTypeEnum.VESSEL_SNAPSHOTS_INDEXATION) {
            String dateStr = MapUtils.getString(params, "minUpdateDate", null);
            Date minUpdateDate = StringUtils.isNotBlank(dateStr) ? Dates.fromISODateTimeString(dateStr) : null;
            return vesselSnapshotJob.indexVesselSnapshots(issuer, minUpdateDate);
        }

        // Update location hierarchy
        if (jobType == JobTypeEnum.FILL_LOCATION_HIERARCHY) {
            return locationHierarchyJob.updateLocationHierarchy(issuer);
        }

        throw new SumarisTechnicalException("Unknown job type: " + type);
    }
}

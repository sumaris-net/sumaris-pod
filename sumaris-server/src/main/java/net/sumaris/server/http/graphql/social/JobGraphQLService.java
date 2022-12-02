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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.model.social.UserEvent;
import net.sumaris.core.model.technical.history.ProcessingHistory;
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.service.technical.JobService;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.event.job.JobProgressionVO;
import net.sumaris.core.vo.technical.job.JobVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.security.ISecurityContext;
import net.sumaris.server.service.technical.EntityEventService;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@GraphQLApi
@IsUser
@RequiredArgsConstructor
@Slf4j
public class JobGraphQLService {

    private final JobService jobService;
    private final JobExecutionService jobExecutionService;
    private final ISecurityContext<PersonVO> securityContext;
    private final EntityEventService entityEventService;

    @GraphQLQuery(name = "jobs", description = "Search in jobs")
    public List<JobVO> findAll(@GraphQLArgument(name = "filter") JobFilterVO filter) {
        return jobService.findAll(filter);
    }

    @GraphQLQuery(name = "job", description = "Get a job")
    public JobVO getJob(@GraphQLArgument(name = "id") int id) {
        return jobService.findById(id).orElse(null);
    }

    @GraphQLSubscription(name = "updateJobs", description = "Subscribe to changes on jobs")
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
        return entityEventService.watchEntities(ProcessingHistory.class,
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
    public Publisher<JobProgressionVO> updateJobProgression(
        @GraphQLNonNull @GraphQLArgument(name = "id") final int id,
        @GraphQLArgument(name = "interval") Integer interval
    ) {

        log.info("Checking progression for Job#{}", id);

        return jobExecutionService.watchJobProgression(id)
            .toFlowable(BackpressureStrategy.LATEST);
    }
}

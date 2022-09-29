package net.sumaris.server.http.graphql.social;

/*-
 * #%L
 * Quadrige3 Core :: Server
 * %%
 * Copyright (C) 2017 - 2022 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import net.sumaris.core.service.technical.JobExecutionService;
import net.sumaris.core.service.technical.JobService;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.job.JobFilterVO;
import net.sumaris.core.vo.technical.job.JobVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.technical.EntityEventService;
import org.apache.activemq.security.SecurityContext;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
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
    private final SecurityContext securityContext;
    private final EntityEventService entityEventService;

    @Autowired
    private AuthService authService;

    @GraphQLQuery(name = "jobs", description = "Search in jobs")
    public List<JobVO> findJobs(@GraphQLArgument(name = "filter") JobFilterVO filter) {
        return jobService.findAll(filter);
    }

    @GraphQLQuery(name = "job", description = "Get a job")
    public JobVO getJob(@GraphQLArgument(name = "id") int id) {
        return jobService.find(id).orElse(null);
    }

    @GraphQLSubscription(name = "updateJobs", description = "Subscribe to changes on jobs")
    public Publisher<List<JobVO>> updateJobs(
        @GraphQLArgument(name = "filter") JobFilterVO filter,
        @GraphQLArgument(name = "interval") Integer interval
    ) {

        filter = JobFilterVO.nullToDefault(filter);
        // Is user is NOT an admin
        if (!authService.isAdmin()) {
            PersonVO user = authService.getAuthenticatedUser().orElse(null);
            if (filter.getUserId() == null) {
                // default user
                filter.setUserId(user.getId());
            }
//        if (CollectionUtils.isEmpty(filter.getStatus())) {
//            // Default status (only active jobs)
//            filter.setStatus(List.of(JobStatusEnum.PENDING, JobStatusEnum.RUNNING));
//        }

        log.info("Checking jobs for User#{} by events and every {} sec", filter.getUserId(), interval);

        JobFilterVO finalFilter = filter;
        return entityEventService.watchEntities(
                Job.class,
                Observables.distinctUntilChanged(() -> {
                    log.debug("Checking jobs for User#{}", finalFilter.getUserId());
                    // find next 10 events
                    List<JobVO> list = jobService.findAll(
                        finalFilter,
                        Pageables.create(0, 10, Sort.Direction.DESC, UserEvent.Fields.UPDATE_DATE)
                    ).getContent();

                    // get max event date of current result and set next filter
                    list.stream().map(JobVO::getUpdateDate).max(Timestamp::compareTo).ifPresent(finalFilter::setLastUpdateDate);

                    return list.isEmpty() ? Optional.empty() : Optional.of(list);
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

        if (jobService.find(id).isEmpty()) {
            log.debug("Job not found (id={})", id);
            //noinspection unchecked
            return (Publisher<JobProgressionVO>) Observable.empty();
        }

        log.info("Checking progression for Job#{}", id);

        return jobExecutionService.watchJobProgression(id)
            .toFlowable(BackpressureStrategy.LATEST);

    }
}

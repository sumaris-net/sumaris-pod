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

package net.sumaris.server.http.graphql.social;

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.social.EventLevelEnum;
import net.sumaris.core.model.social.EventTypeEnum;
import net.sumaris.core.model.social.SystemRecipientEnum;
import net.sumaris.core.model.social.UserEvent;
import net.sumaris.core.service.social.UserEventService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.reactive.Observables;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.social.UserEventFilterVO;
import net.sumaris.core.vo.social.UserEventVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.technical.EntityWatchService;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Useful service to send message, or data (e.g. to send debug data to developer or administrator)
 */
@Service
@GraphQLApi
@RequiredArgsConstructor
@Slf4j
public class UserEventGraphQLService {

    final static int DEFAULT_PAGE_SIZE = 100;
    final static int MAX_PAGE_SIZE = 1000;

    private final UserEventService userEventService;

    private final EntityWatchService entityWatchService;

    private final AuthService authService;


    @GraphQLQuery(name = "userEvents", description = "Search in user events")
    @IsUser
    public List<UserEventVO> findUserEvents(
        @GraphQLArgument(name = "filter") UserEventFilterVO filter,
        @GraphQLArgument(name = "page") Page page){

        filter = sanitizeFilter(filter);

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

        return userEventService.findAll(filter, page);
    }


    @GraphQLSubscription(name = "updateUserEvents", description = "Subscribe to changes on user events")
    @IsUser
    public Publisher<List<UserEventVO>> updateUserEvents(
        @GraphQLArgument(name = "filter") UserEventFilterVO filter,
        @GraphQLArgument(name = "interval", defaultValue = "10", description = "Minimum interval to find changes, in seconds.") final Integer intervalInSeconds) {

        filter = sanitizeFilter(filter);

        // get date filter from filter if exists or get the max creation date from server
        if (filter.getStartDate() == null) {
            filter.setStartDate(userEventService.getLastCreationDate(filter.getRecipients()));
        }

        log.info("Checking events for User#{} by events and every {} sec", filter.getRecipients(), intervalInSeconds);

        UserEventFilterVO finalFilter = filter;
        final Page page = Page.builder()
            .offset(0)
            .size(10)
            .sortBy(UserEventVO.Fields.CREATION_DATE)
            .sortDirection(SortDirection.DESC)
            .build();

        return entityWatchService.watchEntities(
                UserEvent.class,
                Observables.distinctUntilChanged(() -> {
                    log.debug("Checking events for User#{} from {}", finalFilter.getRecipients(), finalFilter.getStartDate());

                    // find next 10 events
                    List<UserEventVO> list = userEventService.findAll(finalFilter, page);

                    // get max event date of current result and set next filter
                    list.stream().map(UserEventVO::getCreationDate).max(Date::compareTo).ifPresent(finalFilter::setStartDate);
                    return list.isEmpty() ? Optional.empty() : Optional.of(list);
                }),
                intervalInSeconds,
                false)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLQuery(name = "userEventsCount", description = "Count user events")
    @IsUser
    public Long countUserEvents(
        @GraphQLArgument(name = "filter") UserEventFilterVO filter){

        filter = sanitizeFilter(filter);

        // get date filter from filter if exists or get the max read date from server
        if (filter.getStartDate() == null) {
            filter.setStartDate(userEventService.getLastReadDate(filter.getRecipients()));
        }

        return userEventService.count(filter);
    }

    @GraphQLSubscription(name = "updateUserEventsCount", description = "Subscribe to changes on user events count")
    @IsUser
    public Publisher<Long> updateUserEventsCount(
        @GraphQLArgument(name = "filter") UserEventFilterVO filter,
        @GraphQLArgument(name = "interval", defaultValue = "10", description = "Minimum interval to find changes, in seconds.") final Integer intervalInSeconds) {

        filter = sanitizeFilter(filter);

        // get date filter from filter if exists or get the max read date from server
        if (filter.getStartDate() == null) {
            filter.setStartDate(userEventService.getLastReadDate(filter.getRecipients()));
        }

        log.info("Checking events count for User#{} by events and every {} sec", filter.getRecipients(), intervalInSeconds);

        UserEventFilterVO finalFilter = filter;
        return entityWatchService.watchEntitiesCount(
                UserEvent.class,
                Observables.distinctUntilChanged(() -> {
                    log.debug("Checking events count for User#{} from {}", finalFilter.getRecipients(), finalFilter.getStartDate());
                    // find new user events
                    List<UserEventVO> list = userEventService.findAll(finalFilter);
                    // get max read date of current result and set next filter
                    list.stream().map(UserEventVO::getReadDate).filter(Objects::nonNull).max(Date::compareTo).ifPresent(finalFilter::setStartDate);
                    return list.isEmpty() ? Optional.empty() : Optional.of(list);
                }),
                intervalInSeconds,
                false)
            .toFlowable(BackpressureStrategy.LATEST);
    }

    @GraphQLMutation(name = "saveUserEvent", description = "Sent data to admin, for debug")
    @IsUser
    public UserEventVO saveUserEvent(@GraphQLArgument(name = "userEvent") UserEventVO event) {
        Preconditions.checkNotNull(event, "Invalid user event");

        // Compatibility with version < 1.29.0
        if (event.getType() == null && event.getEventType() != null) {
            event.setType(EventTypeEnum.valueOf(event.getEventType()));
        }

        // Read type
        EventTypeEnum type = event.getType();

        // Is user is NOT an admin
        if (!authService.isAdmin()) {
            PersonVO user = authService.getAuthenticatedUser().orElse(null);
            Preconditions.checkNotNull(user);

            // Check issuer = himself
            if (event.getIssuer() == null) {
                event.setIssuer(user.getPubkey());
            }
            else {
                Preconditions.checkArgument(Objects.equals(user.getPubkey(), event.getIssuer()));
            }

            // Check event type = DEBUG_DATA or INBOX_MESSAGE
            Preconditions.checkArgument (type == EventTypeEnum.DEBUG_DATA || type == EventTypeEnum.INBOX_MESSAGE,
                "Invalid user event type: " + type.name());
        }

        // Use SYSTEM as default recipient, for debug data
        if (type == EventTypeEnum.DEBUG_DATA && StringUtils.isBlank(event.getRecipient())) {
            event.setRecipient(SystemRecipientEnum.SYSTEM.getLabel());
            // Fill default level as DEBUG
            if (event.getLevel() == null) {
                event.setLevel(EventLevelEnum.DEBUG);
            }
        }

        // Fill default level to INFO
        if (event.getLevel() == null) {
            event.setLevel(EventLevelEnum.INFO);
        }

        return userEventService.save(event);
    }

    @GraphQLMutation(name = "markAsReadUserEvents", description = "Mark as read user events")
    @IsUser
    public void markAsReadUserEvents(@GraphQLArgument(name = "ids") List<Integer> userEventIds) {
        userEventService.markAsRead(userEventIds);
    }

    @GraphQLMutation(name = "deleteUserEvent", description = "Delete a user event")
    @IsAdmin
    public void deleteUserEvent(@GraphQLArgument(name = "id") int id) {
        userEventService.delete(id);
    }

    @GraphQLMutation(name = "deleteUserEvents", description = "Delete many user events")
    @IsAdmin
    public void deleteUserEvents(@GraphQLArgument(name = "ids") List<Integer> ids) {
        userEventService.delete(ids);
    }

    /* -- protected functions -- */

    protected UserEventFilterVO sanitizeFilter(UserEventFilterVO filter) {
        filter = UserEventFilterVO.nullToEmpty(filter);

        // Limit to events to self (recipient)
        if (!authService.isAdmin()) {
            PersonVO user = authService.getAuthenticatedUser().orElse(null);
            Preconditions.checkNotNull(user);
            filter.setRecipient(user.getPubkey());
        }

        // Fill recipients from recipient (for compatibility)
        if (filter.getRecipients() == null && filter.getRecipient() != null) {
            filter.setRecipients(new String[]{filter.getRecipient()});
        }
        // Fill issuers from issuer (for compatibility)
        if (filter.getIssuers() == null && filter.getIssuer() != null) {
            filter.setIssuers(new String[]{filter.getIssuer()});
        }
        return filter;
    }
}

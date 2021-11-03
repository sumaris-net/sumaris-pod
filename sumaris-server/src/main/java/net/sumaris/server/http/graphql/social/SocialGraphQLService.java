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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.social.EventTypeEnum;
import net.sumaris.core.model.social.SystemRecipientEnum;
import net.sumaris.core.service.social.UserEventService;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.social.UserEventFilterVO;
import net.sumaris.core.vo.social.UserEventVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsAdmin;
import net.sumaris.server.http.security.IsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Useful service to send message, or data (e.g. to send debug data to developer or administrator)
 */
@Service
@GraphQLApi
@Transactional
public class SocialGraphQLService {

    final static int DEFAULT_PAGE_SIZE = 100;
    final static int MAX_PAGE_SIZE = 1000;

    @Autowired
    private UserEventService userEventService;

    @Autowired
    private AuthService authService;

    /* -- User event -- */

    @GraphQLMutation(name = "saveUserEvent", description = "Sent data to admin, for debug")
    @IsUser
    public UserEventVO saveUserEvent(@GraphQLArgument(name = "userEvent") UserEventVO userEvent) {
        Preconditions.checkNotNull(userEvent, "Invalid user event");

        EventTypeEnum type = EventTypeEnum.byLabel(userEvent.getEventType());

        // Is user is NOT an admin
        if (!authService.isAdmin()) {
            PersonVO user = authService.getAuthenticatedUser().orElse(null);
            Preconditions.checkNotNull(user);

            // Check issuer = himself
            if (userEvent.getIssuer() == null) {
                userEvent.setIssuer(user.getPubkey());
            }
            else {
                Preconditions.checkArgument(Objects.equals(user.getPubkey(), userEvent.getIssuer()));
            }

            // Check event type = DEBUG_DATA or INBOX_MESSAGE
            Preconditions.checkArgument (type == EventTypeEnum.DEBUG_DATA || type == EventTypeEnum.INBOX_MESSAGE,
                    "Invalid user event type: " + type.getLabel());
        }

        // Use SYSTEM as default recipient, for debug data
        if (type == EventTypeEnum.DEBUG_DATA && StringUtils.isBlank(userEvent.getRecipient())) {
            userEvent.setRecipient(SystemRecipientEnum.SYSTEM.getLabel());
        }

        return userEventService.save(userEvent);
    }

    @GraphQLQuery(name = "userEvents", description = "Search in user events")
    @Transactional(readOnly = true)
    @IsUser
    public List<UserEventVO> findUserEvents(
            @GraphQLArgument(name = "filter") UserEventFilterVO filter,
            @GraphQLArgument(name = "page") Page page){

        // Limit to events to self (recipient)
        if (!authService.isAdmin()) {
           PersonVO user = authService.getAuthenticatedUser().orElse(null);
           Preconditions.checkNotNull(user);

           if (filter == null) filter = new UserEventFilterVO();
           filter.setRecipient(user.getPubkey());
        }

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


    /* -- User interaction -- */



}

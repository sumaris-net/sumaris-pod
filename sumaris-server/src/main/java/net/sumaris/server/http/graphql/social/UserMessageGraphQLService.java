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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import lombok.NonNull;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.social.UserMessageService;
import net.sumaris.server.util.social.MessageTypeEnum;
import net.sumaris.server.util.social.MessageVO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Useful service to send message, or data (e.g. to send debug data to developer or administrator)
 */
@Service
@GraphQLApi
@Transactional
public class UserMessageGraphQLService {

    @Autowired
    private UserMessageService userMessageService;

    @Autowired
    private AuthService authService;

    @GraphQLMutation(name = "sendMessage", description = "Sent a message")
    @IsUser
    public boolean sendMessage(@GraphQLArgument(name = "message") @NonNull MessageVO message) {

        boolean isAdmin = authService.isAdmin();
        boolean isSupervisor = isAdmin || authService.isSupervisor();

        // Force type, when not supervisor and not admin
        if (!isSupervisor) {
            message.setType(MessageTypeEnum.INBOX_MESSAGE);
        }

        // If not admin: use the authenticated user has issuer
        boolean forceIssuer = !isAdmin
            || (message.getIssuerId() == null && message.getIssuer() == null);

        // Use current authenticated user, as issuer
        if (forceIssuer){
            PersonVO user = authService.getAuthenticatedUser().orElseThrow(UnauthorizedException::new);
            message.setIssuer(user);
            message.setIssuerId(null);
        }

        // Only admin can use recipientFilter
        if (message.getRecipientFilter() != null && !isSupervisor) {
            throw new UnauthorizedException("Only admin or supervisor can use " + MessageVO.Fields.RECIPIENT_FILTER);
        }

        // Limit number of recipients, if not admin
        // - limit to 1 for not supervisor
        boolean toManyRecipients = !isAdmin
            && (!isSupervisor && ArrayUtils.getLength(message.getRecipients()) > 1);
        if (toManyRecipients) throw new UnauthorizedException("Too many recipients");

        userMessageService.send(message);

        return true;
    }
}

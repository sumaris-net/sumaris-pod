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

package net.sumaris.server.http.graphql.administration;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.vo.administration.user.AccountVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsGuest;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.administration.AccountService;
import net.sumaris.server.service.administration.ImageService;
import net.sumaris.server.service.technical.ChangesPublisherService;
import org.reactivestreams.Publisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Slf4j
public class AccountGraphQLService {

    @Resource
    private AuthService authService;

    @Resource
    private AccountService accountService;

    @Resource
    private ChangesPublisherService changesPublisherService;

    @Resource
    private ImageService imageService;

    @GraphQLQuery(name = "account", description = "Load a user account")
    @Transactional(readOnly = true)
    public AccountVO loadAccount() {
        PersonVO person = this.authService.getAuthenticatedUser()
            .orElseThrow(() -> new UnauthorizedException("Accès refusé"));
        AccountVO result = accountService.getById(person.getId());
        imageService.fillAvatar(result);
        return result;
    }

    /* -- Mutations -- */

    @GraphQLMutation(name = "createAccount", description = "Create an account")
    public AccountVO createAccount(@GraphQLArgument(name = "account") AccountVO account) {
        return accountService.createAccount(account);
    }

    @GraphQLMutation(name = "saveAccount", description = "Create or update an account")
    @IsUser
    @PreAuthorize("hasRole('ROLE_ADMIN') or #account.pubkey == authentication.name or #account.username == authentication.name")
    public AccountVO saveAccount(@P("account") @GraphQLArgument(name = "account") AccountVO account) {
        return accountService.saveAccount(account);
    }

    @GraphQLMutation(name = "confirmAccountEmail", description = "Confirm an account email")
    public boolean confirmEmail(@GraphQLArgument(name="email") String email,
                                @GraphQLArgument(name="code") String signatureHash) {
        accountService.confirmEmail(email, signatureHash);
        return true;
    }

    @GraphQLMutation(name = "sendAccountConfirmationEmail", description = "Resent confirmation email")
    @IsGuest
    public boolean sendConfirmationEmail(@GraphQLArgument(name="email") String email,
                                         @GraphQLArgument(name="locale", defaultValue = "en_GB") String locale) {
        accountService.sendConfirmationEmail(email, locale);
        return true;
    }

    /* -- Subscriptions -- */

    @GraphQLSubscription(name = "updateAccount", description = "Subscribe to an account update")
    @IsUser
    @Transactional(readOnly = true)
    public Publisher<AccountVO> updateAccount(
            @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer intervalInSecond) {

        PersonVO person = this.authService.getAuthenticatedUser().get();
        return changesPublisherService.getPublisher(Person.class, AccountVO.class, person.getId(), intervalInSecond, true);
    }

}
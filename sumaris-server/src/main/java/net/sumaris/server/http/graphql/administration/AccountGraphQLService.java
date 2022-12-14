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

import com.google.common.base.Preconditions;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLSubscription;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.UnauthorizedException;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.vo.administration.user.AccountVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.administration.user.Persons;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.http.security.IsGuest;
import net.sumaris.server.http.security.IsUser;
import net.sumaris.server.service.administration.AccountService;
import net.sumaris.server.service.administration.ImageService;
import net.sumaris.server.service.administration.UserSettingsService;
import net.sumaris.server.service.technical.EntityWatchService;
import org.nuiton.i18n.I18n;
import org.reactivestreams.Publisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@GraphQLApi
@Transactional
@Slf4j
public class AccountGraphQLService {

    @Resource
    private AuthService authService;

    @Resource
    private AccountService accountService;

    @Resource
    private UserSettingsService userSettingsService;

    @Resource
    private EntityWatchService entityEventService;

    @Resource
    private ImageService imageService;

    @GraphQLQuery(name = "account", description = "Load a user account")
    @Transactional(readOnly = true)
    public AccountVO loadAccount(
        @GraphQLArgument(name = "pubkey") String pubkey // Deprecated in 1.8.0
    ) {
        if (pubkey != null) {
            log.warn("Deprecated used of GraphQL 'account' query. Since version 1.8.0, the 'pubkey' argument has been deprecated, and will be ignored.");
        }

        PersonVO person = this.authService.getAuthenticatedUser().orElse(null);

        // Check if user exists
        if (person == null) {
            throw new UnauthorizedException(I18n.t("sumaris.error.account.unauthorized"));
        }

        // Check if user has been disabled
        if (Persons.isDisableOrDeleted(person)) {
            authService.cleanCacheForUser(person);
            throw new UnauthorizedException(I18n.t("sumaris.error.account.unauthorized"));
        }

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

    @GraphQLMutation(name = "saveSettings", description = "Save user settings")
    @IsGuest
    public UserSettingsVO saveSettings(@GraphQLArgument(name="settings") UserSettingsVO settings) {
        Preconditions.checkNotNull(settings);

        // Set account pubkey into issuer
        PersonVO user = authService.getAuthenticatedUser().orElseThrow(() -> new AccessDeniedException("Forbidden"));
        settings.setIssuer(user.getPubkey());

        return userSettingsService.save(settings);
    }

    /* -- Subscriptions -- */

    @GraphQLSubscription(name = "updateAccount", description = "Subscribe to any account update")
    @IsGuest
    @Transactional(readOnly = true)
    public Publisher<AccountVO> updateAccount(
        @GraphQLArgument(name = "interval", defaultValue = "30", description = "Minimum interval to find changes, in seconds.") final Integer intervalInSecond
    ) {

        Integer personId = this.authService.getAuthenticatedUserId().orElse(null);
        return entityEventService.watchEntity(Person.class, AccountVO.class, personId, intervalInSecond, true)
            .toFlowable(BackpressureStrategy.LATEST);
    }

}

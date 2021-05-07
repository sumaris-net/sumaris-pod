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

package net.sumaris.server.http.security;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Root authentication provider, that delegate auth to other providers
 */
@Slf4j
public class MultiAuthenticationProviderDelegator implements AuthenticationProvider {

    private Collection<AuthenticationProvider> delegates;


    public MultiAuthenticationProviderDelegator(Collection<AuthenticationProvider> delegates) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(delegates));
        this.delegates = delegates;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return delegates.stream()
            .anyMatch(p -> p.supports(aClass));
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        AuthenticationException firstError = null;
        for (AuthenticationProvider provider : delegates) {
            if (provider.supports(authentication.getClass())) {
                try {
                    return provider.authenticate(authentication);
                } catch (AuthenticationException e) {
                    log.debug(e.getMessage());
                    // Failed: but continue
                    if (firstError == null) firstError = e;
                }
            }
        }

        // Rethrow the first error
        if (firstError != null) throw firstError;

        // No error = no provider
        throw new InternalAuthenticationServiceException("No authentication provider found");
    }
}

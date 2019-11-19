package net.sumaris.server.http.security;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static lombok.AccessLevel.PACKAGE;

/**
 * @author peck7 on 30/11/2018.
 */
@Component
@AllArgsConstructor(access = PACKAGE)
public class TokenAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    @NonNull
    private AuthService authService;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        // Nothing to do
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final Object token = authentication.getCredentials();
        return Optional
                .ofNullable(token)
                .map(String::valueOf)
                .flatMap(authService::authenticate)
                .orElseThrow(() -> new UsernameNotFoundException("Cannot find user with authentication token=" + token));
    }
}

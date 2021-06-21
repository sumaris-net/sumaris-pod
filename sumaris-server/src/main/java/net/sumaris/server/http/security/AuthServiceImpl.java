package net.sumaris.server.http.security;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.JmsConfiguration;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.service.administration.AccountService;
import net.sumaris.server.service.crypto.ServerCryptoService;
import net.sumaris.server.util.security.AuthTokenVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.Attributes2GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAttributes2GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service("authService")
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final ValidationExpiredCache challenges;
    private final ValidationExpiredCacheMap<AuthUserDetails> checkedTokens;
    private final ValidationExpiredCacheMap<AuthUserDetails> checkedUsernames;
    private final boolean debug;

    @Autowired
    private ServerCryptoService cryptoService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PersonRepository personRepository;

    private Attributes2GrantedAuthoritiesMapper authoritiesMapper;

    private boolean enableAuthToken;
    private boolean enableAuthBasic;

    @Autowired
    public AuthServiceImpl(Environment environment, SumarisServerConfiguration config) {

        int challengeLifeTimeInSeconds = Integer.parseInt(environment.getProperty(SumarisServerConfigurationOption.AUTH_CHALLENGE_LIFE_TIME.getKey(), SumarisServerConfigurationOption.AUTH_CHALLENGE_LIFE_TIME.getDefaultValue()));
        this.challenges = new ValidationExpiredCache(challengeLifeTimeInSeconds);

        int tokenLifeTimeInSeconds = Integer.parseInt(environment.getProperty(SumarisServerConfigurationOption.AUTH_TOKEN_LIFE_TIME.getKey(), SumarisServerConfigurationOption.AUTH_TOKEN_LIFE_TIME.getDefaultValue()));
        this.checkedTokens = new ValidationExpiredCacheMap<>(tokenLifeTimeInSeconds);
        this.checkedUsernames = new ValidationExpiredCacheMap<>(tokenLifeTimeInSeconds);

        authoritiesMapper = new SimpleAttributes2GrantedAuthoritiesMapper();

        this.enableAuthToken = config.enableAuthToken();
        this.enableAuthBasic = config.enableAuthBasic();

        this.debug = log.isDebugEnabled();
    }

    @JmsListener(destination = "updatePerson", containerFactory = JmsConfiguration.CONTAINER_FACTORY_NAME)
    protected void onPersonSaved(PersonVO person) {

        if (!StringUtils.isNotBlank(person.getPubkey())) return;
        List<String> tokens = accountService.getAllTokensByPubkey(person.getPubkey());

        // Clean cache (because user can be disabled)
        Beans.getStream(tokens).forEach(checkedTokens::remove);
        Optional.ofNullable(person.getUsername()).ifPresent(checkedUsernames::remove);
        Optional.ofNullable(person.getUsernameExtranet()).ifPresent(checkedUsernames::remove);
    }

    @Override
    public UserDetails authenticateByToken(String token) throws AuthenticationException{
        Preconditions.checkArgument(enableAuthToken);

        // First check anonymous user
        if (AnonymousUserDetails.TOKEN.equals(token)) return AnonymousUserDetails.INSTANCE;

        // Check if present in cache
        if (checkedTokens.contains(token)) return checkedTokens.get(token);

        // Parse the token
        AuthTokenVO authData;
        try {
            authData = AuthTokenVO.parse(token);
        } catch (ParseException e) {
            throw new BadCredentialsException("Authentication failed. Invalid token: " + token);
        }

        // Try to authenticate
        PersonVO user = validateToken(authData);

        // Add username to auth data
        //authData.setUsername(user.getUsername() != null ? user.getUsername() : user.getUsernameExtranet());

        AuthUserDetails userDetails = new AuthUserDetails(authData, getAuthorities(user));

        log.debug("Authentication succeed for user with pubkey {{}}", user.getPubkey().substring(0, 8));

        // Add token to cache
        checkedTokens.add(token, userDetails);

        return userDetails;
    }

    @Override
    public UserDetails authenticateByUsername(String username, UsernamePasswordAuthenticationToken authentication) {
        Preconditions.checkArgument(enableAuthBasic);

        // First check anonymous user
        if (AnonymousUserDetails.TOKEN.equals(username)) return AnonymousUserDetails.INSTANCE;

        // Check if present in cache
        if (checkedUsernames.contains(username)) return checkedUsernames.get(username);

        // Check user exists
        PersonVO user = personRepository.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("Authentication failed. User not found: " + username));

        // Check account is enable (or temporary)
        checkEnabledAccount(user);

        AuthTokenVO.AuthTokenVOBuilder authToken = AuthTokenVO.builder()
            .username(username);

        // Is token authentication enabled
        if (enableAuthToken) {
            String password = String.valueOf(authentication.getCredentials());
            if (StringUtils.isBlank(password)) throw new BadCredentialsException("Blank password");

            // Generate the pubkey
            String pubkey = CryptoUtils.encodeBase58(cryptoService.getKeyPair(username, password).getPubKey());

            // Update the database pubkey, if need
            boolean pubkeyChanged = !Objects.equals(pubkey, user.getPubkey());
            if (pubkeyChanged) {
                log.info("Updating pubkey of user {id: {}}. Previous pubkey: {{}}, new: {{}}",
                    user.getId(),
                    user.getPubkey() != null ? user.getPubkey().substring(0, 8) : "null",
                    pubkey.substring(0, 8));

                user.setPubkey(pubkey);
                user = personRepository.save(user);
            }

            authToken.pubkey(pubkey);
        }

        AuthUserDetails userDetails = new AuthUserDetails(authToken.build(), getAuthorities(user));

        log.debug("Authentication succeed for user with username {{}}", username);

        checkedUsernames.add(username, userDetails);

        return userDetails;
    }

    private PersonVO validateToken(AuthTokenVO authData) throws AuthenticationException {
        String pubkey = authData != null ? authData.getPubkey() : null;

        // Check pubkey is valid
        if (!CryptoUtils.isValidPubkey(pubkey)) {
            throw new BadCredentialsException("Authentication failed. Bad pubkey format: " + pubkey);
        }

        // Check user exists
        PersonVO user = personRepository.findByPubkey(pubkey)
            .orElseThrow(() -> new BadCredentialsException("Authentication failed. User not found: " + pubkey));

        // Check account is enable (or temporary)
        checkEnabledAccount(user);

        // Token exists on database: check as new challenge response
        final String token = authData.asToken();
        boolean isStoredToken = accountService.isStoredToken(token, pubkey);
        if (!isStoredToken) {
            log.debug("Unknown token. Check if response to new challenge...");

            // Make sure the challenge exists and not expired
            if (!challenges.contains(authData.getChallenge())) {
                throw new BadCredentialsException("Authentication failed. Challenge not found or expired: " + authData.getChallenge());
            }
        }

        // Check signature
        if (!cryptoService.verify(authData.getChallenge(), authData.getSignature(), pubkey)) {
            throw new BadCredentialsException("Authentication failed. Bad challenge signature in token: " + authData);
        }

        // Auth success !

        // Remove from the challenge list
        challenges.remove(authData.getChallenge());

        // Save this new token to database
        if (!isStoredToken) {
            try {
                accountService.addToken(token, pubkey);
            } catch (RuntimeException e) {
                // Log then continue
                log.error("Could not save auth token.", e);
            }
        }

        return user;
    }

    @Override
    public Optional<PersonVO> getAuthenticatedUser() {
        Optional<AuthUserDetails> principal = getAuthPrincipal();

        if (!principal.isPresent())  return Optional.empty(); // Skip if no principal

        // If pubkey enable, try to resolve using the pubkey
        if (enableAuthToken) {
            Optional<PersonVO> result = principal
                .map(AuthUserDetails::getPubkey)
                .filter(Objects::nonNull)
                .flatMap(personRepository::findByPubkey);
            if (result.isPresent()) return result;
        }

        // Else, try to resolve user by username
        if (enableAuthBasic) {
            Optional<PersonVO> result = principal.map(AuthUserDetails::getUsername)
                .filter(Objects::nonNull)
                .flatMap(personRepository::findByUsername);
            if (result.isPresent()) return result;
        }

        return Optional.empty();
    }

    @Override
    public boolean hasAuthority(String authority) {
        return hasUpperOrEqualsAuthority(getAuthorities(), authority);
    }

    /* -- internal methods -- */

    public Optional<AuthUserDetails> getAuthPrincipal() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null
            && securityContext.getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) securityContext.getAuthentication();

            Object principal = authToken.getPrincipal();
            if (principal instanceof AuthUserDetails) return Optional.of((AuthUserDetails) principal);

            String tokenOrPassword = String.valueOf(authToken.getCredentials());
            if (enableAuthToken && this.checkedTokens.contains(tokenOrPassword)) {
                return Optional.of(this.checkedTokens.get(tokenOrPassword));
            }

            return Optional.ofNullable(this.checkedUsernames.get(authToken.getName()));
        }
        return Optional.empty(); // Not auth
    }

    protected String getMainAuthority(Collection<? extends GrantedAuthority> authorities) {
        if (CollectionUtils.isEmpty(authorities)) return PRIORITIZED_AUTHORITIES.get(PRIORITIZED_AUTHORITIES.size() - 1); // Last role
        return PRIORITIZED_AUTHORITIES.stream()
            .map(role -> Beans.getList(authorities).stream().map(GrantedAuthority::getAuthority).filter(authority -> role.equals(authority)).findFirst().orElse(null))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(PRIORITIZED_AUTHORITIES.get(PRIORITIZED_AUTHORITIES.size() - 1)); // Last role
    }

    protected boolean hasUpperOrEqualsAuthority(Collection<? extends GrantedAuthority> authorities,
                                                String expectedAuthority) {
        int expectedRoleIndex = PRIORITIZED_AUTHORITIES.indexOf(expectedAuthority);
        int actualRoleIndex = PRIORITIZED_AUTHORITIES.indexOf(getMainAuthority(authorities));
        return expectedRoleIndex != -1 && actualRoleIndex <= expectedRoleIndex;
    }

    protected Collection<? extends GrantedAuthority> getAuthorities() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null) {
            return securityContext.getAuthentication().getAuthorities();
        }
        return null;
    }

    private void checkEnabledAccount(PersonVO person) throws DataNotFoundException {
        // Cannot auth if user has been deleted or is disable
        StatusEnum status = StatusEnum.valueOf(person.getStatusId());
        if (StatusEnum.DISABLE.equals(status) || StatusEnum.DELETED.equals(status)) {
            throw new DisabledException("Account is disabled");
        }
    }

    public AuthTokenVO createNewChallenge() {
        String challenge = newChallenge();
        String signature = cryptoService.sign(challenge);

        AuthTokenVO result = AuthTokenVO.builder()
            .pubkey(cryptoService.getServerPubkey())
            .challenge(challenge)
            .signature(signature)
            .build();

        if (debug) log.debug("New authentication challenge: " + result.toString());

        // Add challenge to cache
        challenges.add(challenge);

        return result;
    }

    /* -- new challenge -- */

    private String newChallenge() {
        byte[] randomNonce = cryptoService.getBoxRandomNonce();
        String randomNonceStr = CryptoUtils.encodeBase64(randomNonce);
        return cryptoService.hash(randomNonceStr);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(PersonVO person) {
        return authoritiesMapper.getGrantedAuthorities(
            Beans.getStream(person.getProfiles())
            .map(UserProfileEnum::valueOf)
            .map(Enum::name)
            .collect(Collectors.toSet()));
    }
}

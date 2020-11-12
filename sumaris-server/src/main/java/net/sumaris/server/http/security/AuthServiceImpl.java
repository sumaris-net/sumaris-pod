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

import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.administration.user.PersonSpecifications;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.service.administration.AccountService;
import net.sumaris.server.service.crypto.ServerCryptoService;
import net.sumaris.server.util.security.AuthDataVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.Attributes2GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAttributes2GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service("authService")
public class AuthServiceImpl implements AuthService {

    /**
     * Logger.
     */
    private static final Logger log =
        LoggerFactory.getLogger(AuthServiceImpl.class);

    private final ValidationExpiredCache challenges;
    private final ValidationExpiredCacheMap<AuthUser> checkedTokens;
    private final boolean debug;

    @Autowired
    private ServerCryptoService cryptoService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PersonRepository personRepository;

    private Attributes2GrantedAuthoritiesMapper authoritiesMapper;

    @Autowired
    public AuthServiceImpl(Environment environment) {

        int challengeLifeTimeInSeconds = Integer.parseInt(environment.getProperty(SumarisServerConfigurationOption.AUTH_CHALLENGE_LIFE_TIME.getKey(), SumarisServerConfigurationOption.AUTH_CHALLENGE_LIFE_TIME.getDefaultValue()));
        this.challenges = new ValidationExpiredCache(challengeLifeTimeInSeconds);

        int tokenLifeTimeInSeconds = Integer.parseInt(environment.getProperty(SumarisServerConfigurationOption.AUTH_TOKEN_LIFE_TIME.getKey(), SumarisServerConfigurationOption.AUTH_TOKEN_LIFE_TIME.getDefaultValue()));
        this.checkedTokens = new ValidationExpiredCacheMap<>(tokenLifeTimeInSeconds);

        authoritiesMapper = new SimpleAttributes2GrantedAuthoritiesMapper();

        this.debug = log.isDebugEnabled();
    }

    @PostConstruct
    public void registerListeners() {
        // Listen person update, to update the cache
        personRepository.addListener(new PersonSpecifications.Listener() {
            @Override
            public void onSave(PersonVO person) {
                if (!StringUtils.isNotBlank(person.getPubkey())) return;
                List<String> tokens = accountService.getAllTokensByPubkey(person.getPubkey());
                if (CollectionUtils.isEmpty(tokens)) return;
                tokens.forEach(checkedTokens::remove);
            }

            @Override
            public void onDelete(int id) {
                // Will be remove when cache expired
            }
        });
    }

    @Override
    public Optional<AuthUser> authenticate(String token) {

        // First check anonymous user
        if (AnonymousUser.TOKEN.equals(token)) return Optional.of(AnonymousUser.INSTANCE);

        // Check if present in cache
        if (checkedTokens.contains(token)) return Optional.of(checkedTokens.get(token));

        // Parse the token
        AuthDataVO authData;
        try {
            authData = AuthDataVO.parse(token);
        } catch (ParseException e) {
            log.warn("Authentication failed. Invalid token: " + token);
            return Optional.empty();
        }

        // Try to authenticate
        AuthUser authUser = authenticate(authData);

        return Optional.ofNullable(authUser);
    }

    private AuthUser authenticate(AuthDataVO authData) {

        // Check if pubkey can authenticate
        try {
            if (authData.getPubkey().length() < 6) {
                if (debug) log.debug("Authentication failed. Bad pubkey format: " + authData.getPubkey());
                return null;
            }
            if (!canAuth(authData.getPubkey())) {
                if (debug) log.debug("Authentication failed. User is not allowed to authenticate: " + authData.getPubkey());
                return null;
            }
        } catch (DataNotFoundException | DataRetrievalFailureException e) {
            log.debug("Authentication failed. User not found: " + authData.getPubkey());
            return null;
        }

        // Token exists on database: check as new challenge response
        boolean isStoredToken = accountService.isStoredToken(authData.asToken(), authData.getPubkey());
        if (!isStoredToken) {
            log.debug("Unknown token. Check if response to new challenge...");

            // Make sure the challenge exists and not expired
            if (!challenges.contains(authData.getChallenge())) {
                if (debug)
                    log.debug("Authentication failed. Challenge not found or expired: " + authData.getChallenge());
                return null;
            }
        }

        // Check signature
        if (!cryptoService.verify(authData.getChallenge(), authData.getSignature(), authData.getPubkey())) {
            if (debug) log.debug("Authentication failed. Bad challenge signature in token: " + authData.toString());
            return null;
        }

        // Auth success !

        // Force challenge to expire
        challenges.remove(authData.getChallenge());

        // Get authorities
        List<GrantedAuthority> authorities = getAuthorities(authData.getPubkey());

        // Create authenticated user
        AuthUser authUser = new AuthUser(authData, authorities);

        // Add token to store
        String token = authData.toString();
        checkedTokens.add(token, authUser);

        if (!isStoredToken) {
            // Save this new token to database
            try {
                accountService.addToken(token, authData.getPubkey());
            } catch (RuntimeException e) {
                // Log then continue
                log.error("Could not save auth token.", e);
            }
        }

        if (debug) log.debug(String.format("Authentication succeed for user with pubkey {%s}", authData.getPubkey().substring(0, 6)));

        return authUser;
    }

    @Override
    public Optional<PersonVO> getAuthenticatedUser() {
        return getAuthPrincipal()
                .map(AuthUser::getPubkey)
                .filter(Objects::nonNull)
                .map(personRepository::findByPubkey);
    }

    @Override
    public boolean hasAuthority(String authority) {
        return hasUpperOrEqualsAuthority(getAuthorities(), authority);
    }

    /* -- internal methods -- */

    public Optional<AuthUser> getAuthPrincipal() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null) {
            return Optional.ofNullable((AuthUser) securityContext.getAuthentication().getPrincipal());
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

    private List<GrantedAuthority> getAuthorities(String pubkey) {
        List<Integer> profileIds = accountService.getProfileIdsByPubkey(pubkey);

        return new ArrayList<>(authoritiesMapper.getGrantedAuthorities(profileIds.stream()
            .map(id -> UserProfileEnum.getLabelById(id).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())));

    }

    private boolean canAuth(final String pubkey) throws DataNotFoundException {
        PersonVO person = personRepository.findByPubkey(pubkey);
        if (person == null) {
            throw new DataRetrievalFailureException(I18n.t("sumaris.error.account.notFound"));
        }

        // Cannot auth if user has been deleted or is disable
        StatusEnum status = StatusEnum.valueOf(person.getStatusId());
        if (StatusEnum.DISABLE.equals(status) || StatusEnum.DELETED.equals(status)) {
            return false;
        }

        // TODO: check if necessary ?
        /*
        List<Integer> userProfileIds = accountService.getProfileIdsByPubkey(pubkey);
        boolean result = CollectionUtils.containsAny(userProfileIds, AUTH_ACCEPTED_PROFILES);
        if (debug) log.debug(String.format("User with pubkey {%s} %s authenticate, because he has this profiles: %s", pubkey.substring(0,6), (result ? "can" : "cannot"), userProfileIds));
        return result;
        */

        return true;
    }

    public AuthDataVO createNewChallenge() {
        String challenge = newChallenge();
        String signature = cryptoService.sign(challenge);

        AuthDataVO result = new AuthDataVO(cryptoService.getServerPubkey(), challenge, signature);

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
}

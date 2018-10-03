package net.sumaris.server.http.security;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.referential.StatusId;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.config.SumarisServerConfigurationOption;
import net.sumaris.server.service.administration.AccountService;
import net.sumaris.server.service.crypto.ServerCryptoService;
import net.sumaris.server.vo.security.AuthDataVO;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;

@Service("authService")
public class AuthServiceImpl implements AuthService {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    private final List<Integer> AUTH_ACCEPTED_PROFILES = ImmutableList.of(UserProfileEnum.ADMIN.id, UserProfileEnum.USER.id, UserProfileEnum.SUPERVISOR.id);

    private final ValidationExpiredCache challenges;
    private final ValidationExpiredCache checkedTokens;
    private final boolean debug;

    @Autowired
    private ServerCryptoService cryptoService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PersonDao personDao;

    @Autowired
    public AuthServiceImpl(Environment environment) {

        int challengeLifeTimeInSeconds = Integer.parseInt(environment.getProperty(SumarisServerConfigurationOption.AUTH_CHALLENGE_LIFE_TIME.getKey(), SumarisServerConfigurationOption.AUTH_CHALLENGE_LIFE_TIME.getDefaultValue()));
        this.challenges = new ValidationExpiredCache(challengeLifeTimeInSeconds);

        int tokenLifeTimeInSeconds = Integer.parseInt(environment.getProperty(SumarisServerConfigurationOption.AUTH_TOKEN_LIFE_TIME.getKey(), SumarisServerConfigurationOption.AUTH_TOKEN_LIFE_TIME.getDefaultValue()));
        this.checkedTokens = new ValidationExpiredCache(tokenLifeTimeInSeconds);

        this.debug = log.isDebugEnabled();
    }

    public boolean authenticate(String token) {

        // Check if present in cache
        if (checkedTokens.contains(token)) return true;

        try {
            AuthDataVO authData = AuthDataVO.parse(token);
            return authenticate(authData);
        } catch(ParseException e) {
            log.warn("Authentication failed. Invalid token: " + token);
            return false;
        }
    }

    public boolean authenticate(AuthDataVO authData) {

        // Check if pubkey can authenticate
        try {
            if (authData.getPubkey().length() < 6) {
                if (debug) log.debug("Authentication failed. Bad pubkey format: " + authData.getPubkey());
                return false;
            }
            if (!canAuth(authData.getPubkey())) {
                if (debug) log.debug("Authentication failed. User is not allowed to authenticate: " + authData.getPubkey());
                return false;
            }
        } catch(DataNotFoundException | DataRetrievalFailureException e) {
            log.debug("Authentication failed. User not found: " + authData.getPubkey());
            return false;
        }

        // Token exists on database: check as new challenge response
        boolean isStoredToken = accountService.isStoredToken(authData.asToken(), authData.getPubkey());
        if (!isStoredToken) {
            log.debug("Unknown token. Check if response to new challenge...");

            // Make sure the challenge exists and not expired
            if (!challenges.contains(authData.getChallenge())) {
                if (debug)
                    log.debug("Authentication failed. Challenge not found or expired: " + authData.getChallenge());
                return false;
            }
        }

        // Check signature
        if (!cryptoService.verify(authData.getChallenge(), authData.getSignature(), authData.getPubkey())) {
            if (debug) log.debug("Authentication failed. Bad challenge signature in token: " + authData.toString());
            return false;
        }

        // Auth success !

        // Force challenge to expire
        challenges.remove(authData.getChallenge());

        // Add token to store
        String token = authData.toString();
        checkedTokens.add(token);

        if(!isStoredToken) {
            // Save this new token to database
            try {
                accountService.addToken(token, authData.getPubkey());
            } catch (RuntimeException e) {
                // Log then continue
                log.error("Could not save auth token.", e);
            }
        }

        if (debug) log.debug(String.format("Authentication succeed for user with pubkey {%s}", authData.getPubkey().substring(0, 6)));

        return true;
    }

    public boolean canAuth(final String pubkey) throws DataNotFoundException {
        PersonVO person = personDao.getByPubkeyOrNull(pubkey);
        if (person == null) {
            throw new DataRetrievalFailureException(I18n.t("sumaris.error.account.notFound"));
        }

        // Cannot auth if user has been deleted or is disable
        StatusId status = StatusId.valueOf(person.getStatusId());
        if (StatusId.DISABLE.equals(status) || StatusId.DELETED.equals(status)) {
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

    protected String newChallenge() {
        byte[] randomNonce = cryptoService.getBoxRandomNonce();
        String randomNonceStr = CryptoUtils.encodeBase64(randomNonce);
        String randomHash = cryptoService.hash(randomNonceStr);
        return randomHash;
    }
}

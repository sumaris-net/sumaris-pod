package net.sumaris.server.http.security;

import com.google.common.collect.ImmutableList;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.service.administration.AccountService;
import net.sumaris.server.service.crypto.ServerCryptoService;
import net.sumaris.server.service.technical.ChangesPublisherServiceImpl;
import net.sumaris.server.vo.security.AuthDataVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

@Component("authService")
public class AuthServiceImpl implements AuthService {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(ChangesPublisherServiceImpl.class);

    private final List<Integer> ACCEPTED_USER_PROFILES = ImmutableList.of(UserProfileEnum.ADMIN.id, UserProfileEnum.USER.id, UserProfileEnum.SUPERVISOR.id);

    private ValidationExpiredCache challenges;
    private ValidationExpiredCache checkedTokens;

    @Autowired
    private ServerCryptoService cryptoService;

    @Autowired
    private AccountService accountService;

    @Autowired
    public AuthServiceImpl(SumarisServerConfiguration config) {
        this.challenges = new ValidationExpiredCache(config.getAuthChallengeLifeTime());
        this.checkedTokens = new ValidationExpiredCache(config.getAuthTokenLifeTimeInSeconds());
    }

    public boolean authenticate(String token) {

        // Check if present in cache
        if (checkedTokens.contains(token)) {
            return true;
        }

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
                log.warn("Authentication failed. Bad pubkey format: " + authData.getPubkey());
                return false;
            }
            if (!canAuth(authData.getPubkey())) {
                log.warn("Authentication failed. User is not allowed to authenticate: " + authData.getPubkey());
                return false;
            }
        } catch(DataNotFoundException e) {
            log.warn("Authentication failed. User not found: " + authData.getPubkey());
            return false;
        }

        // Check challenge exists and not expired
        if (!challenges.contains(authData.getChallenge())) {
            log.warn("Authentication failed. Challenge not found or expired: " + authData.getChallenge());
            return false;
        }

        // Check signature
        if (!cryptoService.verify(authData.getChallenge(), authData.getSignature(), authData.getPubkey())) {
            log.warn("Authentication failed. Bad challenge signature: " + authData.toString());
            return false;
        }

        // Success !
        // Force challenge to expire
        challenges.remove(authData.getChallenge());

        // Add token to store
        String token = authData.toString();
        checkedTokens.add(token);

        // Add token to database
        accountService.addToken(token, authData.getPubkey());

        return true;
    }

    public boolean canAuth(String pubkey) throws DataNotFoundException {
        List<Integer> userProfileIds = accountService.getProfileIdsByPubkey(pubkey);
        return CollectionUtils.containsAny(userProfileIds, ACCEPTED_USER_PROFILES);
    }

    public AuthDataVO createNewChallenge() {
        String challenge = newChallenge();
        String signature = cryptoService.sign(challenge);

        AuthDataVO result = new AuthDataVO(cryptoService.getServerPubkey(), challenge, signature);

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

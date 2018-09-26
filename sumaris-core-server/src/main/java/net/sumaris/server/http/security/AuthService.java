package net.sumaris.server.http.security;

import net.sumaris.server.vo.security.AuthDataVO;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface AuthService {

    boolean authenticate(String token);

    boolean authenticate(AuthDataVO authData);

    @Transactional(readOnly = true)
    boolean canAuth(String pubkey);

    @Transactional(readOnly = true)
    AuthDataVO createNewChallenge();
}

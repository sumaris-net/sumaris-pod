package net.sumaris.server.http.security;

import net.sumaris.server.vo.security.AuthDataVO;

public interface AuthService {

    boolean authenticate(String token);

    boolean authenticate(AuthDataVO authData);

    boolean canAuth(String pubkey);

    AuthDataVO createNewChallenge();
}

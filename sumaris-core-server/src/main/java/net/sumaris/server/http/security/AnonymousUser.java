package net.sumaris.server.http.security;

import net.sumaris.server.vo.security.AuthDataVO;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * Default anonymous user when no authentication action nor provided token
 *
 * Its role is ROLE_GUEST corresponding to {@link net.sumaris.core.model.referential.UserProfileEnum#GUEST}
 *
 * @author peck7 on 03/12/2018.
 */
class AnonymousUser extends AuthUser {

    static final AnonymousUser INSTANCE = new AnonymousUser();

    private AnonymousUser() {
        super(new AuthDataVO("anonymous", "anonymous", "anonymous"), AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

}

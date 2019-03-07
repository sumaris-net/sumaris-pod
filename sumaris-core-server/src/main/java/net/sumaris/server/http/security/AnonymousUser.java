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
public class AnonymousUser extends AuthUser {

    static final AnonymousUser INSTANCE = new AnonymousUser();

    static final String TOKEN = "anonymous";

    private AnonymousUser() {
        super(null,
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    @Override
    public String getUsername() {
        return TOKEN;
    }

    @Override
    public String getPassword() {
        return TOKEN;
    }
}

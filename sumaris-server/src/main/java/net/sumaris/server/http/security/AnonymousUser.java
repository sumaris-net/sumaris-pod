package net.sumaris.server.http.security;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

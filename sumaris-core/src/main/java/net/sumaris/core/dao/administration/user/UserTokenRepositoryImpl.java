package net.sumaris.core.dao.administration.user;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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
import net.sumaris.core.model.administration.user.UserToken;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import java.util.Date;

/**
 * @author peck7 on 20/08/2020.
 */
public class UserTokenRepositoryImpl
    extends SimpleJpaRepository<UserToken, Integer>
    implements UserTokenSpecifications {

    public UserTokenRepositoryImpl(EntityManager em) {
        super(UserToken.class, em);
    }

    @Override
    public void add(String token, String pubkey) {
        Preconditions.checkNotNull(token);
        Preconditions.checkNotNull(pubkey);

        UserToken entity = new UserToken();

        entity.setCreationDate(new Date());
        entity.setToken(token);
        entity.setPubkey(pubkey);

        // Save entityName
        saveAndFlush(entity);
    }

}

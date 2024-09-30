package net.sumaris.core.dao.administration.user;

/*-
 * #%L
 * SUMARiS:: Core
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

import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.vo.administration.user.PersonVO;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonRepository extends SumarisJpaRepository<Person, Integer, PersonVO>, PersonSpecifications {

    boolean existsByEmailMD5(String emailMD5);

    boolean existsByEmail(String email);

    boolean existsByPubkey(String pubkey);

    Person findByEmailMD5(String emailMD5);

    @Query("select min(p.avatar.id) from Person p where p.pubkey = :pubkey and p.avatar.id is not null")
    Optional<Integer> findAvatarIdByPubkey(@Param("pubkey") String pubkey);
}

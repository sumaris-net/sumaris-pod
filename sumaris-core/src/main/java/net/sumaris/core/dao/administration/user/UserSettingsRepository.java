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

import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.administration.user.UserSettings;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author peck7 on 20/08/2020.
 */
public interface UserSettingsRepository
    extends SumarisJpaRepository<UserSettings, Integer, UserSettingsVO>, UserSettingsSpecifications {

    @Modifying
    @Query("update UserSettings e set e.issuer = :newPubkey where e.issuer = :oldPubkey")
    void updateIssuerPubkey(@Param("oldPubkey") String oldPubkey, @Param("newPubkey") String newPubkey);
}

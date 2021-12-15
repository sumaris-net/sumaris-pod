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
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.administration.user.UserSettings;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.apache.commons.collections4.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import java.util.List;
import java.util.Optional;

/**
 * @author peck7 on 20/08/2020.
 */
public class UserSettingsRepositoryImpl
    extends SumarisJpaRepositoryImpl<UserSettings, Integer, UserSettingsVO>
    implements UserSettingsSpecifications {

    protected UserSettingsRepositoryImpl(EntityManager entityManager) {
        super(UserSettings.class, UserSettingsVO.class, entityManager);
        setCheckUpdateDate(false);
        setLockForUpdate(false);
    }

    @Override
    public Optional<UserSettingsVO> findByIssuer(String issuer) {
        List<UserSettings> settings = findAll(BindableSpecification.where((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get(UserSettingsVO.Fields.ISSUER), issuer)));
        if (CollectionUtils.isEmpty(settings)) {
            return Optional.empty();
        }
        if (settings.size() > 1)
            throw new NonUniqueResultException("should not happened because issuer has a unique index");
        return Optional.of(toVO(settings.get(0)));
    }

    @Override
    public UserSettingsVO save(UserSettingsVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getLocale(), "Missing 'settings.locale'");
        Preconditions.checkNotNull(vo.getLatLongFormat(), "Missing 'settings.latLongformat'");

        return super.save(vo);
    }

}

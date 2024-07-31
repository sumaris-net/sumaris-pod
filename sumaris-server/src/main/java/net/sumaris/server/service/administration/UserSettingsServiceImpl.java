package net.sumaris.server.service.administration;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 - 2022 SUMARiS Consortium
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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.administration.user.UserSettingsRepository;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("userSettingsService")
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    @Override
    public Optional<UserSettingsVO> findByIssuer(String issuer) {
        return userSettingsRepository.findByIssuer(issuer);
    }

    @Override
    public UserSettingsVO save(@NonNull UserSettingsVO settings) {
        Preconditions.checkNotNull(settings.getIssuer());
        return userSettingsRepository.save(settings);
    }

    @Override
    public void changePubkeyByIssuer(@NonNull String newIssuer, @NonNull String oldIssuer) {
        Optional<UserSettingsVO> userSettings = userSettingsRepository.findByIssuer(oldIssuer);
        if (userSettings.isPresent()) {
            userSettings.get().setIssuer(newIssuer);
            userSettingsRepository.save(userSettings.get());
        }
    }
}

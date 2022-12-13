package net.sumaris.server.service.administration;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.sumaris.core.dao.administration.user.UserSettingsRepository;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service("userSettingsService")
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;

    public Optional<UserSettingsVO> findByIssuer(String issuer) {
        return userSettingsRepository.findByIssuer(issuer);
    }

    @Override
    public UserSettingsVO save(@NonNull UserSettingsVO settings) {
        Preconditions.checkNotNull(settings.getIssuer());
        return userSettingsRepository.save(settings);
    }
}

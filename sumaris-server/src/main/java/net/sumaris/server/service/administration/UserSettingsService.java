package net.sumaris.server.service.administration;

import lombok.NonNull;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public interface UserSettingsService {

    @Transactional(readOnly = true)
    Optional<UserSettingsVO> findByIssuer(String issuer);

    UserSettingsVO save(@NonNull UserSettingsVO settings);
}

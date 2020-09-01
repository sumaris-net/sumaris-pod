package net.sumaris.core.dao.administration.user;

import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.administration.user.UserSettings;
import net.sumaris.core.vo.administration.user.UserSettingsVO;

/**
 * @author peck7 on 20/08/2020.
 */
public interface UserSettingsRepository
    extends SumarisJpaRepository<UserSettings, Integer, UserSettingsVO>, UserSettingsSpecifications {


}

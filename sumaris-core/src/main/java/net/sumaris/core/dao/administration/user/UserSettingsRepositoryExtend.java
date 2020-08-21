package net.sumaris.core.dao.administration.user;

import net.sumaris.core.vo.administration.user.UserSettingsVO;

/**
 * @author peck7 on 20/08/2020.
 */
public interface UserSettingsRepositoryExtend {

    UserSettingsVO getByIssuer(String issuer);

}

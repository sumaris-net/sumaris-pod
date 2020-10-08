package net.sumaris.core.dao.administration.user;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.administration.user.UserSettings;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.apache.commons.collections4.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import java.util.List;

/**
 * @author peck7 on 20/08/2020.
 */
public class UserSettingsRepositoryImpl
    extends SumarisJpaRepositoryImpl<UserSettings, Integer, UserSettingsVO>
    implements UserSettingsSpecifications {

    protected UserSettingsRepositoryImpl(EntityManager entityManager) {
        super(UserSettings.class, UserSettingsVO.class, entityManager);
    }

    @Override
    public UserSettingsVO getByIssuer(String issuer) {
        List<UserSettings> settings = findAll((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(UserSettingsVO.Fields.ISSUER), issuer));
        if (CollectionUtils.isNotEmpty(settings)) {
            if (settings.size() > 1)
                throw new NonUniqueResultException("should not happened because issuer has a unique index");
            return toVO(settings.get(0));
        }
        return null;
    }

    @Override
    public UserSettingsVO save(UserSettingsVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getLocale(), "Missing 'settings.locale'");
        Preconditions.checkNotNull(vo.getLatLongFormat(), "Missing 'settings.latLongformat'");

        return super.save(vo);
    }

}

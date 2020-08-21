package net.sumaris.core.dao.administration.user;

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
    implements UserTokenRepositoryExtend {

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

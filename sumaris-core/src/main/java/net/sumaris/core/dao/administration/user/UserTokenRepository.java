package net.sumaris.core.dao.administration.user;

import net.sumaris.core.model.administration.user.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author peck7 on 20/08/2020.
 */
public interface UserTokenRepository
    extends JpaRepository<UserToken, Integer>, UserTokenRepositoryExtend {

    interface TokenOnly {
        String getToken();
    }

    boolean existsByTokenAndPubkey(String token, String pubkey);

    List<TokenOnly> findTokenByPubkey(String pubkey);

}

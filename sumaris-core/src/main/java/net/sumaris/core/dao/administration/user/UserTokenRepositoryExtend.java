package net.sumaris.core.dao.administration.user;

/**
 * @author peck7 on 20/08/2020.
 */
public interface UserTokenRepositoryExtend {

    void add(String token, String pubkey);

}

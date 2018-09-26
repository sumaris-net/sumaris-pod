package net.sumaris.server.dao.technical;

import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository("entityDao")
public class EntityDaoImpl extends HibernateDaoSupport implements EntityDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(EntityDaoImpl.class);


    @Override
    public <T> T get(Class<? extends T> entityClass, Serializable id) {
        return super.get(entityClass, id);
    }

}

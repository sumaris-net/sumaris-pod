package net.sumaris.server.dao.technical;

import java.io.Serializable;

public interface EntityDao {

    <T> T get(Class<? extends T> entityClass, Serializable id);

}

package net.sumaris.core.dao.technical.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.LockModeType;
import java.io.Serializable;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@NoRepositoryBean
public interface SumarisJpaRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    <C extends Serializable> C load(Class<C> clazz, Serializable id);

    <C extends Serializable> C get(Class<? extends C> clazz, Serializable id);

    <C extends Serializable> C get(Class<? extends C> clazz, Serializable id, LockModeType lockModeType);

    T createEntity();
}

package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepository;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

import javax.persistence.LockModeType;
import java.io.Serializable;
import java.util.List;

/**
 * @author peck7 on 03/04/2020.
 */
@NoRepositoryBean
public interface ReferentialRepository<
    E extends IItemReferentialEntity,
    V extends IReferentialVO,
    F extends Serializable
    >
    extends SumarisJpaRepository<E, Integer> {

    List<V> findAll(@Nullable F filter);

    Page<V> findAll(@Nullable F filter, Pageable pageable);

    List<V> findAll(@Nullable F filter, @Nullable DataFetchOptions fetchOptions);

    Page<V> findAll(@Nullable F filter, Pageable pageable, @Nullable DataFetchOptions fetchOptions);

    Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions);

    Page<V> findAll(F filter, int offset, int size, String sortAttribute,
                    SortDirection sortDirection, DataFetchOptions fetchOptions);

    List<V> findAllAsVO(@Nullable Specification<E> spec);
    Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable);
    List<V> findAllAsVO(@Nullable Specification<E> spec, DataFetchOptions fetchOptions);
    Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable, DataFetchOptions fetchOptions);

    long count(F filter);

    V get(int id);

    V get(int id, DataFetchOptions fetchOptions);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    V save(V vo);

    Specification<E> toSpecification(@Nullable F filter);
}

package net.sumaris.core.dao.referential;

import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * Base Repository class for Referential entities
 *
 * @author peck7 on 03/04/2020.
 */
public class ReferentialRepositoryImpl<E extends IItemReferentialEntity, V extends IReferentialVO, F extends Serializable>
    extends SumarisJpaRepositoryImpl<E, Integer, V>
    implements ReferentialRepository<E, V, F>, ReferentialSpecifications {

    private static final Logger LOG = LoggerFactory.getLogger(ReferentialRepositoryImpl.class);

    private boolean checkUpdateDate = true;

    public ReferentialRepositoryImpl(Class<E> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
    }

    public boolean isCheckUpdateDate() {
        return checkUpdateDate;
    }

    public void setCheckUpdateDate(boolean checkUpdateDate) {
        this.checkUpdateDate = checkUpdateDate;
    }

    @Override
    public List<V> findAll(F filter) {
        return findAll(toSpecification(filter)).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable) {
        return findAll(toSpecification(filter), pageable)
            .map(this::toVO);
    }

    @Override
    public List<V> findAll(F filter, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter)).stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter), pageable)
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions) {
        return findAll(PageRequest.of(offset / size, size, Sort.Direction.fromString(sortDirection.toString()), sortAttribute))
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public Page<V> findAll(F filter, int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter), getPageable(offset, size, sortAttribute, sortDirection))
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public List<V> findAllAsVO(Specification<E> spec) {
        return super.findAll(spec).stream()
            .map(this::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAllAsVO(Specification<E> spec, Pageable pageable) {
        return super.findAll(spec, pageable)
            .map(this::toVO);
    }

    @Override
    public List<V> findAllAsVO(Specification<E> spec, DataFetchOptions fetchOptions) {
        return super.findAll(spec).stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAllAsVO(Specification<E> spec, Pageable pageable, DataFetchOptions fetchOptions) {
        return super.findAll(spec, pageable)
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public long count(F filter) {
        return count(toSpecification(filter));
    }

    @Override
    public V get(int id) {
        return toVO(this.getOne(id));
    }

    @Override
    public V get(int id, DataFetchOptions fetchOptions) {
        return toVO(this.getOne(id), fetchOptions);
    }

    @Override
    public V save(V vo) {
        E entity = toEntity(vo);

        if (checkUpdateDate) {
            // Check update date
            Daos.checkUpdateDateForUpdate(vo, entity);
        }

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        E savedEntity = save(entity);

        vo.setId(savedEntity.getId());

        return vo;
    }

    public E toEntity(V vo) {
        E entity;
        if (vo.getId() != null) {
            entity = getOne(vo.getId());
        } else {
            entity = createEntity();
        }
        toEntity(vo, entity, true);
        return entity;
    }

    public void toEntity(V source, E target, boolean copyIfNull) {

        // properties
        Beans.copyProperties(source, target);

    }

    public V toVO(E source) {
        return toVO(source, null);
    }

    public V toVO(E source, DataFetchOptions fetchOptions) {
        V target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    public void toVO(E source, V target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        Beans.copyProperties(source, target);
    }

    public V createVO() {
        try {
            return getVOClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Class<V> getVOClass() {
        throw new NotImplementedException("Not implemented yet. Should be override by subclass");
    }

    @Override
    public Specification<E> toSpecification(F filter) {
        throw new NotImplementedException("Not implemented yet. Should be override by subclass");
    }

}

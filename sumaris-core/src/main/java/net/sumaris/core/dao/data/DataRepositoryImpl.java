package net.sumaris.core.dao.data;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.administration.user.DepartmentDao;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 30/03/2020.
 */
@NoRepositoryBean
public class DataRepositoryImpl<E extends IDataEntity<ID>, ID extends Integer, V extends IDataVO<ID>, F extends Serializable>
    extends SumarisJpaRepositoryImpl<E, ID>
    implements DataRepository<E, ID, V, F> {

    /**
     * Logger.
     */
    private static final Logger log =
        LoggerFactory.getLogger(DataRepositoryImpl.class);

    private boolean checkUpdateDate = true;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private DepartmentDao departmentDao;

    public DataRepositoryImpl(Class<E> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
    }

    @Override
    public List<V> findAll(F filter) {
        return findAll(toSpecification(filter)).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<V> findAll(F filter, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter)).stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable) {
        return findAll(toSpecification(filter), pageable)
            .map(this::toVO);
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter), pageable)
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public List<V> findAll(F filter, net.sumaris.core.dao.technical.Page page, DataFetchOptions fetchOptions) {
        return findAll(filter, getPageable(page), fetchOptions)
                .stream().collect(Collectors.toList());
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
    public List<V> findAllVO(@Nullable Specification<E> spec) {
        return super.findAll(spec).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Page<V> findAllVO(@Nullable Specification<E> spec, Pageable pageable) {
        return super.findAll(spec, pageable).map(this::toVO);
    }

    @Override
    public Page<V> findAllVO(@Nullable Specification<E> spec, Pageable pageable, DataFetchOptions fetchOptions) {
        return super.findAll(spec, pageable).map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public List<V> findAllVO(@Nullable Specification<E> spec, DataFetchOptions fetchOptions) {
        return super.findAll(spec).stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public long count(F filter) {
        return count(toSpecification(filter));
    }

    @Override
    public V get(ID id) {
        return toVO(this.getOne(id));
    }

    @Override
    public V get(ID id, DataFetchOptions fetchOptions) {
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

        // Update VO
        vo.setId(savedEntity.getId());
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }

    @Override
    public V control(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public V validate(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public V unvalidate(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    public E toEntity(V vo) {
        Preconditions.checkNotNull(vo);
        E entity;
        if (vo.getId() != null) {
            entity = getOne(vo.getId());
        } else {
            entity = createEntity();
        }

        // Remember the entity's update date
        Date entityUpdateDate = entity.getUpdateDate();

        toEntity(vo, entity, true);

        // Restore the update date (can be override by Beans.copyProperties())
        entity.setUpdateDate(entityUpdateDate);

        return entity;
    }

    public void toEntity(V source, E target, boolean copyIfNull) {

        // Data properties
        DataDaos.copyDataProperties(getEntityManager(), source, target, copyIfNull);

        // Observers
        if (source instanceof IWithObserversEntity && target instanceof IWithObserversEntity) {
            copyObservers((IWithObserversEntity<Integer, PersonVO>)source, (IWithObserversEntity<Integer, Person>)target, copyIfNull);
        }
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

        target.setQualityFlagId(source.getQualityFlag().getId());

        // Vessel
        if (source instanceof IWithVesselEntity && target instanceof IWithVesselSnapshotEntity) {
            VesselSnapshotVO vesselSnapshot = new VesselSnapshotVO();
            vesselSnapshot.setId((Integer) ((IWithVesselEntity) source).getVessel().getId());
            ((IWithVesselSnapshotEntity<Integer, VesselSnapshotVO>) target).setVesselSnapshot(vesselSnapshot);
        }

        // Recorder department
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = departmentDao.toDepartmentVO(source.getRecorderDepartment());
            target.setRecorderDepartment(recorderDepartment);
        }

        // Observers
        if (source instanceof IWithObserversEntity && target instanceof IWithObserversEntity) {
            Set<Person> sourceObservers = ((IWithObserversEntity) source).getObservers();
            if ((fetchOptions == null || fetchOptions.isWithObservers()) && CollectionUtils.isNotEmpty(sourceObservers)) {
                Set<PersonVO> observers = sourceObservers.stream()
                    .map(personDao::toPersonVO)
                    .collect(Collectors.toSet());
                ((IWithObserversEntity<Integer, PersonVO>) target).setObservers(observers);
            }
        }
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
    public Specification<E> toSpecification(@Nullable F filter) {
        throw new NotImplementedException("Not implemented yet. Should be override by subclass");
    }

    /* -- protected methods -- */

    protected void copyVessel(IWithVesselSnapshotEntity<Integer, VesselSnapshotVO> source,
                              IWithVesselEntity<Integer, Vessel> target,
                              boolean copyIfNull) {
        DataDaos.copyVessel(getEntityManager(), source, target, copyIfNull);
    }

    protected void copyObservers(IWithObserversEntity<Integer, PersonVO> source,
                                 IWithObserversEntity<Integer, Person> target,
                                 boolean copyIfNull) {
        DataDaos.copyObservers(getEntityManager(), source, target, copyIfNull);
    }

    protected boolean isCheckUpdateDate() {
        return checkUpdateDate;
    }

    protected void setCheckUpdateDate(boolean checkUpdateDate) {
        this.checkUpdateDate = checkUpdateDate;
    }
}

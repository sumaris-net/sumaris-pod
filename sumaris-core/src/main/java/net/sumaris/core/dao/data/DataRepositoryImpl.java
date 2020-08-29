package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.google.common.base.Preconditions;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.Pageables;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.QualityFlagEnum;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.data.IWithObserversEntity;
import net.sumaris.core.model.data.IWithVesselEntity;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.filter.IDataFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 30/03/2020.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@NoRepositoryBean
public abstract class DataRepositoryImpl<E extends IDataEntity<ID>, ID extends Integer, V extends IDataVO<ID>, F extends IDataFilter>
    extends SumarisJpaRepositoryImpl<E, ID, V>
    implements DataRepository<E, ID, V, F>, DataSpecifications<E> {

    /**
     * Logger.
     */
    private static final Logger log =
        LoggerFactory.getLogger(DataRepositoryImpl.class);

    private String[] copyExcludeProperties = new String[]{IUpdateDateEntityBean.Fields.UPDATE_DATE};

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    protected DataRepositoryImpl(Class<E> domainClass, Class<V> voClass, EntityManager entityManager) {
        super(domainClass, voClass, entityManager);
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
        return findAll(filter, page.asPageable(), fetchOptions)
                .stream().collect(Collectors.toList());
    }

    @Override
    public Page<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions) {
        return findAll(Pageables.create(offset, size, sortAttribute, sortDirection))
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public Page<V> findAll(F filter, int offset, int size, String sortAttribute, SortDirection sortDirection, DataFetchOptions fetchOptions) {
        return findAll(toSpecification(filter), Pageables.create(offset, size, sortAttribute, sortDirection))
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
    public V control(V vo) {
        Preconditions.checkNotNull(vo);

        E entity = getOne(vo.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("E {%s} not found", vo.getId()));
        }

        // Check update date
        Daos.checkUpdateDateForUpdate(vo, entity);

        // Lock entityName
        lockForUpdate(entity);

        // TODO CONTROL PROCESS HERE
        Date controlDate = getDatabaseCurrentTimestamp();
        entity.setControlDate(controlDate);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        vo.setControlDate(controlDate);
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }

    @Override
    public V validate(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public V unvalidate(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public V qualify(V vo) {
        Preconditions.checkNotNull(vo);

        E entity = getOne(vo.getId());
        if (entity == null) {
            throw new DataRetrievalFailureException(String.format("E {%s} not found", vo.getId()));
        }

        // Check update date
        if (isCheckUpdateDate()) Daos.checkUpdateDateForUpdate(vo, entity);

        // Lock entityName
        if (isLockForUpdate()) lockForUpdate(entity);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        int qualityFlagId = vo.getQualityFlagId() != null ? vo.getQualityFlagId() : 0;

        // If not qualify, then remove the qualification date
        if (qualityFlagId == QualityFlagEnum.NOT_QUALIFED.getId()) {
            entity.setQualificationDate(null);
        }
        else {
            entity.setQualificationDate(newUpdateDate);
        }
        // Apply a find, because can return a null value (e.g. if id is not in the DB instance)
        entity.setQualityFlag(find(QualityFlag.class, qualityFlagId));

        // TODO UNVALIDATION PROCESS HERE
        // - insert into qualification history

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        vo.setQualificationDate(entity.getQualificationDate());
        vo.setQualityFlagId(entity.getQualityFlag() != null ? entity.getQualityFlag().getId() : 0);
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }

    public void toEntity(V source, E target, boolean copyIfNull) {
        DataDaos.copyDataProperties(getEntityManager(), source, target, copyIfNull, getCopyExcludeProperties());
    }

    public V toVO(E source) {
        return toVO(source, null);
    }

    public V toVO(E source, DataFetchOptions fetchOptions) {
        V target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    @Override
    public void toVO(E source, V target, boolean copyIfNull) {
        toVO(source, target, null, copyIfNull);
    }

    public void toVO(E source, V target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        Beans.copyProperties(source, target);

        // Quality flag
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Recorder department
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = departmentRepository.toVO(source.getRecorderDepartment());
            target.setRecorderDepartment(recorderDepartment);
        }

        // Vessel
        if (source instanceof IWithVesselEntity && target instanceof IWithVesselSnapshotEntity) {
            VesselSnapshotVO vesselSnapshot = new VesselSnapshotVO();
            vesselSnapshot.setId((Integer) ((IWithVesselEntity) source).getVessel().getId());
            ((IWithVesselSnapshotEntity<Integer, VesselSnapshotVO>) target).setVesselSnapshot(vesselSnapshot);
        }

        // Observers
        if (source instanceof IWithObserversEntity && target instanceof IWithObserversEntity) {
            Set<Person> sourceObservers = ((IWithObserversEntity) source).getObservers();
            if ((fetchOptions == null || fetchOptions.isWithObservers()) && CollectionUtils.isNotEmpty(sourceObservers)) {
                Set<PersonVO> observers = sourceObservers.stream()
                    .map(personRepository::toVO)
                    .collect(Collectors.toSet());
                ((IWithObserversEntity<Integer, PersonVO>) target).setObservers(observers);
            }
        }
    }

    protected Specification<E> toSpecification(F filter) {
        // default specification
        return BindableSpecification
            .where(hasRecorderDepartmentId(filter.getRecorderDepartmentId()));
    }

    /* -- protected methods -- */

    protected String[] getCopyExcludeProperties() {
        return this.copyExcludeProperties;
    }

    protected void setCopyExcludeProperties(String... excludedProperties) {
        this.copyExcludeProperties = excludedProperties;
    }

}

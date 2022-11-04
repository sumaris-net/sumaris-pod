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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.user.DepartmentRepository;
import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.BindableSpecification;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonFetchOptions;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.IDataFetchOptions;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import net.sumaris.core.vo.filter.IDataFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 30/03/2020.
 */
@NoRepositoryBean
@Slf4j
public abstract class DataRepositoryImpl<E extends IDataEntity<Integer>, V extends IDataVO<Integer>, F extends IDataFilter, O extends IDataFetchOptions>
    extends SumarisJpaRepositoryImpl<E, Integer, V>
    implements DataRepository<E, V, F, O>, DataSpecifications<Integer, E> {

    protected static PersonFetchOptions PERSON_FETCH_OPTIONS = PersonFetchOptions.builder()
        .withDepartment(true)
        .withUserProfiles(false)
        .build();

    private String[] copyExcludeProperties = new String[]{IUpdateDateEntity.Fields.UPDATE_DATE};

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    protected DataRepositoryImpl(Class<E> domainClass, Class<V> voClass, EntityManager entityManager) {
        super(domainClass, voClass, entityManager);
    }

    @Override
    public List<V> findAll(F filter) {
        return findAll(filter, (O)null);
    }

    @Override
    public List<V> findAll(F filter, O fetchOptions) {
        return findAll(toSpecification(filter, fetchOptions))
            .stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable) {
        return findAll(toSpecification(filter), pageable)
            .map(this::toVO);
    }

    @Override
    public Page<V> findAll(F filter, Pageable pageable, O fetchOptions) {
        return findAll(toSpecification(filter, fetchOptions), pageable)
            .map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public List<V> findAll(@Nullable F filter, @Nullable net.sumaris.core.dao.technical.Page page, O fetchOptions) {
        Specification<E> spec = filter != null ? toSpecification(filter, fetchOptions) : null;
        TypedQuery<E> query = getQuery(spec, page, getDomainClass());

        // Add hints
        configureQuery(query, fetchOptions);

        return streamQuery(query)
            .map(entity -> toVO(entity, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public List<V> findAll(@Nullable F filter, int offset, int size, String sortAttribute, SortDirection sortDirection, O fetchOptions) {
        Specification<E> spec = filter != null ? toSpecification(filter, fetchOptions) : null;
        TypedQuery<E> query = getQuery(spec, offset, size, sortAttribute, sortDirection, getDomainClass());

        // Add hints
        configureQuery(query, fetchOptions);

        return streamQuery(query)
            .map(entity -> toVO(entity, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public List<V> findAll(int offset, int size, String sortAttribute, SortDirection sortDirection, O fetchOptions) {
        return findAll(null, offset, size, sortAttribute, sortDirection, fetchOptions);
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
    public Page<V> findAllVO(@Nullable Specification<E> spec, Pageable pageable, O fetchOptions) {
        return super.findAll(spec, pageable).map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public List<V> findAllVO(@Nullable Specification<E> spec, O fetchOptions) {
        return super.findAll(spec)
            .stream()
            .map(e -> this.toVO(e, fetchOptions))
            .collect(Collectors.toList());
    }

    @Override
    public long count(F filter) {
        return count(toSpecification(filter));
    }

    @Override
    public Optional<V> findById(int id) {
        return findById(id, null);
    }

    @Override
    public Optional<V> findById(int id, O fetchOptions) {
        return super.findById(id).map(entity -> toVO(entity, fetchOptions));
    }

    @Override
    public V get(Integer id) {
        return toVO(this.getById(id));
    }

    @Override
    public V get(Integer id, O fetchOptions) {
        return toVO(this.getById(id), fetchOptions);
    }

    @Override
    public V control(V vo) {
        Preconditions.checkNotNull(vo);
        E entity = getById(vo.getId());

        // Check update date
        if (isCheckUpdateDate()) Daos.checkUpdateDateForUpdate(vo, entity);

        // Lock entityName
        if (isLockForUpdate()) lockForUpdate(entity);

        // TODO CONTROL PROCESS HERE
        Date newUpdateDate = getDatabaseCurrentDate();
        entity.setControlDate(newUpdateDate);
        entity.setQualificationComments(vo.getQualificationComments());

        // Update update_dt
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        getEntityManager().merge(entity);

        // Update source
        vo.setControlDate(newUpdateDate);
        vo.setUpdateDate(newUpdateDate);

        return vo;
    }

    @Override
    public V validate(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public V unValidate(V vo) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public V qualify(V vo) {
        Preconditions.checkNotNull(vo);
        E entity = getById(vo.getId());

        // Check update date
        if (isCheckUpdateDate()) Daos.checkUpdateDateForUpdate(vo, entity);

        // Lock entityName
        if (isLockForUpdate()) lockForUpdate(entity);

        // Update update_dt
        Date newUpdateDate = getDatabaseCurrentDate();
        entity.setUpdateDate(newUpdateDate);

        int qualityFlagId = vo.getQualityFlagId() != null ? vo.getQualityFlagId() : 0;

        // If not qualify, then remove the qualification date
        if (qualityFlagId == QualityFlagEnum.NOT_QUALIFIED.getId()) {
            entity.setQualificationDate(null);
        }
        else {
            entity.setQualificationDate(newUpdateDate);
        }
        // Apply a find (and NOT a getReference)
        // because can return a null value (e.g. if id is not in the DB instance)
        QualityFlag value = find(QualityFlag.class, qualityFlagId, LockModeType.NONE);
        entity.setQualityFlag(value);

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

    public V toVO(E source, O fetchOptions) {
        if (source == null) return null;
        V target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    @Override
    public void toVO(E source, V target, boolean copyIfNull) {
        toVO(source, target, null, copyIfNull);
    }

    @SuppressWarnings("unchecked")
    public void toVO(E source, V target, O fetchOptions, boolean copyIfNull) {
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
            vesselSnapshot.setId(((IWithVesselEntity<Integer, Vessel>) source).getVessel().getId());
            ((IWithVesselSnapshotEntity<Integer, VesselSnapshotVO>) target).setVesselSnapshot(vesselSnapshot);
        }

        // Observers
        if (source instanceof IWithObserversEntity && target instanceof IWithObserversEntity) {
            if (fetchOptions == null || fetchOptions.isWithObservers()) {
                Set<Person> sourceObservers = ((IWithObserversEntity<Integer, Person>) source).getObservers();
                if (CollectionUtils.isNotEmpty(sourceObservers)) {
                    Set<PersonVO> observers = sourceObservers.stream()
                            .map(Person::getId)
                            .map(personRepository::findVOById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toSet());
                    ((IWithObserversEntity<Integer, PersonVO>) target).setObservers(observers);
                }
            }
        }
    }


    protected final Specification<E> toSpecification(F filter) {
        return toSpecification(filter, null);
    }

    protected Specification<E> toSpecification(F filter, O fetchOptions) {
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

    protected void configureQuery(TypedQuery<E> query, @Nullable O fetchOptions) {
        // Can be override by subclasses
    }

}

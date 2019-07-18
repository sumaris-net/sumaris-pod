package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import net.sumaris.core.dao.administration.programStrategy.ProgramDao;
import net.sumaris.core.dao.administration.user.DepartmentDao;
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.jpa.SumarisJpaRepositoryImpl;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.*;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramFetchOptions;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import net.sumaris.core.vo.filter.IRootDataFilter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RootDataRepositoryImpl<
        E extends IRootDataEntity<ID>,
        ID extends Integer,
        V extends IRootDataVO<ID>,
        F extends IRootDataFilter
        >
        extends SumarisJpaRepositoryImpl<E, Integer>
        implements RootDataRepository<E, ID, V, F> {

    /**
     * Logger.
     */
    private static final Logger log =
            LoggerFactory.getLogger(RootDataRepositoryImpl.class);

    @Autowired
    private PersonDao personDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private ProgramDao programDao;

    public RootDataRepositoryImpl(Class<E> domainClass,
                                  EntityManager entityManager) {
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
                .map(e -> this.toVO(e));
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
    public List<V> findAllAsVO(@Nullable Specification<E> spec) {
        return super.findAll(spec).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable) {
        return super.findAll(spec, pageable).map(this::toVO);
    }

    @Override
    public Page<V> findAllAsVO(@Nullable Specification<E> spec, Pageable pageable, DataFetchOptions fetchOptions) {
        return super.findAll(spec, pageable).map(e -> this.toVO(e, fetchOptions));
    }

    @Override
    public List<V> findAllAsVO(@Nullable Specification<E> spec, DataFetchOptions fetchOptions) {
        return super.findAll(spec).stream()
                .map(e -> this.toVO(e, fetchOptions))
                .collect(Collectors.toList());
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

        if (entity.getId() == null) {
            entity.setCreationDate(new Date());
        }

        // Check update date
        Daos.checkUpdateDateForUpdate(vo, entity);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        E savedEntity = save(entity);

        vo.setId(savedEntity.getId());

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
        E entity;
        if (vo.getId() != null) {
            entity = getOne(vo.getId());
        } else {
            entity = createEntity();
        }
        toEntity(vo, entity, true);
        return entity;
    }

    //@Override
    public void toEntity(V source, E target, boolean copyIfNull) {
        DataDaos.copyRootDataProperties(getEntityManager(), source, target, copyIfNull);

        // Observers
        if (source instanceof IWithObserversEntity && target instanceof IWithObserversEntity) {
            Set<PersonVO> sourceObservers = ((IWithObserversEntity) source).getObservers();
            Set<Person> targetObservers = SetUtils.emptyIfNull(((IWithObserversEntity) target).getObservers());
            if (copyIfNull || sourceObservers != null) {
                if (CollectionUtils.isEmpty(sourceObservers)) {
                    if (CollectionUtils.isNotEmpty(targetObservers)) {
                        targetObservers.clear();
                    }
                } else {
                    Map<Integer, Person> observersToRemove = Beans.splitById(targetObservers);
                    sourceObservers.stream()
                            .map(IEntity::getId)
                            .forEach(personId -> {
                                if (observersToRemove.remove(personId) == null) {
                                    // Add new item
                                    targetObservers.add(load(Person.class, personId));
                                }
                            });

                    // Remove deleted tableNames
                    targetObservers.removeAll(observersToRemove.values());
                }
            }
        }
    }


    //@Override
    public V toVO(E source) {
        return toVO(source, null);
    }

    //@Override
    public V toVO(E source, DataFetchOptions fetchOptions) {
        V target = createVO();
        toVO(source, target, fetchOptions, true);
        return target;
    }

    //@Override
    public void toVO(E source, V target, DataFetchOptions fetchOptions, boolean copyIfNull) {
        Beans.copyProperties(source, target);

        target.setQualityFlagId(source.getQualityFlag().getId());

        // Program
        if (source.getProgram() != null) {
            target.setProgram(programDao.toProgramVO(source.getProgram(),
                    ProgramFetchOptions.builder().withProperties(false)
                            .build()));
        }

        // Vessel
        if (source instanceof IWithVesselEntity && target instanceof IWithVesselFeaturesEntity) {
            VesselFeaturesVO vesselFeatures = new VesselFeaturesVO();
            vesselFeatures.setVesselId((Integer) ((IWithVesselEntity) source).getVessel().getId());
            ((IWithVesselFeaturesEntity<Integer, VesselFeaturesVO>) target).setVesselFeatures(vesselFeatures);
        }

        // Recorder department
        if (fetchOptions == null || fetchOptions.isWithRecorderDepartment()) {
            DepartmentVO recorderDepartment = departmentDao.toDepartmentVO(source.getRecorderDepartment());
            target.setRecorderDepartment(recorderDepartment);
        }

        // Recorder person
        if ((fetchOptions == null || fetchOptions.isWithRecorderPerson()) && source.getRecorderPerson() != null) {
            PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
            target.setRecorderPerson(recorderPerson);
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

    //@Override
    public V createVO() {
        try {
            return getVOClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //@Override
    public Class<V> getVOClass() {
        throw new NotImplementedException("Not implemented yet. Should be override by subclass");
    }

    @Override
    public Specification<E> toSpecification(@Nullable F filter) {
        throw new NotImplementedException("Not implemented yet. Should be override by subclass");
    }

    /* -- protected methods -- */


    protected void copyVessel(IWithVesselFeaturesEntity<Integer, VesselFeaturesVO> source,
                              IWithVesselEntity<Integer, Vessel> target,
                              boolean copyIfNull) {
        DataDaos.copyVessel(getEntityManager(), source, target, copyIfNull);
    }


    protected void copyObservers(IWithObserversEntity<Integer, PersonVO> source,
                                 IWithObserversEntity<Integer, Person> target,
                                 boolean copyIfNull) {
        DataDaos.copyObservers(getEntityManager(), source, target, copyIfNull);
    }


}

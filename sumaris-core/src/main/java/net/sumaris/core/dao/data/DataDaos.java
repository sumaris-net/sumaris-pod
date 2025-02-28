package net.sumaris.core.dao.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.collect.Sets;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.IDataVO;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.data.VesselSnapshotVO;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public class DataDaos extends Daos {

    protected DataDaos() {
        super();
        // helper class does not instantiate
    }

    public static <T extends Serializable> void copyRootDataProperties(EntityManager entityManager,
                                                                       IRootDataVO<T> source,
                                                                       IRootDataEntity<T> target,
                                                                       boolean copyIfNull,
                                                                       String... excludeProperty) {
        copyDataProperties(entityManager, source, target, copyIfNull, excludeProperty);

        // Recorder person
        copyRecorderPerson(entityManager, source, target, copyIfNull);

        // Program
        copyProgram(entityManager, source, target, copyIfNull);

        // Vessel (optional on a root - e.g. ObservedLocation)
        if (source instanceof IWithVesselSnapshotEntity && target instanceof IWithVesselEntity) {
            copyVessel(entityManager, (IWithVesselSnapshotEntity<T, VesselSnapshotVO>)source, (IWithVesselEntity<T, Vessel>)target, copyIfNull);
        }

        // Observers (optional on a root)
        if (source instanceof IWithObserversEntity && target instanceof IWithObserversEntity) {
            copyObservers(entityManager, (IWithObserversEntity<T, PersonVO>)source, (IWithObserversEntity<T, Person>)target, copyIfNull);
        }
    }

    public static <T extends Serializable> void copyDataProperties(EntityManager entityManager,
                                                                   IDataVO<T> source,
                                                                   IDataEntity<T> target,
                                                                   boolean copyIfNull,
                                                                   String... excludeProperty) {

        Beans.copyProperties(source, target, excludeProperty);

        // Recorder department
        copyRecorderDepartment(entityManager, source, target, copyIfNull);

        // Quality flag
        copyQualityFlag(entityManager, source, target, copyIfNull);

    }

    public static <T extends Serializable> void copyRecorderDepartment(EntityManager entityManager,
                                                                       IWithRecorderDepartmentEntity<T, DepartmentVO> source,
                                                                       IWithRecorderDepartmentEntity<T, Department> target,
                                                                       boolean copyIfNull) {
        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            } else {
                target.setRecorderDepartment(load(entityManager, Department.class, source.getRecorderDepartment().getId()));
            }
        }
    }

    public static <T extends Serializable> void copyRecorderPerson(EntityManager entityManager,
                                                                   IWithRecorderPersonEntity<T, PersonVO> source,
                                                                   IWithRecorderPersonEntity<T, Person> target,
                                                                   boolean copyIfNull) {
        if (copyIfNull || source.getRecorderPerson() != null) {
            if (source.getRecorderPerson() == null || source.getRecorderPerson().getId() == null) {
                target.setRecorderPerson(null);
            } else {
                target.setRecorderPerson(load(entityManager, Person.class, source.getRecorderPerson().getId()));
            }
        }
    }

    public static <T extends Serializable> void copyObservers(EntityManager entityManager,
                                                              IWithObserversEntity<T, PersonVO> source,
                                                              IWithObserversEntity<T, Person> target,
                                                              boolean copyIfNull) {
        // Observers
        if (copyIfNull || source.getObservers() != null) {
            if (CollectionUtils.isEmpty(source.getObservers())) {
                if (target.getId() != null && CollectionUtils.isNotEmpty(target.getObservers())) {
                    target.getObservers().clear();
                }
            } else {
                Set<Person> observers = target.getId() != null ? target.getObservers() : Sets.newHashSet();
                Map<Integer, Person> observersToRemove = Beans.splitById(observers);
                source.getObservers().stream()
                        .filter(Objects::nonNull)
                        .map(IEntity::getId)
                        .filter(Objects::nonNull)
                        .forEach(personId -> {
                            if (observersToRemove.remove(personId) == null) {
                                // Add new item
                                observers.add(load(entityManager, Person.class, personId));
                            }
                        });

                // Remove remaining observers (if any)
                observers.removeAll(observersToRemove.values());

                // affect observers
                target.setObservers(observers);
            }
        }
    }

    public static <T extends Serializable> void copyVessel(EntityManager entityManager,
                                                           IWithVesselSnapshotEntity<T, ? extends VesselSnapshotVO> source,
                                                           IWithVesselEntity<T, Vessel> target,
                                                           boolean copyIfNull) {
        // Vessel
        Integer vesselId = source.getVesselId() != null ? source.getVesselId() : (source.getVesselSnapshot() != null ? source.getVesselSnapshot().getId() : null);
        if (copyIfNull || vesselId != null) {
            if (vesselId == null) {
                target.setVessel(null);
            } else {
                target.setVessel(load(entityManager, Vessel.class, vesselId));
            }
        }
    }

    public static <T extends Serializable> void copyQualityFlag(EntityManager entityManager,
                                                                IDataVO<T> source,
                                                                IDataEntity<T> target,
                                                                boolean copyIfNull) {
        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                int defaultQualityFlagId = SumarisConfiguration.getInstance().getDefaultQualityFlagId();
                source.setQualityFlagId(defaultQualityFlagId);
                target.setQualityFlag(load(entityManager, QualityFlag.class, defaultQualityFlagId));
            } else {
                target.setQualityFlag(load(entityManager, QualityFlag.class, source.getQualityFlagId()));
            }
        }
    }

    public static <T extends Serializable> void copyProgram(EntityManager entityManager,
                                                            IRootDataVO<T> source,
                                                            IRootDataEntity<T> target,
                                                            boolean copyIfNull) {
        // Program
        if (copyIfNull || (source.getProgram() != null && (source.getProgram().getId() != null || source.getProgram().getLabel() != null))) {
            if (source.getProgram() == null || (source.getProgram().getId() == null && source.getProgram().getLabel() == null)) {
                target.setProgram(null);
            }
            // Load by id
            else if (source.getProgram().getId() != null) {
                target.setProgram(load(entityManager, Program.class, source.getProgram().getId()));
            }
            // Load by label
            else {
                if (copyIfNull) {
                    throw new SumarisTechnicalException("Missing program.id !");
                }
                else {
                    target.setProgram(new Program());
                    Beans.copyProperties(source.getProgram(), target.getProgram());
                }
            }
        }
    }

    /* -- protected method -- */

    protected static <C> C load(EntityManager em, Class<? extends C> clazz, Serializable id) {
        return em.unwrap(Session.class).load(clazz, id);
    }

}

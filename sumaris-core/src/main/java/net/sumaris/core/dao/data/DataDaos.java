package net.sumaris.core.dao.data;

import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.model.IDataEntity;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.IRootDataVO;
import net.sumaris.core.vo.data.IWithVesselFeaturesVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import org.apache.commons.collections4.CollectionUtils;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.Map;

/**
 * Helper class for data DAOs
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class DataDaos extends Daos {

    protected DataDaos() {
        super();
        // helper class does not instantiate
    }

    public static <T extends Serializable> void copyDataRootProperties(EntityManager em,
                                                                       IRootDataVO<T> source,
                                                                       IRootDataEntity<T> target,
                                                                       boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Program
        if (copyIfNull || (source.getProgram() != null && (source.getProgram().getId() != null || source.getProgram().getLabel() != null))) {
            if (source.getProgram() == null || (source.getProgram().getId() == null && source.getProgram().getLabel() == null)) {
                target.setProgram(null);
            }
            // Load by id
            else if (source.getProgram().getId() != null){
                target.setProgram(em.getReference(Program.class, source.getProgram().getId()));
            }
            // Load by label
            else {
                throw new SumarisTechnicalException("Missing program.id !");
            }
        }

        // Recorder department
        copyRecorderDepartment(em, source, target, copyIfNull);

        // Recorder person
        copyRecorderPerson(em, source, target, copyIfNull);

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(em.getReference(QualityFlag.class, SumarisConfiguration.getInstance().getDefaultQualityFlagId()));
            }
            else {
                target.setQualityFlag(em.getReference(QualityFlag.class, source.getQualityFlagId()));
            }
        }

    }

    public static <T extends Serializable> void copyRecorderDepartment(EntityManager em,
                                                                       IWithRecorderDepartmentEntityBean<T, DepartmentVO> source,
                                                                       IWithRecorderDepartmentEntityBean<T, Department> target,
                                                                       boolean copyIfNull) {
        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(em.getReference(Department.class, source.getRecorderDepartment().getId()));
            }
        }
    }

    public static <T extends Serializable> void copyRecorderPerson(EntityManager em,
                                          IWithRecorderPersonEntityBean<T, PersonVO> source,
                                          IWithRecorderPersonEntityBean<T, Person> target,
                                          boolean copyIfNull) {
        if (copyIfNull || source.getRecorderPerson() != null) {
            if (source.getRecorderPerson() == null || source.getRecorderPerson().getId() == null) {
                target.setRecorderPerson(null);
            }
            else {
                target.setRecorderPerson(em.getReference(Person.class, source.getRecorderPerson().getId()));
            }
        }
    }

    public static <T extends Serializable> void copyObservers(EntityManager em,
                                     IWithObserversEntityBean<T, PersonVO> source,
                                     IWithObserversEntityBean<T, Person> target,
                                     boolean copyIfNull) {
        // Observers
        if (copyIfNull || source.getObservers() != null) {
            if (CollectionUtils.isEmpty(source.getObservers())) {
                if (CollectionUtils.isNotEmpty(target.getObservers())) {
                    target.getObservers().clear();
                }
            }
            else {
                Map<Integer, Person> observersToRemove = Beans.splitById(target.getObservers());
                source.getObservers().stream()
                        .map(IDataEntity::getId)
                        .forEach(personId -> {
                            if (observersToRemove.remove(personId) == null) {
                                // Add new item
                                target.getObservers().add(em.getReference(Person.class, personId));
                            }
                        });

                // Remove deleted items
                target.getObservers().removeAll(observersToRemove.values());
            }
        }
    }

    public static <T extends Serializable> void copyVessel(EntityManager em,
                                  IWithVesselFeaturesVO<T, ? extends VesselFeaturesVO> source,
                                  IWithVesselEntity<T> target,
                                  boolean copyIfNull) {
        // Vessel
        if (copyIfNull || (source.getVesselFeatures() != null && source.getVesselFeatures().getVesselId() != null)) {
            if (source.getVesselFeatures() == null || source.getVesselFeatures().getVesselId() == null) {
                target.setVessel(null);
            }
            else {
                target.setVessel(em.getReference(Vessel.class, source.getVesselFeatures().getVesselId()));
            }
        }
    }

}

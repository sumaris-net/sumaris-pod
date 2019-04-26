package net.sumaris.core.dao.data;

import com.google.common.collect.Sets;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.exception.SumarisTechnicalException;
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
import net.sumaris.core.vo.data.IWithVesselFeaturesVO;
import net.sumaris.core.vo.data.VesselFeaturesVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public abstract class BaseDataDaoImpl extends HibernateDaoSupport {

    public <T extends Serializable> void copyRootDataProperties(IRootDataVO<T> source,
                                                                IRootDataEntity<T> target,
                                                                boolean copyIfNull) {
        copyDataProperties(source, target, copyIfNull);

        // Recorder person
        copyRecorderPerson(source, target, copyIfNull);

        // Program
        copyProgram(source, target, copyIfNull);
    }

    public <T extends Serializable> void copyDataProperties(IDataVO<T> source,
                                                            IDataEntity<T> target,
                                                            boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Recorder department
        copyRecorderDepartment(source, target, copyIfNull);

        // Quality flag
        copyQualityFlag(source, target, copyIfNull);
    }

    public <T extends Serializable> void copyRecorderDepartment(IWithRecorderDepartmentEntityBean<T, DepartmentVO> source,
                                                                IWithRecorderDepartmentEntityBean<T, Department> target,
                                                                boolean copyIfNull) {
        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }
    }

    public <T extends Serializable> void copyRecorderPerson(IWithRecorderPersonEntityBean<T, PersonVO> source,
                                                            IWithRecorderPersonEntityBean<T, Person> target,
                                                            boolean copyIfNull) {
        if (copyIfNull || source.getRecorderPerson() != null) {
            if (source.getRecorderPerson() == null || source.getRecorderPerson().getId() == null) {
                target.setRecorderPerson(null);
            }
            else {
                target.setRecorderPerson(load(Person.class, source.getRecorderPerson().getId()));
            }
        }
    }

    public <T extends Serializable> void copyObservers(IWithObserversEntityBean<T, PersonVO> source,
                                                       IWithObserversEntityBean<T, Person> target,
                                                       boolean copyIfNull) {
        // Observers
        if (copyIfNull || source.getObservers() != null) {
            if (CollectionUtils.isEmpty(source.getObservers())) {
                if (target.getId() != null && CollectionUtils.isNotEmpty(target.getObservers())) {
                    target.getObservers().clear();
                }
            }
            else {
                Set<Person> observers = target.getId() != null ? target.getObservers() : Sets.newHashSet();
                Map<Integer, Person> observersToRemove = Beans.splitById(observers);
                source.getObservers().stream()
                        .map(net.sumaris.core.dao.technical.model.IDataEntity::getId)
                        .filter(Objects::nonNull)
                        .forEach(personId -> {
                            if (observersToRemove.remove(personId) == null) {
                                // Add new item
                                observers.add(load(Person.class, personId));
                            }
                        });

                // Remove deleted items
                if (MapUtils.isNotEmpty(observersToRemove)) {
                    observers.removeAll(observersToRemove.values());
                }
                target.setObservers(observers);
            }
        }
    }

    public <T extends Serializable> void copyVessel(IWithVesselFeaturesVO<T, ? extends VesselFeaturesVO> source,
                                                    IWithVesselEntity<T> target,
                                                    boolean copyIfNull) {
        // Vessel
        if (copyIfNull || (source.getVesselFeatures() != null && source.getVesselFeatures().getVesselId() != null)) {
            if (source.getVesselFeatures() == null || source.getVesselFeatures().getVesselId() == null) {
                target.setVessel(null);
            }
            else {
                target.setVessel(load(Vessel.class, source.getVesselFeatures().getVesselId()));
            }
        }
    }

    public <T extends Serializable> void copyQualityFlag(IDataVO<T> source,
                                                         IDataEntity<T> target,
                                                         boolean copyIfNull) {
        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, SumarisConfiguration.getInstance().getDefaultQualityFlagId()));
            }
            else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }
    }
    
    public <T extends Serializable> void copyProgram(IRootDataVO<T> source,
                                                     IRootDataEntity<T> target,
                                                     boolean copyIfNull) {
        // Program
        if (copyIfNull || (source.getProgram() != null && (source.getProgram().getId() != null || source.getProgram().getLabel() != null))) {
            if (source.getProgram() == null || (source.getProgram().getId() == null && source.getProgram().getLabel() == null)) {
                target.setProgram(null);
            }
            // Load by id
            else if (source.getProgram().getId() != null) {
                target.setProgram(load(Program.class, source.getProgram().getId()));
            }
            // Load by label
            else {
                throw new SumarisTechnicalException("Missing program.id !");
            }
        }
    }
}

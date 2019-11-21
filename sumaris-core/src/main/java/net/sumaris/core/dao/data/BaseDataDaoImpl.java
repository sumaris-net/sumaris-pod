package net.sumaris.core.dao.data;

import com.google.common.collect.Sets;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IEntity;
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
import net.sumaris.core.vo.data.VesselSnapshotVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.criteria.*;
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

        // Copy root data properties without exception
        copyRootDataProperties(source, target, copyIfNull, (String) null);
    }

    public <T extends Serializable> void copyRootDataProperties(IRootDataVO<T> source,
                                                                IRootDataEntity<T> target,
                                                                boolean copyIfNull,
                                                                String... exceptProperties) {

        // Copy data properties except some properties if specified
        copyDataProperties(source, target, copyIfNull, exceptProperties);

        // Recorder person
        copyRecorderPerson(source, target, copyIfNull);

        // Program
        copyProgram(source, target, copyIfNull);
    }

    public <T extends Serializable> void copyDataProperties(IDataVO<T> source,
                                                            IDataEntity<T> target,
                                                            boolean copyIfNull) {

        // Copy data properties without exception
        copyDataProperties(source, target, copyIfNull, (String) null);
    }

    public <T extends Serializable> void copyDataProperties(IDataVO<T> source,
                                                            IDataEntity<T> target,
                                                            boolean copyIfNull,
                                                            String... exceptProperties) {

        // Copy bean properties except some properties if specified
        Beans.copyProperties(source, target, exceptProperties);

        // Recorder department
        copyRecorderDepartment(source, target, copyIfNull);

        // Quality flag
        copyQualityFlag(source, target, copyIfNull);
    }

    public <T extends Serializable> void copyRecorderDepartment(IWithRecorderDepartmentEntity<T, DepartmentVO> source,
                                                                IWithRecorderDepartmentEntity<T, Department> target,
                                                                boolean copyIfNull) {
        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            } else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }
    }

    public <T extends Serializable> void copyRecorderPerson(IWithRecorderPersonEntity<T, PersonVO> source,
                                                            IWithRecorderPersonEntity<T, Person> target,
                                                            boolean copyIfNull) {
        if (copyIfNull || source.getRecorderPerson() != null) {
            if (source.getRecorderPerson() == null || source.getRecorderPerson().getId() == null) {
                target.setRecorderPerson(null);
            } else {
                target.setRecorderPerson(load(Person.class, source.getRecorderPerson().getId()));
            }
        }
    }

    public <T extends Serializable> void copyObservers(IWithObserversEntity<T, PersonVO> source,
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
                        .map(IEntity::getId)
                        .filter(Objects::nonNull)
                        .forEach(personId -> {
                            if (observersToRemove.remove(personId) == null) {
                                // Add new item
                                observers.add(load(Person.class, personId));
                            }
                        });

                // Remove deleted tableNames
                if (MapUtils.isNotEmpty(observersToRemove)) {
                    observers.removeAll(observersToRemove.values());
                }
                target.setObservers(observers);
            }
        }
    }

    public void copyVessel(IWithVesselSnapshotEntity<Integer, VesselSnapshotVO> source,
                           IWithVesselEntity<Integer, Vessel> target,
                           boolean copyIfNull) {
        // Vessel
        if (copyIfNull || (source.getVesselSnapshot() != null && source.getVesselSnapshot().getId() != null)) {
            if (source.getVesselSnapshot() == null || source.getVesselSnapshot().getId() == null) {
                target.setVessel(null);
            } else {
                target.setVessel(load(Vessel.class, source.getVesselSnapshot().getId()));
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
            } else {
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

    /**
     * Add a orderBy on query
     *
     * @param query the query
     * @param cb criteria builder
     * @param root the root of the query
     * @param sortAttribute the sort attribute (can be a nested attribute)
     * @param sortDirection the direction
     * @param <T> type of query
     * @return the query itself
     */
    protected <T> CriteriaQuery<T> addSorting(CriteriaQuery<T> query,
                                              CriteriaBuilder cb,
                                              Root<?> root, String sortAttribute, SortDirection sortDirection) {
        // Add sorting
        if (StringUtils.isNotBlank(sortAttribute)) {
            Expression<?> sortExpression = composePath(root, sortAttribute);
            query.orderBy(SortDirection.DESC.equals(sortDirection) ?
                cb.desc(sortExpression) :
                cb.asc(sortExpression)
            );
        }
        return query;
    }

    /**
     * Compose a Path from root, accepting nested property name
     *
     * @param root the root expression
     * @param attributePath the attribute path, can contains '.'
     * @param <X> Type of Path
     * @return the composed Path
     */
    protected <X> Path<X> composePath(Root<?> root, String attributePath) {

        String[] paths = attributePath.split("\\.");
        From<?, ?> from = root; // starting from root
        Path<X> result = null;

        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];

            if (i == paths.length - 1) {
                // last path, get it
                result = from.get(path);
            } else {
                // need a join
                from = from.join(path);
            }
        }

        return result;
    }
}

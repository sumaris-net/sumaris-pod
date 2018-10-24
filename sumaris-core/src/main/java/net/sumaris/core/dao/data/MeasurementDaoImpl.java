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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import net.sumaris.core.dao.referential.PmfmDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.data.batch.Batch;
import net.sumaris.core.model.data.batch.BatchQuantificationMeasurement;
import net.sumaris.core.model.data.batch.BatchSortingMeasurement;
import net.sumaris.core.model.data.measure.*;
import net.sumaris.core.model.data.sample.Sample;
import net.sumaris.core.model.data.sample.SampleMeasurement;
import net.sumaris.core.model.referential.Pmfm;
import net.sumaris.core.model.referential.QualitativeValue;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.referential.ParameterValueType;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository("measurementDao")
public class MeasurementDaoImpl extends HibernateDaoSupport implements MeasurementDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(MeasurementDaoImpl.class);

    static {
        I18n.n("sumaris.persistence.table.vesselUseMeasurement");
        I18n.n("sumaris.persistence.table.gearUseMeasurement");
        I18n.n("sumaris.persistence.table.physicalGearMeasurement");
        I18n.n("sumaris.persistence.table.sampleMeasurement");
        I18n.n("sumaris.persistence.table.batchSortingMeasurement");
        I18n.n("sumaris.persistence.table.batchQuantificationMeasurement");
    }

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PmfmDao pmfmDao;


    @Override
    public List<MeasurementVO> getVesselUseMeasurementsByTripId(int tripId) {
        return getMeasurementsByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.PROPERTY_TRIP,
                tripId,
                VesselUseMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    public List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId) {
        return getMeasurementsByParentId(PhysicalGearMeasurement.class,
                PhysicalGearMeasurement.PROPERTY_PHYSICAL_GEAR,
                physicalGearId,
                PhysicalGearMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MeasurementVO> getVesselUseMeasurementsByOperationId(int operationId) {
        return getMeasurementsByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.PROPERTY_OPERATION,
                operationId,
                VesselUseMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MeasurementVO> getGearUseMeasurementsByOperationId(int operationId) {
        return getMeasurementsByParentId(GearUseMeasurement.class,
                GearUseMeasurement.PROPERTY_OPERATION,
                operationId,
                GearUseMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MeasurementVO> getSampleMeasurements(int sampleId) {
        return getMeasurementsByParentId(SampleMeasurement.class,
                SampleMeasurement.PROPERTY_SAMPLE,
                sampleId,
                SampleMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, String> getSampleMeasurementsMap(int sampleId) {
        return getMeasurementsMapByParentId(SampleMeasurement.class,
                SampleMeasurement.PROPERTY_SAMPLE,
                sampleId,
                SampleMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, String> getBatchSortingMeasurementsMap(int batchId) {
        return getMeasurementsMapByParentId(BatchSortingMeasurement.class,
                BatchSortingMeasurement.PROPERTY_BATCH,
                batchId,
                BatchSortingMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, String> getBatchQuantificationMeasurementsMap(int batchId) {
        return getMeasurementsMapByParentId(BatchQuantificationMeasurement.class,
                BatchQuantificationMeasurement.PROPERTY_BATCH,
                batchId,
                BatchQuantificationMeasurement.PROPERTY_ID
        );
    }

    @Override
    public <T extends IMeasurementEntity>  MeasurementVO toMeasurementVO(T source) {
        if (source == null) return null;

        MeasurementVO target = new MeasurementVO();

        Beans.copyProperties(source, target);

        // Pmfm Id
        if (source.getPmfm() != null) {
            target.setPmfmId(source.getPmfm().getId());
        }

        // Qualitative value
        if (source.getQualitativeValue() != null){
            ReferentialVO qv = referentialDao.toReferentialVO(source.getQualitativeValue());
            target.setQualitativeValue(qv);
        }

        // Quality flag
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Recorder department
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class);
        target.setRecorderDepartment(recorderDepartment);

        // Entity Name
        target.setEntityName(source.getClass().getSimpleName());

        // If vessel use measurement
        if (source instanceof VesselUseMeasurement) {
            VesselUseMeasurement vum = (VesselUseMeasurement)source;
            if (vum.getTrip() != null) {
                target.setTripId(vum.getTrip().getId());
            }
            else if (vum.getOperation() != null) {
                target.setOperationId(vum.getOperation().getId());
            }
        }

        // If gear use measurement
        else if (source instanceof GearUseMeasurement) {
            GearUseMeasurement gum = (GearUseMeasurement)source;
            if (gum.getOperation() != null) {
                target.setOperationId(gum.getOperation().getId());
            }
        }

        // If physical gear measurement
        else if (source instanceof PhysicalGearMeasurement) {
            PhysicalGearMeasurement pgm = (PhysicalGearMeasurement)source;
            if (pgm.getPhysicalGear() != null) {
                target.setPhysicalGearId(pgm.getPhysicalGear().getId());
            }
        }

        return target;
    }

    @Override
    public List<MeasurementVO> saveVesselUseMeasurementsByTripId(final int tripId, List<MeasurementVO> sources) {
        Trip parent = get(Trip.class, tripId);
        return saveMeasurements(VesselUseMeasurement.class, sources, parent.getMeasurements());
    }

    @Override
    public List<MeasurementVO> savePhysicalGearMeasurementByPhysicalGearId(final int physicalGearId, List<MeasurementVO> sources) {
        PhysicalGear parent = get(PhysicalGear.class, physicalGearId);
        return saveMeasurements(PhysicalGearMeasurement.class, sources, parent.getMeasurements());
    }

    @Override
    public List<MeasurementVO> saveGearUseMeasurementsByOperationId(final int operationId, List<MeasurementVO> sources) {
        Operation parent = get(Operation.class, operationId);
        return saveMeasurements(GearUseMeasurement.class, sources, parent.getGearUseMeasurements());
    }

    @Override
    public List<MeasurementVO> saveVesselUseMeasurementsByOperationId(final int operationId, List<MeasurementVO> sources) {
        Operation parent = get(Operation.class, operationId);
        return saveMeasurements(VesselUseMeasurement.class, sources, parent.getVesselUseMeasurements());
    }

    @Override
    public List<MeasurementVO> saveSampleMeasurements(final int sampleId, List<MeasurementVO> sources) {
        Sample parent = get(Sample.class, sampleId);
        return saveMeasurements(SampleMeasurement.class, sources, parent.getSampleMeasurements());
    }

    @Override
    public Map<Integer, String> saveSampleMeasurementsMap(final int sampleId, Map<Integer, String> sources) {
        Sample parent = get(Sample.class, sampleId);
        return saveMeasurementsMap(SampleMeasurement.class, sources, parent.getSampleMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveBatchSortingMeasurements(int batchId, List<MeasurementVO> sources) {
        Batch parent = get(Batch.class, batchId);
        return saveMeasurements(BatchSortingMeasurement.class, sources, parent.getSortingMeasurements());
    }

    @Override
    public List<MeasurementVO> saveBatchQuantificationMeasurements(int batchId, List<MeasurementVO> sources) {
        Batch parent = get(Batch.class, batchId);
        return saveMeasurements(BatchSortingMeasurement.class, sources, parent.getSortingMeasurements());
    }

    @Override
    public Map<Integer, String> saveBatchSortingMeasurementsMap(int batchId, Map<Integer, String> sources) {
        Batch parent = get(Batch.class, batchId);
        return saveMeasurementsMap(BatchSortingMeasurement.class, sources, parent.getSortingMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveBatchQuantificationMeasurementsMap(int batchId, Map<Integer, String> sources) {
        Batch parent = get(Batch.class, batchId);
        return saveMeasurementsMap(BatchQuantificationMeasurement.class, sources, parent.getQuantificationMeasurements(), parent);
    }

    @Override
    public <T extends IMeasurementEntity> List<MeasurementVO> saveMeasurements(
            final Class<? extends IMeasurementEntity> entityClass,
            List<MeasurementVO> sources,
            List<T> target) {

        // Remember existing measurements, to be able to remove unused measurements
        // note: Need Beans.getList() to avoid NullPointerException if target=null
        final Map<Integer, T> sourceToRemove = Beans.splitById(Beans.getList(target));

        List<MeasurementVO> result = sources.stream()
                .map(source -> {
                    // Remove from the existing list
                    if (source.getId() != null) sourceToRemove.remove(source.getId());

                    // Save it
                    return save(entityClass, source);
                })
                .collect(Collectors.toList());

        // Remove unused items
        if (MapUtils.isNotEmpty(sourceToRemove)) {
            sourceToRemove.values().forEach(entity -> getEntityManager().remove(entity));
        }

        return result;
    }

    /* -- protected methods -- */

    protected <T extends IMeasurementEntity> Map<Integer, String> saveMeasurementsMap(
            final Class<? extends T> entityClass,
            Map<Integer, String> sources,
            List<T> target,
            final IDataEntity<?> parent) {

        final EntityManager session = getEntityManager();

        // Remember existing measurements, to be able to remove unused measurements
        // note: Need Beans.getList() to avoid NullPointerException if target=null
        final Map<Integer, T> sourceToRemove = Beans.splitByProperty(Beans.getList(target), IMeasurementEntity.PROPERTY_PMFM + "." + IMeasurementEntity.PROPERTY_ID);

        int rankOrder = 1;
        for (Map.Entry<Integer, String> source: sources.entrySet()) {
            Integer pmfmId = source.getKey();
            String value = source.getValue();
            // Get existing meas and remove it from list to remove
            IMeasurementEntity entity = sourceToRemove.remove(pmfmId);

            // Exists
            boolean isNew = (entity == null);
            if (isNew) {
                try {
                    entity = entityClass.newInstance();
                } catch (IllegalAccessException | InstantiationException e) {
                    throw new SumarisTechnicalException(e);
                }
            }

            // Make sure to set pmfm
            if (entity.getPmfm() == null) {
                entity.setPmfm(load(Pmfm.class, pmfmId));
            }

            // Rank order
            if (entity instanceof ISortedMeasurementEntity) {
                ((ISortedMeasurementEntity) entity).setRankOrder(rankOrder++);
            }

            // Is reference ?
            if (entity instanceof BatchQuantificationMeasurement) {
                ((BatchQuantificationMeasurement) entity).setIsReferenceQuantification(rankOrder==1);
            }

            // Fill default properties
            fillDefaultProperties(parent, entity);

            // Set value to entity
            valueToEntity(value, pmfmId, entity);

            // Link to parent
            linkToParent(entity, parent.getClass(), parent.getId(), false);

            // Update update_dt
            Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
            entity.setUpdateDate(newUpdateDate);

            // Save entity
            if (isNew) {
                session.persist(entity);
            } else {
                session.merge(entity);
            }
        }

        // Remove unused items
        if (MapUtils.isNotEmpty(sourceToRemove)) {
            sourceToRemove.values().forEach(entity -> getEntityManager().remove(entity));
        }

        return sources;
    }

    protected MeasurementVO save(Class<? extends IMeasurementEntity> entityClass, MeasurementVO source) {
        Preconditions.checkNotNull(entityClass);
        Preconditions.checkNotNull(source);

        boolean isEmpty = StringUtils.isBlank(source.getAlphanumericalValue()) && source.getNumericalValue() == null
                && (source.getQualitativeValue() == null || source.getQualitativeValue().getId() == null);
        Preconditions.checkArgument(!isEmpty, "Measurement is empty: no value found.");

        EntityManager session = getEntityManager();

        IMeasurementEntity entity = null;
        if (source.getId() != null) {
            entity = get(entityClass, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            try {
                entity = entityClass.newInstance();
            }
            catch(IllegalAccessException | InstantiationException e) {
                throw new SumarisTechnicalException(e);
            }
        }

        // VO -> Entity
        measurementVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            session.persist(entity);
            source.setId(entity.getId());
        } else {
            session.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);
        source.setEntityName(entity.getClass().getSimpleName());

        //session.flush();
        //session.clear();

        return source;
    }



    protected <T extends IMeasurementEntity> List<MeasurementVO> getMeasurementsByParentId(Class<T> entityClass,
                                                                                        String parentPropertyName,
                                                                                        int parentId,
                                                                                        String sortByPropertyName) {
        TypedQuery<T> query = getMeasurementsByParentIdQuery(entityClass, parentPropertyName, parentId, sortByPropertyName);
        return toMeasurementVOs(query.getResultList());
    }

    protected <T extends IMeasurementEntity> Map<Integer, String> getMeasurementsMapByParentId(Class<T> entityClass,
                                                                                           String parentPropertyName,
                                                                                           int parentId,
                                                                                           String sortByPropertyName) {
        TypedQuery<T> query = getMeasurementsByParentIdQuery(entityClass, parentPropertyName, parentId, sortByPropertyName);
        return toMeasurementsMap(query.getResultList());
    }



    protected <T extends IMeasurementEntity> TypedQuery<T> getMeasurementsByParentIdQuery(Class<T> entityClass,
                                                                                           String parentPropertyName,
                                                                                           int parentId,
                                                                                           String sortByPropertyName) {
        Preconditions.checkNotNull(sortByPropertyName);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        ParameterExpression<Integer> idParam = builder.parameter(Integer.class);

        query.select(root)
                .where(builder.equal(root.get(parentPropertyName).get(IRootDataEntity.PROPERTY_ID), idParam))
                // Order byldev
                .orderBy(builder.asc(root.get(sortByPropertyName)));

        return getEntityManager().createQuery(query)
                .setParameter(idParam, parentId);
    }

    protected <T extends IMeasurementEntity> List<MeasurementVO> toMeasurementVOs(List<T> source) {
        return source.stream()
                .map(this::toMeasurementVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected <T extends IMeasurementEntity> Map<Integer, String> toMeasurementsMap(List<T> source) {
        final Map<Integer, String> result = Maps.newIdentityHashMap();
        source.stream()
                .filter(m -> m.getPmfm() != null && m.getPmfm().getId() != null)
                .forEach(m -> {

                    if (m.getPmfm() != null && m.getPmfm().getId() != null) {
                        Object value = this.entityToValue(m);
                        if (value != null) {
                            result.put(m.getPmfm().getId(), value.toString());
                        }
                    }
                });
                //.collect(Collectors.toMap(m -> m.getPmfm().getId(), this::entityToValue))
        return result;
    }


    protected void measurementVOToEntity(MeasurementVO source, IMeasurementEntity target, boolean copyIfNull) {

        Beans.copyProperties(source, target);

        // Pmfm
        target.setPmfm(load(Pmfm.class, source.getPmfmId()));

        // Qualitative value
        if (copyIfNull || source.getQualitativeValue() != null) {
            if (source.getQualitativeValue() == null || source.getQualitativeValue().getId() == null) {
                target.setQualitativeValue(null);
            }
            else {
                target.setQualitativeValue(load(QualitativeValue.class, source.getQualitativeValue().getId()));
            }
        }

        // Recorder department
        if (copyIfNull || source.getRecorderDepartment() != null) {
            if (source.getRecorderDepartment() == null || source.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(load(Department.class, source.getRecorderDepartment().getId()));
            }
        }

        // Quality flag
        if (copyIfNull || source.getQualityFlagId() != null) {
            if (source.getQualityFlagId() == null) {
                target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
            }
            else {
                target.setQualityFlag(load(QualityFlag.class, source.getQualityFlagId()));
            }
        }

        // Trip
        linkToParent(target, Trip.class, source.getTripId(), false);
        // Operation
        linkToParent(target, Operation.class, source.getOperationId(), false);
        // Operation
        linkToParent(target, Operation.class, source.getOperationId(), false);
        // Physical gear
        linkToParent(target, Operation.class, source.getPhysicalGearId(), false);
        // Sample measurement
        linkToParent(target, Sample.class, source.getSampleId(), false);
    }

    protected void valueToEntity(String value, int pmfmId, IMeasurementEntity target) {

        if (value == null) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, "Unable to set value NULL value on a measurement");
        }

        PmfmVO pmfm = pmfmDao.get(pmfmId);

        ParameterValueType type = ParameterValueType.fromPmfm(pmfm);
        switch (type) {
            case BOOLEAN:
                target.setNumericalValue(Boolean.TRUE.equals(value) ? 1d : 0d);
                break;
            case QUALITATIVE_VALUE:
                // If get a object structure (e.g. ReferentialVO), try to get the id
                target.setQualitativeValue(load(QualitativeValue.class, Integer.parseInt(value)));
                break;
            case STRING:
                target.setAlphanumericalValue(value);
                break;
            case DATE:
                target.setAlphanumericalValue(value);
                break;
            case INTEGER:
            case DOUBLE:
                target.setNumericalValue(Double.parseDouble(value));
                break;
            default:
                // Unknown type
                throw new SumarisTechnicalException( String.format("Unable to set measurement value {%s} for the type {%s}", value, type.name().toLowerCase()));
        }
    }

    protected Object entityToValue(IMeasurementEntity source) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getPmfm());
        Preconditions.checkNotNull(source.getPmfm().getId());

        PmfmVO pmfm = pmfmDao.get(source.getPmfm().getId());

        ParameterValueType type = ParameterValueType.fromPmfm(pmfm);
        switch (type) {
            case BOOLEAN:
                return (source.getNumericalValue() != null && source.getNumericalValue().doubleValue() == 1d ? Boolean.TRUE : Boolean.FALSE);
            case QUALITATIVE_VALUE:
                // If get a object structure (e.g. ReferentialVO), try to get the id
                return ((source.getQualitativeValue() != null && source.getQualitativeValue().getId() != null) ? source.getQualitativeValue().getId() : null);
            case STRING:
            case DATE:
                return source.getAlphanumericalValue();
            case INTEGER:
                return ((source.getNumericalValue() != null) ? new Integer(source.getNumericalValue().intValue()) : null);
            case DOUBLE:
                return source.getNumericalValue();
            default:
                // Unknown type
                throw new SumarisTechnicalException( String.format("Unable to read measurement's value for the type {%s}. Measurement id=%s", type.name().toLowerCase(), source.getId()));
        }
    }

    protected void fillDefaultProperties(IDataEntity<?> parent, IMeasurementEntity target) {

        // Recorder department
        if (target.getRecorderDepartment() == null) {
            if (parent.getRecorderDepartment() == null || parent.getRecorderDepartment().getId() == null) {
                target.setRecorderDepartment(null);
            }
            else {
                target.setRecorderDepartment(parent.getRecorderDepartment());
            }
        }

        // Quality flag
        if (target.getQualityFlag() == null) {
            target.setQualityFlag(load(QualityFlag.class, config.getDefaultQualityFlagId()));
        }
    }

    protected void linkToParent(IMeasurementEntity target, final Class<?> parentClass, Serializable parentId, boolean copyIfNull) {

        // If null: skip
        if (parentClass == null || (!copyIfNull && parentId == null)) return;

        // If vessel use measurement
        if (target instanceof VesselUseMeasurement) {

            // Trip
            if (parentClass.isAssignableFrom(Trip.class)) {
                if (parentId == null) {
                    ((VesselUseMeasurement) target).setTrip(null);
                } else {
                    ((VesselUseMeasurement) target).setTrip(load(Trip.class, parentId));
                }
            }

            // Operation
            else if (parentClass.isAssignableFrom(Operation.class)) {
                if (parentId == null) {
                    ((VesselUseMeasurement) target).setOperation(null);
                } else {
                    ((VesselUseMeasurement) target).setOperation(load(Operation.class, parentId));
                }
            }
        }

        // If gear use measurement
        else if (target instanceof GearUseMeasurement) {
            // Operation
            if (parentClass.isAssignableFrom(Operation.class)) {
                if (parentId == null) {
                    ((GearUseMeasurement) target).setOperation(null);
                } else {
                    ((GearUseMeasurement) target).setOperation(load(Operation.class, parentId));
                }
            }
        }

        // If physical gear measurement
        else if (target instanceof PhysicalGearMeasurement) {
            // Physical gear
            if (parentId == null) {
                ((PhysicalGearMeasurement) target).setPhysicalGear(null);
            } else {
                ((PhysicalGearMeasurement) target).setPhysicalGear(load(PhysicalGear.class, parentId));
            }
        }

        // Sample measurement
        else if (target instanceof SampleMeasurement) {
            if (parentId == null) {
                ((SampleMeasurement) target).setSample(null);
            } else {
                ((SampleMeasurement) target).setSample(load(Sample.class, parentId));
            }
        }

        // Batch quantification measurement
        else if (target instanceof BatchQuantificationMeasurement) {
            if (parentId == null) {
                ((BatchQuantificationMeasurement) target).setBatch(null);
            } else {
                ((BatchQuantificationMeasurement) target).setBatch(load(Batch.class, parentId));
            }
        }

        // Batch sorting measurement
        else if (target instanceof BatchSortingMeasurement) {
            if (parentId == null) {
                ((BatchSortingMeasurement) target).setBatch(null);
            } else {
                ((BatchSortingMeasurement) target).setBatch(load(Batch.class, parentId));
            }
        }

        // Unknown measurement class
        else {
            throw new IllegalArgumentException(String.format("Class {%s} not manage yet in this method", target.getClass().getSimpleName()));
        }

    }
}

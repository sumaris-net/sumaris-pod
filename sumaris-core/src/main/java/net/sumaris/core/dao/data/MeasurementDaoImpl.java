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
import com.google.common.collect.*;
import net.sumaris.core.dao.referential.PmfmDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.referential.ParameterValueType;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Repository("measurementDao")
public class MeasurementDaoImpl extends BaseDataDaoImpl implements MeasurementDao {

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
        I18n.n("sumaris.persistence.table.observedLocationMeasurement");
    }

    protected static Multimap<Class<? extends IMeasurementEntity>, PropertyDescriptor> initParentPropertiesMap() {
        Multimap<Class<? extends IMeasurementEntity>, PropertyDescriptor> result = ArrayListMultimap.create();

        // Trip
        result.put(VesselUseMeasurement.class, BeanUtils.getPropertyDescriptor(VesselUseMeasurement.class, VesselUseMeasurement.PROPERTY_TRIP));

        // Physical Gear
        result.put(PhysicalGearMeasurement.class, BeanUtils.getPropertyDescriptor(PhysicalGearMeasurement.class, PhysicalGearMeasurement.PROPERTY_PHYSICAL_GEAR));

        // Operation
        result.put(VesselUseMeasurement.class, BeanUtils.getPropertyDescriptor(VesselUseMeasurement.class, VesselUseMeasurement.PROPERTY_OPERATION));
        result.put(GearUseMeasurement.class, BeanUtils.getPropertyDescriptor(GearUseMeasurement.class, GearUseMeasurement.PROPERTY_OPERATION));

        // Observed location
        result.put(ObservedLocationMeasurement.class, BeanUtils.getPropertyDescriptor(ObservedLocationMeasurement.class, ObservedLocationMeasurement.PROPERTY_OBSERVED_LOCATION));

        // Sample
        result.put(SampleMeasurement.class, BeanUtils.getPropertyDescriptor(SampleMeasurement.class, SampleMeasurement.PROPERTY_SAMPLE));

        // Batch
        result.put(BatchSortingMeasurement.class, BeanUtils.getPropertyDescriptor(BatchSortingMeasurement.class, BatchSortingMeasurement.PROPERTY_BATCH));
        result.put(BatchQuantificationMeasurement.class, BeanUtils.getPropertyDescriptor(BatchQuantificationMeasurement.class, BatchSortingMeasurement.PROPERTY_BATCH));

        // Sale
        result.put(SaleMeasurement.class, BeanUtils.getPropertyDescriptor(SaleMeasurement.class, SaleMeasurement.PROPERTY_SALE));

        // Landing
        result.put(LandingMeasurement.class, BeanUtils.getPropertyDescriptor(LandingMeasurement.class, LandingMeasurement.PROPERTY_LANDING));

        return result;
    }

    private Multimap<Class<? extends IMeasurementEntity>, PropertyDescriptor> parentPropertiesMap = initParentPropertiesMap();

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PmfmDao pmfmDao;


    @Override
    public List<MeasurementVO> getTripVesselUseMeasurements(int tripId) {
        return getMeasurementsByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.PROPERTY_TRIP,
                tripId,
                VesselUseMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, String> getTripVesselUseMeasurementsMap(int tripId) {
        return getMeasurementsMapByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.PROPERTY_TRIP,
                tripId,
                VesselUseMeasurement.PROPERTY_ID
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
    public Map<Integer, String> getPhysicalGearMeasurementsMap(int physicalGearId) {
        return getMeasurementsMapByParentId(PhysicalGearMeasurement.class,
                PhysicalGearMeasurement.PROPERTY_PHYSICAL_GEAR,
                physicalGearId,
                PhysicalGearMeasurement.PROPERTY_ID
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MeasurementVO> getOperationVesselUseMeasurements(int operationId) {
        return getMeasurementsByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.PROPERTY_OPERATION,
                operationId,
                VesselUseMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, String> getOperationVesselUseMeasurementsMap(int operationId) {
        return getMeasurementsMapByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.PROPERTY_OPERATION,
                operationId,
                VesselUseMeasurement.PROPERTY_ID
        );
    }

    @Override
    public Map<Integer, String> getOperationGearUseMeasurementsMap(int operationId) {
        return getMeasurementsMapByParentId(GearUseMeasurement.class,
                GearUseMeasurement.PROPERTY_OPERATION,
                operationId,
                GearUseMeasurement.PROPERTY_ID
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MeasurementVO> getOperationGearUseMeasurements(int operationId) {
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
    public List<MeasurementVO> getObservedLocationMeasurements(int observedLocationId) {
        return getMeasurementsByParentId(ObservedLocationMeasurement.class,
                ObservedLocationMeasurement.PROPERTY_OBSERVED_LOCATION,
                observedLocationId,
                ObservedLocationMeasurement.PROPERTY_RANK_ORDER
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
    @SuppressWarnings("unchecked")
    public Map<Integer, String> getObservedLocationMeasurementsMap(int observedLocationId) {
        return getMeasurementsMapByParentId(ObservedLocationMeasurement.class,
                ObservedLocationMeasurement.PROPERTY_OBSERVED_LOCATION,
                observedLocationId,
                ObservedLocationMeasurement.PROPERTY_ID
        );
    }


    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, String> getLandingMeasurementsMap(int landingId) {
        return getMeasurementsMapByParentId(LandingMeasurement.class,
                LandingMeasurement.PROPERTY_LANDING,
                landingId,
                LandingMeasurement.PROPERTY_ID
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MeasurementVO> getLandingMeasurements(int landingId) {
        return getMeasurementsByParentId(LandingMeasurement.class,
                LandingMeasurement.PROPERTY_LANDING,
                landingId,
                LandingMeasurement.PROPERTY_ID
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
        DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
        target.setRecorderDepartment(recorderDepartment);

        // Entity Name
        target.setEntityName(getEntityName(source));

        return target;
    }

    @Override
    public List<MeasurementVO> saveTripVesselUseMeasurements(final int tripId, List<MeasurementVO> sources) {
        Trip parent = get(Trip.class, tripId);
        return saveMeasurements(VesselUseMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveTripMeasurementsMap(int tripId, Map<Integer, String> sources) {
        Trip parent = get(Trip.class, tripId);
        return saveMeasurementsMap(VesselUseMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> savePhysicalGearMeasurements(final int physicalGearId, List<MeasurementVO> sources) {
        PhysicalGear parent = get(PhysicalGear.class, physicalGearId);
        return saveMeasurements(PhysicalGearMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> savePhysicalGearMeasurementsMap(int physicalGearId, Map<Integer, String> sources) {
        PhysicalGear parent = get(PhysicalGear.class, physicalGearId);
        return saveMeasurementsMap(PhysicalGearMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveOperationGearUseMeasurements(final int operationId, List<MeasurementVO> sources) {
        Operation parent = get(Operation.class, operationId);
        return saveMeasurements(GearUseMeasurement.class, sources, parent.getGearUseMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveOperationVesselUseMeasurements(final int operationId, List<MeasurementVO> sources) {
        Operation parent = get(Operation.class, operationId);
        return saveMeasurements(VesselUseMeasurement.class, sources, parent.getVesselUseMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveOperationGearUseMeasurementsMap(int operationId, Map<Integer, String> sources) {
        Operation parent = get(Operation.class, operationId);
        return saveMeasurementsMap(GearUseMeasurement.class, sources, parent.getGearUseMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveOperationVesselUseMeasurementsMap(int operationId, Map<Integer, String> sources) {
        Operation parent = get(Operation.class, operationId);
        return saveMeasurementsMap(VesselUseMeasurement.class, sources, parent.getVesselUseMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveObservedLocationMeasurements(final int observedLocationId, List<MeasurementVO> sources) {
        ObservedLocation parent = get(ObservedLocation.class, observedLocationId);
        return saveMeasurements(ObservedLocationMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveObservedLocationMeasurementsMap(final int observedLocationId, Map<Integer, String> sources) {
        ObservedLocation parent = get(ObservedLocation.class, observedLocationId);
        return saveMeasurementsMap(ObservedLocationMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveSaleMeasurements(final int saleId, List<MeasurementVO> sources) {
        Sale parent = get(Sale.class, saleId);
        return saveMeasurements(SaleMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveSaleMeasurementsMap(final int saleId, Map<Integer, String> sources) {
        Sale parent = get(Sale.class, saleId);
        return saveMeasurementsMap(SaleMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveLandingMeasurements(final int landingId, List<MeasurementVO> sources) {
        Landing parent = get(Landing.class, landingId);
        return saveMeasurements(LandingMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveLandingMeasurementsMap(final int landingId, Map<Integer, String> sources) {
        Landing parent = get(Landing.class, landingId);
        return saveMeasurementsMap(LandingMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveSampleMeasurements(final int sampleId, List<MeasurementVO> sources) {
        Sample parent = get(Sample.class, sampleId);
        return saveMeasurements(SampleMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveSampleMeasurementsMap(final int sampleId, Map<Integer, String> sources) {
        Sample parent = get(Sample.class, sampleId);
        return saveMeasurementsMap(SampleMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveBatchSortingMeasurements(int batchId, List<MeasurementVO> sources) {
        Batch parent = get(Batch.class, batchId);
        return saveMeasurements(BatchSortingMeasurement.class, sources, parent.getSortingMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveBatchQuantificationMeasurements(int batchId, List<MeasurementVO> sources) {
        Batch parent = get(Batch.class, batchId);
        return saveMeasurements(BatchSortingMeasurement.class, sources, parent.getSortingMeasurements(), parent);
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
    public List<MeasurementVO> saveVesselPhysicalMeasurements(int vesselFeaturesId, List<MeasurementVO> sources) {
        VesselFeatures parent = get(VesselFeatures.class, vesselFeaturesId);
        return saveMeasurements(VesselPhysicalMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveVesselPhysicalMeasurementsMap(int vesselFeaturesId, Map<Integer, String> sources) {
        VesselFeatures parent = get(VesselFeatures.class, vesselFeaturesId);
        return saveMeasurementsMap(VesselPhysicalMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> getVesselFeaturesMeasurements(int vesselFeaturesId) {
        return getMeasurementsByParentId(VesselPhysicalMeasurement.class,
                VesselPhysicalMeasurement.PROPERTY_VESSEL_FEATURES,
                vesselFeaturesId,
                VesselPhysicalMeasurement.PROPERTY_ID
        );
    }

    @Override
    public Map<Integer, String> getVesselFeaturesMeasurementsMap(int vesselFeaturesId) {
        return getMeasurementsMapByParentId(VesselPhysicalMeasurement.class,
                VesselPhysicalMeasurement.PROPERTY_VESSEL_FEATURES,
                vesselFeaturesId,
                VesselPhysicalMeasurement.PROPERTY_ID
        );
    }

    @Override
    public <T extends IMeasurementEntity> List<MeasurementVO> saveMeasurements(
            final Class<? extends IMeasurementEntity> entityClass,
            List<MeasurementVO> sources,
            List<T> target,
            final IDataEntity<?> parent) {

        final EntityManager em = getEntityManager();

        // Remember existing measurements, to be able to remove unused measurements
        // note: Need Beans.getList() to avoid NullPointerException if target=null
        final Map<Integer, T> sourceToRemove = Beans.splitById(Beans.getList(target));

        int rankOrder = 1;
        List<MeasurementVO> result = Lists.newArrayList();
        for (MeasurementVO source: sources) {
            if (isNotEmpty(source)) {
                IMeasurementEntity entity = null;

                // Get existing meas and remove it from list to remove
                if (source.getId() != null) {
                    entity = sourceToRemove.remove(source.getId());
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

                // Update rankOrder
                if (entity instanceof ISortedMeasurementEntity) {
                    ((ISortedMeasurementEntity)entity).setRankOrder(rankOrder);
                    source.setRankOrder(rankOrder);
                    rankOrder++;
                }

                // Set parent
                setParent(entity, parent.getClass(), parent.getId(), false);

                // Update update_dt
                Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
                entity.setUpdateDate(newUpdateDate);

                // Save entityName
                if (isNew) {
                    em.persist(entity);
                    source.setId(entity.getId());
                } else {
                    em.merge(entity);
                }

                source.setUpdateDate(newUpdateDate);
                source.setEntityName(getEntityName(entity));

                result.add(source);
            }
        }

        // Remove unused tableNames
        if (MapUtils.isNotEmpty(sourceToRemove)) {
            sourceToRemove.values().forEach(em::remove);
        }

        return result;
    }

    /* -- protected methods -- */

    protected <T extends IMeasurementEntity> String getEntityName(T source) {
        String classname = source.getClass().getSimpleName();
        int index = classname.indexOf("$HibernateProxy");
        if (index > 0) {
            return classname.substring(0, index);
        }
        return classname;
    }

    protected <T extends IDataEntity> Class<T> getEntityClass(T source) {
        String classname = source.getClass().getName();
        int index = classname.indexOf("$HibernateProxy");
        if (index > 0) {
            try {
                return (Class<T>) Class.forName(classname.substring(0, index));
            }
            catch(ClassNotFoundException t) {
                throw new SumarisTechnicalException(t);
            }
        }
        return (Class<T>)source.getClass();
    }


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

            if (StringUtils.isNotBlank(value)) {
                // Get existing meas and remove it from list to remove
                IMeasurementEntity entity = sourceToRemove.remove(pmfmId);

                // Exists ?
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
                    ((BatchQuantificationMeasurement) entity).setIsReferenceQuantification(rankOrder == 1);
                }

                // Fill default properties
                fillDefaultProperties(parent, entity);

                // Set value to entity
                valueToEntity(value, pmfmId, entity);

                // Link to parent
                setParent(entity, getEntityClass(parent), parent.getId(), false);

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
        }

        // Remove unused tableNames
        if (MapUtils.isNotEmpty(sourceToRemove)) {
            sourceToRemove.values().forEach(entity -> getEntityManager().remove(entity));
        }

        return sources;
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


    protected void measurementVOToEntity(MeasurementVO source,
                                         IMeasurementEntity target,
                                         boolean copyIfNull) {

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

    }

    protected void valueToEntity(String value, int pmfmId, IMeasurementEntity target) {

        if (value == null) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, "Unable to set value NULL value on a measurement");
        }

        PmfmVO pmfm = pmfmDao.get(pmfmId);
        if (pmfm == null) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, "Unable to find pmfm with id=" + pmfmId);
        }

        ParameterValueType type = ParameterValueType.fromPmfm(pmfm);
        if (type == null) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, "Unable to find the type of the pmfm with id=" + pmfmId);
        }

        switch (type) {
            case BOOLEAN:
                target.setNumericalValue(Boolean.parseBoolean(value) || "1".equals(value) ? 1d : 0d);
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

        Preconditions.checkNotNull(pmfm, "Unable to find Pmfm with id=" + source.getPmfm().getId());

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

    protected void setParent(IMeasurementEntity target, final Class<?> parentClass, Serializable parentId, boolean copyIfNull) {

        // If null: skip
        if (parentClass == null || (!copyIfNull && parentId == null)) return;

        // First, try to set using a corresponding parent property
        Collection<PropertyDescriptor> parentDescriptors = parentPropertiesMap.get(target.getClass());
        if (CollectionUtils.isNotEmpty(parentDescriptors)) {

            // Find th right parent property (use the first compatible parent)
            PropertyDescriptor parentProperty = parentDescriptors.stream()
                    .filter(property -> property.getPropertyType().isAssignableFrom(parentClass))
                    .findFirst().orElse(null);

            // If a parent property has been found, use it
            if (parentProperty != null) {
                try {
                    if (parentId == null) {
                        parentProperty.getWriteMethod().invoke(target, new Object[]{null});
                    } else {
                        Object parentEntity = load(parentClass, parentId);
                        parentProperty.getWriteMethod().invoke(target, parentEntity);
                    }
                    return;
                } catch (Exception e) {
                    throw new SumarisTechnicalException(e);
                }
            }
        }

        // No parent property in the global map: continue as a special case

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

    protected boolean isEmpty(MeasurementVO source) {
        return StringUtils.isBlank(source.getAlphanumericalValue()) && source.getNumericalValue() == null
                && (source.getQualitativeValue() == null || source.getQualitativeValue().getId() == null);
    }
    protected boolean isNotEmpty(MeasurementVO source) {
        return !isEmpty(source);
    }

}

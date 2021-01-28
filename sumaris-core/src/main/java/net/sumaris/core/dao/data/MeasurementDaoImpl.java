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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.pmfm.PmfmRepository;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.exception.ErrorCodes;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.programStrategy.ProgramProperty;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.pmfm.Pmfm;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.QuantificationMeasurementVO;
import net.sumaris.core.vo.referential.PmfmVO;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.mutable.MutableShort;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
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
        I18n.n("sumaris.persistence.table.observedLocationMeasurement");
    }

    protected static Multimap<Class<? extends IMeasurementEntity>, PropertyDescriptor> initParentPropertiesMap() {
        Multimap<Class<? extends IMeasurementEntity>, PropertyDescriptor> result = ArrayListMultimap.create();

        // Trip
        result.put(VesselUseMeasurement.class, BeanUtils.getPropertyDescriptor(VesselUseMeasurement.class, VesselUseMeasurement.Fields.TRIP));

        // Physical Gear
        result.put(PhysicalGearMeasurement.class, BeanUtils.getPropertyDescriptor(PhysicalGearMeasurement.class, PhysicalGearMeasurement.Fields.PHYSICAL_GEAR));

        // Operation
        result.put(VesselUseMeasurement.class, BeanUtils.getPropertyDescriptor(VesselUseMeasurement.class, VesselUseMeasurement.Fields.OPERATION));
        result.put(GearUseMeasurement.class, BeanUtils.getPropertyDescriptor(GearUseMeasurement.class, GearUseMeasurement.Fields.OPERATION));

        // Observed location
        result.put(ObservedLocationMeasurement.class, BeanUtils.getPropertyDescriptor(ObservedLocationMeasurement.class, ObservedLocationMeasurement.Fields.OBSERVED_LOCATION));

        // Sample
        result.put(SampleMeasurement.class, BeanUtils.getPropertyDescriptor(SampleMeasurement.class, SampleMeasurement.Fields.SAMPLE));

        // Batch
        result.put(BatchSortingMeasurement.class, BeanUtils.getPropertyDescriptor(BatchSortingMeasurement.class, BatchSortingMeasurement.Fields.BATCH));
        result.put(BatchQuantificationMeasurement.class, BeanUtils.getPropertyDescriptor(BatchQuantificationMeasurement.class, BatchSortingMeasurement.Fields.BATCH));

        // Sale
        result.put(SaleMeasurement.class, BeanUtils.getPropertyDescriptor(SaleMeasurement.class, SaleMeasurement.Fields.SALE));

        // Landing
        result.put(LandingMeasurement.class, BeanUtils.getPropertyDescriptor(LandingMeasurement.class, LandingMeasurement.Fields.LANDING));

        return result;
    }

    private final Multimap<Class<? extends IMeasurementEntity>, PropertyDescriptor> parentPropertiesMap = initParentPropertiesMap();

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PmfmRepository pmfmRepository;


    @Override
    public List<MeasurementVO> getTripVesselUseMeasurements(int tripId) {
        return getMeasurementsByParentId(VesselUseMeasurement.class,
                MeasurementVO.class,
                VesselUseMeasurement.Fields.TRIP,
                tripId,
                VesselUseMeasurement.Fields.RANK_ORDER
        );
    }

    @Override
    public Map<Integer, String> getTripVesselUseMeasurementsMap(int tripId) {
        return getMeasurementsMapByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.Fields.TRIP,
                tripId,
                null
        );
    }

    @Override
    public Map<Integer, Map<Integer, String>> getTripsVesselUseMeasurementsMap(Collection<Integer> tripIds) {
        return getMeasurementsMapByParentIds(VesselUseMeasurement.class,
                VesselUseMeasurement.Fields.TRIP,
                tripIds,
                null
        );
    }

    @Override
    public List<MeasurementVO> getPhysicalGearMeasurements(int physicalGearId) {
        return getMeasurementsByParentId(PhysicalGearMeasurement.class,
                MeasurementVO.class,
                PhysicalGearMeasurement.Fields.PHYSICAL_GEAR,
                physicalGearId,
                PhysicalGearMeasurement.Fields.RANK_ORDER
        );
    }

    @Override
    public Map<Integer, String> getPhysicalGearMeasurementsMap(int physicalGearId) {
        return getMeasurementsMapByParentId(PhysicalGearMeasurement.class,
                PhysicalGearMeasurement.Fields.PHYSICAL_GEAR,
                physicalGearId,
                null
        );
    }

    @Override
    public List<MeasurementVO> getOperationVesselUseMeasurements(int operationId) {
        return getMeasurementsByParentId(VesselUseMeasurement.class,
                MeasurementVO.class,
                VesselUseMeasurement.Fields.OPERATION,
                operationId,
                VesselUseMeasurement.Fields.RANK_ORDER
        );
    }

    @Override
    public Map<Integer, String> getOperationVesselUseMeasurementsMap(int operationId) {
        return getMeasurementsMapByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.Fields.OPERATION,
                operationId,
                null
        );
    }

    @Override
    public Map<Integer, String> getOperationGearUseMeasurementsMap(int operationId) {
        return getMeasurementsMapByParentId(GearUseMeasurement.class,
                GearUseMeasurement.Fields.OPERATION,
                operationId,
                null
        );
    }

    @Override
    public List<MeasurementVO> getOperationGearUseMeasurements(int operationId) {
        return getMeasurementsByParentId(GearUseMeasurement.class,
                MeasurementVO.class,
                GearUseMeasurement.Fields.OPERATION,
                operationId,
                GearUseMeasurement.Fields.RANK_ORDER
        );
    }

    @Override
    public Map<Integer, Map<Integer, String>> getOperationsVesselUseMeasurementsMap(Collection<Integer> operationIds) {
        return getMeasurementsMapByParentIds(VesselUseMeasurement.class,
                VesselUseMeasurement.Fields.OPERATION,
                operationIds,
                null
        );
    }

    @Override
    public Map<Integer, Map<Integer, String>> getOperationsGearUseMeasurementsMap(Collection<Integer> operationIds) {
        return getMeasurementsMapByParentIds(GearUseMeasurement.class,
                GearUseMeasurement.Fields.OPERATION,
                operationIds,
                null
        );
    }


    @Override
    public List<MeasurementVO> getObservedLocationMeasurements(int observedLocationId) {
        return getMeasurementsByParentId(ObservedLocationMeasurement.class,
                MeasurementVO.class,
                ObservedLocationMeasurement.Fields.OBSERVED_LOCATION,
                observedLocationId,
                ObservedLocationMeasurement.Fields.RANK_ORDER
        );
    }

    @Override
    public Map<Integer, String> getObservedLocationMeasurementsMap(int observedLocationId) {
        return getMeasurementsMapByParentId(ObservedLocationMeasurement.class,
                ObservedLocationMeasurement.Fields.OBSERVED_LOCATION,
                observedLocationId,
                null
        );
    }

    @Override
    public List<MeasurementVO> getSampleMeasurements(int sampleId) {
        return getMeasurementsByParentId(SampleMeasurement.class,
                MeasurementVO.class,
                SampleMeasurement.Fields.SAMPLE,
                sampleId,
                SampleMeasurement.Fields.RANK_ORDER
        );
    }

    @Override
    public Map<Integer, String> getSampleMeasurementsMap(int sampleId) {
        return getMeasurementsMapByParentId(SampleMeasurement.class,
                SampleMeasurement.Fields.SAMPLE,
                sampleId,
                null
        );
    }


    @Override
    public Map<Integer, Map<Integer, String>> getBatchesSortingMeasurementsMap(Collection<Integer> ids) {
        return getMeasurementsMapByParentIds(BatchSortingMeasurement.class,
                BatchSortingMeasurement.Fields.BATCH,
                ids,
                null
        );
    }

    @Override
    public Map<Integer, Map<Integer, String>> getBatchesQuantificationMeasurementsMap(Collection<Integer> ids) {
        return getMeasurementsMapByParentIds(BatchQuantificationMeasurement.class,
                BatchQuantificationMeasurement.Fields.BATCH,
                ids,
                null
        );
    }

    @Override
    public Map<Integer, String> getBatchSortingMeasurementsMap(int batchId) {
        return getMeasurementsMapByParentId(BatchSortingMeasurement.class,
                BatchSortingMeasurement.Fields.BATCH,
                batchId,
                null
        );
    }

    @Override
    public Map<Integer, String> getBatchQuantificationMeasurementsMap(int batchId) {
        return getMeasurementsMapByParentId(BatchQuantificationMeasurement.class,
                BatchQuantificationMeasurement.Fields.BATCH,
                batchId,
                null
        );
    }

    @Override
    public Map<Integer, String> getLandingMeasurementsMap(int landingId) {
        return getMeasurementsMapByParentId(LandingMeasurement.class,
                LandingMeasurement.Fields.LANDING,
                landingId,
                null
        );
    }

    @Override
    public List<MeasurementVO> getLandingMeasurements(int landingId) {
        return getMeasurementsByParentId(LandingMeasurement.class,
                MeasurementVO.class,
                LandingMeasurement.Fields.LANDING,
                landingId,
                LandingMeasurement.Fields.ID
        );
    }

    @Override
    public List<MeasurementVO> getSaleMeasurements(int saleId) {
        return getMeasurementsByParentId(SaleMeasurement.class,
                MeasurementVO.class,
            SaleMeasurement.Fields.SALE,
            saleId,
            SaleMeasurement.Fields.ID
        );
    }

    @Override
    public Map<Integer, String> getSaleMeasurementsMap(int saleId) {
        return getMeasurementsMapByParentId(SaleMeasurement.class,
            SaleMeasurement.Fields.SALE,
            saleId,
            null
        );
    }

    @Override
    public <T extends IMeasurementEntity, V extends MeasurementVO>  V toMeasurementVO(T source, Class<? extends V> voClass) {
        if (source == null) return null;

        try {
            V target = voClass.newInstance();

            Beans.copyProperties(source, target);

            // Pmfm Id
            if (source.getPmfm() != null) {
                target.setPmfmId(source.getPmfm().getId());
            }

            // Qualitative value
            if (source.getQualitativeValue() != null){
                ReferentialVO qv = referentialDao.toVO(source.getQualitativeValue());
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
        } catch (InstantiationException | IllegalAccessException e) {
            throw new SumarisTechnicalException(e);
        }
    }

    @Override
    public List<MeasurementVO> saveTripVesselUseMeasurements(final int tripId, List<MeasurementVO> sources) {
        Trip parent = getOne(Trip.class, tripId);
        return saveMeasurements(VesselUseMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveTripMeasurementsMap(int tripId, Map<Integer, String> sources) {
        Trip parent = getOne(Trip.class, tripId);
        return saveMeasurementsMap(VesselUseMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> savePhysicalGearMeasurements(final int physicalGearId, List<MeasurementVO> sources) {
        PhysicalGear parent = getOne(PhysicalGear.class, physicalGearId);
        return saveMeasurements(PhysicalGearMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> savePhysicalGearMeasurementsMap(int physicalGearId, Map<Integer, String> sources) {
        PhysicalGear parent = getOne(PhysicalGear.class, physicalGearId);
        return saveMeasurementsMap(PhysicalGearMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveOperationGearUseMeasurements(final int operationId, List<MeasurementVO> sources) {
        Operation parent = getOne(Operation.class, operationId);
        return saveMeasurements(GearUseMeasurement.class, sources, parent.getGearUseMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveOperationVesselUseMeasurements(final int operationId, List<MeasurementVO> sources) {
        Operation parent = getOne(Operation.class, operationId);
        return saveMeasurements(VesselUseMeasurement.class, sources, parent.getVesselUseMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveOperationGearUseMeasurementsMap(int operationId, Map<Integer, String> sources) {
        Operation parent = getOne(Operation.class, operationId);
        return saveMeasurementsMap(GearUseMeasurement.class, sources, parent.getGearUseMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveOperationVesselUseMeasurementsMap(int operationId, Map<Integer, String> sources) {
        Operation parent = getOne(Operation.class, operationId);
        return saveMeasurementsMap(VesselUseMeasurement.class, sources, parent.getVesselUseMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveObservedLocationMeasurements(final int observedLocationId, List<MeasurementVO> sources) {
        ObservedLocation parent = getOne(ObservedLocation.class, observedLocationId);
        return saveMeasurements(ObservedLocationMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveObservedLocationMeasurementsMap(final int observedLocationId, Map<Integer, String> sources) {
        ObservedLocation parent = getOne(ObservedLocation.class, observedLocationId);
        return saveMeasurementsMap(ObservedLocationMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveSaleMeasurements(final int saleId, List<MeasurementVO> sources) {
        Sale parent = getOne(Sale.class, saleId);
        return saveMeasurements(SaleMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveSaleMeasurementsMap(final int saleId, Map<Integer, String> sources) {
        Sale parent = getOne(Sale.class, saleId);
        return saveMeasurementsMap(SaleMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveLandingMeasurements(final int landingId, List<MeasurementVO> sources) {
        Landing parent = getOne(Landing.class, landingId);
        return saveMeasurements(LandingMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveLandingMeasurementsMap(final int landingId, Map<Integer, String> sources) {
        Landing parent = getOne(Landing.class, landingId);
        return saveMeasurementsMap(LandingMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveSampleMeasurements(final int sampleId, List<MeasurementVO> sources) {
        Sample parent = getOne(Sample.class, sampleId);
        return saveMeasurements(SampleMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveSampleMeasurementsMap(final int sampleId, Map<Integer, String> sources) {
        Sample parent = getOne(Sample.class, sampleId);
        return saveMeasurementsMap(SampleMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> getBatchSortingMeasurements(int batchId) {
        return getMeasurementsByParentId(BatchSortingMeasurement.class,
                MeasurementVO.class,
                BatchSortingMeasurement.Fields.BATCH,
                batchId,
                BatchSortingMeasurement.Fields.RANK_ORDER
            );
    }

    @Override
    public List<QuantificationMeasurementVO> getBatchQuantificationMeasurements(int batchId) {
        return getMeasurementsByParentId(BatchQuantificationMeasurement.class,
                QuantificationMeasurementVO.class,
                BatchQuantificationMeasurement.Fields.BATCH,
                batchId,
                BatchQuantificationMeasurement.Fields.ID
            );
    }

    @Override
    public List<MeasurementVO> saveBatchSortingMeasurements(int batchId, List<MeasurementVO> sources) {
        Batch parent = getOne(Batch.class, batchId);
        return saveMeasurements(BatchSortingMeasurement.class, sources, parent.getSortingMeasurements(), parent);
    }

    @Override
    public List<QuantificationMeasurementVO> saveBatchQuantificationMeasurements(int batchId, List<QuantificationMeasurementVO> sources) {
        Batch parent = getOne(Batch.class, batchId);
        return saveMeasurements(BatchQuantificationMeasurement.class, sources, parent.getQuantificationMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveBatchSortingMeasurementsMap(int batchId, Map<Integer, String> sources) {
        Batch parent = getOne(Batch.class, batchId);
        Preconditions.checkNotNull(parent, "Could not found batch with id=" + batchId);
        return saveMeasurementsMap(BatchSortingMeasurement.class, sources, parent.getSortingMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveBatchQuantificationMeasurementsMap(int batchId, Map<Integer, String> sources) {
        Batch parent = getOne(Batch.class, batchId);
        return saveMeasurementsMap(BatchQuantificationMeasurement.class, sources, parent.getQuantificationMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> getProductSortingMeasurementsMap(int productId) {
        return getMeasurementsMapByParentId(ProductSortingMeasurement.class,
            ProductSortingMeasurement.Fields.PRODUCT,
            productId,
            null
        );
    }

    @Override
    public Map<Integer, String> getProductQuantificationMeasurementsMap(int productId) {
        return getMeasurementsMapByParentId(ProductQuantificationMeasurement.class,
            ProductQuantificationMeasurement.Fields.PRODUCT,
            productId,
            null
        );
    }

    @Override
    public List<MeasurementVO> saveProductSortingMeasurements(int productId, List<MeasurementVO> sources) {
        Product parent = getOne(Product.class, productId);
        Preconditions.checkNotNull(parent, "Could not found product with id=" + productId);
        return saveMeasurements(ProductSortingMeasurement.class, sources, parent.getSortingMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveProductQuantificationMeasurements(int productId, List<MeasurementVO> sources) {
        Product parent = getOne(Product.class, productId);
        Preconditions.checkNotNull(parent, "Could not found product with id=" + productId);
        return saveMeasurements(ProductQuantificationMeasurement.class, sources, parent.getQuantificationMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveProductSortingMeasurementsMap(int productId, Map<Integer, String> sources) {
        Product parent = getOne(Product.class, productId);
        Preconditions.checkNotNull(parent, "Could not found product with id=" + productId);
        return saveMeasurementsMap(ProductSortingMeasurement.class, sources, parent.getSortingMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveProductQuantificationMeasurementsMap(int productId, Map<Integer, String> sources) {
        Product parent = getOne(Product.class, productId);
        Preconditions.checkNotNull(parent, "Could not found product with id=" + productId);
        return saveMeasurementsMap(ProductQuantificationMeasurement.class, sources, parent.getQuantificationMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> saveVesselPhysicalMeasurements(int vesselFeaturesId, List<MeasurementVO> sources) {
        VesselFeatures parent = getOne(VesselFeatures.class, vesselFeaturesId);
        return saveMeasurements(VesselPhysicalMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public Map<Integer, String> saveVesselPhysicalMeasurementsMap(int vesselFeaturesId, Map<Integer, String> sources) {
        VesselFeatures parent = getOne(VesselFeatures.class, vesselFeaturesId);
        return saveMeasurementsMap(VesselPhysicalMeasurement.class, sources, parent.getMeasurements(), parent);
    }

    @Override
    public List<MeasurementVO> getVesselFeaturesMeasurements(int vesselFeaturesId) {
        return getMeasurementsByParentId(VesselPhysicalMeasurement.class,
                MeasurementVO.class,
                VesselPhysicalMeasurement.Fields.VESSEL_FEATURES,
                vesselFeaturesId,
                VesselPhysicalMeasurement.Fields.ID
        );
    }

    @Override
    public Map<Integer, String> getVesselFeaturesMeasurementsMap(int vesselFeaturesId) {
        return getMeasurementsMapByParentId(VesselPhysicalMeasurement.class,
                VesselPhysicalMeasurement.Fields.VESSEL_FEATURES,
                vesselFeaturesId,
                null
        );
    }

    @Override
    public <T extends IMeasurementEntity, V extends MeasurementVO> List<V> saveMeasurements(
            final Class<? extends IMeasurementEntity> entityClass,
            List<V> sources,
            List<T> target,
            final IDataEntity<?> parent) {

        final EntityManager em = getEntityManager();

        // Remember existing measurements, to be able to remove unused measurements
        // note: Need Beans.getList() to avoid NullPointerException if target=null
        final Map<Integer, T> sourceToRemove = Beans.splitById(Beans.getList(target));

        MutableShort rankOrder = new MutableShort(1);
        List<V> result = Lists.newArrayList();
        sources.forEach(source -> {
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
                    ((ISortedMeasurementEntity)entity).setRankOrder(rankOrder.getValue());
                    source.setRankOrder(rankOrder.getValue());
                    rankOrder.increment();
                }

                // Is reference ?
                if (entity instanceof IQuantifiedMeasurementEntity) {
                    ((IQuantifiedMeasurementEntity) entity).setIsReferenceQuantification(rankOrder.getValue() == 1);
                    ((IQuantifiedMeasurementEntity) entity).setSubgroupNumber(rankOrder.getValue() == 1 ? null : (short)(rankOrder.getValue() - 1));
                    rankOrder.increment();
                }

                // Set parent
                setParent(entity, getEntityClass(parent), parent.getId(), false);

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

            }
        });

        // Remove unused measurements
        if (MapUtils.isNotEmpty(sourceToRemove)) {
            sourceToRemove.values().forEach(em::remove);
        }

        return result;
    }

    @Override
    public <T extends IMeasurementEntity> List<T> getMeasurementEntitiesByParentId(Class<T> entityClass, String parentPropertyName, int parentId, String sortByPropertyName) {
        return getMeasurementsByParentIdQuery(entityClass, parentPropertyName, parentId, sortByPropertyName)
                .getResultList();
    }

    @Override
    public <T extends IMeasurementEntity> Map<Integer, Collection<T>> getMeasurementEntitiesByParentIds(
            Class<T> entityClass, String parentPropertyName, Collection<Integer> parentIds, String sortByPropertyName) {
        return Beans.<Integer, T>splitByNotUniqueProperty(
                getMeasurementsByParentIdsQuery(entityClass, parentPropertyName, parentIds, sortByPropertyName).getResultList(),
                parentPropertyName + "." + IEntity.Fields.ID)
                .asMap();

    }

    @Override
    public <T extends IMeasurementEntity> Map<Integer, Map<Integer, String>> getMeasurementsMapByParentIds(
            Class<T> entityClass, String parentPropertyName, Collection<Integer> parentIds, String sortByPropertyName) {
        return getMeasurementEntitiesByParentIds(entityClass, parentPropertyName, parentIds, sortByPropertyName)
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> toMeasurementsMap(e.getValue()))
                );

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

    @SuppressWarnings("unchecked")
    protected <T extends IDataEntity<?>> Class<T> getEntityClass(T source) {
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
        final ListMultimap<Integer, T> existingSources = Beans.splitByNotUniqueProperty(Beans.getList(target),
            StringUtils.doting(IMeasurementEntity.Fields.PMFM, IMeasurementEntity.Fields.ID));
        List<T> sourcesToRemove = Beans.getList(existingSources.values());
        short rankOrder = 1;
        for (Integer pmfmId: sources.keySet()) {
            String value = sources.get(pmfmId);

            if (StringUtils.isNotBlank(value)) {
                // Get existing meas and remove it from list to remove
                IMeasurementEntity entity = existingSources.containsKey(pmfmId) ? existingSources.get(pmfmId).get(0) : null;

                // Exists ?
                boolean isNew = (entity == null);
                if (isNew) {
                    try {
                        entity = entityClass.newInstance();
                    } catch (IllegalAccessException | InstantiationException e) {
                        throw new SumarisTechnicalException(e);
                    }
                }
                else {
                    sourcesToRemove.remove(entity);
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
                if (entity instanceof IQuantifiedMeasurementEntity) {
                    ((IQuantifiedMeasurementEntity) entity).setIsReferenceQuantification(rankOrder == 1);
                    ((IQuantifiedMeasurementEntity) entity).setSubgroupNumber(rankOrder++);
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

        // Remove unused measurements
        if (CollectionUtils.isNotEmpty(sourcesToRemove)) {
            sourcesToRemove.stream()
                // if the measurement is part of the sources
                .filter(entity -> sources.containsKey(entity.getPmfm().getId()))
                .forEach(entity -> getEntityManager().remove(entity));
        }

        return sources;
    }

    protected <T extends IMeasurementEntity, V extends MeasurementVO> List<V> getMeasurementsByParentId(Class<T> entityClass,
                                                                                        Class<? extends V> voClass,
                                                                                        String parentPropertyName,
                                                                                        int parentId,
                                                                                        String sortByPropertyName) {
        TypedQuery<T> query = getMeasurementsByParentIdQuery(entityClass, parentPropertyName, parentId, sortByPropertyName);
        return toMeasurementVOs(query.getResultList(), voClass);
    }

    protected <T extends IMeasurementEntity> Map<Integer, String> getMeasurementsMapByParentId(Class<T> entityClass,
                                                                                               String parentPropertyName,
                                                                                               int parentId,
                                                                                               String sortByPropertyName) {
        TypedQuery<T> query = getMeasurementsByParentIdQuery(entityClass, parentPropertyName,
                parentId, sortByPropertyName);
        return toMeasurementsMap(query.getResultList());
    }

    protected <T extends IMeasurementEntity> TypedQuery<T> getMeasurementsByParentIdQuery(Class<T> entityClass,
                                                                                          String parentPropertyName,
                                                                                          int parentId,
                                                                                          @Nullable String sortByPropertyName) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        ParameterExpression<Integer> idParam = builder.parameter(Integer.class);

        query.select(root)
                .where(builder.equal(root.get(parentPropertyName).get(IEntity.Fields.ID), idParam));

        // Order by
        if (sortByPropertyName != null) {
            query.orderBy(builder.asc(root.get(sortByPropertyName)));
        }

        return getEntityManager().createQuery(query)
                .setParameter(idParam, parentId);
    }

    protected <T extends IMeasurementEntity> TypedQuery<T> getMeasurementsByParentIdsQuery(Class<T> entityClass,
                                                                                          String parentPropertyName,
                                                                                          Collection<Integer> parentIds,
                                                                                          @Nullable String sortByPropertyName) {
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> root = query.from(entityClass);


        query.select(root).where(root.get(parentPropertyName).get(IEntity.Fields.ID).in(parentIds));

        // Order by
        if (sortByPropertyName != null) {
            query.orderBy(builder.asc(root.get(sortByPropertyName)));
        }

        return getEntityManager().createQuery(query);
    }

    protected <T extends IMeasurementEntity, V extends MeasurementVO> List<V> toMeasurementVOs(List<T> sources, Class<? extends V> voClass) {
        return sources.stream()
                .map(source -> toMeasurementVO(source, voClass))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public <T extends IMeasurementEntity> Map<Integer, String> toMeasurementsMap(Collection<T> sources) {
        if (sources == null) return null;
        return sources.stream()
                .filter(m -> m.getPmfm() != null && m.getPmfm().getId() != null)
                .collect(Collectors.<T, Integer, String>toMap(
                        m -> m.getPmfm().getId(),
                        this::entityToValueAsStringOrNull,
                        (s1, s2) -> s1
                ));
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
                target.setQualityFlag(load(QualityFlag.class, getConfig().getDefaultQualityFlagId()));
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

        PmfmVO pmfm = pmfmRepository.get(pmfmId);
        if (pmfm == null) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, "Unable to find pmfm with id=" + pmfmId);
        }

        PmfmValueType type = PmfmValueType.fromPmfm(pmfm);
        if (type == null) {
            throw new SumarisTechnicalException(ErrorCodes.BAD_REQUEST, "Unable to find the type of the pmfm with id=" + pmfmId);
        }

        switch (type) {
            case BOOLEAN:
                target.setNumericalValue(Boolean.parseBoolean(value) || "1".equals(value) ? 1d : 0d);
                break;
            case QUALITATIVE_VALUE:
                // If find a object structure (e.g. ReferentialVO), try to find the id
                target.setQualitativeValue(load(QualitativeValue.class, Integer.parseInt(value)));
                break;
            case STRING:
                target.setAlphanumericalValue(value);
                break;
            case DATE:
                target.setAlphanumericalValue(Dates.checkISODateTimeString(value));
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

    protected String entityToValueAsStringOrNull(IMeasurementEntity source) {
        Object value = entityToValue(source);
        return value != null ? value.toString() : null;
    }

    protected Object entityToValue(IMeasurementEntity source) {

        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getPmfm());
        Preconditions.checkNotNull(source.getPmfm().getId());

        // Get PMFM
        // /!\ IMPORTANT: should use a cached method !
        PmfmVO pmfm = pmfmRepository.get(source.getPmfm().getId());

        Preconditions.checkNotNull(pmfm, "Unable to find Pmfm with id=" + source.getPmfm().getId());

        PmfmValueType type = PmfmValueType.fromPmfm(pmfm);
        switch (type) {
            case BOOLEAN:
                return (source.getNumericalValue() != null && source.getNumericalValue() == 1d ? Boolean.TRUE : Boolean.FALSE);
            case QUALITATIVE_VALUE:
                // If find a object structure (e.g. ReferentialVO), try to find the id
                return ((source.getQualitativeValue() != null && source.getQualitativeValue().getId() != null) ? source.getQualitativeValue().getId() : null);
            case STRING:
            case DATE:
                return source.getAlphanumericalValue();
            case INTEGER:
                return ((source.getNumericalValue() != null) ? source.getNumericalValue().intValue() : null);
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
            target.setQualityFlag(load(QualityFlag.class, getConfig().getDefaultQualityFlagId()));
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

        // Product quantification measurement
        else if (target instanceof ProductQuantificationMeasurement) {
            if (parentId == null) {
                ((ProductQuantificationMeasurement) target).setProduct(null);
            } else {
                ((ProductQuantificationMeasurement) target).setProduct(load(Product.class, parentId));
            }
        }

        // Product sorting measurement
        else if (target instanceof ProductSortingMeasurement) {
            if (parentId == null) {
                ((ProductSortingMeasurement) target).setProduct(null);
            } else {
                ((ProductSortingMeasurement) target).setProduct(load(Product.class, parentId));
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

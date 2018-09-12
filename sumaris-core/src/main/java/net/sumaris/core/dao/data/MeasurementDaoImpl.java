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
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.data.*;
import net.sumaris.core.model.data.measure.GearUseMeasurement;
import net.sumaris.core.model.data.measure.IMeasurementEntity;
import net.sumaris.core.model.data.measure.PhysicalGearMeasurement;
import net.sumaris.core.model.data.measure.VesselUseMeasurement;
import net.sumaris.core.model.referential.Pmfm;
import net.sumaris.core.model.referential.QualitativeValue;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.data.MeasurementVO;
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
    }

    @Autowired
    private ReferentialDao referentialDao;


    @Override
    public List<MeasurementVO> getVesselUseMeasurementsByTripId(int tripId) {
        return getMeasurementsByParentId(VesselUseMeasurement.class,
                VesselUseMeasurement.PROPERTY_TRIP,
                tripId,
                VesselUseMeasurement.PROPERTY_RANK_ORDER
        );
    }

    @Override
    public List<MeasurementVO> getPhysicalGearMeasurement(int physicalGearId) {
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

        if (!isNew) {
            // Lock entityName
            // TODO: Use an optimistic lock, as we already lock the parent entity
            //lockForUpdate(entity, LockModeType.OPTIMISTIC);
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
        Preconditions.checkNotNull(sortByPropertyName);

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        ParameterExpression<Integer> idParam = builder.parameter(Integer.class);

        query.select(root)
             .where(builder.equal(root.get(parentPropertyName).get(IRootDataEntity.PROPERTY_ID), idParam))
             // Order byldev
             .orderBy(builder.asc(root.get(sortByPropertyName)));

        TypedQuery<T> q = getEntityManager().createQuery(query)
                .setParameter(idParam, parentId);
        return toMeasurementVOs(q.getResultList());
    }

    protected <T extends IMeasurementEntity> List<MeasurementVO> toMeasurementVOs(List<T> source) {
        return source.stream()
                .map(this::toMeasurementVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

        // If vessel use measurement
        if (target instanceof VesselUseMeasurement) {
            // Trip
            if (copyIfNull || source.getTripId() != null) {
                if (source.getTripId() == null) {
                    ((VesselUseMeasurement) target).setTrip(null);
                } else {
                    ((VesselUseMeasurement) target).setTrip(load(Trip.class, source.getTripId()));
                }
            }

            // Operation
            if (copyIfNull || source.getOperationId() != null) {
                if (source.getOperationId() == null) {
                    ((VesselUseMeasurement) target).setOperation(null);
                } else {
                    ((VesselUseMeasurement) target).setOperation(load(Operation.class, source.getOperationId()));
                }
            }
        }

        // If gear use measurement
        if (target instanceof GearUseMeasurement) {
            // Operation
            if (copyIfNull || source.getOperationId() != null) {
                if (source.getOperationId() == null) {
                    ((GearUseMeasurement) target).setOperation(null);
                } else {
                    ((GearUseMeasurement) target).setOperation(load(Operation.class, source.getOperationId()));
                }
            }
        }

        // If physical gear measurement
        if (target instanceof PhysicalGearMeasurement) {
            // Physical gear
            if (copyIfNull || source.getPhysicalGearId() != null) {
                if (source.getPhysicalGearId() == null) {
                    ((PhysicalGearMeasurement) target).setPhysicalGear(null);
                } else {
                    ((PhysicalGearMeasurement) target).setPhysicalGear(load(PhysicalGear.class, source.getPhysicalGearId()));
                }
            }
        }
    }

}

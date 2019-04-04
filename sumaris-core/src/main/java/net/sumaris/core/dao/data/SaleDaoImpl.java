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
import net.sumaris.core.dao.administration.user.PersonDao;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.data.Vessel;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.SaleVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository("saleDao")
public class SaleDaoImpl extends BaseDataDaoImpl implements SaleDao {

    /** Logger. */
    private static final Logger log =
            LoggerFactory.getLogger(SaleDaoImpl.class);

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PersonDao personDao;

    @Autowired
    private VesselDao vesselDao;

    @Override
    @SuppressWarnings("unchecked")
    public List<SaleVO> getAllByTripId(int tripId) {

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Sale> query = cb.createQuery(Sale.class);
        Root<Sale> saleRoot = query.from(Sale.class);

        query.select(saleRoot);

        ParameterExpression<Integer> tripIdParam = cb.parameter(Integer.class);

        query.where(cb.equal(saleRoot.get(Sale.PROPERTY_TRIP).get(Trip.PROPERTY_ID), tripIdParam));

        return toSaleVOs(getEntityManager().createQuery(query)
                .setParameter(tripIdParam, tripId).getResultList(), false);
    }


    @Override
    public SaleVO get(int id) {
        Sale entity = get(Sale.class, id);
        return toSaleVO(entity, false);
    }

    @Override
    public List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sources) {
        // Load parent entity
        Trip parent = get(Trip.class, tripId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getOperations()));

        // Save each gears
        List<SaleVO> result = sources.stream().map(source -> {
            source.setTripId(tripId);
            source.setProgram(parentProgram);

            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).collect(Collectors.toList());

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::delete);
        }

        return result;
    }

    @Override
    public SaleVO save(SaleVO source) {
        Preconditions.checkNotNull(source);

        EntityManager entityManager = getEntityManager();
        Sale entity = null;
        if (source.getId() != null) {
            entity = get(Sale.class, source.getId());
        }
        boolean isNew = (entity == null);
        if (isNew) {
            entity = new Sale();
        }

        if (!isNew) {
            // Check update date
            checkUpdateDateForUpdate(source, entity);

            // Lock entityName
            //lockForUpdate(entity);
        }

        // Copy some fields from the trip
        copySomeFieldsFromTrip(source);

        // VO -> Entity
        saleVOToEntity(source, entity, true);

        // Update update_dt
        Timestamp newUpdateDate = getDatabaseCurrentTimestamp();
        entity.setUpdateDate(newUpdateDate);

        // Save entityName
        if (isNew) {
            // Force creation date
            entity.setCreationDate(newUpdateDate);
            source.setCreationDate(newUpdateDate);

            entityManager.persist(entity);
            source.setId(entity.getId());
        } else {
            entityManager.merge(entity);
        }

        source.setUpdateDate(newUpdateDate);

        entityManager.flush();
        entityManager.clear();

        return source;
    }

    @Override
    public void delete(int id) {

        log.debug(String.format("Deleting sale {id=%s}...", id));
        delete(Sale.class, id);
    }

    @Override
    public SaleVO toSaleVO(Sale source) {
        return this.toSaleVO(source, true);
    }

    public SaleVO toSaleVO(Sale source, boolean allFields) {
        if (source == null) return null;

        SaleVO target = new SaleVO();

        Beans.copyProperties(source, target);

        // Sale location
        target.setSaleLocation(locationDao.toLocationVO(source.getSaleLocation()));

        // Sale type
        ReferentialVO saleType = referentialDao.toReferentialVO(source.getSaleType());
        target.setSaleType(saleType);

        if (allFields) {
            target.setVesselFeatures(vesselDao.getByVesselIdAndDate(source.getVessel().getId(), source.getStartDateTime()));
            target.setQualityFlagId(source.getQualityFlag().getId());

            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class);
            target.setRecorderDepartment(recorderDepartment);

            // Recorder person
            if (source.getRecorderPerson() != null) {
                PersonVO recorderPerson = personDao.toPersonVO(source.getRecorderPerson());
                target.setRecorderPerson(recorderPerson);
            }
        }

        return target;
    }

    /* -- protected methods -- */

    protected void copySomeFieldsFromTrip(SaleVO target) {
        TripVO source = target.getTrip();
        if (source == null) return;

        target.setRecorderDepartment(source.getRecorderDepartment());
        target.setRecorderPerson(source.getRecorderPerson());
        target.setVesselFeatures(source.getVesselFeatures());
        target.setQualityFlagId(source.getQualityFlagId());

    }

    protected List<SaleVO> toSaleVOs(List<Sale> source, boolean allFields) {
        return this.toSaleVOs(source.stream(), allFields);
    }

    protected List<SaleVO> toSaleVOs(Stream<Sale> source, boolean allFields) {
        return source.map(s -> this.toSaleVO(s, allFields))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected void saleVOToEntity(SaleVO source, Sale target, boolean copyIfNull) {

        copyRootDataProperties(source, target, copyIfNull);

        // Vessel
        if (copyIfNull || (source.getVesselFeatures() != null && source.getVesselFeatures().getVesselId() != null)) {
            if (source.getVesselFeatures() == null || source.getVesselFeatures().getVesselId() == null) {
                target.setVessel(null);
            }
            else {
                target.setVessel(load(Vessel.class, source.getVesselFeatures().getVesselId()));
            }
        }

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || (tripId != null)) {
            if (tripId == null) {
                target.setTrip(null);
            }
            else {
                target.setTrip(load(Trip.class, tripId));
            }
        }

        // Sale location
        if (copyIfNull || source.getSaleLocation() != null) {
            if (source.getSaleLocation() == null || source.getSaleLocation().getId() == null) {
                target.setSaleLocation(null);
            }
            else {
                target.setSaleLocation(load(Location.class, source.getSaleLocation().getId()));
            }
        }

        // Sale type
        if (copyIfNull || source.getSaleType() != null) {
            if (source.getSaleType() == null || source.getSaleType().getId() == null) {
                target.setSaleType(null);
            }
            else {
                target.setSaleType(load(SaleType.class, source.getSaleType().getId()));
            }
        }
    }
}

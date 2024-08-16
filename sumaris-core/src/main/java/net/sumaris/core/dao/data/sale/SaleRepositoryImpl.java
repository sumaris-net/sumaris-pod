package net.sumaris.core.dao.data.sale;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import net.sumaris.core.dao.administration.user.PersonRepository;
import net.sumaris.core.dao.data.RootDataRepositoryImpl;
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.dao.data.fishingArea.FishingAreaRepository;
import net.sumaris.core.dao.referential.ReferentialDao;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.model.data.IWithSalesEntity;
import net.sumaris.core.model.data.Landing;
import net.sumaris.core.model.data.Sale;
import net.sumaris.core.model.data.Trip;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.SaleFetchOptions;
import net.sumaris.core.vo.data.SaleVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.filter.SaleFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * @author peck7 on 01/09/2020.
 */
public class SaleRepositoryImpl
    extends RootDataRepositoryImpl<Sale, SaleVO, SaleFilterVO, SaleFetchOptions>
    implements SaleSpecifications {

    static {
        I18n.n("sumaris.persistence.table.sale");
    }

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ReferentialDao referentialDao;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private FishingAreaRepository fishingAreaRepository;

    protected SaleRepositoryImpl(EntityManager entityManager, GenericConversionService conversionService) {
        super(Sale.class, SaleVO.class, entityManager);
        conversionService.addConverter(Sale.class, SaleVO.class, this::toVO);
    }

    @Override
    public void toVO(Sale source, SaleVO target, SaleFetchOptions fetchOptions, boolean copyIfNull) {
        super.toVO(source, target, fetchOptions, copyIfNull);

        // Sale location
        target.setSaleLocation(locationRepository.toVO(source.getSaleLocation()));

        // Sale type
        ReferentialVO saleType = referentialDao.toVO(source.getSaleType());
        target.setSaleType(saleType);

        // Vessel
        target.setVesselId(source.getVessel().getId());

        // Quality flag
        target.setQualityFlagId(source.getQualityFlag().getId());

        // Trip
        Integer tripId = source.getTrip() != null ? source.getTrip().getId() : null;
        if (tripId != null || copyIfNull) {
            target.setTripId(tripId);
        }

        // Landing
        Integer landingId = source.getLanding() != null ? source.getLanding().getId() : null;
        if (landingId != null || copyIfNull) {
            target.setLandingId(landingId);
        }

        // Fishing areas (default is false)
        if (fetchOptions != null && (fetchOptions.isWithChildrenEntities() || fetchOptions.isWithFishingAreas())) {
            target.setFishingAreas(fishingAreaRepository.findAllVO(fishingAreaRepository.hasSaleId(source.getId())));
        }

        // Batches (default is false)
        if (fetchOptions != null && (fetchOptions.isWithChildrenEntities() || fetchOptions.isWithBatches())) {
            target.setBatches(batchRepository.findAllVO(batchRepository.hasSaleId(source.getId()),
                    BatchFetchOptions.builder()
                            .withChildrenEntities(false) // Use flat list, not a tree
                            .withRecorderDepartment(false)
                            .withMeasurementValues(true)
                            .build()));
        }

        // Fetch children (default is false)
        if (fetchOptions != null && fetchOptions.isWithChildrenEntities()) {

            // Recorder department
            DepartmentVO recorderDepartment = referentialDao.toTypedVO(source.getRecorderDepartment(), DepartmentVO.class).orElse(null);
            target.setRecorderDepartment(recorderDepartment);

            // Recorder person
            if (source.getRecorderPerson() != null) {
                PersonVO recorderPerson = personRepository.toVO(source.getRecorderPerson());
                target.setRecorderPerson(recorderPerson);
            }
        }
    }

    @Override
    public List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sources) {
        // Load parent entity
        Trip parent = getById(Trip.class, tripId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Fill parentId and program
        sources.forEach(source -> {
            source.setTripId(tripId);
            source.setProgram(parentProgram);
        });

        // Save all, by parent
        return saveAllByParent(parent, sources);
    }

    @Override
    public List<SaleVO> saveAllByLandingId(int landingId, List<SaleVO> sources) {
        // Load parent entity
        Landing parent = getById(Landing.class, landingId);
        ProgramVO parentProgram = new ProgramVO();
        parentProgram.setId(parent.getProgram().getId());

        // Fill parentId and program
        sources.forEach(source -> {
            source.setLandingId(landingId);
            source.setProgram(parentProgram);
        });

        // Save all, by parent
        return saveAllByParent(parent, sources);
    }

    @Override
    public void toEntity(SaleVO source, Sale target, boolean copyIfNull) {

        // copy some fields from trip
        TripVO trip = source.getTrip();
        if (trip != null) {
            source.setRecorderDepartment(trip.getRecorderDepartment());
            source.setRecorderPerson(trip.getRecorderPerson());
            source.setVesselSnapshot(trip.getVesselSnapshot());
            source.setQualityFlagId(trip.getQualityFlagId());
        }

        super.toEntity(source, target, copyIfNull);

        // Trip
        Integer tripId = source.getTripId() != null ? source.getTripId() : (source.getTrip() != null ? source.getTrip().getId() : null);
        if (copyIfNull || tripId != null) {
            if (tripId == null) {
                target.setTrip(null);
            }
            else {
                target.setTrip(getReference(Trip.class, tripId));
            }
        }

        // Landing
        Integer landingId = source.getLandingId() != null ? source.getLandingId() : (source.getLanding() != null ? source.getLanding().getId() : null);
        if (copyIfNull || landingId != null) {
            if (landingId == null) {
                target.setLanding(null);
            }
            else {
                target.setLanding(getReference(Landing.class, landingId));
            }
        }

        // Sale location
        if (copyIfNull || source.getSaleLocation() != null) {
            if (source.getSaleLocation() == null || source.getSaleLocation().getId() == null) {
                target.setSaleLocation(null);
            }
            else {
                target.setSaleLocation(getReference(Location.class, source.getSaleLocation().getId()));
            }
        }

        // Sale type
        if (copyIfNull || source.getSaleType() != null) {
            if (source.getSaleType() == null || source.getSaleType().getId() == null) {
                target.setSaleType(null);
            }
            else {
                target.setSaleType(getReference(SaleType.class, source.getSaleType().getId()));
            }
        }
    }


    @Override
    protected Specification<Sale> toSpecification(SaleFilterVO filter, SaleFetchOptions fetchOptions) {
        return super.toSpecification(filter, fetchOptions)
            .and(excludedIds(filter.getExcludedIds()))
            .and(includedIds(filter.getIncludedIds()))
            // Location
            .and(hasSaleLocationIds(filter.getLocationId() != null ? new Integer[]{filter.getLocationId()} : filter.getLocationIds()))
            // Parent
            .and(hasTripId(filter.getTripId()))
            .and(hasLandingId(filter.getLandingId()))
            // Quality
            .and(inDataQualityStatus(filter.getDataQualityStatus()))
            // Denormalization
            .and(needBatchDenormalization(filter.getNeedBatchDenormalization()));
    }

    protected List<SaleVO> saveAllByParent(IWithSalesEntity<Integer, Sale> parent, List<SaleVO> sales) {

        // Remember existing entities
        final List<Integer> sourcesIdsToRemove = Beans.collectIds(Beans.getList(parent.getSales()));

        // Save each entity
        List<SaleVO> result = sales.stream().map(source -> {
            if (source.getId() != null) {
                sourcesIdsToRemove.remove(source.getId());
            }
            return save(source);
        }).toList();

        // Remove unused entities
        if (CollectionUtils.isNotEmpty(sourcesIdsToRemove)) {
            sourcesIdsToRemove.forEach(this::deleteById);
        }

        return result;
    }
}

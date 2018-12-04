package net.sumaris.core.service.data;

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
import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.SaleDao;
import net.sumaris.core.dao.technical.Beans;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.data.TripDao;
import net.sumaris.core.model.data.measure.VesselUseMeasurement;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("tripService")
public class TripServiceImpl implements TripService {

	private static final Log log = LogFactory.getLog(TripServiceImpl.class);

	@Autowired
	protected TripDao tripDao;

	@Autowired
	protected SaleDao saleDao;

	@Autowired
	protected SaleService saleService;

	@Autowired
	protected OperationService operationService;

	@Autowired
	protected PhysicalGearService physicalGearService;

	@Autowired
	protected MeasurementDao measurementDao;


	@Override
	public List<TripVO> getAllTrips(int offset, int size) {
		return findByFilter(null, offset, size, null, null);
	}

	@Override
	public List<TripVO> findByFilter(TripFilterVO filter, int offset, int size) {
		return findByFilter(filter, offset, size, null, null);
	}

	@Override
	public List<TripVO> findByFilter(TripFilterVO filter, int offset, int size, String sortAttribute,
                                     SortDirection sortDirection) {
		if (filter == null) {
			return tripDao.getAllTrips(offset, size, sortAttribute, sortDirection);
		}

		return tripDao.findByFilter(filter, offset, size, sortAttribute, sortDirection);
	}

	@Override
	public Long countByFilter(TripFilterVO filter) {
		return tripDao.countByFilter(filter);
	}

	@Override
	public TripVO get(int tripId) {
		return tripDao.get(tripId);
	}

	@Override
	public <T> T get(int id, Class<T> targetClass) {
		return tripDao.get(id, targetClass);
	}

	@Override
	public TripVO save(final TripVO source, final boolean withOperation) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getProgram(), "Missing program");
		Preconditions.checkArgument(source.getProgram().getId() != null || source.getProgram().getLabel() != null, "Missing program.id or program.label");
		Preconditions.checkNotNull(source.getDepartureDateTime(), "Missing departureDateTime");
		Preconditions.checkNotNull(source.getDepartureLocation(), "Missing departureLocation");
		Preconditions.checkNotNull(source.getDepartureLocation().getId(), "Missing departureLocation.id");
		Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing recorderDepartment");
		Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing recorderDepartment.id");
		Preconditions.checkNotNull(source.getVesselFeatures(), "Missing vesselFeatures");
		Preconditions.checkNotNull(source.getVesselFeatures().getVesselId(), "Missing vesselFeatures.vesselId");
		Preconditions.checkArgument(Objects.isNull(source.getSale()) || CollectionUtils.isEmpty(source.getSales()), "Must not have both 'sales' and 'sale' attributes");

		TripVO savedTrip = tripDao.save(source);

		// Save sales
		if (CollectionUtils.isNotEmpty(source.getSales())) {
			List<SaleVO> sales = Beans.getList(source.getSales());
			sales.forEach(g -> fillDefaultProperties(savedTrip, g));
			sales = saleService.saveAllByTripId(savedTrip.getId(), sales);
			savedTrip.setSales(sales);
		}
		else if (source.getSale() != null) {
			SaleVO sale = source.getSale();
			fillDefaultProperties(savedTrip, sale);
			List<SaleVO> sales = saleService.saveAllByTripId(savedTrip.getId(), ImmutableList.of(sale));
			savedTrip.setSale(sales.get(0));
		}
		else {
			// Remove all
			saleService.saveAllByTripId(savedTrip.getId(), ImmutableList.of());
		}

		// Save physical gears
		List<PhysicalGearVO> gears = Beans.getList(source.getGears());
		gears.forEach(g -> fillDefaultProperties(savedTrip, g));
		gears = physicalGearService.save(savedTrip.getId(), gears);
		savedTrip.setGears(gears);

		// Save operations (only if asked)
		if (withOperation) {
			List<OperationVO> operations = Beans.getList(source.getOperations());
			operations = operationService.saveAllByTripId(savedTrip.getId(), operations);
			savedTrip.setOperations(operations);
		}

		// Save measurements
		List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
		measurements.forEach(m -> fillDefaultProperties(savedTrip, m));
		measurements = measurementDao.saveVesselUseMeasurementsByTripId(savedTrip.getId(), measurements);
		savedTrip.setMeasurements(measurements);

		return savedTrip;
	}

	@Override
	public List<TripVO> save(List<TripVO> trips, final boolean withOperation) {
		Preconditions.checkNotNull(trips);

		return trips.stream()
				.map(t -> save(t, withOperation))
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		tripDao.delete(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	/* protected methods */

	void fillDefaultProperties(TripVO parent, SaleVO sale) {
		if (sale == null) return;

		// Copy recorder department from the parent trip
		if (sale.getRecorderDepartment() == null || sale.getRecorderDepartment().getId() == null) {
            sale.setRecorderDepartment(parent.getRecorderDepartment());
        }
        // Copy recorder person from the parent trip
        if (sale.getRecorderPerson() == null || sale.getRecorderPerson().getId() == null) {
            sale.setRecorderPerson(parent.getRecorderPerson());
        }
        // Copy vessel from the parent trip
        if (sale.getVesselFeatures() == null || sale.getVesselFeatures().getVesselId() == null) {
            sale.setVesselFeatures(parent.getVesselFeatures());
        }

        sale.setTripId(parent.getId());
	}

    void fillDefaultProperties(TripVO parent, PhysicalGearVO gear) {
        if (gear == null) return;

        // Copy recorder department from the parent trip
        if (gear.getRecorderDepartment() == null || gear.getRecorderDepartment().getId() == null) {
            gear.setRecorderDepartment(parent.getRecorderDepartment());
        }
        // Copy recorder person from the parent trip
        if (gear.getRecorderPerson() == null || gear.getRecorderPerson().getId() == null) {
            gear.setRecorderPerson(parent.getRecorderPerson());
        }

        gear.setTripId(parent.getId());
    }

	void fillDefaultProperties(TripVO parent, MeasurementVO measurement) {
		if (measurement == null) return;

		// Copy recorder department from the parent
		if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
			measurement.setRecorderDepartment(parent.getRecorderDepartment());
		}
		// Copy recorder person from the parent
		if (measurement.getRecorderPerson() == null || measurement.getRecorderPerson().getId() == null) {
			measurement.setRecorderPerson(parent.getRecorderPerson());
		}

		measurement.setTripId(parent.getId());
		measurement.setEntityName(VesselUseMeasurement.class.getSimpleName());
	}
}

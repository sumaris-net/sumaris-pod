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
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.sale.SaleRepository;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.SaleMeasurement;
import net.sumaris.core.service.data.vessel.VesselSnapshotService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.filter.SaleFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("saleService")
@Slf4j
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

	protected final SaleRepository saleRepository;

	protected final MeasurementDao measurementDao;

	protected final FishingAreaService fishingAreaService;

	protected final ProductService productService;

	protected final BatchService batchService;

	protected final VesselSnapshotService vesselSnapshotService;

	@Override
	public List<SaleVO> getAllByTripId(int tripId, SaleFetchOptions fetchOptions) {
		List<SaleVO> targets = saleRepository.findAll(SaleFilterVO.builder().tripId(tripId).build(), fetchOptions);

		// Fill vessels
		if (fetchOptions != null && fetchOptions.isWithVesselSnapshot()) this.fillVesselSnapshots(targets);

		return targets;
	}

	@Override
	public List<SaleVO> getAllByLandingId(int landingId, SaleFetchOptions fetchOptions) {
		List<SaleVO> targets = saleRepository.findAll(SaleFilterVO.builder().landingId(landingId).build(), fetchOptions);

		// Fill vessel snapshots
		if (fetchOptions != null && fetchOptions.isWithVesselSnapshot()) this.fillVesselSnapshots(targets);

		return targets;
	}

	@Override
	public SaleVO get(int saleId) {
		return get(saleId, SaleFetchOptions.DEFAULT);
	}

	@Override
	public SaleVO get(int saleId, SaleFetchOptions fetchOptions) {
		SaleVO target = saleRepository.get(saleId, fetchOptions);

		// Fill vessel snapshot
		if (fetchOptions != null && fetchOptions.isWithVesselSnapshot()) fillVesselSnapshot(target);

		return target;
	}

	@Override
	public int getProgramIdById(int id) {
		return saleRepository.get(id, SaleFetchOptions.MINIMAL).getProgram().getId();
	}

	@Override
	public List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sources) {
		Preconditions.checkNotNull(sources);
		sources.forEach(this::checkSale);

		List<SaleVO> saved = saleRepository.saveAllByTripId(tripId, sources);

		saved.forEach(this::saveChildrenEntities);

		return saved;
	}

	@Override
	public List<SaleVO> saveAllByLandingId(int landingId, List<SaleVO> sources) {
		Preconditions.checkNotNull(sources);
		sources.forEach(this::checkSale);

		List<SaleVO> saved = saleRepository.saveAllByLandingId(landingId, sources);

		saved.forEach(this::saveChildrenEntities);

		return saved;
	}

	@Override
	public SaleVO save(SaleVO sale) {
		checkSale(sale);

		// Reset control date
		sale.setControlDate(null);

		SaleVO savedSale = saleRepository.save(sale);

		saveChildrenEntities(savedSale);

		return savedSale;
	}

	@Override
	public List<SaleVO> save(List<SaleVO> sales) {
		Preconditions.checkNotNull(sales);

		return sales.stream()
				.map(this::save)
				.collect(Collectors.toList());
	}

	@Override
	public void delete(int id) {
		saleRepository.deleteById(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	@Override
	public void fillVesselSnapshot(SaleVO target) {
		if (target.getVesselId() != null && target.getVesselSnapshot() == null) {
			target.setVesselSnapshot(vesselSnapshotService.getByIdAndDate(target.getVesselId(), Dates.resetTime(target.getVesselDateTime())));
		}
	}

	@Override
	public void fillVesselSnapshots(List<SaleVO> target) {
		target.parallelStream().forEach(this::fillVesselSnapshot);
	}

	/* -- protected methods -- */

	protected void checkSale(final SaleVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getStartDateTime(), "Missing startDateTime");
		Preconditions.checkNotNull(source.getSaleLocation(), "Missing saleLocation");
		Preconditions.checkNotNull(source.getSaleLocation().getId(), "Missing saleLocation.id");
		Preconditions.checkNotNull(source.getSaleType(), "Missing saleType");
		Preconditions.checkNotNull(source.getSaleType().getId(), "Missing saleType.id");
		Preconditions.checkNotNull(source.getRecorderDepartment(), "Missing sale.recorderDepartment");
		Preconditions.checkNotNull(source.getRecorderDepartment().getId(), "Missing sale.recorderDepartment.id");
	}

	protected void saveChildrenEntities(final SaleVO source) {

		// Save measurements
		if (source.getMeasurementValues() != null) {
			measurementDao.saveSaleMeasurementsMap(source.getId(), source.getMeasurementValues());
		}
		else {
			List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
			measurements.forEach(m -> fillDefaultProperties(source, m, SaleMeasurement.class));
			measurements = measurementDao.saveSaleMeasurements(source.getId(), measurements);
			source.setMeasurements(measurements);
		}

		// Fishing areas
		if (source.getFishingAreas() != null) {
			source.getFishingAreas().forEach(fishingArea -> fillDefaultProperties(source, fishingArea));
			fishingAreaService.saveAllBySaleId(source.getId(), source.getFishingAreas());
		}

		// Save produces
		if (source.getProducts() != null) {
			source.getProducts().forEach(product -> fillDefaultProperties(source, product));
			source.setProducts(
				productService.saveBySaleId(source.getId(), source.getProducts())
			);
		}

		// Save batches
		List<BatchVO> batches = getAllBatches(source);
		if (batches != null) {
			batches = batchService.saveAllBySaleId(source.getId(), batches);

			// Transform saved batches into flat list (e.g. to be used as graphQL query response)
			batches.forEach(batch -> {
				// Set parentId (instead of parent object)
				if (batch.getParentId() == null && batch.getParent() != null) {
					batch.setParentId(batch.getParent().getId());
				}
				// Remove link parent/children
				batch.setParent(null);
				batch.setChildren(null);
			});

			source.setCatchBatch(null);
			source.setBatches(batches);
		}

	}

	protected void fillDefaultProperties(SaleVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
		if (measurement == null) return;

		// Copy recorder department from the parent
		if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
			measurement.setRecorderDepartment(parent.getRecorderDepartment());
		}

		measurement.setEntityName(entityClass.getSimpleName());
	}

	protected void fillDefaultProperties(SaleVO parent, FishingAreaVO fishingArea) {
		fishingArea.setSaleId(parent.getId());
	}

	protected void fillDefaultProperties(SaleVO parent, ProductVO product) {
		if (product == null) return;

		// Copy recorder department from the parent
		if (product.getRecorderDepartment() == null || product.getRecorderDepartment().getId() == null) {
			product.setRecorderDepartment(parent.getRecorderDepartment());
		}

		product.setSaleId(parent.getId());
	}

	protected void fillDefaultProperties(SaleVO parent, BatchVO batch) {
		if (batch == null) return;

		// Copy recorder department from the parent
		if (batch.getRecorderDepartment() == null || batch.getRecorderDepartment().getId() == null) {
			batch.setRecorderDepartment(parent.getRecorderDepartment());
		}

		batch.setSaleId(parent.getId());
	}

	protected void fillDefaultProperties(BatchVO parent, BatchVO batch) {
		if (batch == null) return;

		// Copy recorder department from the parent
		if (batch.getRecorderDepartment() == null || batch.getRecorderDepartment().getId() == null) {
			batch.setRecorderDepartment(parent.getRecorderDepartment());
		}

		if (parent.getId() == null) {
			// Need to be the parent object, when parent has not id yet (see issue #2)
			batch.setParent(parent);
		} else {
			batch.setParentId(parent.getId());
		}
		batch.setSaleId(parent.getSaleId());
	}

	protected List<BatchVO> getAllBatches(SaleVO parent) {
		BatchVO catchBatch = parent.getCatchBatch();
		if (catchBatch == null) return null;

		fillDefaultProperties(parent, catchBatch);
		List<BatchVO> result = Lists.newArrayList();
		addAllBatchesToList(catchBatch, result);
		return result;
	}

	protected void addAllBatchesToList(final BatchVO batch, final List<BatchVO> result) {
		if (batch == null) return;

		// Add the batch itself
		if (!result.contains(batch)) result.add(batch);

		// Process children
		if (CollectionUtils.isNotEmpty(batch.getChildren())) {
			// Recursive call
			batch.getChildren().forEach(child -> {
				fillDefaultProperties(batch, child);
				addAllBatchesToList(child, result);
			});
		}
	}
}

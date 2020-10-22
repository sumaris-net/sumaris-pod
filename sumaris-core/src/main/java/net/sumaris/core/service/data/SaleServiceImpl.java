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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.MeasurementDao;
import net.sumaris.core.dao.data.sale.SaleRepository;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.SaleMeasurement;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.ProductVO;
import net.sumaris.core.vo.data.SaleVO;
import net.sumaris.core.vo.filter.SaleFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("saleService")
@Slf4j
public class SaleServiceImpl implements SaleService {

	@Autowired
	protected SaleRepository saleRepository;

	@Autowired
	protected MeasurementDao measurementDao;

	@Autowired
	protected ProductService productService;

	@Override
	public List<SaleVO> getAllByTripId(int tripId, DataFetchOptions fetchOptions) {
		return saleRepository.findAll(SaleFilterVO.builder().tripId(tripId).build(), fetchOptions);
	}

	@Override
	public SaleVO get(int saleId) {
		return get(saleId, null);
	}

	@Override
	public SaleVO get(int saleId, DataFetchOptions fetchOptions) {
		return saleRepository.get(saleId, fetchOptions);
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
	public SaleVO save(SaleVO sale) {
		checkSale(sale);

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

		// Save produces
		if (source.getProducts() != null) source.getProducts().forEach(product -> fillDefaultProperties(source, product));
		productService.saveBySaleId(source.getId(), source.getProducts());
	}

	protected void fillDefaultProperties(SaleVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
		if (measurement == null) return;

		// Copy recorder department from the parent
		if (measurement.getRecorderDepartment() == null || measurement.getRecorderDepartment().getId() == null) {
			measurement.setRecorderDepartment(parent.getRecorderDepartment());
		}

		measurement.setEntityName(entityClass.getSimpleName());
	}

	protected void fillDefaultProperties(SaleVO parent, ProductVO product) {
		if (product == null) return;

		// Copy recorder department from the parent
		if (product.getRecorderDepartment() == null || product.getRecorderDepartment().getId() == null) {
			product.setRecorderDepartment(parent.getRecorderDepartment());
		}

		product.setSaleId(parent.getId());
	}
}

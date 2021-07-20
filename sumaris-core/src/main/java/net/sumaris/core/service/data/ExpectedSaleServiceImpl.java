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
import net.sumaris.core.dao.data.sale.ExpectedSaleRepository;
import net.sumaris.core.model.data.IMeasurementEntity;
import net.sumaris.core.model.data.SaleMeasurement;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.data.ExpectedSaleVO;
import net.sumaris.core.vo.data.MeasurementVO;
import net.sumaris.core.vo.data.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service("expectedSaleService")
@Slf4j
public class ExpectedSaleServiceImpl implements ExpectedSaleService {

	@Autowired
	protected ExpectedSaleRepository expectedSaleRepository;

	@Autowired
	protected MeasurementDao measurementDao;

	@Autowired
	protected ProductService productService;

	@Override
	public List<ExpectedSaleVO> getAllByTripId(int tripId) {
		return expectedSaleRepository.getAllByTripId(tripId);
	}

//	@Override
//	public ExpectedSaleVO get(int saleId) {
//		return get(saleId, null);
//	}

	@Override
	public List<ExpectedSaleVO> saveAllByTripId(int tripId, List<ExpectedSaleVO> sources) {
		Preconditions.checkNotNull(sources);
		sources.forEach(this::checkExpectedSale);

		List<ExpectedSaleVO> saved = expectedSaleRepository.saveAllByTripId(tripId, sources);

		saved.forEach(this::saveChildrenEntities);

		return saved;
	}

//	@Override
//	public ExpectedSaleVO save(ExpectedSaleVO sale) {
//		checkSale(sale);
//
//		ExpectedSaleVO savedSale = expectedSaleRepository.save(sale);
//
//		saveChildrenEntities(savedSale);
//
//		return savedSale;
//	}
//
//	@Override
//	public List<ExpectedSaleVO> save(List<ExpectedSaleVO> sales) {
//		Preconditions.checkNotNull(sales);
//
//		return sales.stream()
//				.map(this::save)
//				.collect(Collectors.toList());
//	}

	@Override
	public void delete(int id) {
		expectedSaleRepository.deleteById(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}

	/* -- protected methods -- */

	protected void checkExpectedSale(final ExpectedSaleVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getSaleLocation(), "Missing saleLocation");
		Preconditions.checkNotNull(source.getSaleLocation().getId(), "Missing saleLocation.id");
		Preconditions.checkNotNull(source.getSaleType(), "Missing saleType");
		Preconditions.checkNotNull(source.getSaleType().getId(), "Missing saleType.id");
	}

	protected void saveChildrenEntities(final ExpectedSaleVO source) {

		// Save measurements
		if (source.getMeasurementValues() != null) {
			measurementDao.saveExpectedSaleMeasurementsMap(source.getId(), source.getMeasurementValues());
		}
		else {
			List<MeasurementVO> measurements = Beans.getList(source.getMeasurements());
			measurements.forEach(m -> fillDefaultProperties(source, m, SaleMeasurement.class));
			measurements = measurementDao.saveExpectedSaleMeasurements(source.getId(), measurements);
			source.setMeasurements(measurements);
		}

		// Save produces
		if (source.getProducts() != null) source.getProducts().forEach(product -> fillDefaultProperties(source, product));
		source.setProducts(
			productService.saveByExpectedSaleId(source.getId(), source.getProducts())
		);
	}

	protected void fillDefaultProperties(ExpectedSaleVO parent, MeasurementVO measurement, Class<? extends IMeasurementEntity> entityClass) {
		if (measurement == null) return;

		measurement.setEntityName(entityClass.getSimpleName());
	}

	protected void fillDefaultProperties(ExpectedSaleVO parent, ProductVO product) {
		if (product == null) return;

		product.setExpectedSaleId(parent.getId());
	}
}

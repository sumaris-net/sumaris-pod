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
import net.sumaris.core.dao.data.SaleDao;
import net.sumaris.core.vo.data.SaleVO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("saleService")
public class SaleServiceImpl implements SaleService {

	private static final Log log = LogFactory.getLog(SaleServiceImpl.class);

	@Autowired
	protected SaleDao saleDao;

	@Override
	public List<SaleVO> getAllByTripId(int tripId) {
		return saleDao.getAllByTripId(tripId);
	}

	@Override
	public SaleVO get(int saleId) {
		return saleDao.get(saleId);
	}

	@Override
	public List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sources) {
		return saleDao.saveAllByTripId(tripId, sources);
	}

	@Override
	public SaleVO save(SaleVO sale) {
		Preconditions.checkNotNull(sale);
		Preconditions.checkNotNull(sale.getStartDateTime(), "Missing startDateTime");
		Preconditions.checkNotNull(sale.getSaleLocation(), "Missing saleLocation");
		Preconditions.checkNotNull(sale.getSaleLocation().getId(), "Missing saleLocation.id");
		Preconditions.checkNotNull(sale.getSaleType(), "Missing saleType");
		Preconditions.checkNotNull(sale.getSaleType().getId(), "Missing saleType.id");
		Preconditions.checkNotNull(sale.getRecorderDepartment(), "Missing sale.recorderDepartment");
		Preconditions.checkNotNull(sale.getRecorderDepartment().getId(), "Missing sale.recorderDepartment.id");

		return saleDao.save(sale);
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
		saleDao.delete(id);
	}

	@Override
	public void delete(List<Integer> ids) {
		Preconditions.checkNotNull(ids);
		ids.stream()
				.filter(Objects::nonNull)
				.forEach(this::delete);
	}
}

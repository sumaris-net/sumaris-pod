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


import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.SaleFetchOptions;
import net.sumaris.core.vo.data.SaleVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of importing csv file into DB
 * 
 */
@Transactional
public interface SaleService {

	@Transactional(readOnly = true)
	List<SaleVO> getAllByTripId(int tripId, SaleFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<SaleVO> getAllByLandingId(int landingId, SaleFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	SaleVO get(int id);

	@Transactional(readOnly = true)
	SaleVO get(int id, SaleFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	int getProgramIdById(int id);

	List<SaleVO> saveAllByTripId(int tripId, List<SaleVO> sources);

	List<SaleVO> saveAllByLandingId(int landingId, List<SaleVO> sources);

	SaleVO save(SaleVO sale);

	List<SaleVO> save(List<SaleVO> sales);

	void delete(int id);

	void delete(List<Integer> ids);

	void fillVesselSnapshot(SaleVO target);

	void fillVesselSnapshots(List<SaleVO> target);
}

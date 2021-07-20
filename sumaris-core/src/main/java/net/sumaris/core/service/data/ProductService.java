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

import net.sumaris.core.vo.data.ProductVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author ludovic.pecquot@e-is.pro
 * 
 *    Service in charge of operation group beans
 * 
 */
@Transactional
public interface ProductService {

	@Transactional(readOnly = true)
	ProductVO get(Integer id);

	@Transactional(readOnly = true)
	List<ProductVO> getByLandingId(int landingId);

	@Transactional(readOnly = true)
	List<ProductVO> getByOperationId(int operationId);

	@Transactional(readOnly = true)
	List<ProductVO> getBySaleId(int saleId);

	@Transactional(readOnly = true)
	List<ProductVO> getByExpectedSaleId(int expectedSaleId);

	ProductVO save(ProductVO product);

	List<ProductVO> save(List<ProductVO> products);

	List<ProductVO> saveByLandingId(int landingId, List<ProductVO> products);

	List<ProductVO> saveByOperationId(int operationId, List<ProductVO> products);

	List<ProductVO> saveBySaleId(int saleId, List<ProductVO> products);

	List<ProductVO> saveByExpectedSaleId(int id, List<ProductVO> products);

	void delete(int id);

	void delete(List<Integer> ids);

	ProductVO control(ProductVO product);

	ProductVO validate(ProductVO product);

	ProductVO unvalidate(ProductVO product);

	void fillMeasurementsMap(ProductVO product);

}

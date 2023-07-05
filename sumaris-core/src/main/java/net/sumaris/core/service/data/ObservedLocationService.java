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


import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ObservedLocationSaveOptions;
import net.sumaris.core.vo.data.ObservedLocationVO;
import net.sumaris.core.vo.data.ObservedLocationValidateOptions;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of observed location data
 * 
 */
@Transactional
public interface ObservedLocationService {

	@Transactional(readOnly = true)
	List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, Page page, DataFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, int offset, int size);

	@Transactional(readOnly = true)
	List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, int offset, int size, String sortAttribute,
									 SortDirection sortDirection, DataFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	Long count(ObservedLocationFilterVO filter);

	@Transactional(readOnly = true)
	ObservedLocationVO get(int id);

	ObservedLocationVO save(ObservedLocationVO data, ObservedLocationSaveOptions saveOptions);

	List<ObservedLocationVO> save(List<ObservedLocationVO> data, ObservedLocationSaveOptions saveOptions);

	void delete(int id);

	void delete(List<Integer> ids);

    ObservedLocationVO control(ObservedLocationVO data);

	ObservedLocationVO validate(ObservedLocationVO data, ObservedLocationValidateOptions options);

	ObservedLocationVO unvalidate(ObservedLocationVO data, ObservedLocationValidateOptions options);

	ObservedLocationVO qualify(ObservedLocationVO data);
}

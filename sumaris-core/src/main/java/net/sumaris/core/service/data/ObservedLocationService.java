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
import net.sumaris.core.vo.data.*;
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
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
	List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, Page page, ObservedLocationFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, int offset, int size);

	@Transactional(readOnly = true)
	List<ObservedLocationVO> findAll(ObservedLocationFilterVO filter, int offset, int size, String sortAttribute,
									 SortDirection sortDirection, ObservedLocationFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	Long countByFilter(ObservedLocationFilterVO filter);

	@Transactional(readOnly = true)
	ObservedLocationVO get(int id);

	@Transactional(readOnly = true)
	ObservedLocationVO get(int id, ObservedLocationFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	int getProgramIdById(int id);

	ObservedLocationVO save(ObservedLocationVO data, @Nullable ObservedLocationSaveOptions options);

	List<ObservedLocationVO> save(List<ObservedLocationVO> data, @Nullable ObservedLocationSaveOptions options);

	void delete(int id);

	void delete(List<Integer> ids);

    ObservedLocationVO control(ObservedLocationVO data, @Nullable DataControlOptions options);

	ObservedLocationVO validate(ObservedLocationVO data, @Nullable DataValidateOptions options);

	ObservedLocationVO unvalidate(ObservedLocationVO data, @Nullable DataValidateOptions options);

	ObservedLocationVO qualify(ObservedLocationVO data);
}

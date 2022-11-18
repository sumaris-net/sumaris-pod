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


import net.sumaris.core.vo.data.sample.SampleFetchOptions;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.SampleFilterVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 */
@Transactional
public interface SampleService {

	@Transactional(readOnly = true)
	Long countByFilter(SampleFilterVO filter);

	@Transactional(readOnly = true)
	Long countByLandingId(int landingId);

	@Transactional(readOnly = true)
	List<SampleVO> getAllByOperationId(int operationId);

	@Transactional(readOnly = true)
	List<SampleVO> getAllByOperationId(int operationId, SampleFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<SampleVO> getAllByLandingId(int landingId);

	List<SampleVO> saveByOperationId(int operationId, List<SampleVO> samples);

	List<SampleVO> saveByLandingId(int landingId, List<SampleVO> samples);

	@Transactional(readOnly = true)
	SampleVO get(int id);

	@Transactional(readOnly = true)
	SampleVO get(int id, SampleFetchOptions fetchOptions);

	SampleVO save(SampleVO sale);

	List<SampleVO> save(List<SampleVO> samples);

	void delete(int id);

	void delete(List<Integer> ids);

	void deleteAllByLandingId(int landingId);

	void treeToList(final SampleVO sample, final List<SampleVO> result);

}

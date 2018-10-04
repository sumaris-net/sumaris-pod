package net.sumaris.core.service.data.batch;

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


import net.sumaris.core.vo.data.BatchVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 */
@Transactional
public interface BatchService {


	@Transactional(readOnly = true)
	List<BatchVO> getAllByOperationId(int operationId);

	List<BatchVO> saveByOperationId(int operationId, List<BatchVO> sources);

	@Transactional(readOnly = true)
	BatchVO get(int id);

	BatchVO save(BatchVO sale);

	List<BatchVO> save(List<BatchVO> sources);

	void delete(int id);

	void delete(List<Integer> ids);

}

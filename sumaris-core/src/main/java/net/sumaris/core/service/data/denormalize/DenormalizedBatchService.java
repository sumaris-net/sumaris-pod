/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.service.data.denormalize;

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.data.batch.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * @author BLA
 * 
 */
@Transactional
public interface DenormalizedBatchService {

	/**
	 * Return the flat list of all denormalizedBatches (without parent/children filled, by only the parentId)
	 * @param operationId
	 * @return
	 */
	@Transactional(readOnly = true)
	List<DenormalizedBatchVO> getAllByOperationId(int operationId);

	@Transactional(readOnly = true)
	List<DenormalizedBatchVO> getAllByOperationId(int operationId, DenormalizedBatchFetchOptions fetchOptions);

	Optional<DenormalizedBatchVO> findById(int id, DenormalizedBatchFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<DenormalizedBatchVO> findAll(DenormalizedBatchesFilterVO filter,
								int offset,
								int size,
								String sortAttribute,
								SortDirection sortDirection,
								DenormalizedBatchFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	long countByFilter(DenormalizedBatchesFilterVO filter);

	@Transactional(readOnly = true)
	List<DenormalizedBatchVO> findAll(DenormalizedBatchesFilterVO filter, DenormalizedBatchFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<DenormalizedBatchVO> denormalize(BatchVO catchBatch, @NotNull DenormalizedBatchOptions options);

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	List<DenormalizedBatchVO> denormalizeAndSaveByOperationId(int operationId, @Nullable DenormalizedBatchOptions options);

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	List<DenormalizedBatchVO> denormalizeAndSaveBySaleId(int saleId, @Nullable DenormalizedBatchOptions options);

	@Transactional(readOnly = true)
	DenormalizedBatchOptions createOptionsByProgramId(int programId);

	@Transactional(readOnly = true)
	DenormalizedBatchOptions createOptionsByProgramLabel(String programLabel);
}

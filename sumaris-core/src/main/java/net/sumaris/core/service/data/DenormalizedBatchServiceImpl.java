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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.data.batch.BatchRepository;
import net.sumaris.core.dao.data.batch.DenormalizedBatchRepository;
import net.sumaris.core.vo.data.batch.BatchFetchOptions;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("denormalizedBatchService")
@Slf4j
public class DenormalizedBatchServiceImpl implements DenormalizedBatchService {

	@Autowired
	protected DenormalizedBatchRepository denormalizedBatchRepository;

	@Autowired
	protected BatchRepository batchRepository;

	@Override
	public List<DenormalizedBatchVO> denormalize(@NonNull BatchVO catchBatch) {
		Preconditions.checkNotNull(catchBatch);
		return denormalizedBatchRepository.denormalized(catchBatch);
	}

	@Override
	public List<DenormalizedBatchVO> denormalizeAndSaveByOperationId(int operationId) {
		BatchVO catchBatch = batchRepository.getCatchBatchByOperationId(operationId, BatchFetchOptions.builder()
			.withChildrenEntities(true)
			.withMeasurementValues(true)
			.withRecorderDepartment(false)
			.build());
		if (catchBatch == null) return null;
		return denormalizedBatchRepository.saveAllByOperationId(operationId, denormalize(catchBatch));
	}

	@Override
	public List<DenormalizedBatchVO> denormalizeAndSaveBySaleId(int saleId) {
		BatchVO catchBatch = batchRepository.getCatchBatchBySaleId(saleId, BatchFetchOptions.builder()
			.withChildrenEntities(true)
			.withMeasurementValues(true)
			.withRecorderDepartment(false)
			.build());
		if (catchBatch == null) return null;
		return denormalizedBatchRepository.saveAllBySaleId(saleId, denormalize(catchBatch));
	}
}

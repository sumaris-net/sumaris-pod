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
import net.sumaris.core.dao.data.DenormalizedBatchRepository;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.DenormalizedBatchVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("denormalizedBatchService")
public class DenormalizedBatchServiceImpl implements DenormalizedBatchService {

	private static final Logger log = LoggerFactory.getLogger(DenormalizedBatchServiceImpl.class);

	@Autowired
	protected DenormalizedBatchRepository denormalizedBatchRepository;

	@Override
	public List<DenormalizedBatchVO> denormalize(BatchVO catchBatch) {
		Preconditions.checkNotNull(catchBatch);
		return denormalizedBatchRepository.denormalized(catchBatch);
	}

	@Override
	public List<DenormalizedBatchVO> saveAllByOperationId(int operationId, BatchVO catchBatch) {
		return denormalizedBatchRepository.saveAllByOperationId(operationId, denormalize(catchBatch));
	}

	@Override
	public List<DenormalizedBatchVO> saveAllBySaleId(int saleId, BatchVO catchBatch) {
		return denormalizedBatchRepository.saveAllBySaleId(saleId, denormalize(catchBatch));
	}

}

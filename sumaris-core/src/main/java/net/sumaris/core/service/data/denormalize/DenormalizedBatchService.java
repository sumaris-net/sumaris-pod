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

import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.data.batch.DenormalizedBatchVO;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author BLA
 * 
 */
@Transactional
public interface DenormalizedBatchService {

	@Transactional(readOnly = true)
	List<DenormalizedBatchVO> denormalize(BatchVO catchBatch, @NotNull DenormalizedBatchOptions options);

	List<DenormalizedBatchVO> denormalizeAndSaveByOperationId(int operationId, @Nullable DenormalizedBatchOptions options);

	List<DenormalizedBatchVO> denormalizeAndSaveBySaleId(int saleId, @Nullable DenormalizedBatchOptions options);

	@Transactional(readOnly = true)
	DenormalizedBatchOptions createOptionsByProgramId(int programId);

	@Transactional(readOnly = true)
	DenormalizedBatchOptions createOptionsByProgramLabel(String programLabel);
}

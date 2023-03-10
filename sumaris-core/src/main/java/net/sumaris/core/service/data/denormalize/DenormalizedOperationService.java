package net.sumaris.core.service.data.denormalize;

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


import lombok.NonNull;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.core.vo.filter.OperationFilterVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;

/**
 * @author BLA
 * 
 *    Service in charge of operation data
 * 
 */
@Transactional
public interface DenormalizedOperationService {

	@Transactional(readOnly = true)
	DenormalizedBatchOptions createOptionsByProgramId(int programId);

	@Transactional(readOnly = true)
	DenormalizedBatchOptions createOptionsByProgramLabel(String programLabel);

	@Transactional(readOnly = true)
	DenormalizedBatchOptions createOptionsByOperation(@NonNull OperationVO operation,
													  @Nullable DenormalizedBatchOptions inheritedOptions);

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	DenormalizedTripResultVO denormalizeByFilter(@NonNull OperationFilterVO filter, @Nullable DenormalizedBatchOptions options);

}

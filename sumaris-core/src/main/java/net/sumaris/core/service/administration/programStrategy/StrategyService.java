package net.sumaris.core.service.administration.programStrategy;

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


import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author BLA
 * 
 *    Service in charge of importing csv file into DB
 * 
 */
@Transactional()
public interface StrategyService {

	@Transactional(readOnly = true)
	List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<PmfmStrategyVO> findPmfmStrategiesByProgram(int programId, StrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<PmfmStrategyVO> findPmfmStrategiesByProgramAndAcquisitionLevel(int programId, int acquisitionLevelId, StrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<PmfmStrategyVO> findPmfmStrategiesByStrategy(int strategy, StrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<ReferentialVO> getGears(int strategyId);

	@Transactional(readOnly = true)
	List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId);

	@Transactional(readOnly = true)
	List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId);

	StrategyVO save(StrategyVO source);

	List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources);

}

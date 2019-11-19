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


import net.sumaris.core.dao.administration.programStrategy.StrategyDao;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("strategyService")
public class StrategyServiceImpl implements StrategyService {

	private static final Logger log = LoggerFactory.getLogger(StrategyServiceImpl.class);

	@Autowired
	protected StrategyDao strategyDao;

	@Override
	public List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions) {
		return strategyDao.findByProgram(programId, fetchOptions);
	}

	@Override
	public List<PmfmStrategyVO> getPmfmStrategies(int strategyId) {
		return strategyDao.getPmfmStrategies(strategyId);
	}

	@Override
	public List<PmfmStrategyVO> getPmfmStrategiesByAcquisitionLevel(int programId, int acquisitionLevelId) {
		return strategyDao.getPmfmStrategiesByAcquisitionLevel(programId, acquisitionLevelId);
	}

	@Override
	public List<ReferentialVO> getGears(int strategyId) {
		return strategyDao.getGears(strategyId);
	}

	@Override
	public List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId) {
		return strategyDao.getTaxonGroupStrategies(strategyId);
	}

	@Override
	public List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId) {
		return strategyDao.getTaxonNameStrategies(strategyId);
	}

	@Override
	public List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources) {
		List<StrategyVO> result = strategyDao.saveByProgramId(programId, sources);

		// Save pmfm strategy
		//saveMeasurements(result);

		return result;
	}
}

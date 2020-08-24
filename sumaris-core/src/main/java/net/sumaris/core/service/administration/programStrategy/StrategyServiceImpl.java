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


import com.google.common.collect.Maps;
import net.sumaris.core.dao.administration.programStrategy.PmfmStrategyRepository;
import net.sumaris.core.dao.administration.programStrategy.StrategyRepository;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service("strategyService")
public class StrategyServiceImpl implements StrategyService {

	private static final Logger log = LoggerFactory.getLogger(StrategyServiceImpl.class);

	@Autowired
	private StrategyRepository strategyRepository;

	@Autowired
	private PmfmStrategyRepository pmfmStrategyRepository;

	@Override
	public List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions) {
		return strategyRepository.findAll(StrategyFilterVO.strategyFilterBuilder().programId(programId).build(), fetchOptions);
	}

	@Override
	public List<PmfmStrategyVO> findPmfmStrategiesByStrategy(int strategy, StrategyFetchOptions fetchOptions) {
		return pmfmStrategyRepository.findByStrategyId(strategy, fetchOptions);
	}

	@Override
	public List<PmfmStrategyVO> findPmfmStrategiesByProgram(int programId, StrategyFetchOptions fetchOptions) {

		List<StrategyVO> vos = findByProgram(programId, fetchOptions);

		Map<Integer, PmfmStrategyVO> pmfmStrategyByPmfmId = Maps.newHashMap();
		Beans.getStream(vos)
				.flatMap(strategy -> strategy.getPmfmStrategies().stream())
				// Sort by strategyId, acquisitionLevel and rankOrder
				.sorted(Comparator.comparing(ps -> String.format("%s#%s#%s", ps.getStrategyId(), ps.getAcquisitionLevel(), ps.getRankOrder())))
				// Put in the last (last found will override previous)
				.forEach(ps -> pmfmStrategyByPmfmId.put(ps.getPmfmId(), ps));

		return Beans.getList(pmfmStrategyByPmfmId.values());
	}

	@Override
	public List<PmfmStrategyVO> findPmfmStrategiesByProgramAndAcquisitionLevel(int programId, int acquisitionLevelId, StrategyFetchOptions fetchOptions) {
		return pmfmStrategyRepository.findByProgramAndAcquisitionLevel(programId, acquisitionLevelId, fetchOptions);
	}

	@Override
	public List<ReferentialVO> getGears(int strategyId) {
		return strategyRepository.getGears(strategyId);
	}

	@Override
	public List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId) {
		return strategyRepository.getTaxonGroupStrategies(strategyId);
	}

	@Override
	public List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId) {
		return strategyRepository.getTaxonNameStrategies(strategyId);
	}

	@Override
	public StrategyVO save(StrategyVO source) {

		StrategyVO result = strategyRepository.save(source);

		// Save pmfm stratgeies
		List<PmfmStrategyVO> savedPmfmStrategies = pmfmStrategyRepository.saveByStrategyId(result.getId(), Beans.getList(source.getPmfmStrategies()));
		source.setPmfmStrategies(savedPmfmStrategies);

		return result;
	}

	@Override
	public List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources) {
		List<StrategyVO> result = strategyRepository.saveByProgramId(programId, sources);

		// Save pmfm strategies
		sources.forEach(source -> {
			List<PmfmStrategyVO> savedPmfmStrategies = pmfmStrategyRepository.saveByStrategyId(source.getId(), Beans.getList(source.getPmfmStrategies()));
			source.setPmfmStrategies(savedPmfmStrategies);
		});

		return result;
	}
}

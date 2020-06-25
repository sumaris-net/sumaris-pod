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
import net.sumaris.core.dao.administration.programStrategy.StrategyDao;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
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
	private StrategyDao strategyDao;

	@Autowired
	private PmfmStrategyRepository pmfmStrategyRepository;

	@Override
	public List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions) {
		return strategyDao.findByProgram(programId, fetchOptions);
	}

	@Override
	public List<PmfmStrategyVO> findPmfmStrategiesByStrategy(int strategy, boolean enablePmfmInheritance) {
		return pmfmStrategyRepository.findByStrategyId(strategy, enablePmfmInheritance);
	}

	@Override
	public List<PmfmStrategyVO> findPmfmStrategiesByProgram(int programId, boolean enablePmfmInheritance) {

		List<StrategyVO> vos = findByProgram(programId, StrategyFetchOptions.builder()
				.withPmfmStrategyInheritance(enablePmfmInheritance)
				.build());

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
	public List<PmfmStrategyVO> findByProgramAndAcquisitionLevel(int programId, int acquisitionLevelId, boolean enablePmfmInheritance) {
		return pmfmStrategyRepository.findByProgramAndAcquisitionLevel(programId, acquisitionLevelId, enablePmfmInheritance);
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
	public StrategyVO save(StrategyVO source) {

		StrategyVO result = strategyDao.save(source);

		// Save pmfm stratgeies
		List<PmfmStrategyVO> savedPmfmStrategies = pmfmStrategyRepository.saveByStrategyId(result.getId(), Beans.getList(source.getPmfmStrategies()));
		source.setPmfmStrategies(savedPmfmStrategies);

		return result;
	}

	@Override
	public List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources) {
		List<StrategyVO> result = strategyDao.saveByProgramId(programId, sources);

		// Save pmfm strategies
		sources.forEach(source -> {
			List<PmfmStrategyVO> savedPmfmStrategies = pmfmStrategyRepository.saveByStrategyId(source.getId(), Beans.getList(source.getPmfmStrategies()));
			source.setPmfmStrategies(savedPmfmStrategies);
		});

		return result;
	}
}

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.programStrategy.PmfmStrategyRepository;
import net.sumaris.core.dao.administration.programStrategy.StrategyRepository;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service("strategyService")
@Slf4j
public class StrategyServiceImpl implements StrategyService {

	@Autowired
	private StrategyRepository strategyRepository;

	@Autowired
	private PmfmStrategyRepository pmfmStrategyRepository;

	@Override
	public StrategyVO get(int id, StrategyFetchOptions fetchOptions) {
		return strategyRepository.get(id, fetchOptions);
	}

	@Override
	public List<StrategyVO> findByFilter(StrategyFilterVO filter, Pageable pageable, StrategyFetchOptions fetchOptions) {
		return strategyRepository.findAll(filter, pageable, fetchOptions).getContent();
	}

	@Override
	public List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions) {
		return strategyRepository.findAll(StrategyFilterVO.builder().programId(programId).build(), fetchOptions);
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

		// Save children entities
		saveChildrenEntities(source);

		return result;
	}

	@Override
	public List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources) {
		List<StrategyVO> result = strategyRepository.saveByProgramId(programId, sources);

		// Save children entities
		sources.forEach(this::saveChildrenEntities);

		return result;
	}

	@Override
	public void delete(int id) {
		strategyRepository.deleteById(id);
	}

	/* -- protected methods -- */

	protected void saveChildrenEntities(StrategyVO source) {

		// Pmfm strategies
		List<PmfmStrategyVO> savedPmfmStrategies = pmfmStrategyRepository.saveByStrategyId(source.getId(), Beans.getList(source.getPmfmStrategies()));
		source.setPmfmStrategies(savedPmfmStrategies);
	}
}

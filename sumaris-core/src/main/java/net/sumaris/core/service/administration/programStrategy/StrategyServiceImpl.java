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


import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import net.sumaris.core.dao.administration.programStrategy.PmfmStrategyRepository;
import net.sumaris.core.dao.administration.programStrategy.StrategyRepository;
import net.sumaris.core.dao.technical.SortDirection;
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
	public StrategyVO get(int id) {
		return strategyRepository.get(id);
	}

	@Override
	public StrategyVO get(int id, StrategyFetchOptions fetchOptions) {
		return strategyRepository.get(id, fetchOptions);
	}

	@Override
	public StrategyVO getByLabel(String label) {
		Preconditions.checkNotNull(label);
		return strategyRepository.getByLabel(label);
	}

	@Override
	public StrategyVO getByLabel(String label, StrategyFetchOptions fetchOptions) {
		Preconditions.checkNotNull(label);
		return strategyRepository.getByLabel(label, fetchOptions);
	}

	@Override
	public List<StrategyVO> getAll() {
		return strategyRepository.findAll(StrategyFilterVO.builder().build());
	}

	@Override
	public List<StrategyVO> findByFilter(StrategyFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		if (filter == null) filter = StrategyFilterVO.builder().build();
		return strategyRepository.findAll(filter, offset, size, sortAttribute, sortDirection, null).getContent();
	}

	@Override
	public List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions) {
		return strategyRepository.findAll(StrategyFilterVO.builder().programId(programId).build(), fetchOptions);
	}

	@Override
	public List<PmfmStrategyVO> findPmfmStrategiesByStrategy(int strategyId, StrategyFetchOptions fetchOptions) {
		return pmfmStrategyRepository.findByStrategyId(strategyId, fetchOptions);
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
	public List<AppliedStrategyVO> getAppliedStrategies(int strategyId) {
		return strategyRepository.getAppliedStrategies(strategyId);
	}

	@Override
	public List<StrategyDepartmentVO> getStrategyDepartments(int strategyId) {
		return strategyRepository.getStrategyDepartments(strategyId);
	}

	@Override
	public String findNextLabelByProgramId(int programId, String labelPrefix, int nbDigit) {
		return strategyRepository.findNextLabelByProgramId(programId, labelPrefix, nbDigit);
	}

	@Override
	public StrategyVO save(StrategyVO source) {
		StrategyVO result = strategyRepository.save(source);

		saveSubStrategiesByStrategyId(result.getId(), source);

		return result;
	}

	@Override
	public List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources) {
		List<StrategyVO> result = strategyRepository.saveByProgramId(programId, sources);

		sources.forEach(source -> {
			saveSubStrategiesByStrategyId(source.getId(), source);
		});

		return result;
	}

	@Override
	public void delete(int id) {
		strategyRepository.deleteById(id);
	}


	private void saveSubStrategiesByStrategyId(int strategyId, StrategyVO source) {

		// Save taxon Group strategy
		List<TaxonGroupStrategyVO> savedTaxonGroupStrategies = strategyRepository.saveTaxonGroupStrategiesByStrategyId(strategyId, Beans.getList(source.getTaxonGroups()));
		source.setTaxonGroups(savedTaxonGroupStrategies);

		// Save reference Names strategy
		List<TaxonNameStrategyVO> savedReferenceTaxonStrategies = strategyRepository.saveReferenceTaxonStrategiesByStrategyId(strategyId, Beans.getList(source.getTaxonNames()));
		source.setTaxonNames(savedReferenceTaxonStrategies);

		// Save applied strategies
		List<AppliedStrategyVO> savedAppliedStrategies = strategyRepository.saveAppliedStrategiesByStrategyId(strategyId, Beans.getList(source.getAppliedStrategies()));
		source.setAppliedStrategies(savedAppliedStrategies);

		// Save strategy departments
		List<StrategyDepartmentVO> savedStrategyDepartments = strategyRepository.saveStrategyDepartmentsByStrategyId(strategyId, Beans.getList(source.getStrategyDepartments()));
		source.setStrategyDepartments(savedStrategyDepartments);

		// Save pmfm strategies
		List<PmfmStrategyVO> savedPmfmStrategies = pmfmStrategyRepository.saveByStrategyId(strategyId, Beans.getList(source.getPmfmStrategies()));
		source.setPmfmStrategies(savedPmfmStrategies);

		// Save program locations
		strategyRepository.saveProgramLocationsByStrategyId(strategyId);
	}
}
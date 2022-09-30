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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.programStrategy.PmfmStrategyRepository;
import net.sumaris.core.dao.administration.programStrategy.StrategyRepository;
import net.sumaris.core.dao.administration.programStrategy.denormalized.DenormalizedPmfmStrategyRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("strategyService")
@Slf4j
public class StrategyServiceImpl implements StrategyService {

	@Autowired
	private StrategyRepository strategyRepository;

	@Autowired
	private PmfmStrategyRepository pmfmStrategyRepository;


	@Autowired
	private DenormalizedPmfmStrategyRepository denormalizedPmfmStrategyRepository;

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
	public Long countByFilter(StrategyFilterVO filter) {
		return strategyRepository.count(filter);
	}

	@Override
	public Date maxUpdateDateByFilter(StrategyFilterVO filter) {
		return strategyRepository.maxUpdateDateByFilter(filter);
	}

	@Override
	public List<StrategyVO> findByFilter(StrategyFilterVO filter, Page page, StrategyFetchOptions fetchOptions) {
		return strategyRepository.findAll(filter, page, fetchOptions);
	}

	@Override
	public List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions) {
		return strategyRepository.findAll(StrategyFilterVO.builder()
				.programIds(new Integer[]{programId})
			.build(), fetchOptions);
	}

	@Override
	public List<StrategyVO> findNewerByProgramId(int programId, Date updateDate, StrategyFetchOptions fetchOptions) {
		return strategyRepository.findNewerByProgramId(programId, updateDate, fetchOptions);
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
		return strategyRepository.getDepartmentsById(strategyId);
	}

	@Override
	public List<PmfmStrategyVO> findPmfmsByFilter(PmfmStrategyFilterVO filter, PmfmStrategyFetchOptions fetchOptions) {
		return pmfmStrategyRepository.findByFilter(filter, fetchOptions);
	}

	@Override
	public List<DenormalizedPmfmStrategyVO> findDenormalizedPmfmsByFilter(PmfmStrategyFilterVO filter, PmfmStrategyFetchOptions fetchOptions) {
		return denormalizedPmfmStrategyRepository.findByFilter(filter, fetchOptions);
	}

	@Override
	public String computeNextLabelByProgramId(int programId, String labelPrefix, int nbDigit) {
		return strategyRepository.computeNextLabelByProgramId(programId, labelPrefix, nbDigit);
	}

	@Override
	public String computeNextSampleLabelByStrategy(String strategyLabel, String labelSeparator, int nbDigit) {
		return strategyRepository.computeNextSampleLabelByStrategy(strategyLabel, labelSeparator, nbDigit);
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
		log.info("Delete Strategy#{}", id);

		strategyRepository.deleteById(id);
	}

	@Override
	public boolean hasUserPrivilege(int strategyId, int personId, ProgramPrivilegeEnum privilege) {
		return strategyRepository.hasUserPrivilege(strategyId, personId, privilege);
	}

	@Override
	public boolean hasDepartmentPrivilege(int strategyId, int departmentId, ProgramPrivilegeEnum privilege) {
		return strategyRepository.hasDepartmentPrivilege(strategyId, departmentId, privilege);
	}

	/* -- protected methods -- */

	protected void saveChildrenEntities(StrategyVO source) {
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(source.getId());
		saveChildrenEntities(source.getId(), source);
	}

	protected void saveChildrenEntities(int strategyId, StrategyVO source) {
		Preconditions.checkNotNull(source);

		// Save taxon groups
		List<TaxonGroupStrategyVO> savedTaxonGroupStrategies = strategyRepository.saveTaxonGroupStrategiesByStrategyId(strategyId, Beans.getList(source.getTaxonGroups()));
		source.setTaxonGroups(savedTaxonGroupStrategies);

		// Save Taxon references
		List<TaxonNameStrategyVO> savedReferenceTaxonStrategies = strategyRepository.saveReferenceTaxonStrategiesByStrategyId(strategyId, Beans.getList(source.getTaxonNames()));
		source.setTaxonNames(savedReferenceTaxonStrategies);

		// Save applied strategies
		List<AppliedStrategyVO> savedAppliedStrategies = strategyRepository.saveAppliedStrategiesByStrategyId(strategyId, Beans.getList(source.getAppliedStrategies()));
		source.setAppliedStrategies(savedAppliedStrategies);

		// Save departments
		List<StrategyDepartmentVO> savedDepartments = strategyRepository.saveDepartmentsByStrategyId(strategyId, Beans.getList(source.getDepartments()));
		source.setDepartments(savedDepartments);

		// Save pmfms
		List<PmfmStrategyVO> savedPmfmStrategies = pmfmStrategyRepository.saveByStrategyId(strategyId, Beans.getList(source.getPmfms()));
		source.setPmfms(savedPmfmStrategies);

		// Save program locations
		strategyRepository.saveProgramLocationsByStrategyId(strategyId);
	}
}

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


import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.PmfmStrategyFilterVO;
import net.sumaris.core.vo.filter.StrategyFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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
	//TODO BLA: à supprimer (fetch nullable)
	StrategyVO get(int id);

	@Transactional(readOnly = true)
	StrategyVO get(int id, StrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	//TODO BLA: à supprimer (fetch nullable)
	StrategyVO getByLabel(String label);

	@Transactional(readOnly = true)
	StrategyVO getByLabel(String label, StrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	//TODO BLA: à supprimer
	List<StrategyVO> getAll();

	@Transactional(readOnly = true)
	Long countByFilter(StrategyFilterVO filter);

	@Transactional(readOnly = true)
	Date maxUpdateDateByFilter(StrategyFilterVO filter);

	@Transactional(readOnly = true)
	List<StrategyVO> findByFilter(StrategyFilterVO filter, Page page, StrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<StrategyVO> findByProgram(int programId, StrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Cacheable(cacheNames = CacheConfiguration.Names.PMFM_STRATEGIES_BY_FILTER)
	List<PmfmStrategyVO> findPmfmsByFilter(PmfmStrategyFilterVO filter, PmfmStrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Cacheable(cacheNames = CacheConfiguration.Names.DENORMALIZED_PMFM_BY_FILTER)
	List<DenormalizedPmfmStrategyVO> findDenormalizedPmfmsByFilter(PmfmStrategyFilterVO filter, PmfmStrategyFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<ReferentialVO> getGears(int strategyId);

	@Transactional(readOnly = true)
	List<TaxonGroupStrategyVO> getTaxonGroupStrategies(int strategyId);

	@Transactional(readOnly = true)
	List<TaxonNameStrategyVO> getTaxonNameStrategies(int strategyId);

	@Transactional(readOnly = true)
	List<AppliedStrategyVO> getAppliedStrategies(int strategyId);

	@Transactional(readOnly = true)
	List<StrategyDepartmentVO> getStrategyDepartments(int strategyId);

	@Transactional(readOnly = true)
	String computeNextLabelByProgramId(int programId, String labelPrefix, int nbDigit);

	@Transactional(readOnly = true)
	String computeNextSampleLabelByStrategy(String strategyLabel, String labelSeparator, int nbDigit);

	StrategyVO save(StrategyVO source);

	List<StrategyVO> saveByProgramId(int programId, List<StrategyVO> sources);

	void delete(int id);

	boolean hasUserPrivilege(int strategyId, int personId, ProgramPrivilegeEnum privilege);

	boolean hasDepartmentPrivilege(int strategyId, int departmentId, ProgramPrivilegeEnum privilege);

	List<StrategyVO> findNewerByProgramId(final int programId, final Date updateDate, final StrategyFetchOptions fetchOptions);

}

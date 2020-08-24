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
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.programStrategy.StrategyVO;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("programService")
public class ProgramServiceImpl implements ProgramService {

	private static final Logger log = LoggerFactory.getLogger(ProgramServiceImpl.class);

	@Autowired
	protected ProgramRepository programRepository;

	@Autowired
	protected StrategyService strategyService;

	@Override
	public List<ProgramVO> getAll() {
		return programRepository.findAll(ProgramFilterVO.programFilterBuilder().build());
	}

	@Override
	public List<ProgramVO> findByFilter(ProgramFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		if (filter == null) filter = ProgramFilterVO.programFilterBuilder().build();
		return programRepository.findAll(filter, offset, size, sortAttribute, sortDirection, null).getContent();
	}

	@Override
	public ProgramVO get(int id) {
		return programRepository.get(id);
	}

	@Override
	public ProgramVO getByLabel(String label) {
		Preconditions.checkNotNull(label);
		return programRepository.getByLabel(label);
	}

	@Override
	public ProgramVO save(ProgramVO source) {
		Preconditions.checkNotNull(source);
		ProgramVO result = programRepository.save(source);

		// Save strategies
		if (source.getStrategies() != null) {
			List<StrategyVO> savedStrategies = strategyService.saveByProgramId(result.getId(), source.getStrategies());
			result.setStrategies(savedStrategies);
		}

		return result;
	}

	@Override
	public void delete(int id) {
		programRepository.deleteById(id);
	}
}


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
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service("programService")
@Slf4j
public class ProgramServiceImpl implements ProgramService {

	@Autowired
	protected ProgramRepository programRepository;

	@Autowired
	protected StrategyService strategyService;

	@Override
	public List<ProgramVO> getAll() {
		return programRepository.findAll(ProgramFilterVO.builder().build());
	}

	@Override
	public List<ProgramVO> findByFilter(ProgramFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
		if (filter == null) filter = ProgramFilterVO.builder().build();
		return programRepository.findAll(filter, offset, size, sortAttribute, sortDirection, null).getContent();
	}

	@Override
	public ProgramVO get(int id) {
		return programRepository.get(id);
	}

	@Override
	public ProgramVO get(int id, ProgramFetchOptions fetchOptions) {
		return programRepository.get(id, fetchOptions);
	}

	@Override
	public ProgramVO getByLabel(String label) {
		Preconditions.checkNotNull(label);
		return programRepository.getByLabel(label);
	}

	@Override
	public ProgramVO getByLabel(String label, ProgramFetchOptions fetchOptions) {
		Preconditions.checkNotNull(label);
		return programRepository.getByLabel(label, fetchOptions);
	}

	@Override
	public Optional<ProgramVO> findNewerById(int id, Date updateDate, ProgramFetchOptions fetchOptions) {
		Preconditions.checkNotNull(updateDate);
		return programRepository.findIfNewerById(id, updateDate, fetchOptions);
	}

	@Override
	public ProgramVO save(ProgramVO source, ProgramSaveOptions options) {
		Preconditions.checkNotNull(source);
		options = ProgramSaveOptions.defaultIfEmpty(options);

		ProgramVO result = programRepository.save(source);

		if (options.getWithDepartmentsAndPersons()) {

			// Save departments
			List<ProgramDepartmentVO> savedDepartments = programRepository.saveDepartmentsByProgramId(source.getId(), Beans.getList(source.getDepartments()));
			result.setDepartments(savedDepartments);

			// Save persons
			List<ProgramPersonVO> savedPersons = programRepository.savePersonsByProgramId(source.getId(), Beans.getList(source.getPersons()));
			result.setPersons(savedPersons);
		}

		// Save strategies
		if (options.getWithStrategies()) {
			List<StrategyVO> savedStrategies = strategyService.saveByProgramId(result.getId(), source.getStrategies());
			result.setStrategies(savedStrategies);
		}

		return result;
	}

	@Override
	public void delete(int id) {
		programRepository.deleteById(id);
	}

	@Override
	public boolean hasUserPrivilege(int programId, int personId, ProgramPrivilegeEnum privilege) {
		return programRepository.hasUserPrivilege(programId, personId, privilege);
	}

	@Override
	public boolean hasDepartmentPrivilege(int programId, int departmentId, ProgramPrivilegeEnum privilege) {
		return programRepository.hasDepartmentPrivilege(programId, departmentId, privilege);
	}

}


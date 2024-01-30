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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.administration.programStrategy.ProgramRepository;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.annotation.EntityEnums;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service("programService")
@RequiredArgsConstructor
@Slf4j
public class ProgramServiceImpl implements ProgramService {

	protected final ProgramRepository programRepository;

	protected final StrategyService strategyService;

	@Override
	public List<ProgramVO> getAll() {
		return programRepository.findAll(ProgramFilterVO.builder().build());
	}

	@Override
	public List<ProgramVO> findByFilter(@Nullable ProgramFilterVO filter,
										@Nullable Page page,
										@Nullable ProgramFetchOptions fetchOptions) {
		return programRepository.findAll(ProgramFilterVO.nullToEmpty(filter), page, fetchOptions);
	}

	@Override
	public List<ProgramVO> findByFilter(@Nullable ProgramFilterVO filter,
										int offset, int size, String sortAttribute, SortDirection sortDirection) {
		return findByFilter(filter,
			Page.builder().offset(offset).size(size).sortBy(sortAttribute).sortDirection(sortDirection).build(),
			null);
	}

	@Override
	public Long countByFilter(@Nullable ProgramFilterVO filter) {
		return programRepository.count(ProgramFilterVO.nullToEmpty(filter));
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
	public int getIdByLabel(String label) {
		return getByLabel(label).getId();
	}

	@Override
	public Optional<Integer> findIdByLabel(String label) {
		return programRepository.findByLabel(label).map(ProgramVO::getId);
	}

	@Override
	public List<ReferentialVO> getAcquisitionLevelsById(int id) {
		return programRepository.getAcquisitionLevelsById(id);
	}

	@Override
	public boolean hasAcquisitionLevelById(int id, @NonNull AcquisitionLevelEnum acquisitionLevel) {
		if (EntityEnums.isUnresolved(acquisitionLevel)) return false;
		final int expectedId = acquisitionLevel.getId();
		return Beans.getStream(programRepository.getAcquisitionLevelsById(id))
			.anyMatch(item -> expectedId == item.getId());
	}

	@Override
	public boolean hasAcquisitionLevelByLabel(String label, AcquisitionLevelEnum acquisitionLevel) {
		ProgramVO program = getByLabel(label);
		return hasAcquisitionLevelById(program.getId(), acquisitionLevel);
	}

	@Override
	public List<ProgramPrivilegeEnum> getAllPrivilegesByUserId(int programId, int personId) {
		return programRepository.getAllPrivilegeIdsByUserId(programId, personId);
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

	@Override
	public boolean hasPropertyValueByProgramLabel(@NonNull String label, @NonNull ProgramPropertyEnum property, @NonNull String expectedValue) {
		String value = getPropertyValueByProgramLabel(label, property);
		// If boolean: true = TRUE
		if (property.getType() == Boolean.class) {
			return expectedValue.equalsIgnoreCase(value);
		}

		return expectedValue.equals(value);
	}


	@Override
	public boolean hasPropertyValueByProgramId(@NonNull Integer id, @NonNull ProgramPropertyEnum property, @NonNull String expectedValue){
		String value = getPropertyValueByProgramId(id, property);
		// If boolean: true = TRUE
		if (property.getType() == Boolean.class) {
			return expectedValue.equalsIgnoreCase(value);
		}

		return expectedValue.equals(value);
	}

	@Override
	public String getPropertyValueByProgramLabel(@NonNull String label, @NonNull ProgramPropertyEnum property) {
		return programRepository.findByLabel(label)
			.map(program -> program.getProperties().get(property.getKey()))
			.orElse(property.getDefaultValue());
	}

	@Override
	public String getPropertyValueByProgramId(@NonNull Integer id, @NonNull ProgramPropertyEnum property) {
		return programRepository.findVOById(id)
			.map(program -> program.getProperties().get(property.getKey()))
			.orElse(property.getDefaultValue());
	}
}


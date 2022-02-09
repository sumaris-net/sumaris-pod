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


import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.programStrategy.ProgramPrivilegeEnum;
import net.sumaris.core.vo.administration.programStrategy.*;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.filter.ProgramFilterVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author BLA
 * 
 *    Service in charge of importing csv file into DB
 * 
 */
@Transactional()
public interface ProgramService {

	@Transactional(readOnly = true)
	ProgramVO get(int id);

	@Transactional(readOnly = true)
	ProgramVO get(int id, ProgramFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	ProgramVO getByLabel(String label);

	ProgramVO getByLabel(String label, ProgramFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	Optional<ProgramVO> findNewerById(int id, final Date updateDate, ProgramFetchOptions fetchOptions);

	@Transactional(readOnly = true)
	List<ProgramVO> getAll();

	@Transactional(readOnly = true)
	List<ProgramVO> findByFilter(ProgramFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

	ProgramVO save(ProgramVO program, ProgramSaveOptions options);

	void delete(int id);

	boolean hasUserPrivilege(int programId, int personId, ProgramPrivilegeEnum privilege);

	boolean hasDepartmentPrivilege(int programId, int departmentId, ProgramPrivilegeEnum privilege);

	boolean hasPropertyValue(String label, String propertyName, String expectedValue);

}

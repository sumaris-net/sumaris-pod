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


import net.sumaris.core.dao.administration.programStrategy.ProgramDao;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("programService")
public class ProgramServiceImpl implements ProgramService {

	private static final Log log = LogFactory.getLog(ProgramServiceImpl.class);

	@Autowired
	protected ProgramDao  programDao;

	@Override
	public List<ProgramVO> getAll() {
		return programDao.getAll();
	}

	@Override
	public ProgramVO get(int id) {
		return programDao.get(id);
	}

	@Override
	public ProgramVO getByLabel(String label) {
		return programDao.getByLabel(label);
	}
}


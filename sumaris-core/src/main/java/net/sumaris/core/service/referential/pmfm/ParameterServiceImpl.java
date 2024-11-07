/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.service.referential.pmfm;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.referential.pmfm.ParameterRepository;
import net.sumaris.core.vo.referential.pmfm.ParameterVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("parameterService")
@Slf4j
public class ParameterServiceImpl implements ParameterService {

	@Autowired
	protected ParameterRepository parameterRepository;

	@Override
	public ParameterVO getByLabel(final String label) {
		Preconditions.checkNotNull(label);
		Preconditions.checkArgument(label.trim().length() > 0);
		return parameterRepository.getByLabel(label.trim());
	}

	@Override
	public ParameterVO get(int parameterId) {
		return parameterRepository.get(parameterId);
	}

	@Override
	public ParameterVO save(ParameterVO parameter) {
		return parameterRepository.save(parameter);
	}
}

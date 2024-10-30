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

import net.sumaris.core.vo.referential.pmfm.ParameterVO;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ParameterService {

    @Transactional(readOnly = true)
    ParameterVO getByLabel(String label);

    @Transactional(readOnly = true)
    ParameterVO get(int pmfmId);

    ParameterVO save(ParameterVO pmfm);
}

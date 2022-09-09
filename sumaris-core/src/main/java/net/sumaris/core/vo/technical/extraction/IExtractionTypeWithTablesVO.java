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

package net.sumaris.core.vo.technical.extraction;

import net.sumaris.core.dao.technical.model.IValueObject;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;

import java.util.Optional;
import java.util.Set;

public interface IExtractionTypeWithTablesVO
    extends IExtractionType<PersonVO, DepartmentVO>, IValueObject<Integer> {

    Set<String> getTableNames();

    Optional<String> findTableNameBySheetName(String sheetName);

    Optional<String> findSheetNameByTableName(String tableName);
}

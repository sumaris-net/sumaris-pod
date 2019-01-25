package net.sumaris.core.service.extraction;

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
import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.extraction.table.ExtractionTableDao;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.vo.extraction.ExtractionTypeVO;
import net.sumaris.core.vo.extraction.ExtractionFilterVO;
import net.sumaris.core.vo.extraction.ExtractionResultVO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("extractionService")
public class ExtractionServiceImpl implements ExtractionService {

	private static final Log log = LogFactory.getLog(ExtractionServiceImpl.class);

	@Autowired
	protected ExtractionTableDao extractionTableDao;

	@Override
	public List<ExtractionTypeVO> getAllTypes() {
		return new ImmutableList.Builder()
				// Add tables
				.addAll(extractionTableDao.getAllTableNames()
						.stream().map(tableName -> {
							ExtractionTypeVO type = new ExtractionTypeVO();
							type.setLabel(tableName);
							type.setCategory(ExtractionTableDao.CATEGORY);
							return type;
						}).collect(Collectors.toList())
				)
				.build();
	}

	@Override
	public ExtractionResultVO getRows(ExtractionTypeVO type, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(type.getLabel());
		Preconditions.checkNotNull(type.getCategory());

		switch (type.getCategory()) {
			case "table":
				return getTableRows(type.getLabel(), filter, offset, size, sort, direction);
			default:
				throw new IllegalArgumentException("Unknown extraction category: " + type.getCategory());
		}
	}

	/* -- protected -- */

	protected ExtractionResultVO getTableRows(String tableName, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
		Preconditions.checkNotNull(tableName);
		Preconditions.checkArgument(offset >= 0);
		Preconditions.checkArgument(size <= 1000, "maximum value for 'size' is: 1000");
		Preconditions.checkArgument(size >= 0, "'size' must be greater or equals to 0");

		DatabaseTableEnum tableEnum = DatabaseTableEnum.valueOf(tableName.toUpperCase());

		return extractionTableDao.getTableRows(tableEnum, filter != null ? filter : new ExtractionFilterVO(), offset, size, sort, direction);
	}
}

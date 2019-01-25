package net.sumaris.core.dao.extraction.table;

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
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisEntityTableMetadata;
import net.sumaris.core.vo.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.envers.query.criteria.ExtendableCriterion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

@Repository("extractionTableDao")
public class ExtractionTableDaoImpl extends HibernateDaoSupport implements ExtractionTableDao {

	private static final Log log = LogFactory.getLog(ExtractionTableDaoImpl.class);


	@Autowired
	protected SumarisConfiguration config;

	@Autowired
	protected SumarisDatabaseMetadata databaseMetadata;

	@Override
	public List<String> getAllTableNames() {
		return ImmutableList.copyOf(databaseMetadata.getTableNames());
	}

	@Override
	public ExtractionResultVO getTable(DatabaseTableEnum table) {
		return getTableRows(table, null, 0, 0, null, null);
	}

	@Override
	public ExtractionResultVO getTableRows(DatabaseTableEnum tableEnum, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
		Preconditions.checkNotNull(tableEnum);
		String tableName = tableEnum.name().toLowerCase();

		SumarisEntityTableMetadata table = databaseMetadata.getEntityTable(tableName);
		Preconditions.checkNotNull(table, "Unknown table: " + tableName);

		ExtractionResultVO result = new ExtractionResultVO();

		ExtractionTypeVO type = new ExtractionTypeVO();
		type.setLabel(table.getName().toLowerCase());
		type.setCategory(CATEGORY);

		// Set columns metadata
		List<ExtractionColumnMetadataVO> columns = table.getColumnNames().stream()
				.map(table::getColumnMetadata)
				.map(this::toExtractionColumnVO)
				.collect(Collectors.toList());
		result.setColumns(columns);

		// Compute the rank order
		String[] orderedColumnNames = ExtractionTableColumnOrder.COLUMNS_BY_TABLE.get(tableEnum);
		if (ArrayUtils.isNotEmpty(orderedColumnNames)) {
			int maxRankOrder = -1;
			for (ExtractionColumnMetadataVO column : columns) {
				int rankOrder = ArrayUtils.indexOf(orderedColumnNames, column.getName().toLowerCase());
				if (rankOrder != -1) {
					column.setRankOrder(rankOrder+1);
					maxRankOrder = Math.max(maxRankOrder, rankOrder+1);
				}
			}
			// Set rankOrder of unknown columns (e.g. new columns)
			for (ExtractionColumnMetadataVO column : columns) {
				if (column.getRankOrder() == null) {
					column.setRankOrder(++maxRankOrder);
				}
			}
		}

		String whereClause = getSqlWhereClause(table, filter);

		// Count rows
		Number total = getRowCount(table, whereClause);
		result.setTotal(total);

		if (size > 0 && total.longValue() > 0) {
			List<String[]> rows = getRows(table, whereClause, offset, size, sort, direction);
			result.setRows(rows);
		}

		return result;
	}




	/* -- protected method -- */

	protected Number getRowCount(SumarisEntityTableMetadata table, String whereClause) {

		String sql = table.getCountAllQuery();

		if (StringUtils.isNotBlank(whereClause)) {
			sql += whereClause;
		}

		Number total = (Number) getEntityManager()
				.createNativeQuery(sql)
				.getSingleResult();
		return total;
	}

	protected List<String[]> getRows(SumarisEntityTableMetadata table, String whereClause, int offset, int size, String sort, SortDirection direction) {

		String tableAlias = "t";

		String sql = table.getSelectAllQuery();

		if (StringUtils.isNotBlank(whereClause)) {
			sql += whereClause;
		}

		// Add order by
		if (StringUtils.isNotBlank(sort)) {
			sql += String.format(" ORDER BY %s.%s %s", tableAlias, sort, (direction != null ? direction.name() : ""));
		}

		Query query = getEntityManager().createNativeQuery(sql)
				.setFirstResult(offset)
				.setMaxResults(size);
		return toTableRowsVO(query.getResultList(), table.getColumnsCount());
	}

	protected List<String[]> toTableRowsVO(List<Object[]> rows, final int resultLength) {
		return rows.stream().map(r -> toTableRowVO(r, resultLength))
				.collect(Collectors.toList());
	}

	protected String[] toTableRowVO(Object[] row, int resultLength){
		String[] result = new String[resultLength];
		if (resultLength <= 0) resultLength = row.length;
		for (int i = 0; i < resultLength ; i++) {
			if (row[i] != null) {
				result[i] = row[i].toString();
			} else {
				result[i] = null;
			}
		}
		return result;
	}

	protected ExtractionColumnMetadataVO toExtractionColumnVO(SumarisColumnMetadata columnMetadata){
		ExtractionColumnMetadataVO column = new ExtractionColumnMetadataVO();
		column.setName(columnMetadata.getName());

		column.setDescription(columnMetadata.getDescription());
		column.setDefaultValue(columnMetadata.getDefaultValue());

		String type;
		switch (columnMetadata.getTypeCode()) {
			case Types.NUMERIC:
			case Types.INTEGER:
			case Types.BIGINT:
				type = "integer";
				break;
			case Types.FLOAT:
			case Types.DOUBLE:
				type = "double";
				break;
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.NVARCHAR:
				type = "string";
				break;
			default:
				type = columnMetadata.getTypeName().toLowerCase();
		}
		column.setType(type);
		return column;
	}

	protected boolean isNumericColumn(SumarisColumnMetadata column) {
		return column.getTypeCode() == Types.NUMERIC
				|| column.getTypeCode() == Types.INTEGER
				|| column.getTypeCode() == Types.DOUBLE
				|| column.getTypeCode() == Types.BIGINT
				|| column.getTypeCode() == Types.DECIMAL
				|| column.getTypeCode() == Types.FLOAT;
	}

	protected String getSqlWhereClause(SumarisEntityTableMetadata table, ExtractionFilterVO filter) {

		String tableAlias = "t";

		String sql = "";

		if (CollectionUtils.isNotEmpty(filter.getCriteria())) {
			sql += " WHERE 1=1";
			for (ExtractionFilterCriterionVO criterion: filter.getCriteria()) {

				// Get the column to filter
				Preconditions.checkNotNull(criterion.getName());
				SumarisColumnMetadata column = table.getColumnMetadata(criterion.getName().toLowerCase());
				Preconditions.checkNotNull(column, String.format("Invalid criterion: '%s' is not a valid column name", criterion.getName()));

				if (criterion.getValue() == null) {
					sql += String.format(" AND %s.%s IS NULL", tableAlias, column.getName());
				}
				else {
					if (isNumericColumn(column)) {
						sql += String.format(" AND %s.%s = %s", tableAlias, column.getName(), criterion.getValue());
					} else {
						sql += String.format(" AND %s.%s = '%s'", tableAlias, column.getName(), criterion.getValue());
					}
				}
			}
		}

		return sql;
	}
}

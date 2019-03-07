package net.sumaris.core.extraction.dao.technical.table;

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


import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.*;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.vo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allow to export rows from a table (in VO), with metadata on each columns
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionTableDao")
@Lazy
public class ExtractionTableDaoImpl extends ExtractionBaseDaoImpl implements ExtractionTableDao {

	private static final Logger log = LoggerFactory.getLogger(ExtractionTableDaoImpl.class);

	@Autowired
	protected SumarisDatabaseMetadata databaseMetadata;

	@Autowired
	protected DataSource dataSource = null;

	@Override
	public List<String> getAllTableNames() {
		return ImmutableList.copyOf(databaseMetadata.getTableNames());
	}

	@Override
	public ExtractionResultVO getTable(String tableName) {
		return getTableRows(tableName, null, 0, 0, null, null);
	}

	@Override
	public ExtractionResultVO getTableRows(String tableName, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
		Preconditions.checkNotNull(tableName);

		SumarisTableMetadata table = databaseMetadata.getTable(tableName.toLowerCase());
		Preconditions.checkNotNull(table, "Unknown table: " + tableName);

		ExtractionResultVO result = new ExtractionResultVO();

		// Set columns metadata
		List<ExtractionColumnMetadataVO> columns = table.getColumnNames().stream()
				.map(table::getColumnMetadata)
				.map(this::toExtractionColumnVO)
				.collect(Collectors.toList());
		result.setColumns(columns);

		// Compute the rank order
		String[] orderedColumnNames = ExtractionTableColumnOrder.COLUMNS_BY_TABLE.get(tableName);
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

		String whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);

		// Count rows
		Number total = getRowCount(table, whereClause);
		result.setTotal(total);

		if (size > 0 && total.longValue() > 0) {
			List<String[]> rows = getRows(table, whereClause, offset, size, sort, direction);
			result.setRows(rows);
		}

		return result;
	}

	@Override
	public void dropTable(String tableName) {
		Preconditions.checkNotNull(tableName);
		Preconditions.checkArgument(tableName.toUpperCase().startsWith("EXT_"));

		log.debug(String.format("Dropping extraction table {%s}...", tableName));
		Connection conn = DataSourceUtils.getConnection(dataSource);
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("DROP TABLE " + tableName.toUpperCase());

		}
		catch (SQLException e) {
			throw new SumarisTechnicalException(String.format("Cannot drop extraction table {%s}...", tableName), e);
		}
		finally {
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
	}

	/* -- protected method -- */

	protected Number getRowCount(SumarisTableMetadata table, String whereClause) {

		String sql = table.getCountAllQuery();

		if (StringUtils.isNotBlank(whereClause)) {
			sql += whereClause;
		}

		Number total = (Number) getEntityManager()
				.createNativeQuery(sql)
				.getSingleResult();
		return total;
	}

	protected List<String[]> getRows(SumarisTableMetadata table, String whereClause, int offset, int size, String sort, SortDirection direction) {

		String tableAlias = "t";

		String sql = table.getSelectAllQuery();

		// Where clause
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



}

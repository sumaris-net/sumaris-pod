package net.sumaris.core.dao.technical.schema;

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
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.tool.hbm2ddl.TableMetadata;

import java.sql.DatabaseMetaData;

/**
 * Specialized of {@link SumarisEntityTableMetadata} for association table.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class SumarisAssociationTableMetadata extends SumarisTableMetadata {

	private final String countDataToUpdateQuery;

	private final String dataToUpdateQuery;

	public SumarisAssociationTableMetadata(TableMetadata delegate,
										   DatabaseMetaData meta) {
		super(delegate, meta, null);
		Preconditions.checkState(CollectionUtils.isNotEmpty(getPkNames()));
		Preconditions.checkState(getPkNames().size() > 1);

		StringBuilder queryParams = new StringBuilder("");
		for (String columnName : getColumnNames()) {
			queryParams.append(", ").append(columnName);
		}
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(queryParams.substring(2));
		query.append(" FROM ").append(getName());

		dataToUpdateQuery = query.toString();
		countDataToUpdateQuery = "SELECT count(*) FROM " + getName();
	}

	@Override
	public String getDataToUpdateQuery() {
		return dataToUpdateQuery;
	}

	@Override
	public String getDataToUpdateQueryWithNull() {
		return dataToUpdateQuery;
	}

	@Override
	public String getCountDataToUpdateQuery() {
		return countDataToUpdateQuery;
	}

	@Override
	public String getCountDataToUpdateQueryWithNull() {
		return countDataToUpdateQuery;
	}

	@Override
	public boolean useUpdateDateColumn() {
		return false;
	}

}

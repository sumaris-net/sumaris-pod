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
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Default implementation of the {@link SumarisTableMetadata} for a
 * {@code entity} table, says with a simple primary key.
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
public class SumarisEntityTableMetadata extends SumarisTableMetadata {

	private final String countDataToUpdateQuery;

	private final String countDataToUpdateQueryWithNull;

	private final String dataToUpdateQuery;

	private final String dataToUpdateQueryWithNull;

	public SumarisEntityTableMetadata(Table delegate,
									  SumarisDatabaseMetadata dbMeta,
									  DatabaseMetaData meta,
									  PersistentClass persistentClass) throws SQLException {
		super(delegate, dbMeta, meta, persistentClass);

		Preconditions.checkState(CollectionUtils.isNotEmpty(getPkNames()));
		Preconditions.checkState(getPkNames().size() == 1);

		StringBuilder queryParams = new StringBuilder("");
		for (String columnName : getColumnNames()) {
			queryParams.append(", ").append(columnName);
		}
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(queryParams.substring(2));
		query.append(" FROM ").append(getName());

		dataToUpdateQueryWithNull = query.toString();
		countDataToUpdateQueryWithNull = "SELECT count(*) FROM " + getName();

		String whereClause;

		if (isWithUpdateDateColumn()) {

			// add a filter
			whereClause = " WHERE (update_date IS NULL OR update_date > ?)";
		} else {
			whereClause = "";
		}
		dataToUpdateQuery = dataToUpdateQueryWithNull + whereClause;
		countDataToUpdateQuery = countDataToUpdateQueryWithNull + whereClause;
	}

	@Override
	public String getDataToUpdateQuery() {
		return dataToUpdateQuery;
	}

	@Override
	public String getDataToUpdateQueryWithNull() {
		return dataToUpdateQueryWithNull;
	}

	@Override
	public String getCountDataToUpdateQuery() {
		return countDataToUpdateQuery;
	}

	@Override
	public String getCountDataToUpdateQueryWithNull() {
		return countDataToUpdateQueryWithNull;
	}

	@Override
	public boolean useUpdateDateColumn() {
		return true;
	}

}

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


import org.hibernate.mapping.Column;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

/**
 * TODO: rename getter
 */
public class SumarisColumnMetadata {

	protected final Column delegate;
	protected final String propertyName;
	protected final String defaultValue;

	// From JDBC meta
	protected final int columnSize;
	protected final int decimalDigits;
	protected final int typeCode;
	protected final String typeName;
	protected final boolean isNullable;

	public SumarisColumnMetadata(ResultSet rs, Column column, String propertyName) throws SQLException {
		this.delegate = column;
		this.propertyName = propertyName;
		this.defaultValue = null;

		// Add additional info from JDBC meta
		this.columnSize = rs.getInt("COLUMN_SIZE");
		this.decimalDigits = rs.getInt("DECIMAL_DIGITS");
		this.typeCode = rs.getInt("DATA_TYPE");
		this.isNullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
		this.typeName = (new StringTokenizer(rs.getString("TYPE_NAME"), "() ")).nextToken();
	}

	public SumarisColumnMetadata(ResultSet rs, Column column, String propertyName, String defaultValue) throws SQLException {
		this.delegate = column;
		this.propertyName = propertyName;
		this.defaultValue = defaultValue;

		// Add additional info from JDBC meta
		// (see https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getColumns-java.lang.String-java.lang.String-java.lang.String-java.lang.String)
		this.columnSize = rs.getInt("COLUMN_SIZE");
		this.decimalDigits = rs.getInt("DECIMAL_DIGITS");
		this.typeCode = rs.getInt("DATA_TYPE");
		this.isNullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
		this.typeName = (new StringTokenizer(rs.getString("TYPE_NAME"), "() ")).nextToken();
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public String getName() {
		return delegate.getName();
	}

	public String getTypeName() {
		return delegate.getSqlType();
	}

	public int getColumnSize() {
		return columnSize;
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public String getNullable() {
		return delegate.isNullable() ? "YES": "NO";
	}

	public String toString() {
		return delegate.toString();
	}

	public int getTypeCode() {
		return typeCode;
	}
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public boolean isNullable() {
		return delegate.isNullable();
	}

	public String getDefaultValue() {
		return defaultValue;
	}
}

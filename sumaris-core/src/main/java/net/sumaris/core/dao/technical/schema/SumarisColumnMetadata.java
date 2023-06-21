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


import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * A database column metadata
 */
public class SumarisColumnMetadata {

	protected final String catalog;
	protected final String schema;
	protected final String table;
	protected final String name;
	protected final String escapedName;
	protected final boolean nullable;

	protected final String defaultValue;

	// From JDBC meta
	protected final int columnSize;
	protected final int decimalDigits;
	protected final int typeCode;
	protected final String typeName;
	protected final boolean isNullable;
	protected final String description;
	protected final int ordinalPosition;

	public SumarisColumnMetadata(SumarisTableMetadata table, ResultSet rs) throws SQLException {
		this(table, rs, null);
	}

	public SumarisColumnMetadata(SumarisTableMetadata table, ResultSet rs, String defaultValue) throws SQLException {
		this.defaultValue = defaultValue;

		// Add additional info from JDBC meta
		// (see https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getColumns-java.lang.String-java.lang.String-java.lang.String-java.lang.String)
		this.catalog = rs.getString("TABLE_CAT");
		this.schema = rs.getString("TABLE_SCHEM");
		this.table = rs.getString("TABLE_NAME");
		this.name = rs.getString("COLUMN_NAME");
		this.escapedName = table.getEscapedColumnName(this.name);
		this.nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
		this.columnSize = rs.getInt("COLUMN_SIZE");
		this.decimalDigits = rs.getInt("DECIMAL_DIGITS");
		this.typeCode = rs.getInt("DATA_TYPE");
		this.isNullable = "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
		this.typeName = (new StringTokenizer(rs.getString("TYPE_NAME"), "() ")).nextToken();
		this.description = rs.getString("REMARKS");
		this.ordinalPosition = rs.getInt("ORDINAL_POSITION");

	}

	public int hashCode() {
		return this.name.hashCode();
	}

	public String getName() {
		return this.name;
	}

	public String getEscapedName() {
		return escapedName;
	}

	public String getNullable() {
		return nullable ? "YES": "NO";
	}

	public boolean isNullable() {
		return nullable;
	}

	public String getTypeName() {
		return typeName;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public int getTypeCode() {
		return typeCode;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getDescription() {
		return description;
	}

	public int getOrdinalPosition() {
		return ordinalPosition;
	}


	public String toString() {
		return new StringBuilder()
				.append(catalog).append('.')
				.append(schema).append('.')
				.append(table).append('.')
				.append(name)
				.toString();
	}

	public boolean equals(Object other) {

		if (this == other) return true;
		if ( !(other instanceof SumarisColumnMetadata) ) return false;

		final SumarisColumnMetadata bean = (SumarisColumnMetadata) other;

		if (!Objects.equals(bean.catalog, catalog) ||
			!Objects.equals(bean.schema, schema) ||
			!Objects.equals(bean.table, table) ||
			!Objects.equals(bean.name, name)) return false;

		return true;
	}


}

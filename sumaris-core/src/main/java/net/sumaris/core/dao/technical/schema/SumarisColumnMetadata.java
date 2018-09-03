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


import org.hibernate.tool.hbm2ddl.ColumnMetadata;

public class SumarisColumnMetadata {

	protected final ColumnMetadata delegate;

	protected final String propertyName;

	public SumarisColumnMetadata(ColumnMetadata columnMetadata, String propertyName) {
		this.delegate = columnMetadata;
		this.propertyName = propertyName;
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public String getName() {
		return delegate.getName();
	}

	public String getTypeName() {
		return delegate.getTypeName();
	}

	public int getColumnSize() {
		return delegate.getColumnSize();
	}

	public int getDecimalDigits() {
		return delegate.getDecimalDigits();
	}

	public String getNullable() {
		return delegate.getNullable();
	}

	public String toString() {
		return delegate.toString();
	}

	public int getTypeCode() {
		return delegate.getTypeCode();
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public boolean isNullable() {
		return !"NO".equals(delegate.getNullable());
	}

}

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

/**
 * A database column metadata, from a model entity
 */
public class SumarisHibernateColumnMetadata extends SumarisColumnMetadata {

	protected final Column delegate;


	public SumarisHibernateColumnMetadata(ResultSet rs, Column column) throws SQLException {
		this(rs, column, null);
	}

	public SumarisHibernateColumnMetadata(ResultSet rs, Column column, String defaultValue) throws SQLException {
		super(rs, defaultValue);
		this.delegate = column;
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public String getName() {
		return delegate.getName();
	}

	public String getNullable() {
		return delegate.isNullable() ? "YES": "NO";
	}

	public String toString() {
		return delegate.toString();
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public boolean isNullable() {
		return delegate.isNullable();
	}
}
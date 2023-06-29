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
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Overrides of the {@link SumarisTableMetadata} with some improvements:
 * <ul>
 * <li>Obtains HSQL slect query via {@link #getSelectHQLQuery()}</li>
 * <li>Use cached JPA data to load index/foreign keys</li>
 * </ul>
 * <p/>
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
@Slf4j
public class SumarisHibernateTableMetadata extends SumarisTableMetadata {

	protected final Table delegate;

	protected final PersistentClass persistentClass;

	protected String hqlSelectQuery;

	protected Map<String, ForeignKey> foreignKeys;

	public SumarisHibernateTableMetadata(Table delegate,
										 SumarisDatabaseMetadata dbMeta,
										 DatabaseMetaData jdbcDbMeta,
										 PersistentClass persistentClass) throws SQLException{
		this(delegate.getQualifiedTableName(), delegate, dbMeta, jdbcDbMeta, persistentClass);
	}

	protected SumarisHibernateTableMetadata(QualifiedTableName tableName,
											Table delegate,
											SumarisDatabaseMetadata dbMeta,
											DatabaseMetaData jdbcDbMeta,
											PersistentClass persistentClass) throws SQLException{
		super(tableName, dbMeta, jdbcDbMeta);
		Preconditions.checkNotNull(dbMeta);

		this.delegate = delegate;
		this.persistentClass = persistentClass;

		init(dbMeta, jdbcDbMeta);
	}

	protected void init(SumarisDatabaseMetadata dbMeta, DatabaseMetaData jdbcDbMeta) throws SQLException {
		if (this.delegate == null) return; // skip, when called by inherited constructor

		// super init
		super.init(dbMeta, jdbcDbMeta);

		// Specific to Hibernate
		try {

			this.foreignKeys = initForeignKeys(delegate);

			this.hqlSelectQuery = persistentClass != null ? String.format(QUERY_HQL_SELECT, this.persistentClass.getEntityName()) : null;

		} catch (Exception e) {
			throw new SQLException("Could not init metadata on table " + tableName.getTableName(), e);
		}
	}

	public String getSelectHQLQuery() {
		return hqlSelectQuery;
	}

	public int getColumnsCount() {
		return columns.size();
	}

	public Map<String, ForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	@Override
	protected Set<String> initPrimaryKeys(DatabaseMetaData jdbcDbMeta) {
		Preconditions.checkNotNull(delegate);
		Map<String, Column> result = Maps.newLinkedHashMap();
		for(Iterator propertyIterator = delegate.getPrimaryKey().getColumnIterator();
			propertyIterator.hasNext(); ) {
			Column column = (Column) propertyIterator.next();
			result.put(column.getName(), column);
		}

		return result.keySet();
	}

	@Override
	protected Map<String, SumarisColumnMetadata> initColumns(QualifiedTableName tableName, DatabaseMetaData jdbcDbMeta) throws SQLException {
		Preconditions.checkNotNull(delegate);
		Map<String, Column> columns = Maps.newLinkedHashMap();
		for (Iterator propertyIterator = delegate.getColumnIterator();
			 propertyIterator.hasNext(); ) {
			Column column = (Column) propertyIterator.next();
			columns.put(column.getName().toLowerCase(), column);
		}

		Map<String, SumarisColumnMetadata> result = Maps.newLinkedHashMap();
		ResultSet rs = jdbcDbMeta.getColumns(getCatalog(), getSchema(), getName(), "%");

		try {
			while (rs.next()) {
				String columnName = rs.getString("COLUMN_NAME").toLowerCase();
				String defaultValue = SumarisConfiguration.getInstance().getColumnDefaultValue(getName(), columnName);

				Column column = columns.get(columnName);
				if (column != null) {
					SumarisHibernateColumnMetadata columnMeta = new SumarisHibernateColumnMetadata(this, rs, column, defaultValue);
					result.put(columnName.toLowerCase(), columnMeta);
				}
				else {
					log.warn(String.format("Column {%s} not mapped in the entity class {%s}", columnName, persistentClass.getEntityName()));
					SumarisColumnMetadata columnMeta = new SumarisColumnMetadata(this, rs, defaultValue);
					result.put(columnName.toLowerCase(), columnMeta);
				}
			}
		}
		finally {
			if (rs != null) {
				rs.close();
			}
		}
		if (result.size() == 0) {
			throw new RuntimeException("Unable to load columns metadata on table " + delegate.getName());
		}

		return result;
	}

	protected Map<String, ForeignKey> initForeignKeys(Table delegate) {
		Map<String, ForeignKey> result = Maps.newLinkedHashMap();
		for(Iterator foreignKeyIterator = delegate.getForeignKeyIterator();
			foreignKeyIterator.hasNext(); ) {
			ForeignKey foreignKey = (ForeignKey) foreignKeyIterator.next();

			result.put(foreignKey.getName().toLowerCase(), foreignKey);
		}

		return result;
	}
}

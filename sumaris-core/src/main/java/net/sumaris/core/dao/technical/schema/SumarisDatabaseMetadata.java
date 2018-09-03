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
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Table;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Sumaris database metadatas.
 * 
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 * @since 1.0
 */
@Lazy
@Component(value = "sumarisDatabaseMetadata")
public class SumarisDatabaseMetadata {

	/** Logger. */
	private static final Log log =
			LogFactory.getLog(SumarisDatabaseMetadata.class);

	protected Map<String, SumarisTableMetadata> tables;
	protected Map<String, SingleTableEntityPersister> entities;

	protected String defaultSchemaName = null;
	protected String defaultCatalogName = null;
	protected Dialect dialect = null;
	protected SessionFactoryImplementor sessionFactoryImpl = null;

	protected DataSource dataSource = null;

	@Autowired
	public SumarisDatabaseMetadata(SessionFactory sessionFactory, DataSource dataSource) {
		super();
		Preconditions.checkNotNull(sessionFactory);
		Preconditions.checkNotNull(dataSource);
		Preconditions.checkArgument(sessionFactory instanceof SessionFactoryImplementor);

		this.dataSource = dataSource;
		sessionFactoryImpl = ((SessionFactoryImplementor) sessionFactory);
		dialect = sessionFactoryImpl.getJdbcServices().getDialect();
		Preconditions.checkNotNull(dialect);

		defaultSchemaName = sessionFactoryImpl.getSettings().getDefaultSchemaName();
		defaultCatalogName = sessionFactoryImpl.getSettings().getDefaultCatalogName();

		tables = Maps.newTreeMap();
		entities = Maps.newHashMap();

		loadAllTables();
	}

	protected void loadAllTables() {

		Map<String, ClassMetadata> allClassMetadata = sessionFactoryImpl.getAllClassMetadata();
		Map<String, SingleTableEntityPersister> allEntities = Maps.newHashMap();
		for (Iterator i = allClassMetadata.values().iterator(); i.hasNext();) {
			SingleTableEntityPersister entity = (SingleTableEntityPersister) i.next();
			allEntities.put(entity.getTableName(), entity);
		}

		Connection conn = null;
		try {
			conn = DataSourceUtils.getConnection(dataSource);
            dialect = sessionFactoryImpl.getJdbcServices().getDialect();

			/*DatabaseInformation databaseInformation = new DatabaseInformationImpl(
                    sessionFactoryImpl.getServiceRegistry(),
                    sessionFactoryImpl.getJdbcServices(), sessionFactoryImpl.
            );
			conn, dialect, true);
			DatabaseMetaData jdbcMeta = conn.getMetaData();
			for (DatabaseTableEnum table : DatabaseTableEnum.values()) {
				String tableName = table.id();
				if (log.isDebugEnabled()) {
					log.debug("Load metas of table: " + tableName);
				}
				String key = Table.qualify(defaultCatalogName, defaultSchemaName, tableName);
				SingleTableEntityPersister entityPersister = allEntities.get(key);
				entities.put(tableName, entityPersister);

				getTable(tableName, defaultSchemaName, defaultCatalogName, false, databaseInformation, jdbcMeta, entityPersister);
			}
		} catch (SQLException e) {
			throw new RuntimeException(
					"Could not init database meta on connection " + conn, e);*/
		} finally {
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
	}

	protected SumarisTableMetadata getTable(String name,
											String schema,
											String catalog,
											boolean isQuoted,
                                            DatabaseInformation databaseInformation,
											DatabaseMetaData jdbcMeta,
											SingleTableEntityPersister entityPersister) throws HibernateException {
		Preconditions.checkNotNull(databaseInformation);
		Preconditions.checkNotNull(jdbcMeta);

		String key = Table.qualify(catalog, schema, name);
		SumarisTableMetadata sumarisTableMetadata = tables.get(key);
		/*if (sumarisTableMetadata == null) {
            QualifiedTableName qtn = new QualifiedTableName(
                    new Identifier(id, isQuoted),
                    new Identifier(schema, isQuoted),
                    new Identifier(catalog, isQuoted));
			TableInformation tableInformation = databaseInformation.getTableInformation(qtn);
			Preconditions.checkNotNull(tableInformation,
					"Could not find db table " + id);
			DatabaseTableEnum table = DatabaseTableEnum.valueOf(id);
			Preconditions.checkNotNull(table,
					"Could not find db table " + table);
			if (table.isAssociation()) {
				sumarisTableMetadata = new SumarisAssociationTableMetadata(tableInformation, jdbcMeta);
			} else {
				sumarisTableMetadata = new SumarisEntityTableMetadata(tableInformation, jdbcMeta, entityPersister);
			}
			Preconditions.checkNotNull(sumarisTableMetadata,
					"Could not find db table " + id);
			tables.put(key, sumarisTableMetadata);
		}*/
		return sumarisTableMetadata;
	}

	protected SumarisTableMetadata getTable(String name,
											String schema,
											String catalog) throws HibernateException {
		String key = Table.qualify(catalog, schema, name);
		SumarisTableMetadata sumarisTableMetadata = tables.get(key);
		/*if (sumarisTableMetadata == null) {
			// Create a new connection then retrieve the metadata :
			Connection conn = null;
			try {
				conn = DataSourceUtils.getConnection(dataSource);

                DatabaseInformation hibernateMeta = new DatabaseInformationImpl(conn, dialect, true);
				DatabaseMetaData jdbcMeta = conn.getMetaData();
				SingleTableEntityPersister entity = entities.get(id);

				return getTable(id, schema, catalog, false, hibernateMeta, jdbcMeta, entity);
			} catch (SQLException e) {
				throw new RuntimeException(
						"Could not init database meta on connection " + conn, e);
			} finally {
				DataSourceUtils.releaseConnection(conn, dataSource);
			}

		}*/
		return sumarisTableMetadata;
	}

	public SumarisTableMetadata getTable(String name) throws HibernateException {
		return getTable(name, defaultSchemaName, defaultCatalogName);
	}

	public SumarisEntityTableMetadata getEntityTable(String name) throws HibernateException {
		SumarisTableMetadata table = getTable(name);
		return (SumarisEntityTableMetadata) table;
	}

	public SumarisAssociationTableMetadata getAssociationTable(String name) throws HibernateException {
		SumarisTableMetadata table = getTable(name);
		return (SumarisAssociationTableMetadata) table;
	}

	public int getTableCount() {
		return tables.size();
	}

	public Set<String> getTableNames() {
		HashSet<String> result = Sets.newHashSet();
		for (SumarisTableMetadata tableMetadata : tables.values()) {
			result.add(tableMetadata.getName());
		}
		return result;
	}

}

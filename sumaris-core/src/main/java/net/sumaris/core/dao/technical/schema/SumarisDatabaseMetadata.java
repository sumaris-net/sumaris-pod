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
import net.sumaris.core.config.SumarisConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
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

	protected final Map<String, SumarisTableMetadata> tables;
	protected final Map<String, PersistentClass> entities;

	protected String defaultSchemaName = null;
	protected String defaultCatalogName = null;
	protected Dialect dialect = null;

	protected DataSource dataSource = null;

	protected final Metadata metadata;

	protected Set<String> sequences;

	protected String sequenceSuffix;

	protected boolean isQuoted = false;

	@Autowired
	public SumarisDatabaseMetadata(DataSource dataSource, SumarisConfiguration configuration) {
		super();
		Preconditions.checkNotNull(dataSource);
		Preconditions.checkNotNull(configuration);

		this.dataSource = dataSource;
		this.metadata = MetadataExtractorIntegrator.INSTANCE.getMetadata();
		this.dialect = metadata.getDatabase().getDialect();

		this.defaultSchemaName = configuration.getJdbcSchema();
		this.defaultCatalogName = configuration.getJdbcCatalog();
		this.sequenceSuffix = configuration.getSequenceSuffix();

		tables = Maps.newTreeMap();
		entities = Maps.newHashMap();

		//loadAllTables();
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

	public Set<String> getSequences() {
		return sequences;
	}

	public String getSequenceSuffix() {
		return sequenceSuffix;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public String getQualifiedTableName(String catalog, String schema, String tableName) {
		return new QualifiedTableName(
				new Identifier(catalog, isQuoted),
				new Identifier(schema, isQuoted),
				new Identifier(tableName, isQuoted))
				.render().toLowerCase();
	}

	/* -- protected methods -- */


	@PostConstruct
	protected void init() {

		Connection conn = null;
		try {
			conn = DataSourceUtils.getConnection(dataSource);

			// Init sequences
			this.sequences = initSequences(conn, dialect);

			// Init tables
			initTables(conn);

		}
		catch(SQLException e) {
			throw new BeanInitializationException("Could not init SumarisDatabaseMetadata", e);
		}
		finally {
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
	}

	/**
	 * <p>initSequences.</p>
	 *
	 * @param connection a {@link java.sql.Connection} object.
	 * @param dialect a {@link org.hibernate.dialect.Dialect} object.
	 * @return a {@link java.util.Set} object.
	 * @throws java.sql.SQLException if any.
	 */
	protected Set<String> initSequences(Connection connection, Dialect dialect)
			throws SQLException {
		Set<String> sequences = Sets.newHashSet();
		if (dialect.supportsSequences()) {
			String sql = dialect.getQuerySequencesString();
			if (sql != null) {

				Statement statement = null;
				ResultSet rs = null;
				try {
					statement = connection.createStatement();
					rs = statement.executeQuery(sql);

					while (rs.next()) {
						sequences.add(StringUtils.lowerCase(rs.getString(1))
								.trim());
					}
				} finally {
					rs.close();
					statement.close();
				}

			}
		}
		return sequences;
	}

	protected void initTables(Connection conn) {
		Map<String, PersistentClass> persistentClassMap = Maps.newHashMap();
		for ( PersistentClass persistentClass : metadata.getEntityBindings()) {

			Table table = persistentClass.getTable();

			log.debug( String.format("Entity: %s is mapped to table: %s",
					persistentClass.getClassName(),
					table.getName()));

			String catalog = StringUtils.isBlank(table.getCatalog()) ? defaultCatalogName : table.getCatalog();
			String schema = StringUtils.isBlank(table.getSchema()) ? defaultSchemaName : table.getSchema();
			String qualifiedTableName = getQualifiedTableName(catalog, schema, table.getName());
			persistentClassMap.put(qualifiedTableName, persistentClass);

			for(Iterator propertyIterator = persistentClass.getPropertyIterator();
				propertyIterator.hasNext(); ) {
				Property property = (Property) propertyIterator.next();

				for(Iterator columnIterator = property.getColumnIterator();
					columnIterator.hasNext(); ) {
					Column column = (Column) columnIterator.next();

					log.debug( String.format("Property: %s is mapped on table column: %s of type: %s",
							property.getName(),
							column.getName(),
							column.getSqlType())
					);
				}
			}
		}

		try {
			DatabaseMetaData jdbcMeta = conn.getMetaData();

			// Init tables
			for (DatabaseTableEnum table : DatabaseTableEnum.values()) {
				String tableName = table.name().toLowerCase();
				if (log.isDebugEnabled()) {
					log.debug("Load metas of table: " + tableName);
				}
				String qualifiedTableName = getQualifiedTableName(defaultCatalogName, defaultSchemaName, tableName);
				PersistentClass persistentClass = persistentClassMap.get(qualifiedTableName);
				entities.put(qualifiedTableName, persistentClass);

				getTable(tableName, defaultSchemaName, defaultCatalogName, jdbcMeta, persistentClass);
			}
		}
		catch (SQLException e) {
			throw new BeanInitializationException(
					"Could not init database meta on connection " + conn, e);
		}
		finally {
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
	}

	protected SumarisTableMetadata getTable(String name,
											String schema,
											String catalog,
											DatabaseMetaData jdbcMeta,
											PersistentClass persistentClass) throws HibernateException, SQLException {
		Preconditions.checkNotNull(jdbcMeta);

		String qualifiedTableName = getQualifiedTableName(catalog, schema, name);
		SumarisTableMetadata sumarisTableMetadata = tables.get(qualifiedTableName);
		if (sumarisTableMetadata == null) {

			if (persistentClass == null) {
				persistentClass = entities.get(qualifiedTableName);
				Preconditions.checkNotNull(persistentClass,
						"Could not find db table " + name);
			}
			Table table = persistentClass.getTable();
			table.setCatalog("PUBLIC");
			table.setSchema("PUBLIC");
			DatabaseTableEnum tableEnum = DatabaseTableEnum.valueOf(name.toUpperCase());
			Preconditions.checkNotNull(tableEnum,
					"Could not find db table " + tableEnum);
			if (tableEnum.isAssociation()) {
				sumarisTableMetadata = new SumarisAssociationTableMetadata(table, this, jdbcMeta);
			} else {
				sumarisTableMetadata = new SumarisEntityTableMetadata(table, this, jdbcMeta, persistentClass);
			}
			Preconditions.checkNotNull(sumarisTableMetadata,
					"Could not find db table " + name);
			tables.put(qualifiedTableName, sumarisTableMetadata);
		}
		return sumarisTableMetadata;
	}

	protected SumarisTableMetadata getTable(String name,
											String schema,
											String catalog) throws HibernateException {
		String qualifiedTableName = getQualifiedTableName(catalog, schema, name);
		SumarisTableMetadata sumarisTableMetadata = tables.get(qualifiedTableName);
		if (sumarisTableMetadata == null) {
			// Create a new connection then retrieve the metadata :
			Connection conn = null;
			try {
				conn = DataSourceUtils.getConnection(dataSource);
				DatabaseMetaData jdbcMeta = conn.getMetaData();
				PersistentClass persistentClass = entities.get(name);
				return getTable(name, schema, catalog, jdbcMeta, persistentClass);
			} catch (SQLException e) {
				throw new RuntimeException(
						"Could not init database meta on connection " + conn, e);
			} finally {
				DataSourceUtils.releaseConnection(conn, dataSource);
			}

		}
		return sumarisTableMetadata;
	}
}

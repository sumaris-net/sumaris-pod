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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.CacheConfiguration;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.*;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
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
@Slf4j
public class SumarisDatabaseMetadata {


	protected final Map<String, SumarisTableMetadata> tables;
	protected final Map<String, PersistentClass> entities;

	protected String defaultSchemaName;
	protected final String defaultCatalogName;
	protected final Dialect dialect;

	protected final DataSource dataSource;

	protected final Metadata metadata;

	protected Set<String> sequences;

	protected final String sequenceSuffix;

	protected final String defaultUpdateDateColumnName;

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

		this.defaultUpdateDateColumnName = "update_date"; // TODO: load it from configuration

		tables = Maps.newTreeMap();
		entities = Maps.newHashMap();

		//loadAllTables();
	}

	@Cacheable(cacheNames = CacheConfiguration.Names.TABLE_META_BY_NAME, key = "#name.toLowerCase()", unless = "#result == null")
	public SumarisHibernateTableMetadata getHibernateTable(String name) throws HibernateException {
		return (SumarisHibernateTableMetadata) getTable(name);
	}

	@Cacheable(cacheNames = CacheConfiguration.Names.TABLE_META_BY_NAME, key = "#name.toLowerCase()", unless = "#result == null")
	public SumarisTableMetadata getTable(String name) throws HibernateException {
		return getTable(name, defaultSchemaName, defaultCatalogName);
	}


	@Caching(evict = {
			@CacheEvict(cacheNames = CacheConfiguration.Names.TABLE_META_BY_NAME, key = "#name.toLowerCase()")
	})
	public void clearCache(String name) {
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

	public QualifiedTableName getQualifiedTableName(String catalog, String schema, String tableName) {
		return new QualifiedTableName(
				Identifier.toIdentifier(catalog),
				Identifier.toIdentifier(schema),
				Identifier.toIdentifier(tableName));
	}

	public String getDefaultUpdateDateColumnName() {
		return defaultUpdateDateColumnName;
	}


	public SumarisTableMetadata getTable(String name,
										 String schema,
										 String catalog) throws HibernateException {
		QualifiedTableName qualifiedTableName = getQualifiedTableName(catalog, schema, name);
		String cacheKey = getCacheKey(qualifiedTableName);
		SumarisTableMetadata sumarisTableMetadata = tables.get(cacheKey);
		if (sumarisTableMetadata == null) {
			// Create a new connection then retrieve the metadata :
			Connection conn = null;
			try {
				conn = DataSourceUtils.getConnection(dataSource);
				DatabaseMetaData jdbcMeta = conn.getMetaData();
				return getTable(qualifiedTableName, jdbcMeta, null);
			} catch (SQLException e) {
				throw new RuntimeException(
					"Could not init database meta on connection " + conn, e);
			} finally {
				DataSourceUtils.releaseConnection(conn, dataSource);
			}

		}
		return sumarisTableMetadata;
	}

	public Set<String> findTableNamesByPrefix(String tablePrefix) {
		return findTableNamesByPrefix(tablePrefix, defaultSchemaName, defaultCatalogName);
	}
	public Set<String> findTableNamesByPrefix(String tablePrefix, String schema, String catalog) {
		// Create a new connection then retrieve the metadata :
		Connection conn = null;
		try {
			conn = DataSourceUtils.getConnection(dataSource);
			DatabaseMetaData jdbcMeta = conn.getMetaData();

			ResultSet rs = jdbcMeta.getTables(catalog, schema, Daos.getEscapedForLike(tablePrefix) /*escape undescrore*/ + "%", null);
			Set<String> result = Sets.newHashSet();
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				if (tableName.toUpperCase().startsWith(tablePrefix.toUpperCase())) {
					result.add(rs.getString("TABLE_NAME"));
				}
				// JDBC meta return a bad tableName (e.g. because '_' special character) => ignore
				else {
					log.trace("Ignoring getTables() result, because table name '{}' not start with '{}'", tableName, tablePrefix);
				}
			}
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(
				"Could not init database meta on connection " + conn, e);
		} finally {
			DataSourceUtils.releaseConnection(conn, dataSource);
		}
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
	 * @param connection a {@link Connection} object.
	 * @param dialect a {@link Dialect} object.
	 * @return a {@link Set} object.
	 * @throws SQLException if any.
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
		for (PersistentClass persistentClass: metadata.getEntityBindings()) {

			Table table = persistentClass.getTable();

			log.debug( String.format("Entity: %s is mapped to table: %s",
					persistentClass.getClassName(),
					table.getName()));

			String catalog = StringUtils.isBlank(table.getCatalog()) ? defaultCatalogName : table.getCatalog();
			String schema = StringUtils.isBlank(table.getSchema()) ? defaultSchemaName : table.getSchema();
			QualifiedTableName qualifiedTableName = getQualifiedTableName(catalog, schema, table.getName());
			String cacheKey = getCacheKey(qualifiedTableName);
			persistentClassMap.put(cacheKey, persistentClass);

			if (log.isDebugEnabled()) {
				for (Iterator propertyIterator = persistentClass.getPropertyIterator();
					 propertyIterator.hasNext(); ) {
					Property property = (Property) propertyIterator.next();

					for (Iterator columnIterator = property.getColumnIterator();
						 columnIterator.hasNext(); ) {
						Selectable columnSelectable = (Selectable) columnIterator.next();
						if (columnSelectable instanceof Column) {
							Column column = (Column) columnSelectable;
							log.debug(String.format("Property: %s is mapped on table column: %s of type: %s",
									property.getName(),
									column.getName(),
									column.getSqlType())
							);
						}
					}
				}
			}
		}

		try {
			DatabaseMetaData jdbcMeta = conn.getMetaData();
			boolean useTableLowercase = Daos.isPostgresqlDatabase(conn);

			// Init tables
			for (DatabaseTableEnum table : DatabaseTableEnum.values()) {
				String tableName = table.name();
				if (useTableLowercase) tableName = tableName.toLowerCase();
				if (log.isDebugEnabled()) {
					log.debug("Load metas of table: " + tableName);
				}
				QualifiedTableName qualifiedTableName = getQualifiedTableName(defaultCatalogName, defaultSchemaName, tableName);
				String cacheKey = getCacheKey(qualifiedTableName);
				PersistentClass persistentClass = persistentClassMap.get(cacheKey);
				entities.put(cacheKey, persistentClass);

				getTable(qualifiedTableName, jdbcMeta, persistentClass);
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

	protected SumarisTableMetadata getTable(QualifiedTableName qualifiedTableName,
											DatabaseMetaData jdbcMeta,
											PersistentClass persistentClass) throws HibernateException, SQLException {
		Preconditions.checkNotNull(qualifiedTableName);
		Preconditions.checkNotNull(jdbcMeta);

		String cacheKey = getCacheKey(qualifiedTableName);
		SumarisTableMetadata sumarisTableMetadata = tables.get(cacheKey);
		if (sumarisTableMetadata == null) {
			// Try to retrieve the persistence class
			if (persistentClass == null) {
				persistentClass = entities.get(cacheKey);
			}

			// If persistence class exists
			if (persistentClass != null) {
				// Get the table mapping
				Table table = persistentClass.getTable();
				table.setName(qualifiedTableName.getTableName().getText()); // Force to keep same case as input
				table.setCatalog(qualifiedTableName.getCatalogName() != null ? qualifiedTableName.getCatalogName().getText() : null);
				table.setSchema(qualifiedTableName.getSchemaName() != null ? qualifiedTableName.getSchemaName().getText() : null);
				sumarisTableMetadata = new SumarisHibernateTableMetadata(table, this, jdbcMeta, persistentClass);
			}

			// No persistence class: load as JDBC table
			else {
				sumarisTableMetadata = new SumarisTableMetadata(qualifiedTableName, this, jdbcMeta);
			}

			// Add to cache (if not an extraction result table)
			String tableName = qualifiedTableName.getTableName().getText();
			// TODO: use include/exclude pattern, by configuration
			if (!tableName.toUpperCase().startsWith("EXT_")
				&& !tableName.toUpperCase().startsWith("AGG_"))  {
				tables.put(cacheKey, sumarisTableMetadata);
			}
		}
		return sumarisTableMetadata;
	}

	protected SumarisTableMetadata getTable(String name,
											String schema,
											String catalog,
											DatabaseMetaData jdbcMeta,
											PersistentClass persistentClass) throws HibernateException, SQLException {
		return getTable(getQualifiedTableName(catalog, schema, name), jdbcMeta, persistentClass);
	}



	private String getCacheKey(QualifiedTableName qualifiedTableName) {
		return qualifiedTableName.render().toLowerCase();
	}
}

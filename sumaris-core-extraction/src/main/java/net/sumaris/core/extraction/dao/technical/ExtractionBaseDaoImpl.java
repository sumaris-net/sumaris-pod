package net.sumaris.core.extraction.dao.technical;

/*-
 * #%L
 * Quadrige3 Core :: Client API
 * %%
 * Copyright (C) 2017 - 2018 Ifremer
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
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.Daos;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.vo.ExtractionContextVO;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.dialect.Dialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataRetrievalFailureException;

import javax.annotation.PostConstruct;
import javax.persistence.Query;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Slf4j
public abstract class ExtractionBaseDaoImpl extends HibernateDaoSupport {

    protected static final String XML_QUERY_PATH = "xmlQuery";
    protected static final String XSL_ORACLE_FILENAME = "xmlQuery/queryOracle.xsl";

    @Autowired
    protected SumarisConfiguration configuration;

    @Autowired
    protected ReferentialService referentialService;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;


    protected DatabaseType databaseType = null;

    protected String dropTableQuery;

    @PostConstruct
    public void init() {
        this.databaseType = Daos.getDatabaseType(configuration.getJdbcURL());
        this.dropTableQuery = getDialect().getDropTableString("%s");
    }

    protected Dialect getDialect() {
        return databaseMetadata.getDialect();
    }

    protected void dropTable(String tableName) {
        Preconditions.checkNotNull(tableName);

        log.debug(String.format("Dropping extraction table {%s}...", tableName));
        try {
            String sql = String.format(dropTableQuery, tableName.toUpperCase());
            getSession().createSQLQuery(sql).executeUpdate();

        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Cannot drop extraction table {%s}...", tableName), e);
        }
    }

    @SuppressWarnings("unchecked")
    protected <R> List<R> query(String query, Class<R> jdbcClass) {
        Query nativeQuery = getEntityManager().createNativeQuery(query);
        Stream<R> resultStream = (Stream<R>) nativeQuery.getResultStream().map(jdbcClass::cast);
        return resultStream.collect(Collectors.toList());
    }

    protected <R> List<R> query(String query, Function<Object[], R> rowMapper) {
        Query nativeQuery = getEntityManager().createNativeQuery(query);
        Stream<Object[]> resultStream = (Stream<Object[]>) nativeQuery.getResultStream();
        return resultStream.map(rowMapper).collect(Collectors.toList());
    }

    protected <R> List<R> query(String query, Function<Object[], R> rowMapper, int offset, int size) {
        Query nativeQuery = getEntityManager().createNativeQuery(query)
                .setFirstResult(offset)
                .setMaxResults(size);
        Stream<Object[]> resultStream = (Stream<Object[]>) nativeQuery.getResultStream();
        return resultStream.map(rowMapper).collect(Collectors.toList());
    }


    protected int queryUpdate(String query) {
        if (log.isDebugEnabled()) log.debug("execute update: " + query);
        Query nativeQuery = getEntityManager().createNativeQuery(query);
        return nativeQuery.executeUpdate();
    }

    /**
     * Create an index
     * @param tableName
     * @param indexName
     * @param columnNames
     * @param isUnique
     */
    protected void createIndex(String tableName,
                               String indexName,
                               Collection<String> columnNames,
                               boolean isUnique) {

        // Create index
        queryUpdate(String.format("CREATE %sINDEX %s on %s (%s)",
                isUnique ? "UNIQUE " : "",
                indexName,
                tableName,
                columnNames.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(","))
        ));
    }

    protected long queryCount(String query) {
        if (log.isDebugEnabled()) log.debug("aggregate: " + query);
        Query nativeQuery = getEntityManager().createNativeQuery(query);
        Object result = nativeQuery.getSingleResult();
        if (result == null)
            throw new DataRetrievalFailureException(String.format("query count result is null.\nquery: %s", query));
        if (result instanceof Number) {
            return ((Number) result).longValue();
        } else {
            throw new DataRetrievalFailureException(String.format("query count result is not a number: %s \nquery: %s", result, query));
        }
    }

    protected Integer getReferentialIdByUniqueLabel(Class<? extends IItemReferentialEntity> entityClass, String label) {
        return referentialService.findByUniqueLabel(entityClass.getSimpleName(), label).getId();
    }

    /**
     * Create a new XML Query
     * @return
     */
    protected XMLQuery createXMLQuery() {
        XMLQuery xmlQuery = applicationContext.getBean("xmlQuery", XMLQuery.class);
        if (this.databaseType == DatabaseType.oracle) {
            xmlQuery.setXSLFileName(XSL_ORACLE_FILENAME);
        }
        return xmlQuery;
    }

    protected void dropTables(@NonNull ExtractionContextVO context) {
        Preconditions.checkNotNull(context.getTableNamePrefix());

        Set<String> tableNames = ImmutableSet.<String>builder()
                .addAll(context.getTableNames())
                .addAll(context.getRawTableNames())
                .build();

        if (CollectionUtils.isEmpty(tableNames)) return;

        tableNames.stream()
                // Keep only tables with EXT_ prefix
                .filter(tableName -> tableName != null && tableName.startsWith(context.getTableNamePrefix()))
                .forEach(tableName -> {
                    try {
                        dropTable(tableName);
                        databaseMetadata.clearCache(tableName);
                    }
                    catch (SumarisTechnicalException e) {
                        log.error(e.getMessage());
                        // Continue
                    }
                });
    }

    protected <F extends ExtractionFilterVO> int cleanRow(String tableName, F filter, String sheetName) {
        Preconditions.checkNotNull(tableName);
        if (filter == null || CollectionUtils.isEmpty(filter.getCriteria())) return 0;

        SumarisTableMetadata table = databaseMetadata.getTable(tableName.toLowerCase());
        Preconditions.checkNotNull(table);

        String whereClauseContent = SumarisTableMetadatas.getInverseSqlWhereClauseContent(table, filter, sheetName, table.getAlias(), true);
        if (StringUtils.isBlank(whereClauseContent)) return 0;

        String deleteQuery = table.getDeleteQuery(whereClauseContent);
        return queryUpdate(deleteQuery);
    }

    protected <C extends ExtractionContextVO> void dropHiddenColumns(C context) {
        Map<String, Set<String>> hiddenColumns = context.getHiddenColumnNames();
        context.getTableNames().forEach(tableName -> {
            dropHiddenColumns(tableName, hiddenColumns.get(tableName));
            databaseMetadata.clearCache(tableName);
        });
    }

    protected void dropHiddenColumns(final String tableName, Set<String> hiddenColumnNames) {
        Preconditions.checkNotNull(tableName);
        if (CollectionUtils.isEmpty(hiddenColumnNames)) return; // Skip

        hiddenColumnNames.forEach(columnName -> {
            String sql = String.format("ALTER TABLE %s DROP column %s", tableName, columnName);
            queryUpdate(sql);
        });
    }
}

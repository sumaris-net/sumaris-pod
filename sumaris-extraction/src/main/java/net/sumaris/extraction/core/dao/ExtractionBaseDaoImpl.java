/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.extraction.core.dao;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.model.IEntity;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.core.vo.technical.extraction.IExtractionTypeWithTablesVO;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.vo.AggregationContextVO;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.hibernate.dialect.Dialect;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataRetrievalFailureException;

import javax.annotation.PostConstruct;
import javax.persistence.Query;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.nuiton.i18n.I18n.t;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Slf4j
public abstract class ExtractionBaseDaoImpl<C extends ExtractionContextVO, F extends ExtractionFilterVO>
    extends HibernateDaoSupport {

    protected static final String XML_QUERY_PATH = "xmlQuery";

    @Autowired
    protected ExtractionConfiguration configuration;

    @Autowired
    protected ReferentialService referentialService;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;

    @Autowired
    protected ResourceLoader resourceLoader;

    protected DatabaseType databaseType = null;

    protected String dropTableQuery;

    protected int hibernateQueryTimeout;

    @PostConstruct
    public void init() {
        this.databaseType = Daos.getDatabaseType(configuration.getJdbcURL());
        this.dropTableQuery = getDialect().getDropTableString("%s");
        this.hibernateQueryTimeout = Math.max(1, Math.round(configuration.getExtractionQueryTimeout() / 1000));
    }

    protected Dialect getDialect() {
        return databaseMetadata.getDialect();
    }

    public ExtractionResultVO read(@NonNull C context, @NonNull F filter, Page page) {

        // Get table name
        String tableName;
        if (StringUtils.isNotBlank(filter.getSheetName())) {
            tableName = context.findTableNameBySheetName(filter.getSheetName())
                .orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.extraction.noData")));
        } else {
            tableName = Beans.getStream(context.getTableNames()).findFirst()
                .orElseThrow(() -> new DataNotFoundException(I18n.t("sumaris.extraction.noData")));
        }

        // Create a filter for rows previous, with only includes/exclude columns,
        // because criterion are not need (already applied when writing temp tables)
        ExtractionFilterVO readFilter = new ExtractionFilterVO();
        readFilter.setIncludeColumnNames(filter.getIncludeColumnNames()); // Copy given include columns
        readFilter.setExcludeColumnNames(SetUtils.union(
            SetUtils.emptyIfNull(filter.getIncludeColumnNames()),
            SetUtils.emptyIfNull(context.getHiddenColumns(tableName))
        ));

        // Force distinct if there is excluded columns AND distinct is enable on the XML query
        boolean enableDistinct = filter.isDistinct() || CollectionUtils.isNotEmpty(readFilter.getExcludeColumnNames())
            && context.isDistinctEnable(tableName);
        readFilter.setDistinct(enableDistinct);

        // Replace default sort attribute
        if (page != null && IEntity.Fields.ID.equalsIgnoreCase(page.getSortBy())) {
            page.setSortBy(null);
        }

        // Get rows from exported tables
        return read(tableName, readFilter, page);
    }


    public <C extends ExtractionContextVO> void clean(C context) {
        dropTables(context);
    }

    protected ExtractionResultVO read(@NonNull String tableName, ExtractionFilterVO filter, Page page) {

        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);

        ExtractionResultVO result = new ExtractionResultVO();

        List<String> columnNames = table.getColumnNames().stream()
            // Include/exclude some columns
            .filter(createIncludeExcludePredicate(filter))
            .collect(Collectors.toList());

        // Set columns metadata
        List<ExtractionTableColumnVO> columns = toProductColumnVOs(table, columnNames, ExtractionTableColumnFetchOptions.FULL);
        result.setColumns(columns);

        String whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);

        // Count rows
        Number total = countFrom(table, whereClause);
        result.setTotal(total);

        if (page.getSize() > 0 && total.longValue() > 0) {
            List<String[]> rows = read(table, filter.isDistinct(), columnNames, whereClause, page);
            result.setRows(rows);
        }

        return result;
    }

    protected void dropTable(String tableName) {
        Preconditions.checkNotNull(tableName);

        log.debug(String.format("Dropping extraction table {%s}...", tableName));
        try {
            String sql = String.format(dropTableQuery, tableName);
            getSession().createSQLQuery(sql).executeUpdate();

        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Cannot drop extraction table {%s}...", tableName), e);
        }
    }

    protected <R> List<R> query(String query, Class<R> jdbcClass) {
        Query nativeQuery = createNativeQuery(query);
        Stream<R> resultStream = (Stream<R>) nativeQuery.getResultStream().map(jdbcClass::cast);
        return resultStream.collect(Collectors.toList());
    }

    protected <R> List<R> query(String query, Function<Object[], R> rowMapper) {
        Query nativeQuery = createNativeQuery(query);
        Stream<Object[]> resultStream = (Stream<Object[]>) nativeQuery.getResultStream();
        return resultStream.map(rowMapper).collect(Collectors.toList());
    }

    protected <R> List<R> query(String query, Function<Object[], R> rowMapper, long offset, int size) {
        Query nativeQuery = createNativeQuery(query)
                .setFirstResult((int)offset)
                .setMaxResults(size);
        Stream<Object[]> resultStream = (Stream<Object[]>) nativeQuery.getResultStream();
        return resultStream.map(rowMapper).collect(Collectors.toList());
    }


    protected int queryUpdate(String query) {
        if (log.isDebugEnabled()) log.debug("execute update: " + query);
        Query nativeQuery = createNativeQuery(query);
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
        Query nativeQuery = createNativeQuery(query);
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
        return new XMLQuery(databaseType);
    }

    protected <C extends ExtractionContextVO> void dropTables(@NonNull C context) {
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

        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
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

    /**
     * Create a native query, with the timeout for extraction (should b longer than the default timeout)
     * @param sql
     * @return
     */
    protected Query createNativeQuery(String sql) {
        Query query = getEntityManager().createNativeQuery(sql);

        // Set the query timeout (in seconds)
        query.unwrap(org.hibernate.query.Query.class)
            .setTimeout(this.hibernateQueryTimeout);

        return query;
    }

    /**
     * Enable/Disable group, depending on the DBMS
     * @param xmlQuery
     */
    protected void setDbms(XMLQuery xmlQuery) {
        if (this.databaseType != null) {
            switch (this.databaseType) {
                case hsqldb:
                case oracle:
                    xmlQuery.setDbms(this.databaseType.name());
                    break;
                case postgresql:
                    xmlQuery.setDbms("pgsql");
                    break;
            }
        }
    }

    protected String formatTableName(String tableName, long time){
        String finalTableName = String.format(tableName, time);
        if (this.databaseType != null) {
            switch (this.databaseType) {
                case hsqldb:
                case oracle:
                    break;
                case postgresql:
                    // IMPORTANT: PostgreSQL is always in lowercase. This is required to get metadata with the exact (final) name
                    finalTableName = finalTableName.toLowerCase();
                    break;
            }
        }
        return finalTableName;
    }


    protected Number countFrom(SumarisTableMetadata table, String whereClause) {

        String sql = table.getCountAllQuery();

        if (org.apache.commons.lang3.StringUtils.isNotBlank(whereClause)) {
            sql += whereClause;
        }

        Number total = (Number) getEntityManager()
            .createNativeQuery(sql)
            .getSingleResult();
        return total;
    }

    protected List<String[]> read(SumarisTableMetadata table,
                                  boolean distinct,
                                  List<String> columnNames,
                                  String whereClause,
                                  Page page) {
        String sql = table.getSelectQuery(distinct, columnNames, whereClause, page.getSortBy(), page.getSortDirection());
        int columnCount = columnNames.size();
        return query(sql, r -> toTableRowVO(r, columnCount), page.getOffset(), page.getSize());
    }

    protected List<String[]> toTableRowsVO(List<Object[]> rows, final int resultLength) {
        return rows.stream().map(r -> toTableRowVO(r, resultLength))
            .collect(Collectors.toList());
    }

    protected String[] toTableRowVO(Object[] row, int columnCount) {
        String[] result = new String[columnCount];
        if (columnCount <= 0) columnCount = row.length;
        for (int i = 0; i < columnCount; i++) {
            if (row[i] != null) {
                result[i] = row[i].toString();
            } else {
                result[i] = null;
            }
        }
        return result;
    }



    protected Predicate<String> createIncludeExcludePredicate(ExtractionFilterVO filter) {
        return createIncludeExcludePredicate(filter.getIncludeColumnNames(), filter.getExcludeColumnNames());
    }

    protected Predicate<String> createIncludeExcludePredicate(Set<String> includes, Set<String> excludes) {
        final boolean includeAll = org.springframework.util.CollectionUtils.isEmpty(includes);
        final boolean excludeNone = org.springframework.util.CollectionUtils.isEmpty(excludes);

        if (includeAll && excludeNone) {
            return (column) -> true;
        }

        return (column) -> {
            boolean isInclude = includeAll || includes.stream().anyMatch((include) -> {
                if (include.contains("*")) {
                    final String regexp = include.replaceAll("[*]", ".*");
                    return column.matches(regexp);
                }
                return column.equalsIgnoreCase(include);
            });

            boolean isExclude = !excludeNone && excludes.stream().anyMatch((exclude) -> {
                if (exclude.contains("*")) {
                    final String regexp = exclude.replaceAll("[*]", ".*");
                    return column.matches(regexp);
                }
                return column.equalsIgnoreCase(exclude);
            });

            return isInclude && !isExclude;
        };
    }

    protected long countFrom(String tableName) {
        Preconditions.checkNotNull(tableName);

        String sql = String.format("SELECT COUNT(*) from %s", tableName);
        return queryCount(sql);
    }

    protected String createSequence(String tableName) {
        Preconditions.checkNotNull(tableName);
        String upperTableName = tableName.toUpperCase();
        Preconditions.checkArgument(upperTableName.startsWith(ExtractionDao.TABLE_NAME_PREFIX)
            || upperTableName.startsWith(AggregationDao.TABLE_NAME_PREFIX));

        // Make sue sequence name length is lower than 30 characters
        if (upperTableName.length() + ExtractionDao.SEQUENCE_NAME_SUFFIX.length() > 30) {
            upperTableName = upperTableName.substring(0, 30 - ExtractionDao.SEQUENCE_NAME_SUFFIX.length());
        }
        String sequenceName = upperTableName + ExtractionDao.SEQUENCE_NAME_SUFFIX;
        try {
            String sql = getDialect().getCreateSequenceStrings(sequenceName, 1, 1)[0];
            getSession().createSQLQuery(sql).executeUpdate();
        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Cannot create sequence '%s': %s", sequenceName, e.getMessage()), e);
        }
        return sequenceName;
    }

    protected void dropSequence(String sequenceName) {
        Preconditions.checkNotNull(sequenceName);
        Preconditions.checkArgument(sequenceName.startsWith(ExtractionDao.TABLE_NAME_PREFIX)
            || sequenceName.startsWith(AggregationDao.TABLE_NAME_PREFIX));
        Preconditions.checkArgument(sequenceName.endsWith(ExtractionDao.SEQUENCE_NAME_SUFFIX));
        try {
            String sql = getDialect().getDropSequenceStrings(sequenceName)[0];
            getSession().createSQLQuery(sql).executeUpdate();
        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Cannot drop sequence '%s': %s", sequenceName, e.getMessage()), e);
        }
    }

    /**
     * Read table metadata, to get column.
     *
     * /!\ Important: column order must be the unchanged !! Otherwise getTableGroupByRows() will not work well
     *
     * @param table
     * @param columnNames
     * @param fetchOptions
     * @return
     */
    protected List<ExtractionTableColumnVO> toProductColumnVOs(SumarisTableMetadata table,
                                                               Collection<String> columnNames,
                                                               ExtractionTableColumnFetchOptions fetchOptions) {

        List<ExtractionTableColumnVO> columns = columnNames.stream()
            // Get column metadata
            .map(table::getColumnMetadata)
            .filter(Objects::nonNull)
            // Transform in VO
            .map(ExtractionProducts::toProductColumnVO)
            .collect(Collectors.toList());

        if (fetchOptions.isWithRankOrder()) {
            ExtractionTableColumnOrder.fillRankOrderByTableName(table.getName(), columns);
        }

        return columns;
    }

    public List<ExtractionTableColumnVO> getColumns(String tableName, ExtractionTableColumnFetchOptions fetchOptions) {
        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);
        return toProductColumnVOs(table, table.getColumnNames(), fetchOptions);
    }

    protected int execute(C context, XMLQuery xmlQuery) {
        String sql = xmlQuery.getSQLQueryAsString();

        // Do column names replacement (e.g. see FREE extraction)
        if (context != null) {
            sql = Daos.sqlReplaceColumnNames(sql, context.getColumnNamesMapping(), false);
        }

        return queryUpdate(sql);
    }


    protected String getQueryFullName(@NonNull C context, String queryName) {
        return getQueryFullName(context.getFormat(), context.getVersion(), queryName);
    }

    protected String getQueryFullName(@NonNull String formatLabel,
                                      @NonNull String formatVersion,
                                      @NonNull String queryName) {
        return String.format("%s/v%s/%s",
            StringUtils.underscoreToChangeCase(formatLabel),
            formatVersion.replaceAll("[.]", "_"),
            queryName);
    }

    protected XMLQuery createXMLQuery(C context, String queryName) {
        return createXMLQuery(getQueryFullName(context, queryName));
    }

    protected XMLQuery createXMLQuery(String queryName) {
        XMLQuery query = createXMLQuery();
        query.setQuery(getXMLQueryClasspathURL(queryName));
        return query;
    }

    protected URL getXMLQueryURL(C context, String queryName) {
        return getXMLQueryClasspathURL(getQueryFullName(context, queryName));
    }

    protected URL getXMLQueryClasspathURL(String queryName) {
        Resource resource = resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + XML_QUERY_PATH + "/" + queryName + ".xml");
        if (!resource.exists())
            throw new SumarisTechnicalException(t("sumaris.extraction.xmlQuery.notFound", queryName));
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new SumarisTechnicalException(e);
        }
    }

}
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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import fr.ifremer.common.xmlquery.AbstractXMLQuery;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.hibernate.HibernateDaoSupport;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.service.referential.ReferentialService;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.dao.technical.schema.SumarisTableUtils;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import net.sumaris.xml.query.XMLQuery;
import net.sumaris.xml.query.XMLQueryImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.hibernate.dialect.Dialect;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.nuiton.i18n.I18n;
import org.nuiton.version.Version;
import org.nuiton.version.Versions;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    protected static final String GROUP_BY_PARAM_NAME = "groupByColumns";

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
    protected Version databaseVersion = null;

    protected String dropTableQuery;

    protected int hibernateQueryTimeout;

    protected boolean production;

    protected boolean enableCleanup = true;

    @PostConstruct
    public void init() {
        this.production = configuration.isProduction();
        this.enableCleanup = configuration.enableExtractionCleanup();
        this.databaseType = Daos.getDatabaseType(configuration.getJdbcURL());
        this.databaseVersion = Daos.getDatabaseVersion(getDataSource());
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
        // DEBUG
        //log.warn("TODO: drop table after an extraction - uncomment code here !!");
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

        String whereClause = SumarisTableUtils.getSqlWhereClause(table, filter);

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
        return queryToStream(query, jdbcClass).collect(Collectors.toList());
    }

    protected <R> Set<R> queryToSet(String query, Class<R> jdbcClass) {
        return queryToStream(query, jdbcClass).collect(Collectors.toSet());
    }

    protected <R> Stream<R> queryToStream(String query, Class<R> jdbcClass) {
        Query nativeQuery = createNativeQuery(query);
        return (Stream<R>) nativeQuery.getResultStream().map(jdbcClass::cast);
    }

    protected <R> List<R> query(String query, Function<Object[], R> rowMapper) {
        Query nativeQuery = createNativeQuery(query);
        Stream<Object[]> resultStream = (Stream<Object[]>) nativeQuery.getResultStream();
        return resultStream.map(rowMapper).toList();
    }

    protected <R> List<R> query(String query, Function<Object[], R> rowMapper, long offset, int size) {
        Query nativeQuery = createNativeQuery(query)
            .setFirstResult((int) offset)
            .setMaxResults(size);
        Stream<Object[]> resultStream = (Stream<Object[]>) nativeQuery.getResultStream();
        return resultStream.map(rowMapper).toList();
    }


    protected int queryUpdate(String query) {
        if (log.isDebugEnabled()) log.debug("execute update: " + query);
        Query nativeQuery = createNativeQuery(query);
        return nativeQuery.executeUpdate();
    }

    /**
     * Create an index
     *
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
        if (log.isDebugEnabled()) log.debug("count: " + query);
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
     *
     * @return
     */
    protected XMLQueryImpl createXMLQuery() {
        return new XMLQueryImpl(databaseType);
    }

    protected <C extends ExtractionContextVO> void dropTables(@NonNull C context) {
        Preconditions.checkNotNull(context.getTableNamePrefix());

        Set<String> tableNames = ImmutableSet.<String>builder()
            .addAll(context.getTableNames())
            .addAll(context.getRawTableNames())
            .build();

        if (CollectionUtils.isEmpty(tableNames) || StringUtils.isBlank(context.getTableNamePrefix())) return;

        tableNames.stream()
            // Filter on tables with the 'EXT_' prefix
            .filter(tableName -> tableName != null && StringUtils.startsWithIgnoreCase(tableName, context.getTableNamePrefix()))
            .forEach(tableName -> {
                try {
                    dropTable(tableName);
                    databaseMetadata.clearCache(tableName);
                } catch (SumarisTechnicalException e) {
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

        String whereClauseContent = SumarisTableUtils.getInverseSqlWhereClauseContent(table, filter, sheetName, table.getAlias(), true);
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
     *
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
     *
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

    protected String formatTableName(String tableName, int id) {
        String finalTableName = String.format(tableName, id);

        // IMPORTANT: PostgreSQL is always in lowercase. This is required to get metadata with the exact (final) name
        if (databaseType == DatabaseType.postgresql) {
            return finalTableName.toLowerCase();
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
            Object cellValue = row[i];
            if (cellValue != null) {
                if (cellValue instanceof Integer[]) {
                    result[i] = Joiner.on(",").join((Integer[])cellValue);
                }
                else {
                    result[i] = cellValue.toString();
                }
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
     * <p>
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

        // Apply default groups
        applyDefaultGroups(xmlQuery);

        // Generate then bind group by columns
        if (StringUtils.isNotBlank(xmlQuery.getGroupByParamName())) {
            computeAndBindGroupBy(xmlQuery);
        }

        // Get the SQL
        String sql = xmlQuery.getSQLQueryAsString();

        // Do column names replacement (e.g. see FREE extraction)
        if (context != null) {
            sql = Daos.sqlReplaceColumnNames(sql, context.getColumnNamesMapping(), false);
        }

        return queryUpdate(sql);
    }

    protected int execute(C context, String sql) {

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
        XMLQueryImpl query = createXMLQuery();
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

    protected void computeAndBindGroupBy(XMLQuery xmlQuery) {
        computeAndBindGroupBy(xmlQuery, xmlQuery.getGroupByParamName());
    }

    protected void computeAndBindGroupBy(XMLQuery xmlQuery, @NonNull String paramName) {
        // Check a <groupby> tag exists with the expected parameter name
        if (!this.production) {
            List<Element> matchesGroupBy = xmlQuery.getGroupByTags(element -> {
                String content = Beans.getStream(element.getContent(Filters.text()))
                    .map(Text::getValue)
                    .collect(Collectors.joining("\n"));
                return content.contains("&" + paramName);
            });
            if (CollectionUtils.isEmpty(matchesGroupBy)) {
                log.warn("Unable to found a <groupby> with parameter '&{}'", paramName);
            }
        }

        boolean supportsSelectAliasInGroupByClause = getDialect().supportsSelectAliasInGroupByClause();

        // Get groupBy columns
        String groupByColumns = xmlQuery.streamSelectElements(e -> {
            boolean disabled = false;
                // Exclude column with different dbms
                String dbms = e.getAttributeValue(AbstractXMLQuery.ATTR_DBMS);
                if (StringUtils.isNotBlank(dbms) && !dbms.contains(this.databaseType.name())) {
                    disabled = true;
                }

                // Exclude column with group 'agg'
                boolean isAgg = xmlQuery.hasGroup(e, "agg");
                if (isAgg) {
                    // Remove the agg group, to avoid the element to be disabled
                    xmlQuery.removeGroup(e, "agg");
                    disabled = true;
                }

                // Exclude disabled columns (by group)
                return !disabled && !xmlQuery.isDisabled(e);
            })
            .map(e -> {
                // Exclude pmfm columns
                //String alias = xmlQuery.getAlias(e);
                // if (alias == null || alias.startsWith("&pmfmlabel")) return null;

                String textContent = StringUtils.trimToNull(xmlQuery.getTextContent(e, " "));

                // Exclude some specific limitation (exclude subquery)
                if (textContent != null && textContent.toUpperCase().contains(" FROM ")) {
                    return null;
                }

                if (supportsSelectAliasInGroupByClause) {

                    // Prefer using <select> content, if match the pattern 'T.<columnName>'
                    if (textContent != null && !"null".equalsIgnoreCase(textContent) && textContent.matches("([a-zA-Z0-9_]+\\.)?[a-zA-Z0-9_]+")) {
                        return textContent.trim().toLowerCase(); // FIXME: Why toLowerCase ???
                    }
                    // Or use alias if more complex column specification (e.g. '(CASE WHEN ...)' or '(SELECT ...)' )
                    String alias = xmlQuery.getAlias(e, false /* keep same case, to be able to replace parameter inside*/);
                    if (alias.startsWith("&")) {
                        alias = MapUtils.getString(xmlQuery.getSqlParameters(), alias.substring(1), alias);
                    }
                    return alias.toLowerCase(); // FIXME: Why toLowerCase ???

                } else {

                    // Use content
                    if (textContent != null && !"null".equalsIgnoreCase(textContent)) {

                        // Specific function (Oracle)
                        // ex: ROW_NUMBER() OVER (PARTITION BY O.TRIP_FK ORDER BY O.START_DATE_TIME) should return O.TRIP_FK,O.START_DATE_TIME
                        Matcher functionMatcher = Pattern
                                .compile("ROW_NUMBER\\(\\) OVER \\(PARTITION BY (([a-zA-Z0-9_]+\\.)?[a-zA-Z0-9_]+) ORDER BY (([a-zA-Z0-9_]+\\.)?[a-zA-Z0-9_]+)\\)")
                                .matcher(textContent.toUpperCase());
                        if (functionMatcher.find()) {
                            // Return the column names to group
                            return functionMatcher.group(1) + "," + functionMatcher.group(3);
                        }

                        // Find parameters
                        Matcher paramterMatcher = Pattern.compile("&[a-zA-Z0-9_]+").matcher(textContent);
                        while (paramterMatcher.find()) {
                            String match = paramterMatcher.group();
                            textContent = textContent.replaceAll(match, MapUtils.getString(xmlQuery.getSqlParameters(), match.substring(1), match));
                        }
                        return textContent.trim();
                    }

                    return null;
                }

            })
            .filter(Objects::nonNull)
            .collect(Collectors.joining(","));

        xmlQuery.bind(paramName, groupByColumns);
    }

    /**
     * Set default groups
     */
    protected void applyDefaultGroups(XMLQuery xmlQuery) {

        // Always disable injectionPoint group to avoid injection point staying on final xml query (if not used to inject pmfm)
        xmlQuery.setGroup("injectionPoint", false);

        if (databaseType == DatabaseType.oracle && databaseVersion != null) {
            boolean isOracle12 = databaseVersion.afterOrEquals(Versions.valueOf("12"));
            xmlQuery.setGroup("oracle11", !isOracle12);
            xmlQuery.setGroup("oracle12", isOracle12);
        }
        else {
            xmlQuery.setGroup("oracle11", false);
            xmlQuery.setGroup("oracle12", false);
        }
    }
}

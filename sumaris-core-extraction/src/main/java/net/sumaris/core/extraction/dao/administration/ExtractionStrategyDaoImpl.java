package net.sumaris.core.extraction.dao.administration;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.specification.administration.StratSpecification;
import net.sumaris.core.extraction.vo.ExtractionFilterCriterionVO;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.administration.ExtractionStrategyContextVO;
import net.sumaris.core.extraction.vo.administration.ExtractionStrategyFilterVO;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.net.URL;

import static org.nuiton.i18n.I18n.t;

/**
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionStrategyDao")
@Lazy
public class ExtractionStrategyDaoImpl<C extends ExtractionStrategyContextVO, F extends ExtractionFilterVO>
        extends ExtractionBaseDaoImpl
        implements ExtractionStrategyDao<C, F> {

    private static final Logger log = LoggerFactory.getLogger(ExtractionStrategyDaoImpl.class);

    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + StratSpecification.ST_SHEET_NAME + "_%s";
    private static final String SM_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + StratSpecification.SM_SHEET_NAME + "_%s";

    protected static final String XML_QUERY_PATH = "xmlQuery";

    @Autowired
    protected ResourceLoader resourceLoader;

    @Override
    public LiveFormatEnum getFormat() {
        return LiveFormatEnum.STRAT;
    }

    @Override
    public <R extends C> R execute(F filter) {
        ExtractionStrategyFilterVO strategyFilter = toStrategyFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setStrategyFilter(strategyFilter);
        context.setFilter(filter);
        context.setId(System.currentTimeMillis());
        context.setFormat(LiveFormatEnum.STRAT);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

        if (log.isInfoEnabled()) {
            StringBuilder filterInfo = new StringBuilder();
            String filterStr = filter != null ? strategyFilter.toString("\n - ") : null;
            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter:").append(filterStr);
            }
            else {
                filterInfo.append("(without filter)");
            }
            log.info(String.format("Starting extraction #%s-%s (raw data / strategies)... %s", context.getLabel(), context.getId(), filterInfo.toString()));
        }

        // Fill context table names
        fillContextTableNames(context);

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the extraction --
        try {
            // Strategy
            long rowCount = createStrategyTable(context);
            if (rowCount == 0) throw new DataNotFoundException(t("sumaris.extraction.noData"));
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // StrategyMonitoring
            rowCount = createStrategyMonitoringTable(context);
            if (rowCount == 0) return context;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            return context;
        }
        catch (PersistenceException e) {
            // If error,clean created tables first, then rethrow the exception
            clean(context);
            throw e;
        }
    }

    @Override
    public void clean(C context) {
        super.dropTables(context);
    }

    /* -- protected methods -- */

    protected Class<? extends ExtractionStrategyContextVO> getContextClass() {
        return ExtractionStrategyContextVO.class;
    }

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionStrategyContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected void fillContextTableNames(C context) {

        // Set unique table names
        context.setStrategyTableName(String.format(ST_TABLE_NAME_PATTERN, context.getId()));
        context.setStrategyMonitoringTableName(String.format(SM_TABLE_NAME_PATTERN, context.getId()));

        // Set sheetname
        context.setStrategySheetName(StratSpecification.ST_SHEET_NAME);
        context.setStrategyMonitoringSheetName(StratSpecification.SM_SHEET_NAME);
    }

    protected long createStrategyTable(C context) {
        XMLQuery xmlQuery = createStrategyQuery(context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getStrategyTableName());

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getStrategyTableName(), context.getFilter(), context.getStrategySheetName());
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getStrategyTableName(),
                    context.getStrategySheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Strategy table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getStrategyTableName());
        }
        return count;
    }

    protected XMLQuery createStrategyQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createStrategyTable");
        xmlQuery.bind("strategyTableName", context.getStrategyTableName());

        // Program labels Filter
        xmlQuery.setGroup("programLabelsFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("programLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));

        // Ids Filter
        xmlQuery.setGroup("idsFilter", CollectionUtils.isNotEmpty(context.getStrategyIds()));
        xmlQuery.bind("ids", Daos.getSqlInNumbers(context.getStrategyIds()));

        // Labels Filter
        xmlQuery.setGroup("labelsFilter", CollectionUtils.isNotEmpty(context.getStrategyLabels()));
        xmlQuery.bind("labels", Daos.getSqlInEscapedStrings(context.getStrategyLabels()));

        // Date filters
        xmlQuery.setGroup("dateFilter", context.getStartDate() != null || context.getEndDate() != null);
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(Dates.resetTime(context.getStartDate())));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(Dates.lastSecondOfTheDay(context.getEndDate())));

        // Location Filter
        xmlQuery.setGroup("locationIdsFilter", CollectionUtils.isNotEmpty(context.getLocationIds()));
        xmlQuery.bind("locationIds", Daos.getSqlInNumbers(context.getLocationIds()));

        xmlQuery.setGroup("oracle", this.databaseType == DatabaseType.oracle);
        xmlQuery.setGroup("hsqldb", this.databaseType == DatabaseType.hsqldb);
        xmlQuery.setGroup("pgsql", this.databaseType == DatabaseType.postgresql);

        return xmlQuery;
    }

    protected long createStrategyMonitoringTable(C context) {
        XMLQuery xmlQuery = createStrategyMonitoringQuery(context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getStrategyMonitoringTableName());

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getStrategyMonitoringTableName(), context.getFilter(), context.getStrategyMonitoringSheetName());
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getStrategyMonitoringTableName(),
                    context.getStrategyMonitoringSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("StrategyMonitoring table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getStrategyMonitoringTableName());
        }
        return count;
    }

    protected XMLQuery createStrategyMonitoringQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createStrategyMonitoringTable");
        xmlQuery.bind("strategyMonitoringTableName", context.getStrategyMonitoringTableName());
        xmlQuery.bind("strategyTableName", context.getStrategyTableName());

        // Bind some referential ids
        xmlQuery.bind("strategyLabelPmfmId", String.valueOf(PmfmEnum.STRATEGY_LABEL.getId()));
        xmlQuery.bind("tagIdPmfmId", String.valueOf(PmfmEnum.TAG_ID.getId()));

        return xmlQuery;
    }

    protected int execute(XMLQuery xmlQuery) {
        return queryUpdate(xmlQuery.getSQLQueryAsString());
    }

    protected long countFrom(String tableName) {
        XMLQuery xmlQuery = createXMLQuery("countFrom");
        xmlQuery.bind("tableName", tableName);
        return queryCount(xmlQuery.getSQLQueryAsString());
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getLabel());
        Preconditions.checkNotNull(context.getVersion());

        return String.format("%s/v%s/%s",
                StringUtils.underscoreToChangeCase(context.getLabel()),
                context.getVersion().replaceAll("[.]", "_"),
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

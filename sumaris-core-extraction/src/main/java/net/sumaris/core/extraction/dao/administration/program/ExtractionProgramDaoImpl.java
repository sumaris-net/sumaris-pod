package net.sumaris.core.extraction.dao.administration.program;

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
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.dao.technical.XMLQuery;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.specification.ProgSpecification;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.administration.program.ExtractionLandingFilterVO;
import net.sumaris.core.extraction.vo.administration.program.ExtractionProgramContextVO;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.service.administration.programStrategy.ProgramService;
import net.sumaris.core.service.administration.programStrategy.StrategyService;
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
@Repository("extractionProgramDao")
@Lazy
public class ExtractionProgramDaoImpl<C extends ExtractionProgramContextVO, F extends ExtractionFilterVO>
        extends ExtractionBaseDaoImpl
        implements ExtractionProgramDao<C, F> {

    private static final Logger log = LoggerFactory.getLogger(ExtractionProgramDaoImpl.class);

    private static final String PR_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ProgSpecification.PR_SHEET_NAME + "_%s";
    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ProgSpecification.ST_SHEET_NAME + "_%s";
    private static final String SM_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ProgSpecification.SM_SHEET_NAME + "_%s";

    protected static final String XML_QUERY_PATH = "xmlQuery";

    @Autowired
    protected StrategyService strategyService;

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Override
    public <R extends C> R execute(F filter) {
        ExtractionLandingFilterVO landingFilter = toTripFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setLandingFilter(landingFilter);
        context.setFilter(filter);
        context.setId(System.currentTimeMillis());
        context.setFormat(LiveFormatEnum.PROG);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

        if (log.isInfoEnabled()) {
            StringBuilder filterInfo = new StringBuilder();
            String filterStr = filter != null ? landingFilter.toString("\n - ") : null;
            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter:").append(filterStr);
            }
            else {
                filterInfo.append("(without filter)");
            }
            log.info(String.format("Starting extraction #%s-%s (raw data / programs)... %s", context.getLabel(), context.getId(), filterInfo.toString()));
        }

        // Fill context table names
        fillContextTableNames(context);

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the extraction --

        try {
            // Program
            long rowCount = createProgramTable(context);
            if (rowCount == 0) throw new DataNotFoundException(t("sumaris.extraction.noData"));
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Strategy
            rowCount = createStrategyTable(context);
            if (rowCount == 0) return context;
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
        super.clean(context);
    }

    /* -- protected methods -- */

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionProgramContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected Class<? extends ExtractionProgramContextVO> getContextClass() {
        return ExtractionProgramContextVO.class;
    }

    protected void fillContextTableNames(C context) {

        // Set unique table names
        context.setProgramTableName(String.format(PR_TABLE_NAME_PATTERN, context.getId()));
        context.setStrategyTableName(String.format(ST_TABLE_NAME_PATTERN, context.getId()));
        context.setStrategyMonitoringTableName(String.format(SM_TABLE_NAME_PATTERN, context.getId()));

        // Set sheetname
        context.setProgramSheetName(ProgSpecification.PR_SHEET_NAME);
        context.setStrategySheetName(ProgSpecification.ST_SHEET_NAME);
        context.setStrategyMonitoringSheetName(ProgSpecification.SM_SHEET_NAME);
    }

    protected long createProgramTable(C context) {

        XMLQuery xmlQuery = createProgramQuery(context);

        // aggregate insertion
        execute(xmlQuery);
        long count = countFrom(context.getProgramTableName());

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getProgramTableName(), context.getFilter(), context.getProgramSheetName());
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getProgramTableName(),
                    context.getProgramSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug(String.format("Program table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getProgramTableName());
        }
        return count;
    }

    protected XMLQuery createProgramQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createProgramTable");
        xmlQuery.bind("programTableName", context.getProgramTableName());
        xmlQuery.bind("strategyTableName", context.getStrategyTableName());

        // Ids Filter
        xmlQuery.setGroup("idsFilter", CollectionUtils.isNotEmpty(context.getProgramIds()));
        xmlQuery.bind("ids", Daos.getSqlInNumbers(context.getProgramIds()));

        // Labels Filter
        xmlQuery.setGroup("labelsFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("labels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));

        return xmlQuery;
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
        xmlQuery.bind("programTableName", context.getProgramTableName());

        // Ids Filter
        xmlQuery.setGroup("idsFilter", CollectionUtils.isNotEmpty(context.getStrategyIds()));
        xmlQuery.bind("ids", Daos.getSqlInNumbers(context.getStrategyIds()));

        // Labels Filter
        xmlQuery.setGroup("labelsFilter", CollectionUtils.isNotEmpty(context.getStrategyLabels()));
        xmlQuery.bind("labels", Daos.getSqlInEscapedStrings(context.getStrategyLabels()));

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(Dates.resetTime(context.getStartDate())));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(Dates.lastSecondOfTheDay(context.getEndDate())));

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

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(Dates.resetTime(context.getStartDate())));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(Dates.lastSecondOfTheDay(context.getEndDate())));

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

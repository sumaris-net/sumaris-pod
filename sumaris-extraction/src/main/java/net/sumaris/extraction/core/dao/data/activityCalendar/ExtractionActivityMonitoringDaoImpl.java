package net.sumaris.extraction.core.dao.data.activityCalendar;

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
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.specification.data.activityCalendar.ActivityMonitoringSpecification;
import net.sumaris.extraction.core.specification.vessel.VesselSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.data.activityCalendar.ExtractionActivityCalendarFilterVO;
import net.sumaris.extraction.core.vo.data.activityCalendar.ExtractionActivityMonitoringContextVO;
import net.sumaris.xml.query.XMLQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.PersistenceException;
import java.net.URL;
import java.text.ParseException;
import java.time.Month;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository("extractionActivityMonitoringDao")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Lazy
@RequiredArgsConstructor
@Slf4j
public class ExtractionActivityMonitoringDaoImpl<C extends ExtractionActivityMonitoringContextVO, F extends ExtractionFilterVO>
        extends ExtractionBaseDaoImpl<C, F>
        implements ExtractionActivityMonitoringDao<C, F> {

    private static final String AM_RAW_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ActivityMonitoringSpecification.AM_RAW_SHEET_NAME + "_%s";
    private static final String AM_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ActivityMonitoringSpecification.AM_SHEET_NAME + "_%s";


    private boolean enableAdagioOptimization = false;
    private String adagioSchema;

    @PostConstruct()
    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady() {
        // Read some config options
        String adagioSchema = this.configuration.getAdagioSchema();
        boolean enableAdagioOptimization = StringUtils.isNotBlank(adagioSchema)
                && this.configuration.enableAdagioOptimization()
                && this.databaseType == DatabaseType.oracle;

        // Check if there is some changes
        boolean hasChanges = !Objects.equals(this.adagioSchema, adagioSchema)
                || this.enableAdagioOptimization != enableAdagioOptimization;

        // Apply changes if need
        if (hasChanges) {
            this.adagioSchema = adagioSchema;
            this.enableAdagioOptimization = enableAdagioOptimization;

            if (this.enableAdagioOptimization) {
                log.info("Enabled extraction {}, using optimization for schema '{}'", VesselSpecification.FORMAT, this.adagioSchema);
            } else {
                log.info("Enabled extraction {} (without schema optimization)", VesselSpecification.FORMAT);
            }

        }
    }

    @Override
    public void init() {
        super.init();

        // -- for DEV only
        // set RAW_SL as a visible sheet
        if (!this.enableCleanup && !this.production) {
            LiveExtractionTypeEnum.ACTIVITY_MONITORING.setSheetNames(ActivityMonitoringSpecification.SHEET_NAMES_DEBUG);
        }
    }

    @Override
    public Set<IExtractionType<?, ?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.ACTIVITY_MONITORING);
    }

    @SneakyThrows
    @Override
    public <R extends C> R execute(F filter) {
        ExtractionActivityCalendarFilterVO activityCalendarFilter = toActivityCalendarFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setActivityCalendarFilter(activityCalendarFilter);
        context.setFilter(filter);
        context.setUpdateDate(new Date());
        context.setType(LiveExtractionTypeEnum.ACTIVITY_MONITORING);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);
        context.addTableName(ActivityMonitoringSpecification.AM_RAW_SHEET_NAME, ActivityMonitoringSpecification.AM_RAW_SHEET_NAME);
        Long startTime = null;


        if (log.isInfoEnabled()) {
            startTime = System.currentTimeMillis();

            StringBuilder filterInfo = new StringBuilder();
            String filterStr = context.getActivityCalendarFilter() != null ? context.getActivityCalendarFilter().toString() : "";

            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter: ").append(filterStr);
            } else {
                filterInfo.append("without filter");
            }
            log.info("Starting extraction {}... {}", context.getFormat(), filterInfo);
        }

        // Fill context table names
        fillContextTableNames(context);

        // -- Execute the extraction --
        try {
            // Raw monitoring table
            long rowCount = createRawMonitoringTable(context);
            if (rowCount == 0) return context;

            // Monitoring table
            rowCount = createMonitoringTable(context);
            if (rowCount == 0) return context;

            return context;
        } catch (PersistenceException | ParseException e) {
            // If error,clean created tables first, then rethrow the exception
            clean(context);
            startTime = null; // Avoid log

            throw e;
        } finally {
            if (startTime != null) {
                log.info("Extraction #{} finished in {}", context.getId(), TimeUtils.printDurationFrom(startTime));

            }
        }
    }
    /* -- protected methods -- */

    protected Class<? extends ExtractionActivityMonitoringContextVO> getContextClass() {
        return ExtractionActivityMonitoringContextVO.class;
    }

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionActivityMonitoringContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected void fillContextTableNames(C context) {

        // Set unique table names
        context.setRawMonitoringTableName(formatTableName(AM_RAW_TABLE_NAME_PATTERN, context.getId()));
        context.setMonitoringTableName(formatTableName(AM_TABLE_NAME_PATTERN, context.getId()));

        // Set sheet name
        context.setRawMonitoringSheetName(ActivityMonitoringSpecification.AM_RAW_SHEET_NAME);
        context.setMonitoringSheetName(ActivityMonitoringSpecification.AM_SHEET_NAME);
    }

    protected long createRawMonitoringTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getRawMonitoringTableName();

        // Create Raw Data Table
        XMLQuery xmlQuery = createRawMonitoringQuery(context);
        execute(context, xmlQuery);

        // Clean row using generic filter
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getRawMonitoringSheetName());
        }

        // Add as a raw table (to be able to clean it later)
        if (count == 0 || this.production || this.enableCleanup) {
            context.addRawTableName(tableName);
        }
        // Keep raw table (for DEBUG only)
        else {
            context.addTableName(tableName,
                    context.getRawMonitoringSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
        }

        return count;
    }

    protected long createMonitoringTable(C context) throws PersistenceException, ParseException {
        String tableName = context.getMonitoringTableName();

        // Create Result Table with an injection
        XMLQuery xmlQuery = createMonitoringQuery(context);
        execute(context, xmlQuery);

        // Count row
        long count = countFrom(tableName);
        if (count > 0) {
            count -= cleanRow(tableName, context.getFilter(), context.getMonitoringSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(tableName,
                    context.getMonitoringSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());
            log.debug("Monitoring table: {} rows inserted", count);
        }
        else {
            context.addRawTableName(tableName);
        }

        return count;
    }

    protected XMLQuery createRawMonitoringQuery(C context) throws PersistenceException {

        Integer year = context.getYear();
        context.setStartDate(Dates.getFirstDayOfYear(year));
        context.setEndDate(Dates.getLastSecondOfYear(year));

        XMLQuery xmlQuery = createXMLQuery(context, "createRawMonitoringTable");
        xmlQuery.bind("rawMonitoringTableName", context.getRawMonitoringTableName());

        // Pmfms
        xmlQuery.bind("surveyQualificationPmfmId", PmfmEnum.SURVEY_QUALIFICATION.getId());

        // Quality flag
        xmlQuery.bind("qualityFlagNotQualified", QualityFlagEnum.NOT_QUALIFIED.getId());

        // Date Filter
        xmlQuery.bind("startDate", Daos.getSqlToDate(context.getStartDate()));
        xmlQuery.bind("endDate", Daos.getSqlToDate(context.getEndDate()));

        // Program filter
        {
            List<String> programLabels = context.getProgramLabels();
            boolean enableFilter = CollectionUtils.isNotEmpty(programLabels);
            xmlQuery.setGroup("programFilter", enableFilter);
            if (enableFilter) xmlQuery.bind("progLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));
        }

        // Year filter
        if (year != null) {
            xmlQuery.setGroup("yearFilter", true);
            xmlQuery.bind("year", year);
        } else {
            xmlQuery.setGroup("filterYear", false);
        }

        // Registration location
        {
            List<String> registrationLocationLabels = context.getRegistrationLocationLabels();
            boolean enableFilter = CollectionUtils.isNotEmpty(registrationLocationLabels);
            xmlQuery.setGroup("registrationLocationFilter", enableFilter);
            xmlQuery.setGroup("!registrationLocationFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("registrationLocationLabels", Daos.getSqlInEscapedStrings(registrationLocationLabels));
        }

        // Base port location
        {
            List<String> basePortLocationLabels = context.getBasePortLocationLabels();
            boolean enableFilter = CollectionUtils.isNotEmpty(basePortLocationLabels);
            xmlQuery.setGroup("basePortLocationFilter", enableFilter);
            xmlQuery.setGroup("!basePortLocationFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("basePortLocationLabels", Daos.getSqlInEscapedStrings(basePortLocationLabels));
        }

        // Vessel code
        {
            List<String> vesselRegistrationCodes = context.getVesselRegistrationCodes();
            boolean enableFilter = CollectionUtils.isNotEmpty(vesselRegistrationCodes);
            xmlQuery.setGroup("vesselRegistrationCodeFilter", enableFilter);
            xmlQuery.setGroup("!vesselRegistrationCodeFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("vesselRegistrationCodes", Daos.getSqlInEscapedStrings(vesselRegistrationCodes));
        }

        // Observers
        {
            List<String> observers = context.getObservers();
            boolean enableFilter = CollectionUtils.isNotEmpty(observers);
            xmlQuery.setGroup("observersFilter", enableFilter);
            xmlQuery.setGroup("!observersFilter", !enableFilter);
            if (enableFilter) xmlQuery.bind("observers", Daos.getSqlInEscapedStrings(observers));
        }

        // Bind group by columns
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }

    protected XMLQuery createMonitoringQuery(C context) throws PersistenceException {

        XMLQuery xmlQuery = createXMLQuery(context, "createMonitoringTable");
        xmlQuery.bind("rawMonitoringTableName", context.getRawMonitoringTableName());
        xmlQuery.bind("monitoringTableName", context.getMonitoringTableName());

        URL injectionQuery = getXMLQueryURL(context, "injectionMonitoringMonthTable");

        // Create a column for each month
        for (Month month : Month.values()) {
            injectMonitoringMonthQuery(context, xmlQuery, injectionQuery, month.getValue());
        }

        // Bind group by columns
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }


    protected void injectMonitoringMonthQuery(C context,
                                              XMLQuery xmlQuery,
                                              URL injectionQuery, int month) throws PersistenceException {

        String tableAlias = (ActivityMonitoringSpecification.COLUMN_MONTH_PREFIX + month).toUpperCase();
        xmlQuery.injectQuery(injectionQuery, "%suffix%", tableAlias);
        xmlQuery.bind("month" + tableAlias, month);
    }
}


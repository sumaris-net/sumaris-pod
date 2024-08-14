package net.sumaris.extraction.core.dao.actimonit;

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
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.specification.actimonit.MonitoringSpecification;
import net.sumaris.extraction.core.specification.vessel.VesselSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.report.ExtractionMonitoringContextVO;
import net.sumaris.xml.query.XMLQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.PersistenceException;
import java.net.URL;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Repository("extractionProcessDao")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Lazy
@RequiredArgsConstructor
@Slf4j
public class ExtractionMonitoringDaoImpl<C extends ExtractionMonitoringContextVO, F extends ExtractionFilterVO>
        extends ExtractionBaseDaoImpl<C, F>
        implements ExtractionMonitoringDao<C, F> {

    private static final String AM_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + MonitoringSpecification.AM_SHEET_NAME + "_%s";


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
                log.info("Enabled actimonit indexation {}, using optimization for schema '{}'", VesselSpecification.FORMAT, this.adagioSchema);
            } else {
                log.info("Enabled actimonit indexation {} (without schema optimization)", VesselSpecification.FORMAT);
            }

        }
    }


    @Override
    public Set<IExtractionType<?, ?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.MONITORING);
    }

    @SneakyThrows
    @Override
    public <R extends C> R execute(F filter) {
        ActivityCalendarFilterVO activityCalendarFilter = toExtractionFilter(filter);

        // Init context
        R context = createNewContext();
        context.setFilter(filter);
        context.setUpdateDate(new Date());
        context.setType(LiveExtractionTypeEnum.MONITORING);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);
        context.setYear(activityCalendarFilter.getYear());
        context.addTableName(MonitoringSpecification.AM_SHEET_NAME, MonitoringSpecification.AM_SHEET_NAME);
        Long startTime = null;


        if (log.isInfoEnabled()) {
            startTime = System.currentTimeMillis();

            StringBuilder filterInfo = new StringBuilder();
            String filterStr = activityCalendarFilter != null ? activityCalendarFilter.toString() : "";

            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter: ").append(filterStr);
            } else {
                filterInfo.append("without filter");
            }
            log.info("Starting extration {}... {}", context.getFormat(), filterInfo);
        }

        // Fill context table names
        fillContextTableNames(context);

        // -- Execute the extraction --
        try {
            createTable(context);
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

    protected Class<? extends ExtractionMonitoringContextVO> getContextClass() {
        return ExtractionMonitoringContextVO.class;
    }

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionMonitoringContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected void fillContextTableNames(C context) {

        // Set sheet name
        context.setSheetName(formatTableName(AM_TABLE_NAME_PATTERN, context.getId()));
        context.setRawTableName(MonitoringSpecification.AM_SHEET_NAME + "_" + new Date().getTime());
        context.setResultTableName(MonitoringSpecification.RESULT_AM_SHEET_NAME + "_" + new Date().getTime());
    }

    protected void createTable(C context) throws PersistenceException, ParseException {
        // Create Raw Data Table
        XMLQuery xmlQuery = createMonitoringQuery(context);
        execute(context, xmlQuery);

        // Create Result Table with an injection
        XMLQuery xmlAggregation = createResultMonitoringQuery(context);
        execute(context, xmlAggregation);

    }

    protected XMLQuery createMonitoringQuery(C context) throws PersistenceException {

        context.setStartRequestDate(Dates.getFirstDayOfYear(context.getYear()));
        context.setEndRequestDate(Dates.getLastSecondOfYear(context.getYear()));

        XMLQuery xmlQuery = createXMLQuery(context, "createMonitoringTable");
        xmlQuery.bind("monitoringTableName", context.getRawTableName());
        //Date Filter
        xmlQuery.setGroup("startDateFilter", context.getStartRequestDate() != null);
        xmlQuery.bind("startDateInitial", Daos.getSqlToDate(context.getStartRequestDate()));
        xmlQuery.setGroup("endDateFilter", context.getEndRequestDate() != null);
        xmlQuery.bind("endDateInitial", Daos.getSqlToDate(context.getEndRequestDate()));

        return xmlQuery;
    }

    protected XMLQuery createResultMonitoringQuery(C context) throws PersistenceException {

        XMLQuery xmlQuery = createXMLQuery(context, "createResultMonitoring");
        URL injectionQuery = getXMLQueryURL(context, "injectionResultMonitoring");


        // Create a column for each month
        Arrays.stream(Month.values()).forEach(
                month -> {
                    try {
                        LocalDateTime lDate = LocalDateTime.of(context.getYear(), month.getValue(), 1, 0, 0, 0);


                        Date minDate = Dates.convertToDate(lDate, configuration.getTimeZone());
                        Date maxDate = Dates.convertToDate(lDate.plusMonths(1).minusSeconds(1), configuration.getTimeZone());

                        context.setMinDate(minDate);
                        context.setMaxDate(maxDate);
                        createResultMonitoringQuery(context, injectionQuery,
                                "M" + month.getValue(), xmlQuery
                        );
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                });

        return xmlQuery;
    }


    protected void createResultMonitoringQuery(C context, URL injectionQuery,
                                               String tableAlias, XMLQuery xmlQuery) throws PersistenceException, ParseException {

        String suffix = StringUtils.capitalize(StringUtils.underscoreToChangeCase(tableAlias));

        xmlQuery.injectQuery(injectionQuery, "%suffix%", suffix);
        xmlQuery.bind("columnAlias" + suffix, suffix);
        xmlQuery.bind("monitoringTableName", context.getRawTableName());
        xmlQuery.bind("resultMonitoringTableName", context.getResultTableName());
        xmlQuery.bind("minDate" + suffix, Daos.getSqlToDate(context.getMinDate()));
        xmlQuery.bind("maxDate" + suffix, Daos.getSqlToDate(context.getMaxDate()));
    }
}


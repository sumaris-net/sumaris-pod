package net.sumaris.extraction.core.dao.administration;

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.event.config.ConfigurationEvent;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.xml.query.XMLQuery;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyContextVO;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import javax.persistence.PersistenceException;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

import static org.nuiton.i18n.I18n.t;


/**
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionStrategyDao")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Lazy
@Slf4j
public class ExtractionStrategyDaoImpl<C extends ExtractionStrategyContextVO, F extends ExtractionFilterVO>
    extends ExtractionBaseDaoImpl<C, F>
    implements ExtractionStrategyDao<C, F> {

    private static final String ST_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + StratSpecification.ST_SHEET_NAME + "_%s";
    private static final String SM_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + StratSpecification.SM_SHEET_NAME + "_%s";

    private boolean enableAdagioOptimization = false;
    private String adagioSchema = null;

    @EventListener({ConfigurationReadyEvent.class, ConfigurationUpdatedEvent.class})
    public void onConfigurationReady(ConfigurationEvent event) {
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
                log.info("Enabled extraction format {}, using optimization for schema '{}'", StratSpecification.FORMAT, this.adagioSchema);
            }
            else {
                log.info("Enabled extraction format {} (without schema optimization)", StratSpecification.FORMAT);
            }

        }
    }

    @Override
    public Set<IExtractionType<?,?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.STRAT);
    }

    @Override
    public <R extends C> R execute(F filter) {
        ExtractionStrategyFilterVO strategyFilter = toStrategyFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setStrategyFilter(strategyFilter);
        context.setFilter(filter);
        context.setUpdateDate(new Date());
        context.setType(LiveExtractionTypeEnum.STRAT);
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
            log.info("Starting extraction {} (raw data / strategies)... {}", context.getFormat(), filterInfo);
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

    /* -- protected methods -- */

    protected Class<? extends ExtractionStrategyContextVO> getContextClass() {
        return ExtractionStrategyContextVO.class;
    }

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionStrategyContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
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

        // execute insertion
        execute(context, xmlQuery);
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
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(Dates.resetTime(context.getStartDate())));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(Dates.lastSecondOfTheDay(context.getEndDate())));

        // Location Filter
        xmlQuery.setGroup("locationIdsFilter", CollectionUtils.isNotEmpty(context.getLocationIds()));
        xmlQuery.bind("locationIds", Daos.getSqlInNumbers(context.getLocationIds()));

        return xmlQuery;
    }

    protected long createStrategyMonitoringTable(C context) {
        XMLQuery xmlQuery = createStrategyMonitoringQuery(context);

        // aggregate insertion
        execute(context, xmlQuery);
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

        xmlQuery.setGroup("adagio", this.enableAdagioOptimization);
        xmlQuery.setGroup("!adagio", !this.enableAdagioOptimization);
        xmlQuery.bind("adagioSchema", this.adagioSchema);

        return xmlQuery;
    }

}

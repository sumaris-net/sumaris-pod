package net.sumaris.extraction.core.dao.vessel;

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
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.specification.vessel.VesselSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyContextVO;
import net.sumaris.extraction.core.vo.vessel.ExtractionVesselContextVO;
import net.sumaris.xml.query.XMLQuery;
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
@Repository("extractionVesselDao")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Lazy
@Slf4j
public class ExtractionVesselDaoImpl<C extends ExtractionVesselContextVO, F extends ExtractionFilterVO>
    extends ExtractionBaseDaoImpl<C, F>
    implements ExtractionVesselDao<C, F> {

    private static final String VE_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + VesselSpecification.VE_SHEET_NAME + "_%s";

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
    public Set<IExtractionType<?, ?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.VESSEL);
    }

    @Override
    public <R extends C> R execute(F filter) {
        VesselFilterVO vesselFilter = toVesselFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setVesselFilter(vesselFilter);
        context.setFilter(filter);
        context.setUpdateDate(new Date());
        context.setType(LiveExtractionTypeEnum.VESSEL);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

        if (log.isInfoEnabled()) {
            StringBuilder filterInfo = new StringBuilder();
            String filterStr = filter != null ? vesselFilter.toString("\n - ") : null;
            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter: ").append(filterStr);
            }
            else {
                filterInfo.append("(without filter)");
            }
            log.info("Starting extraction {}... {}", context.getFormat(), filterInfo);
        }

        // Fill context table names
        fillContextTableNames(context);

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the extraction --
        try {
            // Vessel
            long rowCount = createVesselTable(context);
            if (rowCount == 0) throw new DataNotFoundException(t("sumaris.extraction.noData"));
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

    protected Class<? extends ExtractionVesselContextVO> getContextClass() {
        return ExtractionVesselContextVO.class;
    }

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionVesselContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected void fillContextTableNames(C context) {

        // Set unique table names
        context.setVesselTableName(String.format(VE_TABLE_NAME_PATTERN, context.getId()));

        // Set sheet name
        context.setVesselSheetName(VesselSpecification.VE_SHEET_NAME);
    }

    protected long createVesselTable(C context) {
        XMLQuery xmlQuery = createVesselQuery(context);

        // execute insertion
        long count = execute(context, xmlQuery);

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getVesselTableName(), context.getFilter(), context.getVesselSheetName());
        }

        // Add result table to context
        if (count > 0) {
            context.addTableName(context.getVesselTableName(),
                context.getVesselSheetName(),
                xmlQuery.getHiddenColumnNames(),
                xmlQuery.hasDistinctOption());
            log.debug(String.format("Vessel table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getVesselTableName());
        }
        return count;
    }

    protected XMLQuery createVesselQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createVesselTable");
        xmlQuery.bind("vesselTableName", context.getVesselTableName());

        // Program labels filter
        xmlQuery.setGroup("programLabelsFilter", CollectionUtils.isNotEmpty(context.getProgramLabels()));
        xmlQuery.bind("programLabels", Daos.getSqlInEscapedStrings(context.getProgramLabels()));

        // Ids filter
        xmlQuery.setGroup("idsFilter", CollectionUtils.isNotEmpty(context.getVesselIds()));
        xmlQuery.bind("ids", Daos.getSqlInNumbers(context.getVesselIds()));

        // Status ids filter
        xmlQuery.setGroup("statusIdsFilter", CollectionUtils.isNotEmpty(context.getStatusIds()));
        xmlQuery.bind("statusIds", Daos.getSqlInNumbers(context.getStatusIds()));

        // Date filters
        xmlQuery.setGroup("startDateFilter", context.getStartDate() != null);
        xmlQuery.bind("startDate", Daos.getSqlToDate(Dates.resetTime(context.getStartDate())));
        xmlQuery.setGroup("endDateFilter", context.getEndDate() != null);
        xmlQuery.bind("endDate", Daos.getSqlToDate(Dates.lastSecondOfTheDay(context.getEndDate())));

        // Registration location filter
        xmlQuery.setGroup("registrationLocationIdsFilter", CollectionUtils.isNotEmpty(context.getRegistrationLocationIds()));
        xmlQuery.bind("registrationLocationIds", Daos.getSqlInNumbers(context.getRegistrationLocationIds()));

        // Base port location filter
        xmlQuery.setGroup("basePortLocationIdsFilter", CollectionUtils.isNotEmpty(context.getBasePortLocationIds()));
        xmlQuery.bind("basePortLocationIds", Daos.getSqlInNumbers(context.getBasePortLocationIds()));

        // Generated group by
        xmlQuery.bindGroupBy(GROUP_BY_PARAM_NAME);

        return xmlQuery;
    }


}

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
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.event.config.ConfigurationReadyEvent;
import net.sumaris.core.event.config.ConfigurationUpdatedEvent;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ActimonitFilterVO;
import net.sumaris.extraction.core.dao.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.specification.actimonit.ActiMonitSpecification;
import net.sumaris.extraction.core.specification.vessel.VesselSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.report.ExtractionActimonitContextVO;
import net.sumaris.xml.query.XMLQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.persistence.PersistenceException;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

import static org.nuiton.i18n.I18n.t;

@Repository("extractionProcessDao")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
@Lazy
@Slf4j
public class ExtractionActimonitDaoImpl<C extends ExtractionActimonitContextVO, F extends ExtractionFilterVO>
        extends ExtractionBaseDaoImpl<C, F>
        implements ExtractionActimonitDao<C, F> {


    private static final String AM_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + ActiMonitSpecification.AM_SHEET_NAME + "_%s";

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
        return ImmutableSet.of(LiveExtractionTypeEnum.ACTIMONIT);
    }

    @Override
    public <R extends C> R execute(F filter) {
        ActimonitFilterVO actimonitFilter = toActimonitFilterVO(filter);

        // Init context
        R context = createNewContext();
        context.setActimonitFilter(actimonitFilter);
        context.setFilter(filter);
        context.setUpdateDate(new Date());
        context.setType(LiveExtractionTypeEnum.ACTIMONIT);
        context.setTableNamePrefix(TABLE_NAME_PREFIX);

        if (log.isInfoEnabled()) {
            StringBuilder filterInfo = new StringBuilder();
            String filterStr = filter != null ? actimonitFilter.toString() : "";

            if (StringUtils.isNotBlank(filterStr)) {
                filterInfo.append("with filter: ").append(filterStr);
            } else {
                filterInfo.append("without filter");
            }
            log.info("Starting extration {}... {}", context.getFormat(), filterInfo);
        }

        // Fill context table names
        fillContextTableNames(context);

        // Expected sheet name
        String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;

        // -- Execute the extraction --
        try {
            // Actimonit
            long rowCount = createActimonitTable(context);
            if (rowCount == 0) {
                throw new DataNotFoundException(t("sumaris.extraction.noData"));
            }
            if (sheetName != null && context.hasSheet(sheetName)) {
                return context;
            }

            return context;
        } catch (PersistenceException e) {
            // If error,clean created tables first, then rethrow the exception
            clean(context);
            throw e;
        }
    }
    /* -- protected methods -- */

    protected Class<? extends ExtractionActimonitContextVO> getContextClass() {
        return ExtractionActimonitContextVO.class;
    }

    protected <R extends C> R createNewContext() {
        Class<? extends ExtractionActimonitContextVO> contextClass = getContextClass();
        Preconditions.checkNotNull(contextClass);

        try {
            return (R) contextClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new SumarisTechnicalException("Could not create an instance of context class " + contextClass.getName());
        }
    }

    protected void fillContextTableNames(C context) {

        // Set unique table names
        context.setProcessTableName(String.format(AM_TABLE_NAME_PATTERN, context.getId()));

        // Set sheet name
        context.setProcessSheetName(ActiMonitSpecification.AM_SHEET_NAME);
    }

    protected long createActimonitTable(C context) throws PersistenceException {
        String tableName = context.getProcessTableName();
        XMLQuery xmlQuery = createActimonitQuery(context);

        // execution insertion
        execute(context, xmlQuery);
        long count = countFrom(tableName);

        // CLean row using generic filter
        if (count > 0) {
            context.addTableName(tableName,
                    context.getProcessSheetName(),
                    xmlQuery.getHiddenColumnNames(),
                    xmlQuery.hasDistinctOption());

            log.debug(String.format("Actimonit table: %s rows inserted", count));
        } else {
            context.addRawTableName(tableName);
        }
        return count;
    }

    protected XMLQuery createActimonitQuery(C context) throws PersistenceException {
        XMLQuery xmlQuery = createXMLQuery(context, "injectionMonitoringTable");
        xmlQuery.bind("surveyQualification", "SURVEY_QUALIFICATION");
        return xmlQuery;
    }
}


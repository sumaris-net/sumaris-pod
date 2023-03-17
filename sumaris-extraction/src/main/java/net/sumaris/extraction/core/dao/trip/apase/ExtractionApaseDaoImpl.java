package net.sumaris.extraction.core.dao.trip.apase;

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.DatabaseType;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.administration.programStrategy.ProgramPropertyEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.referential.PmfmValueType;
import net.sumaris.extraction.core.dao.technical.xml.XMLQuery;
import net.sumaris.extraction.core.dao.trip.pmfm.ExtractionPmfmTripDaoImpl;
import net.sumaris.extraction.core.specification.data.trip.ApaseSpecification;
import net.sumaris.extraction.core.specification.data.trip.PmfmTripSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import net.sumaris.extraction.core.vo.trip.apase.ExtractionApaseContextVO;
import net.sumaris.extraction.core.vo.trip.pmfm.ExtractionPmfmTripContextVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import javax.persistence.PersistenceException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extraction for the APASE project (trawl selectivity)
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 */
@Repository("extractionApaseDao")
@Lazy
@Slf4j
public class ExtractionApaseDaoImpl<C extends ExtractionApaseContextVO, F extends ExtractionFilterVO>
        extends ExtractionPmfmTripDaoImpl<C, F>
        implements ApaseSpecification {

    private static final String PG_TABLE_NAME_PATTERN = TABLE_NAME_PREFIX + PG_SHEET_NAME + "_%s";

    public Set<IExtractionType> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.APASE);
    }


    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        try {
            // Stop here, if sheet already filled
            String sheetName = filter != null && filter.isPreview() ? filter.getSheetName() : null;
            if (sheetName != null && context.hasSheet(sheetName)) return context;

            // Physical gear
            long rowCount = createGearTable(context);

            return context;
        }
        finally {
            context.setType(LiveExtractionTypeEnum.APASE);
        }
    }

    @Override
    protected void fillContextTableNames(C context) {
        super.fillContextTableNames(context);

        // Set unique table names
        context.setGearTableName(formatTableName(PG_TABLE_NAME_PATTERN, context.getId()));

        // Set sheet names
        context.setGearSheetName(ApaseSpecification.PG_SHEET_NAME);
    }

    protected long createGearTable(C context) {

        XMLQuery xmlQuery = createGearQuery(context);

        // aggregate insertion
        execute(context, xmlQuery);
        long count = countFrom(context.getGearTableName());

        // Clean row using generic filter
        if (count > 0) {
            count -= cleanRow(context.getGearTableName(), context.getFilter(), context.getGearSheetName());
        }

        if (count > 0) {
            // Add result table to context
            context.addTableName(context.getGearTableName(),
                context.getGearSheetName(),
                xmlQuery.getHiddenColumnNames(),
                xmlQuery.hasDistinctOption());
            log.debug(String.format("Gear table: %s rows inserted", count));
        }
        else {
            context.addRawTableName(context.getGearTableName());
        }


        return count;
    }

    /* -- protected methods -- */

    @Override
    protected boolean isSamplesEnabled(C context) {
        return false;
    }

    protected boolean enableParentOperation(C context) {
        return false;
    }

    protected boolean enableSpeciesListTaxon(C context) {
        return false;
    }

    @Override
    protected boolean enableStationGearPmfms(C context) {
        return false;
    }

    @Override
    protected Class<? extends ExtractionApaseContextVO> getContextClass() {
        return ExtractionApaseContextVO.class;
    }

    @Override
    protected XMLQuery createStationQuery(C context) {
        XMLQuery query = super.createStationQuery(context);

        // Disable gear info (will be on PG table)
        query.setGroup("gearComments", false);

        return query;
    }

    protected XMLQuery createGearQuery(C context) {
        XMLQuery xmlQuery = createXMLQuery(context, "createGearTable");
        xmlQuery.bind("tripTableName", context.getTripTableName());
        xmlQuery.bind("gearTableName", context.getGearTableName());

        // Special case for COST format:
        // - Hide GearType (not in the COST format)
        xmlQuery.setGroup("gearType", false);

        // Bind groupBy columns
        Set<String> excludedColumns = ImmutableSet.of(RdbSpecification.COLUMN_GEAR_TYPE);
        Set<String> groupByColumns = xmlQuery.getColumnNames(e -> !xmlQuery.hasGroup(e, "agg")
            && !excludedColumns.contains(xmlQuery.getAttributeValue(e, "alias", true)));
        xmlQuery.bind("groupByColumns", String.join(",", groupByColumns));

        // Inject physical gear pmfms
        injectPmfmColumns(context, xmlQuery,
            getTripProgramLabels(context),
            AcquisitionLevelEnum.PHYSICAL_GEAR,
            // Excluded Pmfms (already exists as RDB format columns)
            PmfmEnum.SMALLER_MESH_GAUGE_MM.getId(),
            PmfmEnum.SELECTIVITY_DEVICE.getId()
        );

        return xmlQuery;
    }


    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionStationTable":
            case "createGearTable":
            case "injectionPhysicalGearPmfm":
                return getQueryFullName(ApaseSpecification.FORMAT, ApaseSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }

}

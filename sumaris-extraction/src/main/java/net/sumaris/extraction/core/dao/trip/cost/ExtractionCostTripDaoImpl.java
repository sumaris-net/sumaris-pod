package net.sumaris.extraction.core.dao.trip.cost;

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
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.extraction.core.dao.trip.rdb.ExtractionRdbTripDaoImpl;
import net.sumaris.extraction.core.specification.data.trip.CostSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.xml.query.XMLQuery;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionCostTripDao")
@Lazy
@Slf4j
public class ExtractionCostTripDaoImpl<C extends ExtractionRdbTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionRdbTripDaoImpl<C, F>
        implements CostSpecification {

    public ExtractionCostTripDaoImpl() {
        super();
        this.enableRecordTypeColumn = false; // No RECORD_TYPE in this format - Issue #416
    }

    @Override
    public void init() {
        super.init();

        // -- for DEV only
        // set RAW_SL as a visible sheet
        if (!this.enableCleanup && !this.production) {
            LiveExtractionTypeEnum.COST.setSheetNames(CostSpecification.SHEET_NAMES_DEBUG);
        }
    }

    @Override
    public Set<IExtractionType<?,?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.COST);
    }

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);

        context.setType(LiveExtractionTypeEnum.COST);

        return context;
    }

    /* -- protected methods -- */

    protected XMLQuery createStationQuery(C context) {

        XMLQuery xmlQuery = super.createStationQuery(context);

        // Special case for COST format:
        // - Hide GearType (not in the COST format)
        xmlQuery.setGroup("gearType", false);

        return xmlQuery;
    }

    @Override
    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(context);

        // Special case for COST format:

        // - Hide sex columns, then replace by a new columns
        xmlQuery.setGroup("sex", false);
        xmlQuery.setGroup("lengthClass", false);
        xmlQuery.setGroup("numberAtLength", false);

        xmlQuery.injectQuery(getXMLQueryURL(context, "injectionSpeciesLengthTable"));

        return xmlQuery;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionSpeciesLengthTable":
                return getQueryFullName(CostSpecification.FORMAT, CostSpecification.VERSION_1_4, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }
}

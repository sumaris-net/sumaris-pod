package net.sumaris.extraction.core.dao.trip.acost;

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
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevelEnum;
import net.sumaris.core.model.referential.pmfm.PmfmEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.vo.data.batch.DenormalizedBatchOptions;
import net.sumaris.extraction.core.dao.trip.pmfm.ExtractionPmfmTripDaoImpl;
import net.sumaris.extraction.core.specification.data.trip.AcostSpecification;
import net.sumaris.extraction.core.specification.data.trip.ApaseSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import net.sumaris.extraction.core.vo.trip.apase.ExtractionApaseContextVO;
import net.sumaris.extraction.core.vo.trip.pmfm.ExtractionPmfmTripContextVO;
import net.sumaris.xml.query.XMLQuery;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Extraction for the APASE project (trawl selectivity)
 * @author Ludovic Pecquot <ludovic.pecquot@e-is.pro>
 */
@Repository("extractionAcostDao")
@Lazy
@Slf4j
public class ExtractionAcostDaoImpl<C extends ExtractionPmfmTripContextVO, F extends ExtractionFilterVO>
        extends ExtractionPmfmTripDaoImpl<C, F>
        implements AcostSpecification {

    @Override
    public void init() {
        super.init();

        // -- for DEV only
        // set RAW_SL as a visible sheet
        if (!this.enableCleanup && !this.production) {
            LiveExtractionTypeEnum.ACOST.setSheetNames(AcostSpecification.SHEET_NAMES_DEBUG);
        }
    }

    public Set<IExtractionType<?, ?>> getManagedTypes() {
        return ImmutableSet.of(LiveExtractionTypeEnum.ACOST);
    }

    @Override
    public <R extends C> R execute(F filter) {
        R context = super.execute(filter);
        context.setType(LiveExtractionTypeEnum.ACOST);
        return context;
    }

    /* -- protected methods -- */

    @Override
    protected XMLQuery createSpeciesLengthQuery(C context) {
        XMLQuery xmlQuery = super.createSpeciesLengthQuery(context);

        // Insert parent pmfms (from SL table)
        injectPmfmColumns(context, xmlQuery,
                getTripProgramLabels(context),
                AcquisitionLevelEnum.SORTING_BATCH,
                "injectionSpeciesLength_parentPmfm",
                "afterSexInjection",
                // Excluded PMFM (already exists as RDB format columns)
                getSpeciesListExcludedPmfmIds().toArray(new Integer[0])
        );

        return xmlQuery;
    }

    protected String getQueryFullName(C context, String queryName) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(context.getVersion());

        switch (queryName) {
            case "injectionSpeciesLength_parentPmfm":
                return getQueryFullName(AcostSpecification.FORMAT, AcostSpecification.VERSION_1_0, queryName);
            default:
                return super.getQueryFullName(context, queryName);
        }
    }
}

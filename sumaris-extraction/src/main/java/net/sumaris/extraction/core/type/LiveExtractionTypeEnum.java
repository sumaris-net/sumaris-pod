/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.extraction.core.type;

import lombok.NonNull;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.specification.data.trip.*;
import net.sumaris.extraction.core.specification.vessel.VesselSpecification;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum LiveExtractionTypeEnum implements IExtractionType {

    // Administration
    STRAT (StratSpecification.FORMAT, StratSpecification.SHEET_NAMES, StratSpecification.VERSION_1_0),

    // VESSEL
    VESSEL (VesselSpecification.FORMAT, VesselSpecification.SHEET_NAMES, VesselSpecification.VERSION_1_0),

    // Trip
    RDB (RdbSpecification.FORMAT, RdbSpecification.SHEET_NAMES, RdbSpecification.VERSION_1_3),
    COST (CostSpecification.FORMAT, CostSpecification.SHEET_NAMES, CostSpecification.VERSION_1_4),
    FREE1 (Free1Specification.FORMAT, Free1Specification.SHEET_NAMES, Free1Specification.VERSION_1),
    FREE2 (Free2Specification.FORMAT, Free2Specification.SHEET_NAMES, Free2Specification.VERSION_1_9),
    SURVIVAL_TEST (SurvivalTestSpecification.FORMAT, SurvivalTestSpecification.SHEET_NAMES, SurvivalTestSpecification.VERSION_1_0),
    PMFM_TRIP(PmfmTripSpecification.FORMAT, PmfmTripSpecification.SHEET_NAMES, PmfmTripSpecification.VERSION_1_0),
    RJB_TRIP(RjbTripSpecification.FORMAT, RjbTripSpecification.SHEET_NAMES, RjbTripSpecification.VERSION_1_0),
    APASE(ApaseSpecification.FORMAT, ApaseSpecification.SHEET_NAMES, ApaseSpecification.VERSION_1_0)
    ;

    private Integer id;
    private String format;
    private String[] sheetNames;
    private String version;

    LiveExtractionTypeEnum(String format, String[] sheetNames, String version) {
        this.format = format;
        this.version = version;
        this.sheetNames = sheetNames;
        // A negative integer, to be different from a product identifier
        this.id =  -1 * Math.abs(new HashCodeBuilder()
            .append(format)
            .append(sheetNames)
            .append(version)
            .build());
    }

    public Integer getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return getFormat();
    }

    public String getFormat() {
        return format;
    }

    public String getVersion() {
        return version;
    }

    public String[] getSheetNames() {
        return sheetNames;
    }

    @Override
    public Boolean getIsSpatial() {
        return false;
    }

    public static LiveExtractionTypeEnum valueOf(@NonNull String format, @Nullable String version) {
        return findFirst(format, version)
                .orElseThrow(() -> new SumarisTechnicalException(String.format("Unknown live extraction format '%s'", format)));
    }

    public static Optional<LiveExtractionTypeEnum> findFirst(@NonNull IExtractionType type) {
        return findFirst(type.getFormat(), type.getVersion());
    }

    public static Optional<LiveExtractionTypeEnum> findFirst(@NonNull String format, @Nullable String version) {
        return Arrays.stream(values())
                .filter(e -> e.getFormat().equalsIgnoreCase(format)
                        && (version == null || e.getVersion().equalsIgnoreCase(version)))
                .findFirst();
    }

}

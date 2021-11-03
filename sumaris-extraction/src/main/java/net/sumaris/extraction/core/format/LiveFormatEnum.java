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

package net.sumaris.extraction.core.format;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.specification.data.trip.*;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum LiveFormatEnum implements IExtractionFormat {

    // Administration
    STRAT (StratSpecification.FORMAT, StratSpecification.SHEET_NAMES, StratSpecification.VERSION_1_0),

    // Trip
    RDB (RdbSpecification.FORMAT, RdbSpecification.SHEET_NAMES, RdbSpecification.VERSION_1_3),
    COST (CostSpecification.FORMAT, CostSpecification.SHEET_NAMES, CostSpecification.VERSION_1_4),
    FREE1 (Free1Specification.FORMAT, Free1Specification.SHEET_NAMES, Free1Specification.VERSION_1),
    FREE2 (Free2Specification.FORMAT, Free2Specification.SHEET_NAMES, Free2Specification.VERSION_1_9),
    SURVIVAL_TEST (SurvivalTestSpecification.FORMAT, SurvivalTestSpecification.SHEET_NAMES, SurvivalTestSpecification.VERSION_1_0),
    PMFM_TRIP(PmfmTripSpecification.FORMAT, PmfmTripSpecification.SHEET_NAMES, PmfmTripSpecification.VERSION_1_0),
    RJB_TRIP(RjbTripSpecification.FORMAT, RjbTripSpecification.SHEET_NAMES, RjbTripSpecification.VERSION_1_0)
    ;

    private String label;
    private String[] sheetNames;
    private String version;

    LiveFormatEnum(String label, String[] sheetNames, String version) {
        this.label = label;
        this.sheetNames = sheetNames;
        this.version = version;
    }

    public String getLabel() {
        return label;
    }

    public String getVersion() {
        return version;
    }

    public String[] getSheetNames() {
        return sheetNames;
    }

    @Override
    public final ExtractionCategoryEnum getCategory() {
        return ExtractionCategoryEnum.LIVE;
    }

    public static LiveFormatEnum valueOf(@NonNull String label, @Nullable String version) {
        return findFirst(label, version)
                .orElseGet(() -> {
                    if (label.contains(LiveFormatEnum.RDB.name())) {
                        return LiveFormatEnum.RDB;
                    }
                    throw new SumarisTechnicalException(String.format("Unknown live format '%s'", label));
                });
    }

    public static Optional<LiveFormatEnum> findFirst(@NonNull IExtractionFormat format) {
        Preconditions.checkArgument(format.getCategory() == ExtractionCategoryEnum.LIVE, "Invalid format. Must be a LIVE format");
        return findFirst(format.getLabel(), format.getVersion());
    }

    public static Optional<LiveFormatEnum> findFirst(@NonNull String label, @Nullable String version) {
        final String rawFormatLabel = IExtractionFormat.getRawFormatLabel(label);

        return Arrays.stream(values())
                .filter(e -> e.getLabel().equalsIgnoreCase(rawFormatLabel)
                        && (version == null || e.getVersion().equalsIgnoreCase(version)))
                .findFirst();
    }

}

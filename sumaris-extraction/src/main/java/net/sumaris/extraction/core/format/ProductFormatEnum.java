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
import net.sumaris.extraction.core.specification.data.trip.*;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ProductFormatEnum implements IExtractionFormat {

    // Raw data product
    RDB (RdbSpecification.FORMAT, RdbSpecification.SHEET_NAMES, RdbSpecification.VERSION_1_3),

    // Aggregation product
    AGG_RDB (AggRdbSpecification.FORMAT, AggRdbSpecification.SHEET_NAMES, AggRdbSpecification.VERSION_1_3),
    AGG_COST (AggCostSpecification.FORMAT, AggCostSpecification.SHEET_NAMES, AggCostSpecification.VERSION_1_4),
    AGG_FREE (AggFree1Specification.FORMAT, AggFree1Specification.SHEET_NAMES, AggFree1Specification.VERSION_1),
    AGG_SURVIVAL_TEST (AggSurvivalTestSpecification.FORMAT, AggSurvivalTestSpecification.SHEET_NAMES, AggSurvivalTestSpecification.VERSION_1_0),
    AGG_PMFM_TRIP(AggPmfmTripSpecification.FORMAT, AggPmfmTripSpecification.SHEET_NAMES, AggPmfmTripSpecification.VERSION_1_0),
    AGG_RJB_TRIP (AggRjbTripSpecification.FORMAT, AggRjbTripSpecification.SHEET_NAMES, AggRjbTripSpecification.VERSION_1_0)
    ;

    private String label;
    private String[] sheetNames;
    private String version;

    ProductFormatEnum(String label, String[] sheetNames, String version) {
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
        return ExtractionCategoryEnum.PRODUCT;
    }

    public static ProductFormatEnum valueOf(@NonNull String label, @Nullable String version) {
        return findFirst(label, version)
                .orElseGet(() -> {
                    final String rawFormatLabel = IExtractionFormat.getRawFormatLabel(label);
                    if (rawFormatLabel.equalsIgnoreCase(RdbSpecification.FORMAT)) {
                        return ProductFormatEnum.RDB;
                    }
                    throw new SumarisTechnicalException(String.format("Unknown product format found, for label '%s'", label));
                });
    }

    public static Optional<ProductFormatEnum> findFirst(@NonNull IExtractionFormat format) {
        Preconditions.checkArgument(format.getCategory() == ExtractionCategoryEnum.PRODUCT, "Invalid format. Must be a PRODUCT format");
        return findFirst(format.getLabel(), format.getVersion());
    }

    public static Optional<ProductFormatEnum> findFirst(@NonNull String label, String version) {
        final String rawFormatLabel = IExtractionFormat.getRawFormatLabel(label);

        return Arrays.stream(values())
                .filter(e -> e.getLabel().equalsIgnoreCase(rawFormatLabel)
                        && (version == null || e.getVersion().equalsIgnoreCase(version)))
                .findFirst();
    }

}

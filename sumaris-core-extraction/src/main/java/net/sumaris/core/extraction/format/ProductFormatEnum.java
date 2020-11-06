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

package net.sumaris.core.extraction.format;

import net.sumaris.core.extraction.format.specification.AggRdbSpecification;
import net.sumaris.core.extraction.format.specification.AggSurvivalTestSpecification;
import net.sumaris.core.extraction.format.specification.RdbSpecification;
import net.sumaris.core.extraction.vo.ExtractionCategoryEnum;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ProductFormatEnum implements IExtractionFormat {

    // Raw data product
    RDB(RdbSpecification.FORMAT, RdbSpecification.SHEET_NAMES, RdbSpecification.VERSION_1_3),

    // Aggregation product
    AGG_RDB(AggRdbSpecification.FORMAT, AggRdbSpecification.SHEET_NAMES, AggRdbSpecification.VERSION_1_3),
    AGG_SURVIVAL_TEST(AggSurvivalTestSpecification.FORMAT, AggSurvivalTestSpecification.SHEET_NAMES, AggSurvivalTestSpecification.VERSION_1_0)
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
    public ExtractionCategoryEnum getCategory() {
        return ExtractionCategoryEnum.PRODUCT;
    }

    public static Optional<ProductFormatEnum> fromString(@Nullable String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.toUpperCase()));
        }
        catch(IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

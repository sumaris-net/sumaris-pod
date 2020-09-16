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

package net.sumaris.core.extraction.utils;

import net.sumaris.core.extraction.specification.*;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionRawFormatEnum {

    RDB(RdbSpecification.FORMAT, RdbSpecification.SHEET_NAMES, RdbSpecification.VERSION_1_3),
    COST (CostSpecification.FORMAT, CostSpecification.SHEET_NAMES, CostSpecification.VERSION_1_4),
    FREE1 (Free1Specification.FORMAT, Free1Specification.SHEET_NAMES, Free1Specification.VERSION_1),
    FREE2 (Free2Specification.FORMAT, Free2Specification.SHEET_NAMES, Free2Specification.VERSION_1_9),
    SURVIVAL_TEST(SurvivalTestSpecification.FORMAT, SurvivalTestSpecification.SHEET_NAMES, SurvivalTestSpecification.VERSION_1_0)
    ;

    private String label;
    private String[] sheetNames;
    private String version;

    ExtractionRawFormatEnum(String label, String[] sheetNames, String version) {
        this.label = label;
        this.sheetNames = sheetNames;
        this.version = version;
    }
    ExtractionRawFormatEnum(String label, String[] sheetNames) {
        this(label, sheetNames, null);
    }
    ExtractionRawFormatEnum(String label) {
        this(label, null, null);
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

    public static Optional<ExtractionRawFormatEnum> fromString(@Nullable String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.toUpperCase()));
        }
        catch(IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

package net.sumaris.core.extraction.vo;

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

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionRawFormatEnum {

    RDB("RDB", new String[]{"TR", "HH", "SL", "HL"}),
    COST ("COST", new String[]{"TR", "HH", "SL", "HL"}),
    FREE1 ("FREE", new String[]{"TR", "HH", "SL", "HL"}, "1"),
    FREE2 ("FREE", new String[]{"TR", "HH", "SL", "HL"}, "2"),
    SURVIVAL_TEST("SURVIVAL_TEST", new String[]{"TR", "HH", "SL", "HL", "ST", "RL"})
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

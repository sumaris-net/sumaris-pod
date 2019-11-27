package net.sumaris.core.extraction.vo.trip.rdb;

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

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public enum ExtractionRdbTripVersion {

    // Version v1.3 P01 RDB data exchange format
    VERSION_1_3("1.3")
    ;

    private String label;

    ExtractionRdbTripVersion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

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

package net.sumaris.extraction.core.specification.data.trip;

public interface AggPmfmTripSpecification extends AggRdbSpecification {

    String FORMAT = AggSpecification.FORMAT_PREFIX + PmfmTripSpecification.FORMAT;
    String VERSION_1_0 = PmfmTripSpecification.VERSION_1_0;
    String ST_SHEET_NAME = "ST"; // Sample
    String RL_SHEET_NAME = "RL"; // Release

    String COLUMN_INDIVIDUAL_COUNT = "individual_count";

    String[] SHEET_NAMES = {HH_SHEET_NAME, SL_SHEET_NAME, HL_SHEET_NAME, ST_SHEET_NAME, RL_SHEET_NAME};
}

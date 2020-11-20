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

package net.sumaris.core.extraction.format.specification;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface Free2Specification  {
    String FORMAT = "FREE2";
    String VERSION_1_9 = "1.9";

    String TRIP_SHEET_NAME = "MAREE";
    String STATION_SHEET_NAME = "OPERATION_PECHE";
    String GEAR_SHEET_NAME = "ENGINS";
    String STRATEGY_SHEET_NAME = "STRATEGIE";
    String DETAIL_SHEET_NAME = "DETAIL";
    String SPECIES_LIST_SHEET_NAME = "CAPTURES";
    String SPECIES_LENGTH_SHEET_NAME = "MESURES";

    String[] SHEET_NAMES = {TRIP_SHEET_NAME, STATION_SHEET_NAME, GEAR_SHEET_NAME,
            STRATEGY_SHEET_NAME, DETAIL_SHEET_NAME,
            SPECIES_LIST_SHEET_NAME, SPECIES_LENGTH_SHEET_NAME};

}
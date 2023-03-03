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

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface RdbSpecification {

    String FORMAT = "RDB";

    String VERSION_1_3 = "1.3";

    String TR_SHEET_NAME = "TR";
    String HH_SHEET_NAME = "HH";
    String SL_SHEET_NAME = "SL";
    String HL_SHEET_NAME = "HL";
    String CA_SHEET_NAME = "CA";
    String CL_SHEET_NAME = "CL";
    String CE_SHEET_NAME = "CE";

    String COLUMN_RECORD_TYPE = "record_type";
    String COLUMN_PROJECT = "project";
    String COLUMN_YEAR = "year";
    String COLUMN_VESSEL_IDENTIFIER = "vessel_identifier";
    String COLUMN_TRIP_CODE = "trip_code";
    String COLUMN_STATION_NUMBER = "station_number";

    String COLUMN_SAMPLING_METHOD = "sampling_method";

    String COLUMN_GEAR_TYPE = "gear_type";
    String COLUMN_MESH_SIZE = "mesh_size";
    String MAIN_FISHING_DEPTH = "main_fishing_depth";
    String MAIN_WATER_DEPTH = "main_water_depth";

    String COLUMN_DATE = "date";
    String COLUMN_TIME = "time";
    String COLUMN_FISHING_TIME = "fishing_time";
    String COLUMN_INDIVIDUAL_SEX = "individual_sex";
    String COLUMN_WEIGHT = "weight";

    String[] SHEET_NAMES = {
            TR_SHEET_NAME,
            HH_SHEET_NAME,
            SL_SHEET_NAME,
            HL_SHEET_NAME,
            // CA_SHEET_NAME
            CL_SHEET_NAME
    };
}

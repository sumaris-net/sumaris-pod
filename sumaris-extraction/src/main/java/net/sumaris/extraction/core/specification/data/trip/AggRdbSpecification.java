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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import lombok.NonNull;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;

import java.util.Map;
import java.util.Set;

public interface AggRdbSpecification {

    String FORMAT = AggSpecification.FORMAT_PREFIX + RdbSpecification.FORMAT;
    String VERSION_1_3 = RdbSpecification.VERSION_1_3;

    String HH_SHEET_NAME = "HH";
    String SL_SHEET_NAME = "SL";
    String HL_SHEET_NAME = "HL";
    String CA_SHEET_NAME = "CA";
    String CL_SHEET_NAME = "CL";

    // Time columns
    String COLUMN_YEAR  = ProductRdbStation.COLUMN_YEAR;
    String COLUMN_QUARTER = "quarter";
    String COLUMN_MONTH = "month";

    // Space columns
    String COLUMN_AREA = ProductRdbStation.COLUMN_AREA;
    String COLUMN_STATISTICAL_RECTANGLE = ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE;
    String COLUMN_SQUARE = "square";
    String COLUMN_SUB_POLYGON = ProductRdbStation.COLUMN_SUB_POLYGON;

    // Agg columns
    String COLUMN_FISHING_TIME = "fishing_time";
    String COLUMN_STATION_COUNT = "station_count";
    String COLUMN_TRIP_COUNT_BY_FISHING_TIME = "trip_count_by_fishing_time";
    String COLUMN_TRIP_COUNT_BY_STATION = "trip_count_by_station";

    String COLUMN_WEIGHT = "weight";
    String COLUMN_NUMBER_AT_LENGTH = "number_at_length";

    // Other columns
    String COLUMN_NATIONAL_METIER = ProductRdbStation.COLUMN_NATIONAL_METIER;
    String COLUMN_EU_METIER_LEVEL5 = ProductRdbStation.COLUMN_EU_METIER_LEVEL5;
    String COLUMN_EU_METIER_LEVEL6 = ProductRdbStation.COLUMN_EU_METIER_LEVEL6;
    String COLUMN_GEAR_TYPE = ProductRdbStation.COLUMN_GEAR_TYPE;

    // Link columns
    String COLUMN_ID = "id";
    String COLUMN_SAMPLE_IDS = "sample_ids";

    Set<String> SPATIAL_COLUMNS = ImmutableSortedSet.of(COLUMN_AREA, COLUMN_STATISTICAL_RECTANGLE, COLUMN_SUB_POLYGON, COLUMN_SQUARE);
    Set<String> TIME_COLUMNS = ImmutableSortedSet.of(COLUMN_YEAR, COLUMN_QUARTER, COLUMN_MONTH);

    Map<String, Set<String>> AGG_COLUMNS_BY_SHEETNAME = ImmutableMap.<String, Set<String>>builder()
            .put(HH_SHEET_NAME, ImmutableSortedSet.of(
                    COLUMN_FISHING_TIME,
                    COLUMN_STATION_COUNT,
                    COLUMN_TRIP_COUNT_BY_FISHING_TIME,
                    COLUMN_TRIP_COUNT_BY_STATION
            ))
            .put(SL_SHEET_NAME, ImmutableSortedSet.of(
                    COLUMN_WEIGHT
            ))
            .put(HL_SHEET_NAME, ImmutableSortedSet.of(
                    COLUMN_NUMBER_AT_LENGTH
            ))
            .build();


    ImmutableMap<String, String> COLUMN_ALIAS = ImmutableMap.<String, String>builder()
            .put("rect", COLUMN_STATISTICAL_RECTANGLE)
            .put("subPolygon", COLUMN_SUB_POLYGON)
            .build();

    String[] SHEET_NAMES = {HH_SHEET_NAME, SL_SHEET_NAME, HL_SHEET_NAME, CL_SHEET_NAME};

    /**
     * Resolve alias, and convert to lower case
     * @param columnNameOrAlias
     * @return
     */
    static String resolveColumnName(@NonNull String columnNameOrAlias) {
        columnNameOrAlias = columnNameOrAlias.toLowerCase();
        return COLUMN_ALIAS.getOrDefault(columnNameOrAlias, columnNameOrAlias);
    }
}

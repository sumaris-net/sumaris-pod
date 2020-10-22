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

package net.sumaris.core.extraction.specification;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;

import java.util.List;
import java.util.Map;

public interface AggRdbSpecification {

    String FORMAT = RdbSpecification.FORMAT;
    String VERSION_1_3 = RdbSpecification.VERSION_1_3;

    String TR_SHEET_NAME = "HH"; // TODO to remove
    String HH_SHEET_NAME = "HH";
    String SL_SHEET_NAME = "SL";
    String HL_SHEET_NAME = "HL";

    // Time columns
    String COLUMN_YEAR  = ProductRdbStation.COLUMN_YEAR;
    String COLUMN_QUARTER = "quarter";
    String COLUMN_MONTH = "month";

    // Space columns
    String COLUMN_AREA = ProductRdbStation.COLUMN_AREA;
    String COLUMN_STATISTICAL_RECTANGLE = ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE;
    String COLUMN_SQUARE = "square";

    // Agg columns
    String COLUMN_TRIP_COUNT = "trip_count";
    String COLUMN_STATION_COUNT = "station_count";

    // Link columns
    String COLUMN_ID = "id";
    String COLUMN_SAMPLE_IDS = "sample_ids";

    List<String> SPACE_STRATA = ImmutableList.of("area", "rect", "square");
    List<String> TIME_STRATA = ImmutableList.of("year", "quarter", "month");
    Map<String, List<String>> AGG_STRATA_BY_SHEETNAME = ImmutableMap.<String, List<String>>builder()
            .put(HH_SHEET_NAME, ImmutableList.of(COLUMN_TRIP_COUNT, COLUMN_STATION_COUNT))
            .build();


    ImmutableMap<String, String> COLUMN_ALIAS = ImmutableMap.<String, String>builder()
            .put("rect", COLUMN_STATISTICAL_RECTANGLE)
            .build();
}

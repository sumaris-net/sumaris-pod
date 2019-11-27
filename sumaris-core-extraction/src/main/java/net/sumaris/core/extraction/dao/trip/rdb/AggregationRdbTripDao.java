package net.sumaris.core.extraction.dao.trip.rdb;

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

import com.google.common.collect.ImmutableMap;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;

import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface AggregationRdbTripDao<C extends AggregationRdbTripContextVO, F extends ExtractionFilterVO, S extends AggregationStrataVO> {

    String RDB_FORMAT = StringUtils.underscoreToChangeCase(ExtractionRawFormatEnum.RDB.name());

    String HH_SHEET_NAME = ExtractionRdbTripDao.HH_SHEET_NAME;
    String SL_SHEET_NAME = ExtractionRdbTripDao.SL_SHEET_NAME;
    String HL_SHEET_NAME = ExtractionRdbTripDao.HL_SHEET_NAME;
    String CA_SHEET_NAME = ExtractionRdbTripDao.CA_SHEET_NAME;

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

    ImmutableMap<String, String> COLUMN_ALIAS = ImmutableMap.<String, String>builder()
        .put("rect", COLUMN_STATISTICAL_RECTANGLE)
            .build();

    <R extends C> R aggregate(ExtractionProductVO source, F filter);

    AggregationResultVO read(String tableName, F filter, S strata, int offset, int size, String sortAttribute, SortDirection sortDirection);
}

package net.sumaris.extraction.core.dao.technical.table;

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
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.core.model.technical.extraction.rdb.*;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.Map;

/**
 * Determine a default order for columns
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public final class ExtractionTableColumnOrder {

    public static String key(String format, String sheetName) {
        return (format + "-" + sheetName).toUpperCase();
    }

    public final static Map<String, String[]> COLUMNS_BY_SHEET = ImmutableMap.<String, String[]>builder()
            .put(key(RdbSpecification.FORMAT, RdbSpecification.TR_SHEET_NAME), new String[]{
                    RdbSpecification.COLUMN_RECORD_TYPE,
                    ProductRdbTrip.COLUMN_SAMPLING_TYPE,
                    ProductRdbTrip.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductRdbTrip.COLUMN_LANDING_COUNTRY,
                    ProductRdbTrip.COLUMN_YEAR,
                    ProductRdbTrip.COLUMN_PROJECT,
                    ProductRdbTrip.COLUMN_TRIP_CODE,
                    ProductRdbTrip.COLUMN_VESSEL_LENGTH,
                    ProductRdbTrip.COLUMN_VESSEL_POWER,
                    ProductRdbTrip.COLUMN_VESSEL_SIZE,
                    ProductRdbTrip.COLUMN_VESSEL_TYPE,
                    ProductRdbTrip.COLUMN_HARBOUR,
                    ProductRdbTrip.COLUMN_NUMBER_OF_SETS,
                    ProductRdbTrip.COLUMN_DAYS_AT_SEA,
                    ProductRdbTrip.COLUMN_VESSEL_IDENTIFIER,
                    ProductRdbTrip.COLUMN_SAMPLING_COUNTRY,
                    ProductRdbTrip.COLUMN_SAMPLING_METHOD
            })
            .put(key(RdbSpecification.FORMAT, RdbSpecification.HH_SHEET_NAME), new String[]{
                    RdbSpecification.COLUMN_RECORD_TYPE,
                    ProductRdbStation.COLUMN_SAMPLING_TYPE,
                    ProductRdbStation.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductRdbStation.COLUMN_LANDING_COUNTRY,
                    ProductRdbStation.COLUMN_YEAR,
                    ProductRdbStation.COLUMN_PROJECT,
                    ProductRdbStation.COLUMN_TRIP_CODE,
                    ProductRdbStation.COLUMN_STATION_NUMBER,
                    ProductRdbStation.COLUMN_FISHING_VALIDITY,
                    ProductRdbStation.COLUMN_AGGREGATION_LEVEL,
                    ProductRdbStation.COLUMN_CATCH_REGISTRATION,
                    ProductRdbStation.COLUMN_SPECIES_REGISTRATION,
                    ProductRdbStation.COLUMN_DATE,
                    ProductRdbStation.COLUMN_TIME,
                    ProductRdbStation.COLUMN_FISHING_TIME,
                    ProductRdbStation.COLUMN_POS_START_LAT,
                    ProductRdbStation.COLUMN_POS_START_LON,
                    ProductRdbStation.COLUMN_POS_END_LAT,
                    ProductRdbStation.COLUMN_POS_END_LON,
                    ProductRdbStation.COLUMN_AREA,
                    ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE,
                    ProductRdbStation.COLUMN_SUB_POLYGON,
                    ProductRdbStation.COLUMN_MAIN_FISHING_DEPTH,
                    ProductRdbStation.COLUMN_MAIN_WATER_DEPTH,
                    ProductRdbStation.COLUMN_NATIONAL_METIER,
                    ProductRdbStation.COLUMN_EU_METIER_LEVEL5,
                    ProductRdbStation.COLUMN_EU_METIER_LEVEL6,
                    ProductRdbStation.COLUMN_GEAR_TYPE,
                    ProductRdbStation.COLUMN_MESH_SIZE,
                    ProductRdbStation.COLUMN_SELECTION_DEVICE,
                    ProductRdbStation.COLUMN_MESH_SIZE_SELECTION_DEVICE
            })
            .put(key(RdbSpecification.FORMAT, RdbSpecification.SL_SHEET_NAME), new String[]{
                    RdbSpecification.COLUMN_RECORD_TYPE,
                    ProductRdbSpeciesList.COLUMN_SAMPLING_TYPE,
                    ProductRdbSpeciesList.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductRdbSpeciesList.COLUMN_LANDING_COUNTRY,
                    ProductRdbSpeciesList.COLUMN_YEAR,
                    ProductRdbSpeciesList.COLUMN_PROJECT,
                    ProductRdbSpeciesList.COLUMN_TRIP_CODE,
                    ProductRdbSpeciesList.COLUMN_STATION_NUMBER,
                    ProductRdbSpeciesList.COLUMN_SPECIES,
                    ProductRdbSpeciesList.COLUMN_CATCH_CATEGORY,
                    ProductRdbSpeciesList.COLUMN_LANDING_CATEGORY,
                    ProductRdbSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    ProductRdbSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    ProductRdbSpeciesList.COLUMN_SUBSAMPLING_CATEGORY,
                    ProductRdbSpeciesList.COLUMN_SEX,
                    ProductRdbSpeciesList.COLUMN_WEIGHT,
                    ProductRdbSpeciesList.COLUMN_SUBSAMPLE_WEIGHT,
                    ProductRdbSpeciesList.COLUMN_LENGTH_CODE
            })
            .put(key(RdbSpecification.FORMAT, RdbSpecification.HL_SHEET_NAME), new String[]{
                    RdbSpecification.COLUMN_RECORD_TYPE,
                    ProductRdbSpeciesLength.COLUMN_SAMPLING_TYPE,
                    ProductRdbSpeciesLength.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductRdbSpeciesLength.COLUMN_LANDING_COUNTRY,
                    ProductRdbSpeciesLength.COLUMN_YEAR,
                    ProductRdbSpeciesLength.COLUMN_PROJECT,
                    ProductRdbSpeciesLength.COLUMN_TRIP_CODE,
                    ProductRdbSpeciesLength.COLUMN_STATION_NUMBER,
                    ProductRdbSpeciesLength.COLUMN_SPECIES,
                    ProductRdbSpeciesLength.COLUMN_CATCH_CATEGORY,
                    ProductRdbSpeciesLength.COLUMN_LANDING_CATEGORY,
                    ProductRdbSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    ProductRdbSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    ProductRdbSpeciesLength.COLUMN_SUBSAMPLING_CATEGORY,
                    ProductRdbSpeciesLength.COLUMN_SEX,
                    ProductRdbSpeciesLength.COLUMN_INDIVIDUAL_SEX,
                    ProductRdbSpeciesLength.COLUMN_LENGTH_CLASS,
                    ProductRdbSpeciesLength.COLUMN_NUMBER_AT_LENGTH
            })
            .put(key(RdbSpecification.FORMAT, RdbSpecification.CA_SHEET_NAME), new String[]{
                    RdbSpecification.COLUMN_RECORD_TYPE
                    // TODO
            })
            .put(key(RdbSpecification.FORMAT, RdbSpecification.CL_SHEET_NAME), new String[]{
                    RdbSpecification.COLUMN_RECORD_TYPE,
                    ProductRdbLanding.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductRdbLanding.COLUMN_LANDING_COUNTRY,
                    ProductRdbLanding.COLUMN_YEAR,
                    ProductRdbLanding.COLUMN_QUARTER,
                    ProductRdbLanding.COLUMN_MONTH,
                    ProductRdbLanding.COLUMN_AREA,
                    ProductRdbLanding.COLUMN_STATISTICAL_RECTANGLE,
                    ProductRdbLanding.COLUMN_SUB_POLYGON,
                    ProductRdbLanding.COLUMN_SPECIES,
                    ProductRdbLanding.COLUMN_LANDING_CATEGORY,
                    ProductRdbLanding.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    ProductRdbLanding.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    ProductRdbLanding.COLUMN_NATIONAL_METIER,
                    ProductRdbLanding.COLUMN_EU_METIER_LEVEL5,
                    ProductRdbLanding.COLUMN_EU_METIER_LEVEL6,
                    ProductRdbLanding.COLUMN_HARBOUR,
                    ProductRdbLanding.COLUMN_VESSEL_LENGTH_CATEGORY,
                    ProductRdbLanding.COLUMN_UNALLOCATED_CATCH_WEIGHT,
                    ProductRdbLanding.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT,
                    ProductRdbLanding.COLUMN_OFFICIAL_LANDINGS_WEIGHT,
                    ProductRdbLanding.COLUMN_LANDINGS_MULTIPLIER,
                    ProductRdbLanding.COLUMN_OFFICIAL_LANDINGS_VALUE
            })
            .put(key(RdbSpecification.FORMAT, RdbSpecification.CE_SHEET_NAME), new String[]{
                    RdbSpecification.COLUMN_RECORD_TYPE
                    // TODO
            })
            .build();

    public final static Map<String, String[]> COLUMNS_BY_TABLE = ImmutableMap.<String, String[]>builder()
            .put(ProductRdbTrip.TABLE.name(), COLUMNS_BY_SHEET.get(key(RdbSpecification.FORMAT, RdbSpecification.TR_SHEET_NAME)))
            .put(ProductRdbStation.TABLE.name(), COLUMNS_BY_SHEET.get(key(RdbSpecification.FORMAT, RdbSpecification.HH_SHEET_NAME)))
            .put(ProductRdbSpeciesList.TABLE.name(), COLUMNS_BY_SHEET.get(key(RdbSpecification.FORMAT, RdbSpecification.SL_SHEET_NAME)))
            .put(ProductRdbSpeciesLength.TABLE.name(), COLUMNS_BY_SHEET.get(key(RdbSpecification.FORMAT, RdbSpecification.HL_SHEET_NAME)))
            .put(ProductRdbLanding.TABLE.name(), COLUMNS_BY_SHEET.get(key(RdbSpecification.FORMAT, RdbSpecification.CL_SHEET_NAME)))
            .build();

    /**
     * Compute rankOrder, starting at 0
     * @param tableName
     * @param columns
     */
    public static void fillRankOrderByTableName(String tableName, List<ExtractionTableColumnVO> columns) {

        // Workaround need on SUMARiS DB
        if (tableName.toUpperCase().startsWith("P01_ICES")) {
            tableName = tableName.toUpperCase().replaceAll("P01_ICES_", "P01_RDB_");
        }

        String[] orderedColumnNames = ExtractionTableColumnOrder.COLUMNS_BY_TABLE.get(tableName);

        // Important: skip if not known: MUST NOT fill any rankOrder if table not known!
        // This is required to let service apply another rankOrder later (e.g. from format and label)
        if (ArrayUtils.isEmpty(orderedColumnNames)) return;

        fillRankOrderByTableName(orderedColumnNames, columns);
    }

    /**
     * Compute rankOrder, starting at 0
     * @param tableName
     * @param columns
     */
    public static void fillRankOrderByTypeAndSheet(IExtractionType format, String sheetName, List<ExtractionTableColumnVO> columns) {
        fillRankOrderByFormatAndSheet(format.getLabel(), sheetName, columns);
    }

    /**
     * Compute rankOrder, starting at 0
     * @param tableName
     * @param columns
     */
    public static void fillRankOrderByFormatAndSheet(String format, String sheetName, List<ExtractionTableColumnVO> columns) {
        fillRankOrderByTableName(ExtractionTableColumnOrder.COLUMNS_BY_SHEET.get(key(format, sheetName)), columns);
    }

    /* -- internal methods -- */

    protected static void fillRankOrderByTableName(String[] orderedColumnNames, List<ExtractionTableColumnVO> columns) {
        int maxRankOrder = 0;
        if (ArrayUtils.isNotEmpty(orderedColumnNames)) {

            // Set rank Order of well known columns
            maxRankOrder = columns.stream().mapToInt(column -> {
                int rankOrder = ArrayUtils.indexOf(orderedColumnNames, column.getName().toLowerCase());
                if (rankOrder != -1) {
                    column.setRankOrder(rankOrder + 1);
                }
                return rankOrder + 1;
            })
                    .max()
                    .orElse(0);
        }

        // Set rankOrder of all other columns (e.g. new columns)
        MutableInt rankOrder = new MutableInt(maxRankOrder);
        columns.stream()
                .filter(c -> c.getRankOrder() == null)
                .forEach(c -> {
                    rankOrder.increment();
                    c.setRankOrder(rankOrder.getValue());
                });
    }
}

package net.sumaris.core.extraction.dao.technical.table;

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
import net.sumaris.core.model.technical.extraction.rdb.*;

import java.util.Map;

/**
 * Determine a default order for columns
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public final class ExtractionTableColumnOrder {


    public static Map<String, String[]> COLUMNS_BY_TABLE = ImmutableMap.<String, String[]>builder()
            .put(ProductRdbTrip.TABLE.name(), new String[]{
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
            .put(ProductRdbStation.TABLE.name(), new String[]{
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
            .put(ProductRdbSpeciesList.TABLE.name(), new String[]{
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
                    ProductRdbSpeciesList.COLUMN_SUBSAMPLING_WEIGHT,
                    ProductRdbSpeciesList.COLUMN_LENGTH_CODE
            })
            .put(ProductRdbSpeciesLength.TABLE.name(), new String[]{
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
            // TODO CA
            .put(ProductRdbLandingStatistics.TABLE.name(), new String[]{
                    ProductRdbLandingStatistics.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductRdbLandingStatistics.COLUMN_LANDING_COUNTRY,
                    ProductRdbLandingStatistics.COLUMN_YEAR,
                    ProductRdbLandingStatistics.COLUMN_QUARTER,
                    ProductRdbLandingStatistics.COLUMN_MONTH,
                    ProductRdbLandingStatistics.COLUMN_AREA,
                    ProductRdbLandingStatistics.COLUMN_STATISTICAL_RECTANGLE,
                    ProductRdbLandingStatistics.COLUMN_SUB_POLYGON,
                    ProductRdbLandingStatistics.COLUMN_SPECIES,
                    ProductRdbLandingStatistics.COLUMN_LANDING_CATEGORY,
                    ProductRdbLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    ProductRdbLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    ProductRdbLandingStatistics.COLUMN_NATIONAL_METIER,
                    ProductRdbLandingStatistics.COLUMN_EU_METIER_LEVEL5,
                    ProductRdbLandingStatistics.COLUMN_EU_METIER_LEVEL6,
                    ProductRdbLandingStatistics.COLUMN_HARBOUR,
                    ProductRdbLandingStatistics.COLUMN_VESSEL_LENGTH_CATEGORY,
                    ProductRdbLandingStatistics.COLUMN_UNALLOCATED_CATCH_WEIGHT,
                    ProductRdbLandingStatistics.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT,
                    ProductRdbLandingStatistics.COLUMN_OFFICIAL_LANDINGS_WEIGHT,
                    ProductRdbLandingStatistics.COLUMN_LANDINGS_MULTIPLIER,
                    ProductRdbLandingStatistics.COLUMN_OFFICIAL_LANDINGS_VALUE
            })
            // TODO CE
            .build();
}

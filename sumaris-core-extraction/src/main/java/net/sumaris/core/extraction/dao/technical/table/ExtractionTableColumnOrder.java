package net.sumaris.core.extraction.dao.technical.table;

import com.google.common.collect.ImmutableMap;
import net.sumaris.core.model.product.ices.*;

import java.util.Map;

/**
 * Determine a default order for columns
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public final class ExtractionTableColumnOrder {


    public static Map<String, String[]> COLUMNS_BY_TABLE = ImmutableMap.<String, String[]>builder()
            .put(ProductIcesTrip.TABLE.name(), new String[]{
                    ProductIcesTrip.COLUMN_SAMPLING_TYPE,
                    ProductIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductIcesTrip.COLUMN_LANDING_COUNTRY,
                    ProductIcesTrip.COLUMN_YEAR,
                    ProductIcesTrip.COLUMN_PROJECT,
                    ProductIcesTrip.COLUMN_TRIP_CODE,
                    ProductIcesTrip.COLUMN_VESSEL_LENGTH,
                    ProductIcesTrip.COLUMN_VESSEL_POWER,
                    ProductIcesTrip.COLUMN_VESSEL_SIZE,
                    ProductIcesTrip.COLUMN_VESSEL_TYPE,
                    ProductIcesTrip.COLUMN_HARBOUR,
                    ProductIcesTrip.COLUMN_OPERATION_COUNT,
                    ProductIcesTrip.COLUMN_DAYS_AT_SEA,
                    ProductIcesTrip.COLUMN_VESSEL_IDENTIFIER,
                    ProductIcesTrip.COLUMN_SAMPLING_COUNTRY,
                    ProductIcesTrip.COLUMN_SAMPLING_METHOD
            })
            .put(ProductIcesStation.TABLE.name(), new String[]{
                    ProductIcesStation.COLUMN_SAMPLING_TYPE,
                    ProductIcesStation.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductIcesStation.COLUMN_LANDING_COUNTRY,
                    ProductIcesStation.COLUMN_YEAR,
                    ProductIcesStation.COLUMN_PROJECT,
                    ProductIcesStation.COLUMN_TRIP_CODE,
                    ProductIcesStation.COLUMN_STATION_NUMBER,
                    ProductIcesStation.COLUMN_FISHING_VALIDITY,
                    ProductIcesStation.COLUMN_AGGREGATION_LEVEL,
                    ProductIcesStation.COLUMN_CATCH_REGISTRATION,
                    ProductIcesStation.COLUMN_SPECIES_REGISTRATION,
                    ProductIcesStation.COLUMN_DATE,
                    ProductIcesStation.COLUMN_TIME,
                    ProductIcesStation.COLUMN_FISHING_DURATION,
                    ProductIcesStation.COLUMN_POS_START_LAT,
                    ProductIcesStation.COLUMN_POS_START_LON,
                    ProductIcesStation.COLUMN_POS_END_LAT,
                    ProductIcesStation.COLUMN_POS_END_LON,
                    ProductIcesStation.COLUMN_AREA,
                    ProductIcesStation.COLUMN_STATISTICAL_RECTANGLE,
                    ProductIcesStation.COLUMN_SUB_POLYGON,
                    ProductIcesStation.COLUMN_MAIN_FISHING_DEPTH,
                    ProductIcesStation.COLUMN_MAIN_WATER_DEPTH,
                    ProductIcesStation.COLUMN_NATIONAL_METIER,
                    ProductIcesStation.COLUMN_EU_METIER_LEVEL5,
                    ProductIcesStation.COLUMN_EU_METIER_LEVEL6,
                    ProductIcesStation.COLUMN_GEAR_TYPE,
                    ProductIcesStation.COLUMN_MESH_SIZE,
                    ProductIcesStation.COLUMN_SELECTION_DEVICE,
                    ProductIcesStation.COLUMN_MESH_SIZE_SELECTION_DEVICE
            })
            .put(ProductIcesSpeciesList.TABLE.name(), new String[]{
                    ProductIcesSpeciesList.COLUMN_SAMPLING_TYPE,
                    ProductIcesSpeciesList.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductIcesSpeciesList.COLUMN_LANDING_COUNTRY,
                    ProductIcesSpeciesList.COLUMN_YEAR,
                    ProductIcesSpeciesList.COLUMN_PROJECT,
                    ProductIcesSpeciesList.COLUMN_TRIP_CODE,
                    ProductIcesSpeciesList.COLUMN_STATION_NUMBER,
                    ProductIcesSpeciesList.COLUMN_SPECIES,
                    ProductIcesSpeciesList.COLUMN_SEX,
                    ProductIcesSpeciesList.COLUMN_CATCH_CATEGORY,
                    ProductIcesSpeciesList.COLUMN_LANDING_CATEGORY,
                    ProductIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    ProductIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    ProductIcesSpeciesList.COLUMN_SUBSAMPLING_CATEGORY,
                    ProductIcesSpeciesList.COLUMN_WEIGHT,
                    ProductIcesSpeciesList.COLUMN_SUBSAMPLING_WEIGHT,
                    ProductIcesSpeciesList.COLUMN_LENGTH_CODE
            })
            .put(ProductIcesSpeciesLength.TABLE.name(), new String[]{
                    ProductIcesSpeciesLength.COLUMN_SAMPLING_TYPE,
                    ProductIcesSpeciesLength.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductIcesSpeciesLength.COLUMN_LANDING_COUNTRY,
                    ProductIcesSpeciesLength.COLUMN_YEAR,
                    ProductIcesSpeciesLength.COLUMN_PROJECT,
                    ProductIcesSpeciesLength.COLUMN_TRIP_CODE,
                    ProductIcesSpeciesLength.COLUMN_STATION_NUMBER,
                    ProductIcesSpeciesLength.COLUMN_SPECIES,
                    ProductIcesSpeciesLength.COLUMN_CATCH_CATEGORY,
                    ProductIcesSpeciesLength.COLUMN_LANDING_CATEGORY,
                    ProductIcesSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    ProductIcesSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    ProductIcesSpeciesLength.COLUMN_SUBSAMPLING_CATEGORY,
                    ProductIcesSpeciesLength.COLUMN_SEX,
                    ProductIcesSpeciesLength.COLUMN_INDIVIDUAL_SEX,
                    ProductIcesSpeciesLength.COLUMN_LENGTH_CLASS,
                    ProductIcesSpeciesLength.COLUMN_NUMBER_AT_LENGTH
            })
            // TODO CA
            .put(ProductIcesLandingStatistics.TABLE.name(), new String[]{
                    ProductIcesLandingStatistics.COLUMN_VESSEL_FLAG_COUNTRY,
                    ProductIcesLandingStatistics.COLUMN_LANDING_COUNTRY,
                    ProductIcesLandingStatistics.COLUMN_YEAR,
                    ProductIcesLandingStatistics.COLUMN_QUARTER,
                    ProductIcesLandingStatistics.COLUMN_MONTH,
                    ProductIcesLandingStatistics.COLUMN_AREA,
                    ProductIcesLandingStatistics.COLUMN_STATISTICAL_RECTANGLE,
                    ProductIcesLandingStatistics.COLUMN_SUB_POLYGON,
                    ProductIcesLandingStatistics.COLUMN_SPECIES,
                    ProductIcesLandingStatistics.COLUMN_LANDING_CATEGORY,
                    ProductIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    ProductIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    ProductIcesLandingStatistics.COLUMN_NATIONAL_METIER,
                    ProductIcesLandingStatistics.COLUMN_EU_METIER_LEVEL5,
                    ProductIcesLandingStatistics.COLUMN_EU_METIER_LEVEL6,
                    ProductIcesLandingStatistics.COLUMN_HARBOUR,
                    ProductIcesLandingStatistics.COLUMN_VESSEL_LENGTH_CATEGORY,
                    ProductIcesLandingStatistics.COLUMN_UNALLOCATED_CATCH_WEIGHT,
                    ProductIcesLandingStatistics.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT,
                    ProductIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_WEIGHT,
                    ProductIcesLandingStatistics.COLUMN_LANDINGS_MULTIPLIER,
                    ProductIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_VALUE
            })
            // TODO CE
            .build();
}

package net.sumaris.core.extraction.dao.table;

import com.google.common.collect.ImmutableMap;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;
import net.sumaris.core.model.file.ices.*;

import java.util.Map;

public final class ExtractionTableColumnOrder {


    public static Map<DatabaseTableEnum, String[]> COLUMNS_BY_TABLE = ImmutableMap.<DatabaseTableEnum, String[]>builder()
            .put(FileIcesTrip.TABLE, new String[]{
                    FileIcesTrip.COLUMN_SAMPLING_TYPE,
                    FileIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY,
                    FileIcesTrip.COLUMN_LANDING_COUNTRY,
                    FileIcesTrip.COLUMN_YEAR,
                    FileIcesTrip.COLUMN_PROJECT,
                    FileIcesTrip.COLUMN_TRIP_CODE,
                    FileIcesTrip.COLUMN_VESSEL_LENGTH,
                    FileIcesTrip.COLUMN_VESSEL_POWER,
                    FileIcesTrip.COLUMN_VESSEL_SIZE,
                    FileIcesTrip.COLUMN_VESSEL_TYPE,
                    FileIcesTrip.COLUMN_HARBOUR,
                    FileIcesTrip.COLUMN_OPERATION_COUNT,
                    FileIcesTrip.COLUMN_DAYS_AT_SEA,
                    FileIcesTrip.COLUMN_VESSEL_IDENTIFIER,
                    FileIcesTrip.COLUMN_SAMPLING_COUNTRY,
                    FileIcesTrip.COLUMN_SAMPLING_METHOD
            })
            .put(FileIcesStation.TABLE, new String[]{
                    FileIcesStation.COLUMN_SAMPLING_TYPE,
                    FileIcesStation.COLUMN_VESSEL_FLAG_COUNTRY,
                    FileIcesStation.COLUMN_LANDING_COUNTRY,
                    FileIcesStation.COLUMN_YEAR,
                    FileIcesStation.COLUMN_PROJECT,
                    FileIcesStation.COLUMN_TRIP_CODE,
                    FileIcesStation.COLUMN_STATION_NUMBER,
                    FileIcesStation.COLUMN_FISHING_VALIDITY,
                    FileIcesStation.COLUMN_AGGREGATION_LEVEL,
                    FileIcesStation.COLUMN_CATCH_REGISTRATION,
                    FileIcesStation.COLUMN_SPECIES_REGISTRATION,
                    FileIcesStation.COLUMN_DATE,
                    FileIcesStation.COLUMN_TIME,
                    FileIcesStation.COLUMN_FISHING_DURATION,
                    FileIcesStation.COLUMN_POS_START_LAT,
                    FileIcesStation.COLUMN_POS_START_LONG,
                    FileIcesStation.COLUMN_POS_END_LAT,
                    FileIcesStation.COLUMN_POS_END_LONG,
                    FileIcesStation.COLUMN_AREA,
                    FileIcesStation.COLUMN_STATISTICAL_RECTANGLE,
                    FileIcesStation.COLUMN_SUB_POLYGON,
                    FileIcesStation.COLUMN_MAIN_FISHING_DEPTH,
                    FileIcesStation.COLUMN_MAIN_WATER_DEPTH,
                    FileIcesStation.COLUMN_NATIONAL_METIER,
                    FileIcesStation.COLUMN_EU_METIER_LEVEL5,
                    FileIcesStation.COLUMN_EU_METIER_LEVEL6,
                    FileIcesStation.COLUMN_GEAR_TYPE,
                    FileIcesStation.COLUMN_MESH_SIZE,
                    FileIcesStation.COLUMN_SELECTION_DEVICE,
                    FileIcesStation.COLUMN_MESH_SIZE_SELECTION_DEVICE
            })
            .put(FileIcesSpeciesList.TABLE, new String[]{
                    FileIcesSpeciesList.COLUMN_SAMPLING_TYPE,
                    FileIcesSpeciesList.COLUMN_VESSEL_FLAG_COUNTRY,
                    FileIcesSpeciesList.COLUMN_LANDING_COUNTRY,
                    FileIcesSpeciesList.COLUMN_YEAR,
                    FileIcesSpeciesList.COLUMN_PROJECT,
                    FileIcesSpeciesList.COLUMN_TRIP_CODE,
                    FileIcesSpeciesList.COLUMN_STATION_NUMBER,
                    FileIcesSpeciesList.COLUMN_SPECIES,
                    FileIcesSpeciesList.COLUMN_SEX,
                    FileIcesSpeciesList.COLUMN_CATCH_CATEGORY,
                    FileIcesSpeciesList.COLUMN_LANDING_CATEGORY,
                    FileIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    FileIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    FileIcesSpeciesList.COLUMN_SUBSAMPLING_CATEGORY,
                    FileIcesSpeciesList.COLUMN_WEIGHT,
                    FileIcesSpeciesList.COLUMN_SUBSAMPLING_WEIGHT,
                    FileIcesSpeciesList.COLUMN_LENGTH_CODE
            })
            .put(FileIcesSpeciesLength.TABLE, new String[]{
                    FileIcesSpeciesLength.COLUMN_SAMPLING_TYPE,
                    FileIcesSpeciesLength.COLUMN_VESSEL_FLAG_COUNTRY,
                    FileIcesSpeciesLength.COLUMN_LANDING_COUNTRY,
                    FileIcesSpeciesLength.COLUMN_YEAR,
                    FileIcesSpeciesLength.COLUMN_PROJECT,
                    FileIcesSpeciesLength.COLUMN_TRIP_CODE,
                    FileIcesSpeciesLength.COLUMN_STATION_NUMBER,
                    FileIcesSpeciesLength.COLUMN_SPECIES,
                    FileIcesSpeciesLength.COLUMN_CATCH_CATEGORY,
                    FileIcesSpeciesLength.COLUMN_LANDING_CATEGORY,
                    FileIcesSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    FileIcesSpeciesLength.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    FileIcesSpeciesLength.COLUMN_SUBSAMPLING_CATEGORY,
                    FileIcesSpeciesLength.COLUMN_SEX,
                    FileIcesSpeciesLength.COLUMN_INDIVIDUAL_SEX,
                    FileIcesSpeciesLength.COLUMN_LENGTH_CLASS,
                    FileIcesSpeciesLength.COLUMN_NUMBER_AT_LENGTH
            })
            // TODO CA
            .put(FileIcesLandingStatistics.TABLE, new String[]{
                    FileIcesLandingStatistics.COLUMN_VESSEL_FLAG_COUNTRY,
                    FileIcesLandingStatistics.COLUMN_LANDING_COUNTRY,
                    FileIcesLandingStatistics.COLUMN_YEAR,
                    FileIcesLandingStatistics.COLUMN_QUARTER,
                    FileIcesLandingStatistics.COLUMN_MONTH,
                    FileIcesLandingStatistics.COLUMN_AREA,
                    FileIcesLandingStatistics.COLUMN_STATISTICAL_RECTANGLE,
                    FileIcesLandingStatistics.COLUMN_SUB_POLYGON,
                    FileIcesLandingStatistics.COLUMN_SPECIES,
                    FileIcesLandingStatistics.COLUMN_LANDING_CATEGORY,
                    FileIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE,
                    FileIcesLandingStatistics.COLUMN_COMMERCIAL_SIZE_CATEGORY,
                    FileIcesLandingStatistics.COLUMN_NATIONAL_METIER,
                    FileIcesLandingStatistics.COLUMN_EU_METIER_LEVEL5,
                    FileIcesLandingStatistics.COLUMN_EU_METIER_LEVEL6,
                    FileIcesLandingStatistics.COLUMN_HARBOUR,
                    FileIcesLandingStatistics.COLUMN_VESSEL_LENGTH_CATEGORY,
                    FileIcesLandingStatistics.COLUMN_UNALLOCATED_CATCH_WEIGHT,
                    FileIcesLandingStatistics.COLUMN_AREA_MISREPORTED_CATCH_WEIGHT,
                    FileIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_WEIGHT,
                    FileIcesLandingStatistics.COLUMN_LANDINGS_MULTIPLIER,
                    FileIcesLandingStatistics.COLUMN_OFFICIAL_LANDINGS_VALUE
            })
            // TODO CE
            .build();
}

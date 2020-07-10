import {FormFieldDefinitionMap} from "../../../shared/form/field.model";
import {LocationLevelIds} from "../model/model.enum";


export type LandingEditor = 'landing' | 'control' | 'trip';

export const ProgramProperties: FormFieldDefinitionMap = Object.freeze({
  // Trip
  TRIP_SALE_ENABLE: {
    key: "sumaris.trip.sale.enable",
    label: "PROGRAM.OPTIONS.TRIP_SALE_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_OBSERVERS_ENABLE: {
    key: "sumaris.trip.observers.enable",
    label: "PROGRAM.OPTIONS.TRIP_OBSERVERS_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_METIERS_ENABLE: {
    key: "sumaris.trip.metiers.enable",
    label: "PROGRAM.OPTIONS.TRIP_METIERS_ENABLE",
    defaultValue: "false",
    type: 'boolean'
  },
  TRIP_ON_BOARD_MEASUREMENTS_OPTIONAL: {
    key: 'sumaris.trip.onboard.measurements.optional',
    label: "PROGRAM.OPTIONS.TRIP_ON_BOARD_MEASUREMENTS_OPTIONAL",
    defaultValue: "false",
    type: 'boolean'
  },
  TRIP_PHYSICAL_GEAR_RANK_ORDER_ENABLE: {
    key: "sumaris.trip.gear.rankOrder.enable",
    label: "PROGRAM.OPTIONS.TRIP_PHYSICAL_GEAR_RANK_ORDER_ENABLE",
    defaultValue: "false",
    type: 'boolean'
  },
  // Trip map
  TRIP_MAP_ENABLE: {
    key: "sumaris.trip.map.enable",
    label: "PROGRAM.OPTIONS.TRIP_MAP_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_MAP_CENTER: {
    key: "sumaris.trip.map.center",
    label: "PROGRAM.OPTIONS.TRIP_MAP_CENTER",
    defaultValue: "46.879966,-10",
    type: 'string'
  },
  TRIP_MAP_ZOOM: {
    key: "sumaris.trip.map.zoom",
    label: "PROGRAM.OPTIONS.TRIP_MAP_ZOOM",
    defaultValue: 5,
    type: 'integer'
  },
  TRIP_BATCH_TAXON_NAME_ENABLE: {
    key: "sumaris.trip.operation.batch.taxonName.enable",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_TAXON_NAME_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_BATCH_TAXON_GROUP_ENABLE: {
    key: "sumaris.trip.operation.batch.taxonGroup.enable",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_TAXON_GROUP_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_BATCH_TAXON_GROUPS_NO_WEIGHT: {
    key: "sumaris.trip.operation.batch.taxonGroups.noWeight",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_TAXON_GROUPS_NO_WEIGHT",
    defaultValue: "",
    type: 'string'
  },
  TRIP_BATCH_AUTO_FILL: {
    key: "sumaris.trip.operation.batch.autoFill",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_AUTO_FILL",
    defaultValue: "false",
    type: 'boolean'
  },
  TRIP_BATCH_INDIVIDUAL_COUNT_COMPUTE: {
    key: "sumaris.trip.operation.batch.individualCount.compute",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_INDIVIDUAL_COUNT_COMPUTE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_BATCH_MEASURE_INDIVIDUAL_COUNT_ENABLE: {
    key: "sumaris.trip.operation.batch.individualCount.enable",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_MEASURE_INDIVIDUAL_COUNT_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_BATCH_MEASURE_RANK_ORDER_COMPUTE: {
    key: "sumaris.trip.operation.batch.rankOrder.compute",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_MEASURE_RANK_ORDER_COMPUTE",
    defaultValue: "false",
    type: 'boolean'
  },
  TRIP_SAMPLE_TAXON_NAME_ENABLE: {
    key: "sumaris.trip.operation.sample.taxonName.enable",
    label: "PROGRAM.OPTIONS.TRIP_SAMPLE_TAXON_NAME_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_SAMPLE_TAXON_GROUP_ENABLE: {
    key: "sumaris.trip.operation.sample.taxonGroup.enable",
    label: "PROGRAM.OPTIONS.TRIP_SAMPLE_TAXON_GROUP_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_SURVIVAL_TEST_TAXON_NAME_ENABLE: {
    key: "sumaris.trip.operation.survivalTest.taxonName.enable",
    label: "PROGRAM.OPTIONS.TRIP_SURVIVAL_TEST_TAXON_NAME_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_SURVIVAL_TEST_TAXON_GROUP_ENABLE: {
    key: "sumaris.trip.operation.survivalTest.taxonGroup.enable",
    label: "PROGRAM.OPTIONS.TRIP_SURVIVAL_TEST_TAXON_GROUP_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_LATITUDE_SIGN: {
    key: "sumaris.trip.operation.latitude.defaultSign",
    label: "PROGRAM.OPTIONS.TRIP_LATITUDE_DEFAULT_SIGN",
    type: 'enum',
    values: [
      {
        key: '+',
        value: 'N'
      },
      {
        key: '-',
        value: 'S'
      }
    ]
  },
  TRIP_LONGITUDE_SIGN: {
    key: "sumaris.trip.operation.longitude.defaultSign",
    label: "PROGRAM.OPTIONS.TRIP_LONGITUDE_DEFAULT_SIGN",
    type: 'enum',
    values: [
      {
        key: '+',
        value: 'E'
      },
      {
        key: '-',
        value: 'W'
      }
    ]
  },

  // Observed location
  OBSERVED_LOCATION_END_DATE_TIME_ENABLE: {
    key: 'sumaris.observedLocation.endDateTime.enable',
    label: "PROGRAM.OPTIONS.OBSERVED_LOCATION_END_DATE_TIME_ENABLE",
    defaultValue: "false",
    type: 'boolean'
  },
  OBSERVED_LOCATION_LOCATION_LEVEL_IDS: {
    key: 'sumaris.observedLocation.location.level.ids',
    label: "PROGRAM.OPTIONS.OBSERVED_LOCATION_LOCATION_LEVEL_IDS",
    type: 'enum',
    values: [
      {
        key: LocationLevelIds.PORT.toString(),
        value: 'PROGRAM.OPTIONS.LOCATION_LEVEL_PORT'
      },
      {
        key: LocationLevelIds.AUCTION.toString(),
        value: 'PROGRAM.OPTIONS.LOCATION_LEVEL_AUCTION'
      }
    ],
    defaultValue: LocationLevelIds.PORT.toString()
  },
  OBSERVED_LOCATION_AGGREGATED_LANDINGS_ENABLE: {
    key: 'sumaris.observedLocation.aggregatedLandings.enable',
    label: "PROGRAM.OPTIONS.OBSERVED_LOCATION_AGGREGATED_LANDINGS_ENABLE",
    defaultValue: "false",
    type: 'boolean'
  },
  OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM: {
    key: 'sumaris.observedLocation.aggregatedLandings.program',
    label: "PROGRAM.OPTIONS.OBSERVED_LOCATION_AGGREGATED_LANDINGS_PROGRAM",
    defaultValue: "",
    type: 'string'
  },
  OBSERVED_LOCATION_AGGREGATED_LANDINGS_START_DAY: {
    key: 'sumaris.observedLocation.aggregatedLandings.startDay',
    label: "PROGRAM.OPTIONS.OBSERVED_LOCATION_AGGREGATED_LANDINGS_START_DAY",
    defaultValue: "1",
    type: 'integer'
  },
  OBSERVED_LOCATION_AGGREGATED_LANDINGS_DAY_COUNT: {
    key: 'sumaris.observedLocation.aggregatedLandings.dayCount',
    label: "PROGRAM.OPTIONS.OBSERVED_LOCATION_AGGREGATED_LANDINGS_DAY_COUNT",
    defaultValue: "7",
    type: 'integer'
  },

  // Landing
  LANDING_EDITOR: {
    key: 'sumaris.landing.editor',
    label: 'PROGRAM.OPTIONS.LANDING_EDITOR',
    type: 'enum',
    values: [
      {
        key: 'landing',
        value: 'PROGRAM.OPTIONS.LANDING_EDITOR_LANDING'
      },
      {
        key: 'control',
        value: 'PROGRAM.OPTIONS.LANDING_EDITOR_CONTROL'
      },
      {
        key: 'trip',
        value: 'PROGRAM.OPTIONS.LANDING_EDITOR_TRIP'
      }
    ],
    defaultValue: 'landing'
  },
  LANDING_DATE_TIME_ENABLE: {
    key: 'sumaris.landing.dateTime.enable',
    label: "PROGRAM.OPTIONS.LANDING_DATE_TIME_ENABLE",
    defaultValue: "false",
    type: 'boolean'
  },
  LANDING_OBSERVERS_ENABLE: {
    key: "sumaris.landing.observers.enable",
    label: "PROGRAM.OPTIONS.LANDING_OBSERVERS_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  }
});


import {FormFieldDefinition} from "../../../shared/form/field.model";
import {EntitiesStorageTypePolicies} from "../../../core/services/storage/entities-storage.service";
import {EntityStoreTypePolicy} from "../../../core/services/storage/entity-store.class";
import {Trip} from "../model/trip.model";
import {TypePolicies} from "@apollo/client/core";
import {ObservedLocation} from "../model/observed-location.model";
import {Landing} from "../model/landing.model";
import {Operation} from "../model/operation.model";

/**
 * Name of the features (e.g. to be used by settings)
 */
export const TRIP_FEATURE_NAME = 'trip';
export const OBSERVED_LOCATION_FEATURE_NAME = 'observedLocation';

/**
 * Define configuration options
 */
export const TRIP_CONFIG_OPTIONS = Object.freeze({
  TRIP_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.trip.enable',
    label: 'TRIP.OPTIONS.ENABLE',
    type: 'boolean'
  },
  OBSERVED_LOCATION_NEW_NAME: <FormFieldDefinition>{
    key: 'sumaris.observedLocation.new.name',
    label: 'OBSERVED_LOCATION.OPTIONS.NEW_NAME',
    type: 'enum',
    values: [
      {
        key: 'OBSERVED_LOCATION.NEW.TITLE',
        value: 'OBSERVED_LOCATION.NEW.TITLE'
      },
      {
        key: 'OBSERVED_LOCATION.NEW.SAMPLING.TITLE',
        value: 'OBSERVED_LOCATION.NEW.SAMPLING.TITLE'
      },
      {
        key: 'AUCTION_CONTROL.NEW.TITLE',
        value: 'AUCTION_CONTROL.NEW.TITLE'
      }
    ],
    defaultValue: "OBSERVED_LOCATION.NEW.TITLE"
  },
  OBSERVED_LOCATION_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.observedLocation.enable',
    label: 'OBSERVED_LOCATION.OPTIONS.ENABLE',
    type: 'boolean'
  },
  OBSERVED_LOCATION_NAME: <FormFieldDefinition>{
    key: 'sumaris.observedLocation.name',
    label: 'OBSERVED_LOCATION.OPTIONS.NAME',
    type: 'enum',
    values: [
      {
        key: 'MENU.OBSERVED_LOCATIONS',
        value: 'MENU.OBSERVED_LOCATIONS'
      },
      {
        key: 'MENU.OCCASION',
        value: 'MENU.OCCASION'
      },
      {
        key: 'MENU.AUCTION_OCCASION',
        value: 'MENU.AUCTION_OCCASION'
      }
    ],
    defaultValue: 'MENU.OBSERVED_LOCATIONS'
  }
});

export const TRIP_LOCAL_SETTINGS_OPTIONS = Object.freeze({
  SAMPLE_BURST_MODE_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.sample.modal.enableBurstMode',
    label: 'TRIP.SAMPLE.SETTINGS.BURST_MODE_ENABLE',
    type: 'boolean',
    defaultValue: false
  }
});

export const TRIP_GRAPHQL_TYPE_POLICIES = <TypePolicies>{
  'MeasurementVO': {
    keyFields: ['entityName', 'id']
  }
};

/**
 * Define the way the entities will be stored into the local storage
 */
export const TRIP_STORAGE_TYPE_POLICIES = <EntitiesStorageTypePolicies>{
  'TripVO': <EntityStoreTypePolicy<Trip>>{
    mode: 'by-id',
    skipNonLocalEntities: true,
    lightFieldsExcludes: ['measurements', 'sale', 'gears', 'operationGroups', 'operations']
  },

  'OperationVO': <EntityStoreTypePolicy<Operation>>{
    mode: 'by-id',
    skipNonLocalEntities: true,
    lightFieldsExcludes: ["trip", "measurements", "samples", "batches", "catchBatch", "gearMeasurements", 'fishingAreas']
  },

  'ObservedLocationVO': <EntityStoreTypePolicy<ObservedLocation>>{
    mode: 'by-id',
    skipNonLocalEntities: true
  },

  'LandingVO': <EntityStoreTypePolicy<Landing>>{
    mode: 'by-id',
    skipNonLocalEntities: true,
    lightFieldsExcludes: ["samples"]
  },

  // Fake entity, use to store historical data
  'Remote#LandingVO': <EntityStoreTypePolicy<Landing>>{
    skipNonLocalEntities: false // Keep remote entities
  }
};


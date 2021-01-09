import {FormFieldDefinition, FormFieldDefinitionMap} from "../../../shared/form/field.model";
import {EntitiesStorageTypePolicies} from "../../../core/services/storage/entities-storage.service";
import {EntityStoreTypePolicy} from "../../../core/services/storage/entity-store.class";
import {Operation, Trip} from "../model/trip.model";

/**
 * Name of the features (e.g. to be used by settings)
 */
export const TRIP_FEATURE_NAME = 'trip';
export const OBSERVED_LOCATION_FEATURE_NAME = 'observedLocation';

/**
 * Define configuration options
 */
export const TRIP_CONFIG_OPTIONS = <FormFieldDefinitionMap>{
  TRIP_ENABLE: {
    key: 'sumaris.trip.enable',
    label: 'TRIP.OPTIONS.ENABLE',
    type: 'boolean'
  },
  OBSERVED_LOCATION_ENABLE: {
    key: 'sumaris.observedLocation.enable',
    label: 'OBSERVED_LOCATION.OPTIONS.ENABLE',
    type: 'boolean'
  },
  OBSERVED_LOCATION_NAME: {
    key: 'sumaris.observedLocation.name',
    label: 'OBSERVED_LOCATION.OPTIONS.NAME',
    type: 'string'
  }
};

export const TRIP_LOCAL_SETTINGS_OPTIONS = {
  SAMPLE_BURST_MODE_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.sample.modal.enableBurstMode',
    label: 'TRIP.SAMPLE.SETTINGS.BURST_MODE_ENABLE',
    type: 'boolean',
    defaultValue: false
  }
};

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
  }
};

import {TypePolicies} from "@apollo/client/core";


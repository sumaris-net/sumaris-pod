import {FormFieldDefinitionMap} from "../../../shared/form/field.model";

export const TripConfigOptions: FormFieldDefinitionMap = {
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

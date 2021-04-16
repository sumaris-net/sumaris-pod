import {TypePolicies} from "@apollo/client/core";
import {FormFieldDefinition, FormFieldDefinitionMap} from "../../../shared/form/field.model";

export const VESSEL_FEATURE_NAME = 'vessel';

export const VESSEL_GRAPHQL_TYPE_POLICIES = <TypePolicies>{

};

export const VESSEL_CONFIG_OPTIONS: FormFieldDefinitionMap = {
  REFERENTIAL_VESSEL_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.referential.vessel.enable',
    label: 'REFERENTIAL.OPTIONS.VESSELS_ENABLE',
    type: 'boolean',
    defaultValue: 'false'
  }
};

export const VESSEL_LOCAL_SETTINGS_OPTIONS: FormFieldDefinitionMap = {

};

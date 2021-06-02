import {TypePolicies} from "@apollo/client/core";
import { FormFieldDefinition } from "src/app/shared/form/field.model";

export const DATA_GRAPHQL_TYPE_POLICIES = <TypePolicies>{
  'DataReferenceVO': {
    keyFields: ['entityName', 'id']
  }
};

export const DATA_CONFIG_OPTIONS = Object.freeze({
  DATA_QUALITY_PROCESS_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.data.quality.process.enable',
    label: 'CONFIGURATION.OPTIONS.DATA_QUALITY_PROCESS_ENABLE',
    type: 'boolean',
    defaultValue: true
}
});

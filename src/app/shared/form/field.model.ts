import {MatAutocompleteFieldConfig} from "../material/material.autocomplete";


export declare type FormFieldType = 'integer' | 'double' | 'boolean' | 'string' | 'enum' | 'color' | 'peer' | 'entity';

export declare class FormFieldValue {
  key: string;
  value?: string;
}

export declare interface FormFieldDefinition<T = any> {
  key: string;
  label: string;
  defaultValue?: any;
  isTransient?: boolean; // Useful only for remote configuration
  values?: (string|FormFieldValue)[];
  type: FormFieldType;
  autocomplete?: MatAutocompleteFieldConfig<T>;
  extra?: {
    [key: string]: {
      disable?: boolean;
      required: boolean;
    }
  };
}
export declare interface FormFieldDefinitionMap {
  [key: string]: FormFieldDefinition;
}

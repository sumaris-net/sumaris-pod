import {MatAutocompleteFieldAddOptions, MatAutocompleteFieldConfig} from "../material/material.autocomplete";

export declare type DisplayFn = (obj: any) => string;

export declare type FormFieldType = 'integer' | 'double' | 'boolean' | 'string' | 'enum' | 'color' | 'peer' | 'entity';

export declare class FormFieldValue {
  key: string;
  value?: string;
}

export declare interface FormFieldDefinition<T = any> {
  key: string;
  label: string;
  minValue?: number;
  maxValue?: number;
  maximumNumberDecimals?: number;
  defaultValue?: any;
  isTransient?: boolean; // Useful only for remote configuration
  values?: (string|FormFieldValue)[];
  type: FormFieldType;
  autocomplete?: MatAutocompleteFieldAddOptions<T>;
  disabled?: boolean;
  required?: boolean;
  extra?: {
    [key: string]: {
      disabled?: boolean;
      required: boolean;
    }
  };
}
export declare interface FormFieldDefinitionMap {
  [key: string]: FormFieldDefinition;
}

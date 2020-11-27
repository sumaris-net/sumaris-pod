import {MatAutocompleteFieldAddOptions} from "../material/material.autocomplete";
import {ObjectMap, Property} from "../types";

export declare type DisplayFn = (obj: any) => string;

export declare type CompareWithFn = (o1: any, o2: any) => boolean;

export declare type FormFieldType = 'integer' | 'double' | 'boolean' | 'string' | 'enum' | 'color' | 'peer' | 'entity';

export declare interface FormFieldDefinition<T = any> {
  key: string;
  label: string;
  minValue?: number;
  maxValue?: number;
  maximumNumberDecimals?: number;
  defaultValue?: any;
  isTransient?: boolean; // Useful only for remote configuration
  values?: (string|Property)[];
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
export declare type FormFieldDefinitionMap = ObjectMap<FormFieldDefinition>;

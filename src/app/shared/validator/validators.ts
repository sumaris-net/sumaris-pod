import {FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";
import * as moment from 'moment/moment';
import { DATE_ISO_PATTERN, PUBKEY_REGEXP } from "../constants";
import {fromDateISOString, isNil, isNotNil} from "../functions";

export class SharedValidators {

  static validDate(control: FormControl): ValidationErrors | null {
    const value = control.value;
    const date = !value || moment.isMoment(value) ? value : moment(control.value, DATE_ISO_PATTERN);
    if (date && (!date.isValid() || date.year() < 1970)) {
      return { validDate: true };
    }
  }

  static latitude(control: FormControl): ValidationErrors | null {
    const value = control.value;
    if (isNotNil(value) && (value < -90 || value > 90))
      return { validLatitude: true };
  }

  static longitude(control: FormControl): ValidationErrors | null {
    const value = control.value;
    if (isNotNil(value) && (value < -180 || value > 180))
      return { validLongitude: true };
  }

  static object(control: FormControl): ValidationErrors | null {
    const value = control.value;
    if (value && typeof value != 'object')
      return { object: true };
  }

  static entity(control: FormControl): ValidationErrors | null {
    const value = control.value;
    if (value && typeof value != 'object' && (value.id === undefined || value.id === null))
      return { entity: true };
  }

  static pubkey(control: FormControl): ValidationErrors | null {
    const value = control.value;
    if (value && (typeof value != 'string' || !PUBKEY_REGEXP.test(value)))
      return { pubkey: true };
  }

  static integer(control: FormControl): ValidationErrors | null {
    const value = control.value;
    if (isNotNil(value) && value !== "" && !Number.isInteger(value))
      return { integer: true };
  }

  static double(options?: {maxDecimals?: number}): ValidatorFn {
    options = options || {};
    let regexpStr;
    if (isNotNil(options.maxDecimals)) {
      if (options.maxDecimals < 0) throw new Error(`Invalid maxDecimals value: ${options.maxDecimals}`);
      regexpStr = options.maxDecimals > 1 ? `^[0-9]+([.,][0-9]{1,${options.maxDecimals}})?$` : "^[0-9]+([.,][0-9])?$";
    }
    else {
      regexpStr = "^[0-9]+([.,][0-9]*)?$";
    }

    const regexp = new RegExp(regexpStr);
    return (control: FormControl): ValidationErrors | null => {
      const value = control.value;
      if (isNotNil(value) && value !== "" && (Number.isNaN(value) || !regexp.test(value as string))) {
        return { maxDecimals: true };
      }
    };
  }

  static dateIsAfter(startDateField: string, endDateField: string): ValidatorFn {
    return (group: FormGroup): ValidationErrors | null => {
      const endField = group.get(endDateField);
      const startDate = fromDateISOString(group.get(startDateField).value);
      const endDate = fromDateISOString(endField.value);
      if (startDate !== null && endDate !== null && startDate >= endDate) {
        // Update end field
        const endFieldErrors: ValidationErrors = endField.errors || {};
        endFieldErrors['dateIsAfter'] = true;
        endField.setErrors(endFieldErrors);
        // Return the error (should be apply to the parent form)
        return { dateIsAfter: true};
      }
      // OK: remove the existing on the end field
      else if (endField.hasError('dateIsAfter')) {
        const errors = endField.errors;
        if (errors && errors.dateIsAfter) {
          if (Object.getOwnPropertyNames(errors).length > 1) {
            delete errors.dateIsAfter;
            endField.setErrors(errors);
          }
          else {
            endField.setErrors(null);
          }
        }
      }
      return null;
    };
  }

  static requiredIf(fieldName: string, anotherFieldToCheck: string): ValidatorFn {
    return (group: FormGroup): ValidationErrors | null => {
      const control = group.get(fieldName);
      if (isNil(control.value) && isNotNil(group.get(anotherFieldToCheck).value)) {
        const error = { required: true};
        control.setErrors(error);
        return error;
      }
      return null;
    };
  }


}

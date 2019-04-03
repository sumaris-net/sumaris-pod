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
    if (isNotNil(value) && value !== "" && Math.trunc(value) !== value)
      return { integer: true };
  }

  static maxDecimalsPattern(maxDecimals: number): ValidatorFn {
    const regexp = maxDecimals > 1 ? `^[0-9]+(\.[0-9]{1,${maxDecimals}})?$` : `^[0-9]+(\.[0-9])?$`;
    return Validators.pattern(new RegExp(regexp));
  }

  static dateIsAfter(startDateField: string, endDateField: string): ValidatorFn {
    return (group: FormGroup): { [key: string]: boolean } | null => {
      const startDate = fromDateISOString(group.get(startDateField).value);
      const endDate = fromDateISOString(group.get(endDateField).value);
      if ((startDate !== null && endDate !== null) && startDate > endDate) {
        const error = { dateIsAfter: true};
        group.get(endDateField).setErrors(error);
        return error;
      }
      return null;
    };
  }

  static requiredIf(fieldName: string, anotherFieldToCheck: string): ValidatorFn {
    return (group: FormGroup): { [key: string]: boolean } | null => {
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

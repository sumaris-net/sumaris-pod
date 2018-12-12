import {AbstractControl, FormGroup} from "@angular/forms";
import {nullIfUndefined} from "../../shared/shared.module";

export class AppFormUtils {
    static copyForm2Entity = copyForm2Entity;
    static copyEntity2Form = copyEntity2Form;
    static getFormValueFromEntity = getFormValueFromEntity;
    static logFormErrors = logFormErrors;
    static getControlFromPath = getControlFromPath;
    static filterNumberInput = filterNumberInput;
}

/**
   * Fill an entity using the given form value
   * @param source a source form group 
   * @param target an entity to fill
   */
export function copyForm2Entity(source: FormGroup, target: any): Object {
    target = target || {};
    for (let key in source.controls) {
        const control = source.controls[key];
        if (control instanceof FormGroup) {
            target[key] = this.copyForm2Entity(control as FormGroup, target[key]);
        }
        else {
            // Read the form value
            target[key] = control.value;
        }
    }
    return target;
}

/**
 * Fill a form using a source entity
 * @param target 
 * @param source 
 */
export function copyEntity2Form(source: any, target: FormGroup) {
    target.setValue(getFormValueFromEntity(source, target));
}

/**
 * Get entity as a simple object, compatible with the given form
 * @param source an entity (or subentity)
 * @param form 
 */
export function getFormValueFromEntity(source: any, form: FormGroup): { [key: string]: any } {
    const value = {};
    for (let key in form.controls) {
        if (form.controls[key] instanceof FormGroup) {
            value[key] = getFormValueFromEntity(source[key] || {}, form.controls[key] as FormGroup);
        }
        else {
            value[key] = nullIfUndefined(source[key]); // undefined not authorized as control value
        }
    }
    return value;
}

export function logFormErrors(form: FormGroup, logPrefix?: string, path?: string) {
    if (form.valid) return;
    logPrefix = logPrefix || "";
    const value = {};
    if (!path) console.warn(`${logPrefix} Form errors:`);
    for (let key in form.controls) {
        let keyPath = (path ? `${path}/${key}` : key);
        if (form.controls[key] instanceof FormGroup) {
            logFormErrors(form.controls[key] as FormGroup, logPrefix, keyPath);
        }
        else if (form.controls[key]) {
            for (let error in form.controls[key].errors) {
                console.warn(` -> '${keyPath}' (${error})`);
            }
        }
    }
}

export function getControlFromPath(form: FormGroup, path: string): AbstractControl {
    const i = path.indexOf('.');
    if (i == -1) {
        return form.controls[path];
    }
    const key = path.substring(0, i);
    if (form.controls[key] instanceof FormGroup) {
        return getControlFromPath((form.controls[key] as FormGroup), path.substring(i + 1));
    }
    throw new Error(`Invalid form path: '${key}' should be a form group.`);
}

export function filterNumberInput(event: KeyboardEvent, allowDecimals: boolean) {
  //input number entered or one of the 4 direction up, down, left and right
  if ((event.which >= 48 && event.which <= 57) || (event.which >= 37 && event.which <= 40)) {
    //console.debug('input number entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
    // OK
  }
  // Decimal separator
  else if (allowDecimals && event.key == '.' || event.key == ',') {
    //console.debug('input decimal separator entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
    // OK
  }
  else {
    //input command entered of delete, backspace or one of the 4 direction up, down, left and right
    if ((event.keyCode >= 37 && event.keyCode <= 40) || event.keyCode == 46 || event.which == 8|| event.keyCode == 9) {
      //console.debug('input command entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
      // OK
    }
    // Cancel other keyboard events
    else {
      //console.debug('input not number entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
      event.preventDefault();
    }
  }
}

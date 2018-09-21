
import { FormGroup } from "@angular/forms";

export class AppFormUtils {
    static copyForm2Entity = copyForm2Entity;
    static copyEntity2Form = copyEntity2Form;
    static getFormValueFromEntity = getFormValueFromEntity;
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
            const value = control.value;
            // Set target attribute
            if (value && typeof value == "object" && value._isAMomentObject) {
                console.warn("[form.utils] TODO: check if Moment should be converted ?" + value);
                //data[key] = this.dateAdapter.format(value, DATE_ISO_PATTERN);
                target[key] = value;
            }
            else {
                target[key] = value || (value === 0 ? 0 : null); // Do NOT replace 0 by null
            }
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
    const json = getFormValueFromEntity(source, target);
    target.setValue(json);
}

/**
 * Get entity as a simple object, compatible with the given form
 * @param source an entity (or subentity)
 * @param form 
 */
export function getFormValueFromEntity(source: any, form: FormGroup): Object {
    const value = {};
    for (let key in form.controls) {
        if (form.controls[key] instanceof FormGroup) {
            value[key] = getFormValueFromEntity(source[key] || {}, form.controls[key] as FormGroup);
        }
        else {
            if (source[key] && typeof source[key] == "object" && source[key]._isAMomentObject) {
                console.warn("[form.utils] TODO: check if Moment should be converted ?" + source[key]);
                //value[key] = this.dateAdapter.format(data[key], DATE_ISO_PATTERN);
                value[key] = source[key];
            }
            else {
                value[key] = source[key] || (source[key] === 0 ? 0 : null); // Do NOT replace 0 by null
            }
        }
    }
    return value;
}
import {AbstractControl, FormArray, FormBuilder, FormControl, FormGroup} from "@angular/forms";
import {isNil, nullIfUndefined, selectInputContent, toBoolean, filterNumberInput} from "../../shared/shared.module";
import {DATE_ISO_PATTERN} from "../../shared/constants";
import {isMoment} from "moment";
import {Entity} from "../services/model";

export {selectInputContent};

export class AppFormUtils {
  static copyForm2Entity = copyForm2Entity;
  static copyEntity2Form = copyEntity2Form;
  static getFormValueFromEntity = getFormValueFromEntity;
  static logFormErrors = logFormErrors;
  static getControlFromPath = getControlFromPath;
  static filterNumberInput = filterNumberInput;
  static disableControls = disableControls;
  static selectInputContent = selectInputContent;
  static markAsTouched = markAsTouched;
  static markAsPristine = markAsPristine;

  // ArrayForm
  static addValueInArray = addValueInArray;
  static removeValueInArray = removeValueInArray;
  static resizeArray = resizeArray;
  static clearValueInArray = clearValueInArray;
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
    } else {
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
export function copyEntity2Form(source: any, target: FormGroup, opts?: { emitEvent?: boolean; onlySelf?: boolean; }) {
  const json = getFormValueFromEntity(source, target);
  target.patchValue(json, opts);
}

/**
 * Transform an entity into a simple object, compatible with the given form
 * @param source an entity (or subentity)
 * @param form
 */
export function getFormValueFromEntity(source: any, form: FormGroup): { [key: string]: any } {
  const value = {};
  for (let key in form.controls) {
    // If sub-group: recursive call
    if (form.controls[key] instanceof FormGroup) {
      value[key] = getFormValueFromEntity(source[key] || {}, form.controls[key] as FormGroup);
    }
    // If array: try to convert using the first sub-group found
    else if (form.controls[key] instanceof FormArray) {
      if (source[key] instanceof Array) {
        // Try to transform object to key/value
        const control = form.controls[key] as FormArray;
        if (control.length > 0) {
          // Use the first form group, as model
          const itemControl = control.at(0);
          if (itemControl instanceof FormGroup) {
            value[key] = (source[key] || []).map(item => getFormValueFromEntity(item || {}, itemControl));
            if (value[key].length !== control.length) {
              console.warn(`WARN: please resize the FormArray '${key}' to the same length of the input array`);
            }
          }
          else if (itemControl instanceof FormControl){
            value[key] = (source[key] || []).slice(); // copy input values
            // Add empty values if need
            if (value[key].length < control.length) {
              console.warn(`WARN: Adding null value to array values`);
              for (let i = value[key].length; i++; i < control.length) {
                value[key].push(null);
              }
            }
          }
        }
      }
      else if (source[key] === undefined) {
        console.warn("Invalid form value. Expected array but found:", source[key]);
        value[key] = [];
      }
    }
    // Date
    else if (isMoment(source[key])) {
      value[key] = source[key].format(DATE_ISO_PATTERN);
    }
    // Any other control: replace undefined by null value
    else {
      value[key] = nullIfUndefined(source[key]); // undefined not authorized as control value
    }
  }
  return value;
}

export function logFormErrors(control: AbstractControl, logPrefix?: string, path?: string) {
  if (control.valid) return;
  logPrefix = logPrefix || "";
  // Form group
  if (control instanceof FormGroup) {
    if (!path) console.warn(`${logPrefix} Form errors:`);
    for (let error in control.errors) {
      console.warn(`'${logPrefix} -> ${path||''} (${error})`);
    }
    for (let key in control.controls) {
      logFormErrors(control.controls[key], logPrefix, (path ? `${path}/${key}` : key)); // Recursive call
    }
  }
  // Form array
  else if (control instanceof FormArray) {
    control.controls.forEach((child, index) => {
      logFormErrors(child, logPrefix, (path ? `${path}#${index}` : `#${index}`)); // Recursive call
    });
  }
  // Other control
  else {
    for (let error in control.errors) {
      console.warn(`'${logPrefix} -> ${path||''} (${error})`);
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


export function disableControls(form: FormGroup, paths: string[]) {
  (paths || []).forEach(path => {
    const control = AppFormUtils.getControlFromPath(form, path);
    if (control) {
      control.disable();
    }
  });
}


export function addValueInArray(formBuilder: FormBuilder,
                                form: FormGroup,
                                arrayName: string,
                                createControl: (value?: any) => AbstractControl,
                                equals: (v1: any, v2: any) => boolean,
                                isEmpty: (value: any) => boolean,
                                value: any,
                                options?: { emitEvent: boolean; }): boolean {
  options = options || {emitEvent: true};
  console.debug("[form] Adding " + arrayName);

  let arrayControl = form.get(arrayName) as FormArray;
  let hasChanged = false;
  let index = -1;

  if (!arrayControl) {
    arrayControl = formBuilder.array([]);
    form.addControl(arrayName, arrayControl);
  } else {

    // Search if value already exists
    if (!isEmpty(value)) {
      index = (arrayControl.value || []).findIndex(v => equals(value, v));
    }

    // If value not exists, but last value is empty: use it
    if (index === -1 && arrayControl.length && isEmpty(arrayControl.at(arrayControl.length - 1).value)) {
      index = arrayControl.length - 1;
    }
  }

  // Replace the existing value
  if (index !== -1) {
    if (!isEmpty(value)) {
      arrayControl.at(index).patchValue(value, options);
      hasChanged = true;
    }
  } else {
    const control = createControl(value);
    arrayControl.push(control);
    index = arrayControl.length - 1;
    hasChanged = true;
  }

  if (hasChanged) {
    if (isNil(options.emitEvent) || options.emitEvent) {
      // Mark array control dirty
      if (!isEmpty(value)) {
        arrayControl.markAsDirty();
      }
    }
  }

  return hasChanged;
}

export function resizeArray(formBuilder: FormBuilder,
                            form: FormGroup,
                            arrayName: string,
                            createControl: () => AbstractControl,
                            length: number): boolean {
  let arrayControl = form.get(arrayName) as FormArray;
  if (!arrayControl) {
    arrayControl = formBuilder.array([]);
    form.addControl(arrayName, arrayControl);
  }
  const hasChanged = arrayControl.length !== length;

  // Increase size
  if (arrayControl.length < length) {
    while (arrayControl.length < length) {
      arrayControl.push(createControl());
    }
  }

  // Or reduce
  else if (arrayControl.length > length) {
    while (arrayControl.length > length) {
      arrayControl.removeAt(arrayControl.length - 1);
    }
  }

  return hasChanged;
}

export function removeValueInArray(form: FormGroup,
                                   arrayName: string,
                                   isEmpty: (value: any) => boolean,
                                   index: number,
                                   opt?: {
                                     allowEmptyArray: boolean;
                                   }): boolean {
  const arrayControl = form.get(arrayName) as FormArray;

  arrayControl.removeAt(index);
  arrayControl.markAsDirty();
  return true;
}

export function clearValueInArray(form: FormGroup,
                                  arrayName: string,
                                  isEmpty: (value: any) => boolean,
                                  index: number): boolean {
  const arrayControl = form.get(arrayName) as FormArray;

  const control = arrayControl.at(index);
  if (isEmpty(control.value)) return false; // skip (not need to clear)

  if (control instanceof FormGroup) {
    AppFormUtils.copyEntity2Form({}, control);
  }
  else if (control instanceof FormArray) {
    control.setValue([]);
  }
  else {
    control.setValue(null);
  }
  arrayControl.markAsDirty();
  return true;
}

export function markAsTouched(form: FormGroup, opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
  if (!form) return;
  form.markAsTouched(opts);
  Object.keys(form.controls)
    .map(key => form.controls[key])
    .filter(control => control.enabled)
    .forEach(control => {
      if (control instanceof FormGroup) {
        markAsTouched(control, opts); // recursive call
      }
      else {
        control.markAsTouched(opts || {onlySelf: true});
        control.updateValueAndValidity(opts || {onlySelf: true, emitEvent: false});
      }
    });
}

export function markAsPristine(form: FormGroup, opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
  if (!form) return;
  form.markAsPristine();
  Object.getOwnPropertyNames(form.controls)
    .forEach(key => {
      const control = form.get(key);
      if (control instanceof FormGroup) {
        markAsPristine(control, opts); // recursive call
      }
      else {
        control.markAsPristine(opts || {onlySelf: true});
        control.updateValueAndValidity(opts || {onlySelf: true, emitEvent: false});
      }
    });
}

export class FormArrayHelper<T = Entity<T>> {

  private readonly arrayControl: FormArray;
  private allowEmptyArray: boolean;

  constructor(
    private formBuilder: FormBuilder,
    private form: FormGroup,
    private arrayName: string,
    private createControl: (value?: T) => AbstractControl,
    private equals: (v1: T, v2: T) => boolean,
    private isEmpty: (value: T) => boolean,
    options?: {
      allowEmptyArray: boolean;
    }
  ) {

    // Make sure to create the array
    this.arrayControl = form.get(arrayName) as FormArray;
    if (!this.arrayControl) {
      console.warn(`[form] Missing array control ${arrayName}: will add it!`);
      this.arrayControl = formBuilder.array([]);
      form.addControl(arrayName, this.arrayControl);
    }

    this.allowEmptyArray = toBoolean(options && options.allowEmptyArray, false);
  }

  add(value?: T, options?: { emitEvent: boolean }): boolean {
    return addValueInArray(this.formBuilder, this.form, this.arrayName, this.createControl, this.equals, this.isEmpty, value, options);
  }

  removeAt(index: number) {
    // Do not remove if last criterion
    if (!this.allowEmptyArray && this.arrayControl.length === 1) {
      return clearValueInArray(this.form, this.arrayName, this.isEmpty, index);
    }
    else {
      return removeValueInArray(this.form, this.arrayName, this.isEmpty, index);
    }
  }

  resize(length: number): boolean {
    return resizeArray(this.formBuilder, this.form, this.arrayName, this.createControl, length);
  }

  clearAt(index: number): boolean {
    return clearValueInArray(this.form, this.arrayName, this.isEmpty, index);
  }

  isLast(index: number): boolean {
    return (this.arrayControl.length - 1) === index;
  }

  removeAllEmpty() {
    let index = this.arrayControl.controls.findIndex(c => this.isEmpty(c.value));
    while(index !== -1) {
      this.removeAt(index);
      index = this.arrayControl.controls.findIndex(c => this.isEmpty(c.value));
    }
  }

  size(): number {
    return this.arrayControl.length;
  }

  at(index: number): AbstractControl {
    return this.arrayControl.at(index) as AbstractControl;
  }
}

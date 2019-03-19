import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Configuration} from "./model";

@Injectable()
export class ConfigValidatorService {

  constructor(
    private formBuilder: FormBuilder
  ) {
  }

  getFormGroup(data?: Configuration): FormGroup {
    return this.formBuilder.group({
      id: [data && data.id || null],
      label: [data && data.label || null, Validators.compose([Validators.required, Validators.max(50)])],
      name: [data && data.name || null, Validators.compose([Validators.required, Validators.max(100)])],
      updateDate: [data && data.updateDate || null],
      creationDate: [data && data.creationDate || null],
      statusId: [data && data.statusId || null, Validators.required],
      properties: this.formBuilder.array((data && this.getMapAsArray(data.properties) || [{key: 'default'}]).map(property => this.getPropertyFormGroup(property)))
    });
  }

  getPropertyFormGroup(data?: {key: string; value?: string;}): FormGroup {
    return this.formBuilder.group({
      key: [data && data.key || null, Validators.compose([Validators.required, Validators.max(50)])],
      value: [data && data.value || null, Validators.compose([Validators.required, Validators.max(100)])]
    });
  }

  getMapAsArray(properties?: Map<string, string>): {key: string; value?: string;}[] {
    return Object.getOwnPropertyNames(properties || {})
      .map(key => {
        return {
          key,
          value: properties[key]
        };
      });
  }

  getArrayAsMap(properties?: {key: string; value?: string;}[]) : Map<string, string> {
    const result = new Map<string, string>();
    properties.forEach(item => result.set(item.key, item.value));
    return result;
  }
}

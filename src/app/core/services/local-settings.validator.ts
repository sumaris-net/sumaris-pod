import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {LocalSettings} from "./model";

@Injectable()
export class LocalSettingsValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: LocalSettings): FormGroup {
    return this.formBuilder.group({
      locale: [data && data.locale || null, Validators.required],
      latLongFormat: [data && data.latLongFormat || null, Validators.required],
      usageMode: [data && data.usageMode || 'DESK', Validators.required],
      peerUrl: [data && data.peerUrl, Validators.required]
    });
  }
}

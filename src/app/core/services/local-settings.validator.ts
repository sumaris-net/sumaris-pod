import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators} from "@angular/forms";
import {LocalSettings} from "./model";
import {SharedValidators} from "../../shared/validator/validators";
import {NetworkService} from "./network.service";

@Injectable()
export class LocalSettingsValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder,
    private networkService: NetworkService
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
    }, {
      asyncValidators: (group: FormGroup) => this.peerAlive(group.get('peerUrl'))
    });
  }

  protected async peerAlive(peerUrlControl: AbstractControl): Promise<ValidationErrors | null>  {
    const alive = await this.networkService.checkPeerAlive(peerUrlControl.value);

    if (!alive) {
      // Update end field
      const errors: ValidationErrors = peerUrlControl.errors || {};
      errors['peerAlive'] = true;
      peerUrlControl.setErrors(errors);
      peerUrlControl.markAsTouched({onlySelf: true});
      // Return the error (should be apply to the parent form)
      return { peerAlive: true};
    }
    // OK: remove the existing on control
    else {
      SharedValidators.clearError(peerUrlControl, 'peerAlive');
    }
  }
}

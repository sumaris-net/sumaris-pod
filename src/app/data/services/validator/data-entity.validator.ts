import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {DataEntity} from "../model/data-entity.model";
import {toBoolean, toNumber} from "../../../shared/functions";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {Optional} from "@angular/core";

export interface DataEntityValidatorOptions {
  isOnFieldMode?: boolean;
}

export abstract class DataEntityValidatorService<T extends DataEntity<T>, O extends DataEntityValidatorOptions = DataEntityValidatorOptions>
  implements ValidatorService {

  protected constructor(
    protected formBuilder: FormBuilder,
    @Optional() protected settings?: LocalSettingsService
    ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: T, opts?: O): FormGroup {

    opts = this.fillDefaultOptions(opts);

    return this.formBuilder.group(
      this.getFormGroupConfig(data, opts),
      this.getFormGroupOptions(data)
    );
  }

  getFormGroupConfig(data?: T, opts?: O): {
    [key: string]: any;
  } {

    return {
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      controlDate: [data && data.controlDate || null],
      qualificationDate: [data && data.qualificationDate || null],
      qualificationComments: [data && data.qualificationComments || null],
      recorderDepartment: [data && data.recorderDepartment || null, SharedValidators.entity]
    };
  }

  getFormGroupOptions(data?: T, opts?: O): {
    [key: string]: any;
  } {
    return {};
  }

  updateFormGroup(formGroup: FormGroup, opts?: O) {
    // Must be override by subclasses
    console.warn(`TODO: Please implement ${this.constructor.name}.updateFormGroup()`);
  }


  /* -- protected methods -- */

  protected fillDefaultOptions(opts?: O): O {
    opts = opts || {} as O;

    opts.isOnFieldMode = toBoolean(opts.isOnFieldMode, this.settings && this.settings.isUsageMode('FIELD') || false);

    return opts;
  }
}

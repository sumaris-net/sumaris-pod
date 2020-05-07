import {Person} from "../../../core/services/model";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {DataEntity, DataRootEntity, DataRootVesselEntity, IWithObserversEntity} from "../model/base.model";
import {Program} from "../../../referential/services/model";
import {toBoolean, toNumber} from "../../../shared/functions";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {Optional} from "@angular/core";

export interface DataEntityValidatorOptions {
  isOnFieldMode?: boolean;
}

export abstract class DataEntityValidatorService<T extends DataEntity<T>, O extends DataEntityValidatorOptions = DataEntityValidatorOptions> implements ValidatorService {

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
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      controlDate: [data && data.controlDate || null],
      qualificationDate: [data && data.qualificationDate || null],
      qualificationComments: [data && data.qualificationComments || null],
      recorderDepartment: [data && data.recorderDepartment || null, SharedValidators.entity]
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

export interface DataRootEntityValidatorOptions extends DataEntityValidatorOptions {
  withObservers?: boolean;
  program?: Program;
}

export abstract class DataRootEntityValidatorService<T extends DataRootEntity<T>, O extends DataRootEntityValidatorOptions = DataRootEntityValidatorOptions>
  extends DataEntityValidatorService<T, O> {

  protected constructor(
    protected formBuilder: FormBuilder,
    @Optional() protected settings?: LocalSettingsService
    ) {
    super(formBuilder, settings);
  }

  getFormGroupConfig(data?: T, opts?: O): {
    [key: string]: any;
  } {

    return Object.assign(
      super.getFormGroupConfig(data),
      {
        program: [data && data.program || null, Validators.compose([Validators.required, SharedValidators.entity])],
        creationDate: [data && data.creationDate || null],
        recorderPerson: [data && data.recorderPerson || null, SharedValidators.entity],
        comments: [data && data.comments || null, Validators.maxLength(2000)],
        synchronizationStatus: [data && data.synchronizationStatus || null]
      });
  }

  getObserversFormArray(data?: IWithObserversEntity<T>) {
    return this.formBuilder.array(
      (data && data.observers || [null]).map(observer => this.getObserverControl(observer)),
      SharedValidators.requiredArrayMinLength(1)
    );
  }

  getObserverControl(observer?: Person): FormControl {
    return this.formBuilder.control(observer || null, [Validators.required, SharedValidators.entity]);
  }
}


export abstract class DataRootVesselEntityValidatorService<T extends DataRootVesselEntity<T>, O extends DataRootEntityValidatorOptions = DataRootEntityValidatorOptions>
  extends DataRootEntityValidatorService<T, O> {

  protected constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormGroupConfig(data?: T, opts?: O): {
    [key: string]: any;
  } {

    return Object.assign(
      super.getFormGroupConfig(data),
      {
        vesselSnapshot: ['', Validators.compose([Validators.required, SharedValidators.entity])]
      });
  }
}

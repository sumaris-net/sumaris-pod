import {
  Person,
} from "../../../core/services/model";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {DataEntity, DataRootEntity, DataRootVesselEntity, IWithObserversEntity} from "../model/base.model";

export abstract class DataEntityValidatorService<T extends DataEntity<T>> implements ValidatorService {

  protected constructor(
    protected formBuilder: FormBuilder
    ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: T): FormGroup {
    return this.formBuilder.group(
      this.getFormConfig(data),
      this.getFormOptions(data)
    );
  }

  getFormConfig(data?: T): {
    [key: string]: any;
  } {

    return {
      id: [''],
      updateDate: [''],
      controlDate: [''],
      qualificationDate: [''],
      qualificationComments: [''],
      recorderDepartment: ['', SharedValidators.entity]
    };
  }

  getFormOptions(data?: T): {
    [key: string]: any;
  } {
    return {};
  }
}


export abstract class DataRootEntityValidatorService<T extends DataRootEntity<T>>
  extends DataEntityValidatorService<T> {

  protected constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormConfig(data?: T): {
    [key: string]: any;
  } {

    return Object.assign(
      super.getFormConfig(data),
      {
        program: ['', Validators.compose([Validators.required, SharedValidators.entity])],
        creationDate: [''],
        recorderPerson: ['', SharedValidators.entity],
        comments: ['', Validators.maxLength(2000)]
      });
  }

  getObserversArray(data?: IWithObserversEntity<T>) {
    return this.formBuilder.array(
      (data && data.observers || []).map(this.getObserverControl),
      SharedValidators.requiredArrayMinLength(1)
    );
  }

  getObserverControl(observer?: Person): FormControl {
    return this.formBuilder.control(observer || '', [Validators.required, SharedValidators.entity]);
  }
}


export abstract class DataRootVesselEntityValidatorService<T extends DataRootVesselEntity<T>>
  extends DataRootEntityValidatorService<T> {

  protected constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormConfig(data?: T): {
    [key: string]: any;
  } {

    return Object.assign(
      super.getFormConfig(data),
      {
        vesselFeatures: ['', Validators.compose([Validators.required, SharedValidators.entity])]
      });
  }
}

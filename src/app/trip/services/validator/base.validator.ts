import {
  Cloneable,
  Entity,
  entityToString,
  fromDateISOString,
  isNil,
  isNotNil,
  LocationLevelIds,
  personsToString,
  personToString,
  referentialToString,
  StatusIds,
  toDateISOString
} from "../../../core/core.module";
import {
  Department,
  EntityUtils,
  GearLevelIds,
  getPmfmName,
  Person,
  PmfmStrategy,
  QualityFlagIds,
  Referential,
  ReferentialRef,
  TaxonGroupIds,
  AcquisitionLevelCodes,
  VesselFeatures,
  vesselFeaturesToString
} from "../../../referential/referential.module";
import {Moment} from "moment/moment";
import {IWithProgramEntity} from "../../../referential/services/model";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {DataEntity, DataRootEntity, DataRootVesselEntity, IWithObserversEntity} from "../model/base.model";
import {Operation} from "../model/trip.model";


export abstract class DataEntityValidatorService<T extends DataEntity<T>> implements ValidatorService {

  protected constructor(
    protected formBuilder: FormBuilder) {
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
      // TODO: FIXME if required, should be set when creating the new entity
      recorderDepartment: ['', SharedValidators.entity]
      //recorderDepartment: ['', Validators.compose([Validators.required, SharedValidators.entity])]
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

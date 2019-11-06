import {ChangeDetectionStrategy, Component, Input, OnInit, ViewChild} from "@angular/core";
import {AppForm, EntityUtils, FormArrayHelper, StatusIds} from "../../core/core.module";
import {AggregationStrata, AggregationType, ExtractionColumn} from "../services/extraction.model";
import {FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {AggregationTypeValidatorService} from "../services/validator/aggregation-type.validator";
import {ReferentialForm} from "../../referential/form/referential.form";
import {BehaviorSubject} from "rxjs";
import {arraySize} from "../../shared/functions";
import {DateAdapter, MatTable} from "@angular/material";
import {Moment} from "moment";
import {LocalSettingsService} from "../../core/services/local-settings.service";

@Component({
  selector: 'app-aggregation-type-form',
  templateUrl: './aggregation-type.form.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AggregationTypeForm extends AppForm<AggregationType> implements OnInit {

  $timeColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $spaceColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $aggColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $techColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  aggFunctions = [
    {
      value: 'SUM',
      name: 'EXTRACTION.AGGREGATION.EDIT.AGG_FUNCTION.SUM'
    },
    {
      value: 'AVG',
      name: 'EXTRACTION.AGGREGATION.EDIT.AGG_FUNCTION.AVG'
    }
  ];

  stratumHelper: FormArrayHelper<AggregationStrata>;

  form: FormGroup;
  stratumFormArray: FormArray;

  @Input()
  showError = true;

  @ViewChild('referentialForm', {static: true}) referentialForm: ReferentialForm;

  get value(): any {
    const json = this.form.value;

    // Re add label, because missing when field disable
    json.label = this.form.get('label').value;

    return json;
  }

  set value(value: any) {
    this.setValue(value);
  }

  constructor(protected dateAdapter: DateAdapter<Moment>,
              protected formBuilder: FormBuilder,
              protected settings: LocalSettingsService,
              protected validatorService: AggregationTypeValidatorService) {
    super(dateAdapter,
      validatorService.getFormGroup(),
      settings);

    // Stratum
    this.stratumHelper = new FormArrayHelper<AggregationStrata>(
      this.formBuilder,
      this.form,
      'stratum',
      (strata) => validatorService.getStrataFormGroup(strata),
      (v1, v2) => (!v1 && !v2) || (v1 && v2 && v1.label === v2.label),
      EntityUtils.isEmpty,
      {
        allowEmptyArray: false
      }
    );
    this.stratumFormArray = this.form.controls.stratum as FormArray;

  }

  ngOnInit(): void {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'AggregationType';

    // Override status list i18n
    this.referentialForm.statusList = [
      {
        id: StatusIds.ENABLE,
        icon: 'eye',
        label: 'EXTRACTION.AGGREGATION.EDIT.STATUS_ENUM.PUBLIC'
      },
      {
        id: StatusIds.TEMPORARY,
        icon: 'eye-off',
        label: 'EXTRACTION.AGGREGATION.EDIT.STATUS_ENUM.PRIVATE'
      },
      {
        id: StatusIds.DISABLE,
        icon: 'close',
        label: 'EXTRACTION.AGGREGATION.EDIT.STATUS_ENUM.DISABLE'
      }
    ];
  }

  /* -- protected -- */

  setValue(data: AggregationType, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {

    // If spatial, load columns
    if (data && data.isSpatial) {
      // If spatial product, make sure there is one strata
      this.stratumHelper.resize(Math.max(1, arraySize(data.stratum)));
    }
    else {
      this.stratumHelper.resize(0);
    }

    super.setValue(data, opts);
  }


}

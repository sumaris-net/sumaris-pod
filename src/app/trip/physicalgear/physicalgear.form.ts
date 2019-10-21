import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {PhysicalGearValidatorService} from "../services/physicalgear.validator";
import {isNotNil, PhysicalGear} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {Subject} from 'rxjs';
import {distinctUntilChanged, filter, mergeMap, tap} from 'rxjs/operators';
import {LocalSettingsService, PlatformService} from '../../core/core.module';
import {
  AcquisitionLevelCodes,
  EntityUtils,
  ProgramService,
  ReferentialRef,
  ReferentialRefService,
  referentialToString
} from "../../referential/referential.module";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {FormBuilder} from "@angular/forms";
import {selectInputContent} from "../../core/form/form.utils";
import {suggestFromArray} from "../../shared/functions";
import {InputElement} from "../../shared/material/focusable";

@Component({
  selector: 'app-physical-gear-form',
  templateUrl: './physicalgear.form.html',
  styleUrls: ['./physicalgear.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearForm extends MeasurementValuesForm<PhysicalGear> implements OnInit {

  private _gears: ReferentialRef[] = [];

  programSubject = new Subject<string>();
  mobile: boolean;

  @Input() showComment = true;

  @Input() tabindex: number;

  @Input()
  set program(value: string) {
    this.programSubject.next(value);
  }

  @ViewChild("firstInput", { static: true }) firstInputField: InputElement;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected platform: PlatformService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected validatorService: PhysicalGearValidatorService,
    protected referentialRefService: ReferentialRefService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd, validatorService.getFormGroup());
    this._enable = true;
    this.mobile = platform.mobile;
    this.requiredGear = true;

    // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.PHYSICAL_GEAR;

    this.registerSubscription(
      this.programSubject
        .pipe(
          filter(isNotNil),
          distinctUntilChanged(),
        )
        .subscribe(async (value) => {
          if (this._program !== value) {
            this._gears = await this.programService.loadGears(value);
            this._program = value;
            if (!this.loading) this._onRefreshPmfms.emit();
          }
        })
    );
  }

  ngOnInit() {
    super.ngOnInit();

    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;

    // Combo: gears
    this.registerAutocompleteConfig('gear', {
      suggestFn: async (value, options) => suggestFromArray<ReferentialRef>(this._gears, value, options),
      showAllOnFocus: false
    });

    this.form.controls['gear'].valueChanges
      .pipe(
        filter(value => EntityUtils.isNotEmpty(value) && !this.loading)
      )
      .subscribe(value => {
        this.data.gear = value;
        this.gear = value.label;
      });
  }

  setValue(data: PhysicalGear, opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    if (data && EntityUtils.isNotEmpty(data.gear)) {
      this.gear = data.gear.label;
    }
    super.setValue(data, opts);
  }

  focusFirstInput() {
    this.firstInputField.focus();
  }

  /* -- protected methods -- */

  protected async safeSetValue(data: PhysicalGear): Promise<void> {
    if (data && EntityUtils.isNotEmpty(data.gear)) {
      this.gear = data.gear.label;
    }
    await super.safeSetValue(data);
  }

  referentialToString = referentialToString;
  selectInputContent = selectInputContent;
}

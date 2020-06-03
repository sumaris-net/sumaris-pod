import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {PhysicalGearValidatorService} from "../services/physicalgear.validator";
import {Moment} from 'moment/moment';
import {BehaviorSubject, Subject} from 'rxjs';
import {distinctUntilChanged, filter} from 'rxjs/operators';
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
import {isNotNil} from "../../shared/functions";
import {InputElement, selectInputContent} from "../../shared/inputs";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {PhysicalGear} from "../services/model/trip.model";
import {DateAdapter} from "@angular/material/core";
import {ReferentialUtils} from "../../core/services/model";

@Component({
  selector: 'app-physical-gear-form',
  templateUrl: './physicalgear.form.html',
  styleUrls: ['./physicalgear.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearForm extends MeasurementValuesForm<PhysicalGear> implements OnInit {

  gearsSubject = new BehaviorSubject<ReferentialRef[]>(undefined);
  programSubject = new Subject<string>();
  mobile: boolean;

  @Input() showComment = true;

  @Input() tabindex: number;

  @Input() canEditRankOrder = false;

  @Input()
  set program(value: string) {
    this.programSubject.next(value);
  }

  @Input()
  set gears(value: ReferentialRef[]) {
    this.gearsSubject.next(value);
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
            this.gearsSubject.next(await this.programService.loadGears(value));
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
    this.registerAutocompleteField('gear', {
      items: this.gearsSubject,
      mobile: this.mobile
    });

    this.form.get('gear').valueChanges
      .pipe(
        filter(value => ReferentialUtils.isNotEmpty(value) && !this.loading)
      )
      .subscribe(value => {
        this.data.gear = value;
        this.gear = value.label;
      });
  }

  setValue(data: PhysicalGear, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; }) {
    if (data && ReferentialUtils.isNotEmpty(data.gear)) {
      this.gear = data.gear.label;
    }
    super.setValue(data, opts);
  }

  focusFirstInput() {
    this.firstInputField.focus();
  }

  /* -- protected methods -- */

  protected async safeSetValue(data: PhysicalGear, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; }): Promise<void> {

    if (data && ReferentialUtils.isNotEmpty(data.gear)) {
      this.gear = data.gear.label;
    }

    await super.safeSetValue(data, opts);
  }

  referentialToString = referentialToString;
  selectInputContent = selectInputContent;
}

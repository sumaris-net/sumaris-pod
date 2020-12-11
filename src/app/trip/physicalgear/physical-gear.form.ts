import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnInit, ViewChild} from '@angular/core';
import {PhysicalGearValidatorService} from "../services/validator/physicalgear.validator";
import {Moment} from 'moment';
import {BehaviorSubject, Subject} from 'rxjs';
import {distinctUntilChanged, filter} from 'rxjs/operators';
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/validator/measurement.validator";
import {FormBuilder} from "@angular/forms";
import {isNotNil} from "../../shared/functions";
import {InputElement, selectInputContent} from "../../shared/inputs";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {PhysicalGear} from "../services/model/trip.model";
import {DateAdapter} from "@angular/material/core";
import {ReferentialRef, referentialToString, ReferentialUtils} from "../../core/services/model/referential.model";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ProgramService} from "../../referential/services/program.service";
import {EnvironmentService} from "../../../environments/environment.class";

@Component({
  selector: 'app-physical-gear-form',
  templateUrl: './physical-gear.form.html',
  styleUrls: ['./physical-gear.form.scss'],
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
    protected referentialRefService: ReferentialRefService,
    @Inject(EnvironmentService) protected environment
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
          distinctUntilChanged()
        )
        .subscribe(async (programLabel) => {
          if (this._program !== programLabel) {
            this.gearsSubject.next(await this.programService.loadGears(programLabel));
            this._program = programLabel as string;
            if (!this.loading) this._onRefreshPmfms.emit();
          }
        })
    );

    this.debug = !environment.production;
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
        this.gearId = value.id;
      });
  }

  setValue(data: PhysicalGear, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; }) {
    if (data && ReferentialUtils.isNotEmpty(data.gear)) {
      this.gearId = data.gear.id;
    }
    super.setValue(data, opts);
  }

  focusFirstInput() {
    this.firstInputField.focus();
  }

  /* -- protected methods -- */

  protected async safeSetValue(data: PhysicalGear, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; }): Promise<void> {

    if (data && ReferentialUtils.isNotEmpty(data.gear)) {
      this.gearId = data.gear.id;
    }

    await super.safeSetValue(data, opts);
  }

  referentialToString = referentialToString;
  selectInputContent = selectInputContent;
}

import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {PhysicalGearValidatorService} from "../services/physicalgear.validator";
import {isNotNil, PhysicalGear} from "../services/trip.model";
import {Moment} from 'moment/moment'
import {DateAdapter} from "@angular/material";
import {merge, Observable, Subject} from 'rxjs';
import {debounceTime, distinct, filter, map, tap} from 'rxjs/operators';
import {AcquisitionLevelCodes, LocalSettingsService, PlatformService} from '../../core/core.module';
import {
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

@Component({
  selector: 'app-physical-gear-form',
  templateUrl: './physicalgear.form.html',
  styleUrls: ['./physicalgear.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearForm extends MeasurementValuesForm<PhysicalGear> implements OnInit {

  private _gears: ReferentialRef[] = [];

  $gears: Observable<ReferentialRef[]>;
  programSubject = new Subject<string>();
  onShowGearDropdown = new Subject<any>();

  mobile: boolean;

  @Input() showComment = true;

  @Input() tabindex: number;

  @Input()
  set program(value: string) {
    this.programSubject.next(value);
  }

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
          distinct()
        )
        .subscribe(async (value) => {
          if (this._program !== value && isNotNil(value)) {
            this._gears = await this.programService.loadGears(value);
            this._program = value;
            if (!this.loading) this._onRefreshPmfms.emit();
          }
        })
    );
  }

  async ngOnInit() {

    // Combo: gears
    this.$gears = merge(
      this.onShowGearDropdown
        .pipe(map((_) => "*")),
      this.form.controls['gear'].valueChanges
        .pipe(debounceTime(250))
    )
      .pipe(
        map((value: any) => {
          if (EntityUtils.isNotEmpty(value)) {
            return [value];
          }
          value = (typeof value === "string" && value !== '*') && value || undefined;
          if (!value) return this._gears; // all gears
          // Search on label or name
          const ucValue = value.toUpperCase();
          return this._gears.filter(g =>
            (g.label && g.label.toUpperCase().indexOf(ucValue) === 0)
            || (g.name && g.name.toUpperCase().indexOf(ucValue) !== -1)
          );
        }),
        // Save implicit value
        tap(res => this.updateImplicitValue('gear', res))
      );

    this.form.controls['gear'].valueChanges
      .filter(value => EntityUtils.isNotEmpty(value) && !this.loading)
      .subscribe(value => {
        this.data.gear = value;
        this.gear = value.label;
      });
  }

  public setValue(data: PhysicalGear) {
    if (data && EntityUtils.isNotEmpty(data.gear)) {
      this.gear = data.gear.label;
    }
    super.setValue(data);
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

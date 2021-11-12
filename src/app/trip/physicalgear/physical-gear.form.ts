import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, EventEmitter, Input, OnInit, Output, QueryList, ViewChild, ViewChildren } from '@angular/core';
import {PhysicalGearValidatorService} from "../services/validator/physicalgear.validator";
import {Moment} from 'moment';
import { BehaviorSubject, merge } from 'rxjs';
import { distinctUntilChanged, filter, map, mergeMap, tap } from 'rxjs/operators';
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {MeasurementsValidatorService} from "../services/validator/measurement.validator";
import {FormBuilder} from "@angular/forms";
import { GetFocusableInputOptions, isNotNil, toNumber } from '@sumaris-net/ngx-components';
import {InputElement, selectInputContent} from "@sumaris-net/ngx-components";
import {PlatformService}  from "@sumaris-net/ngx-components";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {PhysicalGear} from "../services/model/trip.model";
import {DateAdapter} from "@angular/material/core";
import {ReferentialRef, focusNextInput, ReferentialUtils}  from "@sumaris-net/ngx-components";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {environment} from "../../../environments/environment";
import {ProgramRefService} from "../../referential/services/program-ref.service";

@Component({
  selector: 'app-physical-gear-form',
  templateUrl: './physical-gear.form.html',
  styleUrls: ['./physical-gear.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearForm extends MeasurementValuesForm<PhysicalGear> implements OnInit {

  gearsSubject = new BehaviorSubject<ReferentialRef[]>(undefined);
  mobile: boolean;

  @Input() showComment = true;
  @Input() tabindex: number;
  @Input() canEditRankOrder = false;

  @Input()
  set gears(value: ReferentialRef[]) {
    this.gearsSubject.next(value);
  }

  @Output() onSubmit = new EventEmitter<any>();

  @ViewChild("firstInput", { static: true }) firstInputField: InputElement;
  @ViewChildren('inputField') inputFields: QueryList<ElementRef>;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected platform: PlatformService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected validatorService: PhysicalGearValidatorService,
    protected referentialRefService: ReferentialRefService,
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programRefService, settings, cd, validatorService.getFormGroup(), {
      //allowSetValueBeforePmfms: true
    });
    this._enable = true;
    this.mobile = platform.mobile;
    this.requiredGear = true;

    // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.PHYSICAL_GEAR;

    // Load gears from program
    this.registerSubscription(
      this.$programLabel
        .pipe(
          filter(isNotNil),
          distinctUntilChanged(),
          mergeMap(program => this.programRefService.loadGears(program))
        )
        .subscribe(gears => this.gearsSubject.next(gears))
    );

    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.tabindex = toNumber(this.tabindex, 1);

    // Combo: gears
    this.registerAutocompleteField('gear', {
      items: this.gearsSubject,
      mobile: this.mobile
    });

    // Propage data.gear into gearId
    this.registerSubscription(
      this.form.get('gear').valueChanges
        .pipe(
          filter(ReferentialUtils.isNotEmpty)
        )
        .subscribe(gear => {
          this.data = this.data || new PhysicalGear();
          this.data.gear = gear;
          this.gearId = gear.id;
        })
    );
  }

  focusFirstInput() {
    this.firstInputField.focus();
  }

  focusNextInput(event: UIEvent, opts?: Partial<GetFocusableInputOptions>): boolean {

    // DEBUG
    //return focusNextInput(event, this.inputFields, opts{debug: this.debug, ...opts});

    return focusNextInput(event, this.inputFields, opts);
  }

  /* -- protected methods -- */

  protected onApplyingEntity(data: PhysicalGear, opts?: {[key: string]: any;}) {

    if (!data) return; // Skip

    super.onApplyingEntity(data, opts);

    // Propage the gear
    if (ReferentialUtils.isNotEmpty(data.gear)) {
      this.gearId = data.gear.id;
    }
  }

  selectInputContent = selectInputContent;
}

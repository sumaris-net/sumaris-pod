import {Component, Input} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {TranslateService} from "@ngx-translate/core";
import {FormBuilder, Validators} from "@angular/forms";
import {AppFormUtils} from "../../../core/form/form.utils";
import {AppForm} from "../../../core/form/form.class";
import {DateAdapter} from "@angular/material/core";
import * as momentImported from "moment";
import {Moment} from "moment";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {ReferentialRefService} from "../../../referential/services/referential-ref.service";
import {PlatformService} from "../../../core/services/platform.service";
import {SharedValidators} from "../../../shared/validator/validators";
import {ObservedLocationOfflineFilter} from "../../services/observed-location.service";
import {LocationLevelIds} from "../../../referential/services/model/model.enum";
import {ProgramRefService} from "../../../referential/services/program-ref.service";
import DurationConstructor = moment.unitOfTime.DurationConstructor;

const moment = momentImported;

@Component({
  selector: 'app-observed-location-offline-modal',
  styleUrls: [
    './observed-location-offline.modal.scss'
  ],
  templateUrl: './observed-location-offline.modal.html'
})
export class ObservedLocationOfflineModal extends AppForm<ObservedLocationOfflineFilter> {

  loading = true;
  mobile: boolean;

  periodDurations: { value: number; unit: DurationConstructor; }[] = [
    { value: 1, unit: 'week' },
    { value: 15, unit: 'day' },
    { value: 1,  unit: 'month' },
    { value: 3,  unit: 'month' },
    { value: 6,  unit: 'month' }
  ];
  periodDurationLabels: { key: string; label: string; value: Moment; }[];

  @Input() title = 'OBSERVED_LOCATION.OFFLINE_MODAL.TITLE';

  get value(): any {
    return this.getValue();
  }

  set value(data: any) {
    this.setValue(data);
  }

  get valid(): boolean {
    return this.form.valid;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected viewCtrl: ModalController,
    protected translate: TranslateService,
    protected formBuilder: FormBuilder,
    protected platform: PlatformService,
    protected programRefService: ProgramRefService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter,
      formBuilder.group({
        program: [null, Validators.compose([Validators.required, SharedValidators.entity])],
        enableHistory: [true, Validators.required],
        location: [null, SharedValidators.entity],
        periodDuration: ['15day', Validators.required],
      }),
      settings);
    this._enable = true; // Enable by default
    this.mobile = platform.mobile;

    // Prepare start date items
    const datePattern = translate.instant('COMMON.DATE_PATTERN');
    this.periodDurationLabels = this.periodDurations.map(v => {
      const date = moment().utc(false)
        .add(-1 * v.value, v.unit);
      return {
        key: `${v.value} ${v.unit}`,
        label: `${date.fromNow(true/*no suffix*/)} (${date.format(datePattern)})`,
        value: date.startOf('day')
      };
    });
  }

  ngOnInit() {
    super.ngOnInit();

    // Program
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Program'
      },
      mobile: this.mobile
    });

    // Location
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelIds: [LocationLevelIds.AUCTION, LocationLevelIds.PORT]
      },
      mobile: this.mobile
    });

    // Enable/disable sub controls, from the 'enable history' checkbox
    const subControls = [
      this.form.get('program'),
      this.form.get('location'),
      this.form.get('startDate')
    ];
    this.form.get('enableHistory').valueChanges.subscribe(enable => {
      if (enable) {
        subControls.forEach(control => {
          control.enable();
          control.setValidators(Validators.required);
        });
      }
      else {
        subControls.forEach(control => {
          control.disable();
          control.setValidators(null);
        });
      }
    });

  }

  async setValue(value: ObservedLocationOfflineFilter | any) {
    if (!value) return; // skip

    const json = {
      program: null,
      location: null,
      periodDuration: null
    };
    // Program
    if (value.programLabel) {
      json.program = await this.programRefService.loadByLabel(value.programLabel);
    }

    // Location
    if (value.locationId) {
      json.location = await this.referentialRefService.loadById(value.locationId, 'Location');
    }

    // Duration period
    if (value.periodDuration && value.periodDurationUnit) {
      json.periodDuration = `${value.periodDuration} ${value.periodDurationUnit}`;
    }

    this.form.patchValue(json);
  }

  getValue(): ObservedLocationOfflineFilter {
    const json = this.form.value;

    // DEBUG
    console.debug("[observed-location-offline] Modal form.value:", json);

    const value = new ObservedLocationOfflineFilter();

    // Set program
    value.programLabel = json.program && json.program.label || json.program;

    // Location
    value.locationId = json.location && json.location.id || json.location;

    // Set start date
    if (json.enableHistory && json.periodDuration) {
      const startDateItem = this.periodDurationLabels.find(item => item.key === json.periodDuration);
      value.startDate = startDateItem && startDateItem.value;

      // Keep value of periodDuration (to be able to save it in local settings)
      const parts = json.periodDuration.split(' ');
      value.periodDuration = +parts[0];
      value.periodDurationUnit = parts[1] as DurationConstructor;
    }

    // DEBUG
    console.debug("[observed-location-offline] Modal result value:", value);


    return value;
  }

  cancel() {
    this.viewCtrl.dismiss(null, 'CANCEL');
  }

  async validate(event?: UIEvent) {
    this.form.markAllAsTouched();

    if (!this.form.valid) {
      await AppFormUtils.waitWhilePending(this.form);
      if (this.form.invalid) {
        AppFormUtils.logFormErrors(this.form, '[offline-import-config] ');
        return; // stop
      }
    }

    return this.viewCtrl.dismiss(this.getValue(), 'OK');
  }

}

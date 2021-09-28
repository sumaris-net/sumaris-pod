import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input} from '@angular/core';
import {ModalController} from '@ionic/angular';
import {TranslateService} from '@ngx-translate/core';
import {FormBuilder, Validators} from '@angular/forms';
import {AppForm, AppFormUtils, LocalSettingsService, PlatformService, SharedValidators} from '@sumaris-net/ngx-components';
import {DateAdapter} from '@angular/material/core';
import * as momentImported from 'moment';
import {Moment} from 'moment';
import {ReferentialRefService} from '../../../referential/services/referential-ref.service';
import {ProgramRefQueries, ProgramRefService} from '../../../referential/services/program-ref.service';
import {Program} from '../../../referential/services/model/program.model';
import {TripOfflineFilter} from '@app/trip/services/filter/trip.filter';
import DurationConstructor = moment.unitOfTime.DurationConstructor;

const moment = momentImported;

@Component({
  selector: 'app-trip-offline-modal',
  styleUrls: [
    './trip-offline.modal.scss'
  ],
  templateUrl: './trip-offline.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripOfflineModal extends AppForm<TripOfflineFilter> {

  loading = true;
  mobile: boolean;

  periodDurations: { value: number; unit: DurationConstructor; }[] = [
    {value: 1, unit: 'week'},
    {value: 15, unit: 'day'},
    {value: 1, unit: 'month'},
    {value: 3, unit: 'month'},
    {value: 6, unit: 'month'}
  ];
  periodDurationLabels: { key: string; label: string; startDate: Moment; }[];

  @Input() title = 'TRIP.OFFLINE_MODAL.TITLE';

  get value(): any {
    return this.getValue();
  }

  set value(data: any) {
    this.setValue(data);
  }

  get valid(): boolean {
    return this.form.valid;
  }

  markAsLoaded() {
    if (this.loading) {
      this.loading = false;
      this.markForCheck();
    }
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected viewCtrl: ModalController,
    protected translate: TranslateService,
    protected formBuilder: FormBuilder,
    protected platform: PlatformService,
    protected programRefService: ProgramRefService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter,
      formBuilder.group({
        program: [null, Validators.compose([Validators.required, SharedValidators.entity])],
        periodDuration: ['15day', Validators.required],
      }),
      settings);
    this._enable = false; // Disable by default
    this.mobile = platform.mobile;

    // Prepare start date items
    const datePattern = translate.instant('COMMON.DATE_PATTERN');
    this.periodDurationLabels = this.periodDurations.map(v => {
      const date = moment().utc(false)
        .add(-1 * v.value, v.unit); // Substract the period, from now
      return {
        key: `${v.value} ${v.unit}`,
        label: `${date.fromNow(true/*no suffix*/)} (${date.format(datePattern)})`,
        startDate: date.startOf('day') // Reset time
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
  }

  async setValue(value: TripOfflineFilter | any) {
    if (!value) return; // skip

    const json = {
      program: null,
      periodDuration: null
    };
    // Program
    if (value.programLabel) {
      json.program = await this.programRefService.loadByLabel(value.programLabel, {query: ProgramRefQueries.loadLight});
    }

    // Duration period
    if (value.periodDuration && value.periodDurationUnit) {
      json.periodDuration = `${value.periodDuration} ${value.periodDurationUnit}`;
    }

    this.form.patchValue(json);

    this.enable();
    this.markAsLoaded();
  }

  getValue(): TripOfflineFilter {
    const json = this.form.value;

    // DEBUG
    console.debug('[trip-offline] Modal form.value:', json);

    const value = new TripOfflineFilter();

    // Set program
    value.programLabel = json.program && json.program.label || json.program;

    // Set start date
    if (json.periodDuration) {
      const periodDuration = this.periodDurationLabels.find(item => item.key === json.periodDuration);
      value.startDate = periodDuration && periodDuration.startDate;

      // Keep value of periodDuration (to be able to save it in local settings)
      const parts = json.periodDuration.split(' ');
      value.periodDuration = +parts[0];
      value.periodDurationUnit = parts[1] as DurationConstructor;
    }

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

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

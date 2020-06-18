import {ChangeDetectorRef, Component, EventEmitter, Input, OnInit} from '@angular/core';
import {AppForm, isNil, isNotNil} from "../../core/core.module";
import {AggregatedLanding} from "../services/model/aggregated-landing.model";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {FormBuilder} from "@angular/forms";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ModalController} from "@ionic/angular";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {NetworkService} from "../../core/services/network.service";
import {AggregatedLandingValidatorService} from "../services/validator/aggregated-landing.validator";
import {BehaviorSubject} from "rxjs";
import {firstNotNilPromise} from "../../shared/observables";
import {filter} from "rxjs/operators";
import * as moment from "moment";
import {ObservedLocation} from "../services/model/observed-location.model";
import {AggregatedLandingService} from "../services/aggregated-landing.service";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";

@Component({
  selector: 'app-aggregated-landings',
  templateUrl: './aggregated-landings.form.html',
  styleUrls: ['./aggregated-landings.form.scss'],
})
export class AggregatedLandingsForm extends AppForm<AggregatedLanding[]> implements OnInit {



  @Input() showError = true;

  mobile: boolean;
  $loadingControls = new BehaviorSubject<boolean>(false);
  controlsLoaded = false;
  onRefresh = new EventEmitter<any>();
  private _onRefreshControls = new EventEmitter<any>();
  private _onRefreshStrategy = new EventEmitter<any>();
  private _program: string;
  private _acquisitionLevel: string;
  private _nbDays: number;
  private _startDate: Moment;
  dates: Moment[];

  set nbDays(value: number) {
    if (value && value !== this._nbDays) {
      this._nbDays = value;
      this._onRefreshControls.emit();
    }
  }

  set startDate(value: Moment) {
    if (value && (!this._startDate || !value.isSame(this._startDate, "day"))) {
      this._startDate = value;
      this._onRefreshControls.emit();
    }
  }

  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
      this._program = value;
      this._onRefreshStrategy.emit();
    }
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value && isNotNil(value)) {
      this._acquisitionLevel = value;
      this._onRefreshStrategy.emit();
    }
  }

  set value(data: any) {
    this.safeSetValue(data);
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected dataService: AggregatedLandingService,
    protected validatorService: AggregatedLandingValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    public network: NetworkService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getFormGroup(null), settings);
    this.mobile = this.settings.mobile;

    this._acquisitionLevel = AcquisitionLevelCodes.LANDING; // default
  }

  ngOnInit() {

    this.registerSubscription(
      this._onRefreshControls
        .subscribe(() => this.refreshControls('constructor'))
    );

    this.registerSubscription(
      this._onRefreshStrategy
        .subscribe(() => this.refreshStrategy('constructor'))
    );

  }

  // protected async loaded(): Promise<any> {
  //   if (!this.loading) return true;
  //   do {
  //     await delay(100);
  //   } while (!this.loading);
  //   return true;
  // }

  async ready(): Promise<void> {
    // Wait pmfms load, and controls load
    if (this.$loadingControls.getValue() === true && this.controlsLoaded === false) {
      if (this.debug) console.debug(`[aggregated-landings-form] waiting form to be ready...`);
      await firstNotNilPromise(this.$loadingControls
        .pipe(
          filter((loadingControls) => loadingControls === false && this.controlsLoaded === true)
        ));
    }
  }


  protected markForCheck() {
    this.cd.markForCheck();
  }

  private refreshControls(event?: any) {
    if (isNil(this._startDate) || isNil(this._nbDays)) {
      return;
    }
    if (this.$loadingControls.getValue() !== true) this.$loadingControls.next(true);
    if (this.debug) console.debug('[aggregated-landings-form] UpdateControls from ' + event);

    // Refresh main controls
    const dates: Moment[] = [];
    for (let d = 0; d < this._nbDays; d++) {
      dates[d] = moment(this._startDate).add(d, "day");
    }
    this.dates = dates;

    this.controlsLoaded = true;
    this.$loadingControls.next(false);
    if (this.debug) console.debug('[aggregated-landings-form] Controls updated');
  }

  private refreshStrategy(event?: any) {
    if (isNil(this._program) || isNil(this._acquisitionLevel)) {
      return;
    }

    // Refresh strategy

  }

  addVessel() {
    // TODO
  }

  setParent(data: ObservedLocation) {
    if (!data) {
      this.value = [];
    } else {
      this.startDate = data.startDateTime;
      // this.dataService.loadAll(undefined, undefined, undefined, undefined,
      //   {
      //     programLabel: this._program,
      //     locationId: data.location.id,
      //     startDate: data.startDateTime,
      //     endDate: data.endDateTime || moment(data.startDateTime).add(this._nbDays, "day")
      //   }).then(res => this.value = res.data);
    }
  }

  private async safeSetValue(data: any) {

    await this.ready();

    this.setValue(data, {emitEvent: true});

  }


  // setValue(data: AggregatedLanding[], opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
  //   super.setValue(data, opts);
  // }
}

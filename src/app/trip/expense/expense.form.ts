import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {FormBuilder} from "@angular/forms";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {MeasurementsForm} from "../measurement/measurements.form.component";
import {isNotNil, PmfmStrategy} from "../../referential/services/model";
import {ProgramService} from "../../referential/services/program.service";
import {filterNotNil} from "../../shared/observables";
import {PlatformService} from "../../core/services/platform.service";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {BehaviorSubject} from "rxjs";
import {remove, removeAll} from "../../shared/functions";

@Component({
  selector: 'app-expense-form',
  templateUrl: './expense.form.html',
  styleUrls: ['./expense.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseForm extends MeasurementsForm implements OnInit {

  mobile: boolean;
  $totalPmfm = new BehaviorSubject<PmfmStrategy>(undefined);
  $fuelTypePmfm = new BehaviorSubject<PmfmStrategy>(undefined);
  $fuelPmfms = new BehaviorSubject<PmfmStrategy[]>([]);
  $engineOilPmfms = new BehaviorSubject<PmfmStrategy[]>([]);
  $hydraulicOilPmfms = new BehaviorSubject<PmfmStrategy[]>([]);
  $icePmfms = new BehaviorSubject<PmfmStrategy[]>([]);
  $baitPmfms = new BehaviorSubject<PmfmStrategy[]>([]);
  $miscPmfms = new BehaviorSubject<PmfmStrategy[]>([]);

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementsValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected programService: ProgramService,
    protected platform: PlatformService
  ) {
    super(dateAdapter, measurementsValidatorService, formBuilder, programService, settings, cd);
    this.mobile = platform.mobile;

  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(filterNotNil(this.$pmfms).subscribe(pmfms => {

      // Wait form controls ready
      this.ready().then(() => {
        const expensePmfms: PmfmStrategy[] = pmfms.slice();
        // dispatch pmfms and data
        this.$totalPmfm.next(remove(expensePmfms, this.mapTotalPmfm));
        this.$fuelTypePmfm.next(remove(expensePmfms, this.mapFuelTypePmfm));
        this.$fuelPmfms.next(removeAll(expensePmfms, this.mapFuelPmfms));
        this.$engineOilPmfms.next(removeAll(expensePmfms, this.mapEngineOilPmfms));
        this.$hydraulicOilPmfms.next(removeAll(expensePmfms, this.mapHydraulicPmfms));
        this.$icePmfms.next(removeAll(expensePmfms, this.mapIcePmfms));
        this.$baitPmfms.next(removeAll(expensePmfms, this.mapBaitPmfms));
        this.$miscPmfms.next(expensePmfms);

      });

    }));


  }

  mapTotalPmfm(pmfm: PmfmStrategy): boolean {
    return pmfm.label === 'TOTAL_COST';
  }

  mapFuelTypePmfm(pmfm: PmfmStrategy): boolean {
    return pmfm.label === 'FUEL_TYPE';
  }

  mapFuelPmfms(pmfm: PmfmStrategy): boolean {
    return pmfm.label.startsWith('FUEL_');
  }

  mapEngineOilPmfms(pmfm: PmfmStrategy): boolean {
    return pmfm.label.startsWith('ENGINE_OIL_');
  }

  mapHydraulicPmfms(pmfm: PmfmStrategy): boolean {
    return pmfm.label.startsWith('HYDRAULIC_OIL_');
  }

  mapIcePmfms(pmfm: PmfmStrategy): boolean {
    return pmfm.label.startsWith('ICE_');
  }

  // mapMiscPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
  //   return pmfms.filter(pmfm => pmfm.label === 'LANDING_COST' || pmfm.label === 'FOOD_COST' || pmfm.label === 'GEAR_LOST_COST' || pmfm.label === 'OTHER_COST');
  // }

  mapBaitPmfms(pmfm: PmfmStrategy): boolean {
    return pmfm.label.startsWith('BAIT_');
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

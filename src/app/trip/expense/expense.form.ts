import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {AppForm, isNotNil} from "../../core/core.module";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {FormBuilder} from "@angular/forms";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {MeasurementsForm} from "../measurement/measurements.form.component";
import {AcquisitionLevelCodes, PmfmStrategy} from "../../referential/services/model";
import {Measurement} from "../services/model/measurement.model";
import {ProgramService} from "../../referential/services/program.service";
import {BehaviorSubject, combineLatest, forkJoin, Observable, pipe} from "rxjs";
import {filterNotNil} from "../../shared/observables";

@Component({
  selector: 'app-expense-form',
  templateUrl: './expense.form.html',
  styleUrls: ['./expense.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseForm extends AppForm<Measurement[]> implements OnInit {

  @Input() showError = false;

  @ViewChild('globalMeasurementsForm', {static: true}) globalMeasurementsForm: MeasurementsForm;
  @ViewChild('fuelMeasurementsForm', {static: true}) fuelMeasurementsForm: MeasurementsForm;
  @ViewChild('engineOilMeasurementsForm', {static: true}) engineOilMeasurementsForm: MeasurementsForm;
  @ViewChild('hydraulicOilMeasurementsForm', {static: true}) hydraulicOilMeasurementsForm: MeasurementsForm;
  @ViewChild('iceMeasurementsForm', {static: true}) iceMeasurementsForm: MeasurementsForm;
  @ViewChild('miscMeasurementsForm', {static: true}) miscMeasurementsForm: MeasurementsForm;

  private $expensePmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  private $data = new BehaviorSubject<Measurement[]>(undefined);

  @Input()
  set program(program: string) {
    if (isNotNil(program)) {
      this.loadPmfms(program);
    }
  }

  set value(data: any) {
    this.setValue(data);
  }

  get value(): any {
    return this.$data.getValue(); // todo recompose all measurements
  }

  get error(): string {
    return super.error
    || this.globalMeasurementsForm.error
    || this.fuelMeasurementsForm.error
    || this.engineOilMeasurementsForm.error
    || this.hydraulicOilMeasurementsForm.error
    || this.iceMeasurementsForm.error
    || this.miscMeasurementsForm.error
    ;
  }

  set error(error) {
    super.error = error;
  }

  get invalid(): boolean {
    return super.invalid
      || !this.globalMeasurementsForm || this.globalMeasurementsForm.invalid
      || !this.fuelMeasurementsForm || this.fuelMeasurementsForm.invalid
      || !this.engineOilMeasurementsForm || this.engineOilMeasurementsForm.invalid
      || !this.hydraulicOilMeasurementsForm || this.hydraulicOilMeasurementsForm.invalid
      || !this.iceMeasurementsForm || this.iceMeasurementsForm.invalid
      || !this.miscMeasurementsForm || this.miscMeasurementsForm.invalid
      ;
  }

  get dirty(): boolean {
    return super.dirty
      || (this.globalMeasurementsForm && this.globalMeasurementsForm.dirty)
      || (this.fuelMeasurementsForm && this.fuelMeasurementsForm.dirty)
      || (this.engineOilMeasurementsForm && this.engineOilMeasurementsForm.dirty)
      || (this.hydraulicOilMeasurementsForm && this.hydraulicOilMeasurementsForm.dirty)
      || (this.iceMeasurementsForm && this.iceMeasurementsForm.dirty)
      || (this.miscMeasurementsForm && this.miscMeasurementsForm.dirty)
      ;
  }

  get pending(): boolean {
    return super.pending
      || (!this.globalMeasurementsForm || (this.globalMeasurementsForm.dirty && this.globalMeasurementsForm.pending))
      || (!this.fuelMeasurementsForm || (this.fuelMeasurementsForm.dirty && this.fuelMeasurementsForm.pending))
      || (!this.engineOilMeasurementsForm || (this.engineOilMeasurementsForm.dirty && this.engineOilMeasurementsForm.pending))
      || (!this.hydraulicOilMeasurementsForm || (this.hydraulicOilMeasurementsForm.dirty && this.hydraulicOilMeasurementsForm.pending))
      || (!this.iceMeasurementsForm || (this.iceMeasurementsForm.dirty && this.iceMeasurementsForm.pending))
      || (!this.miscMeasurementsForm || (this.miscMeasurementsForm.dirty && this.miscMeasurementsForm.pending))
      ;
  }

  get valid(): boolean {
    return super.valid
      && this.globalMeasurementsForm && this.globalMeasurementsForm.valid
      && this.fuelMeasurementsForm && this.fuelMeasurementsForm.valid
      && this.engineOilMeasurementsForm && this.engineOilMeasurementsForm.valid
      && this.hydraulicOilMeasurementsForm && this.hydraulicOilMeasurementsForm.valid
      && this.iceMeasurementsForm && this.iceMeasurementsForm.valid
      && this.miscMeasurementsForm && this.miscMeasurementsForm.valid
      ;
  }

  get empty(): boolean {
    return super.empty
      && (!this.globalMeasurementsForm || (!this.globalMeasurementsForm.dirty && !this.globalMeasurementsForm.form.touched))
      && (!this.fuelMeasurementsForm || (!this.fuelMeasurementsForm.dirty && !this.fuelMeasurementsForm.form.touched))
      && (!this.engineOilMeasurementsForm || (!this.engineOilMeasurementsForm.dirty && !this.engineOilMeasurementsForm.form.touched))
      && (!this.hydraulicOilMeasurementsForm || (!this.hydraulicOilMeasurementsForm.dirty && !this.hydraulicOilMeasurementsForm.form.touched))
      && (!this.iceMeasurementsForm || (!this.iceMeasurementsForm.dirty && !this.iceMeasurementsForm.form.touched))
      && (!this.miscMeasurementsForm || (!this.miscMeasurementsForm.dirty && !this.miscMeasurementsForm.form.touched))
      ;
  }

  disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    super.disable(opts);
    this.globalMeasurementsForm.disable(opts);
    this.fuelMeasurementsForm.disable(opts);
    this.engineOilMeasurementsForm.disable(opts);
    this.hydraulicOilMeasurementsForm.disable(opts);
    this.iceMeasurementsForm.disable(opts);
    this.miscMeasurementsForm.disable(opts);
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    super.enable(opts);
    this.globalMeasurementsForm.enable(opts);
    this.fuelMeasurementsForm.enable(opts);
    this.engineOilMeasurementsForm.enable(opts);
    this.hydraulicOilMeasurementsForm.enable(opts);
    this.iceMeasurementsForm.enable(opts);
    this.miscMeasurementsForm.enable(opts);
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected programService: ProgramService
  ) {
    super(dateAdapter, formBuilder.group({}), settings);
  }

  ngOnInit() {

    this.registerSubscription(
      combineLatest([filterNotNil(this.$expensePmfms), filterNotNil(this.$data)])
      .subscribe(([expensePmfms, data]) => {
        // dispatch pmfms and data
        this.setValueToForm(this.globalMeasurementsForm, expensePmfms, this.mapGlobalPmfms, data);
        this.setValueToForm(this.fuelMeasurementsForm, expensePmfms, this.mapFuelPmfms, data);
        this.setValueToForm(this.engineOilMeasurementsForm, expensePmfms, this.mapEngineOilPmfms, data);
        this.setValueToForm(this.hydraulicOilMeasurementsForm, expensePmfms, this.mapHydraulicPmfms, data);
        this.setValueToForm(this.iceMeasurementsForm, expensePmfms, this.mapIcePmfms, data);
        this.setValueToForm(this.miscMeasurementsForm, expensePmfms, this.mapMiscPmfms, data);
      })
    );

  }

  private loadPmfms(program: string) {
    this.programService.loadProgramPmfms(program, {acquisitionLevel: AcquisitionLevelCodes.EXPENSE})
      .then(expensePmfms => this.$expensePmfms.next(expensePmfms));
  }

  private setValueToForm(form: MeasurementsForm, pmfms: PmfmStrategy[], mapFn: (pmfms: PmfmStrategy[]) => PmfmStrategy[], data: Measurement[]) {
    const filteredPmfms = mapFn(pmfms);
    form.setPmfms(filteredPmfms);
    form.value = this.filterData(data, filteredPmfms);
  }


  setValue(data: Measurement[], opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    // don't call super
    this.$data.next(data);
  }

  filterData(data: Measurement[], pmfms: PmfmStrategy[]): Measurement[] {
    const pmfmIds = pmfms.map(pmfm => pmfm.pmfmId);
    return data.filter(value => pmfmIds.includes(value.pmfmId));
  }

  mapGlobalPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    return pmfms.filter(pmfm => pmfm.label === 'TOTAL_COST');
  }

  mapFuelPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    return pmfms.filter(pmfm => pmfm.label.startsWith('FUEL_'));
  }

  mapEngineOilPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    return pmfms.filter(pmfm => pmfm.label.startsWith('ENGINE_OIL_'));
  }

  mapHydraulicPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    return pmfms.filter(pmfm => pmfm.label.startsWith('HYDRAULIC_OIL_'));
  }

  mapIcePmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    return pmfms.filter(pmfm => pmfm.label.startsWith('ICE_'));
  }

  mapMiscPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    return pmfms.filter(pmfm => pmfm.label === 'LANDING_COST' || pmfm.label === 'FOOD_COST' || pmfm.label === 'GEAR_LOST_COST' || pmfm.label === 'OTHER_COST');
  }

  mapBaitPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    return pmfms.filter(pmfm => pmfm.label.startsWith('BAIT_'));
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

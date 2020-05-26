import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {FormBuilder, FormControl, Validators} from "@angular/forms";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {MeasurementsForm} from "../measurement/measurements.form.component";
import {PmfmStrategy} from "../../referential/services/model";
import {ProgramService} from "../../referential/services/program.service";
import {filterNotNil} from "../../shared/observables";
import {PlatformService} from "../../core/services/platform.service";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {BehaviorSubject} from "rxjs";
import {filterNumberInput, isNotEmptyArray, isNotNilOrNaN, remove, removeAll, round, selectInputContent} from "../../shared/functions";
import {ObjectMap} from "../../core/services/model";
import {debounceTime, filter} from "rxjs/operators";
import {Measurement} from "../services/model/measurement.model";
import {SharedValidators} from "../../shared/validator/validators";
import {ExpenseValidatorService} from "../services/validator/expense.validator";

type TupleType = 'quantity' | 'unitPrice' | 'total';

class TupleValue {
  computed: boolean;
  type: TupleType;
}

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
  $fuelPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  fuelTuple: ObjectMap<TupleValue> = undefined;
  $engineOilPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  engineOilTuple: ObjectMap<TupleValue> = undefined;
  $hydraulicOilPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  hydraulicOilTuple: ObjectMap<TupleValue> = undefined;
  $iceTotalPmfm = new BehaviorSubject<PmfmStrategy>(undefined);
  $iceTypePmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  $baitPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  $miscPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  calculating = false;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected expenseValidatorService: ExpenseValidatorService,
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected programService: ProgramService,
    protected platform: PlatformService
  ) {
    super(dateAdapter, expenseValidatorService, formBuilder, programService, settings, cd);
    this.mobile = platform.mobile;
    this.keepRankOrder = true;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(filterNotNil(this.$pmfms).subscribe(pmfms => {

      // Wait form controls ready
      this.ready().then(() => {
        const expensePmfms: PmfmStrategy[] = pmfms.slice();
        // dispatch pmfms
        this.$totalPmfm.next(remove(expensePmfms, this.mapTotalPmfm));
        this.$fuelTypePmfm.next(remove(expensePmfms, this.mapFuelTypePmfm));

        this.$fuelPmfms.next(removeAll(expensePmfms, this.mapFuelPmfms));
        this.fuelTuple = this.getValidTuple(this.$fuelPmfms.getValue());
        this.registerTupleSubscription(this.fuelTuple);

        this.$engineOilPmfms.next(removeAll(expensePmfms, this.mapEngineOilPmfms));
        this.engineOilTuple = this.getValidTuple(this.$engineOilPmfms.getValue());
        this.registerTupleSubscription(this.engineOilTuple);

        this.$hydraulicOilPmfms.next(removeAll(expensePmfms, this.mapHydraulicPmfms));
        this.hydraulicOilTuple = this.getValidTuple(this.$hydraulicOilPmfms.getValue());
        this.registerTupleSubscription(this.hydraulicOilTuple);

        this.initIcePmfms(removeAll(expensePmfms, this.mapIcePmfms));


        this.$baitPmfms.next(removeAll(expensePmfms, this.mapBaitPmfms));
        // remaining pmfms go to miscellaneous part
        this.$miscPmfms.next(expensePmfms);


      });

    }));

    // ice type
    this.registerAutocompleteField('iceType', {
      showAllOnFocus: true,
      items: this.$iceTypePmfms,
      attributes: ['unitLabel'],
      columnNames: ['REFERENTIAL.PMFM.UNIT']
    });
  }

  protected getValue(): Measurement[] {
    const values = super.getValue();

    // reset computed values from tuples
    this.resetComputedValues(values, this.fuelTuple);
    this.resetComputedValues(values, this.engineOilTuple);
    this.resetComputedValues(values, this.hydraulicOilTuple);

    // parse ice values
    const iceTypePmfms: PmfmStrategy[] = this.$iceTypePmfms.getValue() || []
    if (iceTypePmfms.length) {
      iceTypePmfms.forEach(iceTypePmfm => {
        const value = values.find(value => value.pmfmId === iceTypePmfm.pmfmId)
        if (value) {
          if (this.form.value.iceType && this.form.value.iceType.pmfmId === value.pmfmId) {
            value.numericalValue = this.form.value.iceAmount;
          } else {
            value.numericalValue = undefined;
          }
        }
      });
    }

    return values;
  }

  setValue(data: Measurement[], opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    super.setValue(data, opts);

    // initial calculation
    this.calculateInitialValues(this.fuelTuple);
    this.calculateInitialValues(this.engineOilTuple);
    this.calculateInitialValues(this.hydraulicOilTuple);

    // set iceType and iceAmount value
    const iceType = (this.$iceTypePmfms.getValue() || [])
      .find(pmfm => this.form.get(pmfm.pmfmId.toString()) && isNotNilOrNaN(this.form.get(pmfm.pmfmId.toString()).value));
    const iceAmount = iceType && this.form.get(iceType.pmfmId.toString()).value || undefined;
    this.form.patchValue({iceAmount: iceAmount, iceType: iceType});

  }


  registerTupleSubscription(tuple: ObjectMap<TupleValue>) {
    if (tuple) {
      Object.keys(tuple).forEach(pmfmId => {
        this.registerSubscription(this.form.get(pmfmId).valueChanges
          .pipe(
            filter(_ => !this.applyingValue && !this.calculating),
            debounceTime(250)
          )
          .subscribe(value => {
            this.calculateValues(tuple, pmfmId, value);
          }));
      });
    }
  }

  calculateValues(tuple: ObjectMap<TupleValue>, sourcePmfmId: string, value: any) {
    if (this.calculating)
      return;

    try {
      if (this.debug) {
        console.debug('[expenseForm] calculateValues:', JSON.stringify(tuple), sourcePmfmId, value);
      }
      this.calculating = true;

      // get current values (not computed)
      const values = {quantity: undefined, unitPrice: undefined, total: undefined};
      Object.keys(tuple).forEach(pmfmId => {
        if (!tuple[pmfmId].computed) {
          values[tuple[pmfmId].type] = this.form.get(pmfmId).value || undefined;
        }
      });

      // choose which part is to calculate
      let targetType: TupleType;
      switch (tuple[sourcePmfmId].type) {
        case "quantity":
          if (values.unitPrice) {
            targetType = "total";
            values.total = value && round(value * values.unitPrice) || undefined;
          } else if (values.total) {
            targetType = "unitPrice";
            values.unitPrice = value && value > 0 && round(values.total / value) || undefined;
          }
          break;
        case "unitPrice":
          if (values.quantity) {
            targetType = "total";
            values.total = value && round(value * values.quantity) || undefined;
          } else if (values.total) {
            targetType = "quantity";
            values.quantity = value && value > 0 && round(values.total / value) || undefined;
          }
          break;
        case "total":
          if (values.quantity) {
            targetType = "unitPrice";
            values.unitPrice = value && values.quantity > 0 && round(value / values.quantity) || undefined;
          } else if (values.unitPrice) {
            targetType = "quantity";
            values.quantity = value && values.unitPrice > 0 && round(value / values.unitPrice) || undefined;
          }
          break;
      }

      if (targetType) {
        // set values and tuple computed state
        const patch = {};
        Object.keys(tuple).forEach(targetPmfmId => {
          if (targetPmfmId === sourcePmfmId) {
            tuple[targetPmfmId].computed = false;
          }
          if (tuple[targetPmfmId].type === targetType) {
            tuple[targetPmfmId].computed = true;
            patch[targetPmfmId] = values[targetType];
          }
        });
        this.form.patchValue(patch);
        Object.keys(patch).forEach(pmfmId => this.form.get(pmfmId).markAsPristine());
      }

    } finally {
      this.calculating = false;
    }
  }

  calculateInitialValues(tuple: ObjectMap<TupleValue>) {
    if (tuple) {
      const pmfmIdWithValue = Object.keys(tuple).find(pmfmId => {
        return !tuple[pmfmId].computed && isNotNilOrNaN(this.form.get(pmfmId).value);
      });
      if (pmfmIdWithValue) {
        this.calculateValues(tuple, pmfmIdWithValue, this.form.get(pmfmIdWithValue).value);
      }
    }
  }

  resetComputedValues(values: Measurement[], tuples: ObjectMap<TupleValue>) {
    if (tuples && values && values.length) {
      values.forEach(value => {
        const tuple = tuples[value.pmfmId.toString()];
        if (tuple && tuple.computed) {
          value.numericalValue = undefined;
        }
      });
    }
  }

  initIcePmfms(icePmfms: PmfmStrategy[]) {
    if (isNotEmptyArray(icePmfms)) {
      const remainingPmfms = icePmfms.slice();
      this.$iceTotalPmfm.next(remove(remainingPmfms, this.isTotalPmfm));
      this.$iceTypePmfms.next(remainingPmfms);
    }
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

  mapBaitPmfms(pmfm: PmfmStrategy): boolean {
    return pmfm.label.startsWith('BAIT_');
  }

  getValidTuple(pmfms: PmfmStrategy[]): ObjectMap<TupleValue> {
    if (pmfms) {
      const quantityPmfm = pmfms.find(this.isQuantityPmfm);
      const unitPricePmfm = pmfms.find(this.isUnitPricePmfm);
      const totalPmfm = pmfms.find(this.isTotalPmfm);
      if (quantityPmfm && unitPricePmfm && totalPmfm) {
        const tuple: ObjectMap<TupleValue> = {};
        tuple[quantityPmfm.pmfmId.toString()] = {computed: false, type: "quantity"};
        tuple[unitPricePmfm.pmfmId.toString()] = {computed: false, type: "unitPrice"};
        tuple[totalPmfm.pmfmId.toString()] = {computed: false, type: "total"};
        return tuple;
      }
    }
    return {};
  }

  isQuantityPmfm(pmfm: PmfmStrategy): boolean {
    return pmfm.label.endsWith('VOLUME');
  }

  isUnitPricePmfm(pmfm: PmfmStrategy): boolean {
    return pmfm.label.endsWith('UNIT_PRICE');
  }

  isTotalPmfm(pmfm: PmfmStrategy): boolean {
    return pmfm.label.endsWith('COST');
  }

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    this.calculating = true;
    super.enable(opts);
    this.calculating = false;
  }

  disable(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    this.calculating = true;
    super.disable(opts);
    this.calculating = false;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  filterNumberInput = filterNumberInput;
  selectInputContent = selectInputContent;
}

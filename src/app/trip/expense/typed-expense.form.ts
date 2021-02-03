import {MeasurementsForm} from "../measurement/measurements.form.component";
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from "@angular/core";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {FormBuilder} from "@angular/forms";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {PlatformService} from "../../core/services/platform.service";
import {TypedExpenseValidatorService} from "../services/validator/typed-expense.validator";
import {BehaviorSubject} from "rxjs";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {filterNotNil} from "../../shared/observables";
import {isNotEmptyArray, isNotNilOrNaN, remove, removeAll} from "../../shared/functions";
import {Measurement} from "../services/model/measurement.model";
import {FormFieldDefinition} from "../../shared/form/field.model";
import {debounceTime, filter} from "rxjs/operators";
import {ProgramRefService} from "../../referential/services/program-ref.service";

@Component({
  selector: 'app-typed-expense-form',
  templateUrl: './typed-expense.form.html',
  styleUrls: ['./typed-expense.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TypedExpenseForm extends MeasurementsForm {

  mobile: boolean;
  $typePmfm = new BehaviorSubject<PmfmStrategy>(undefined);
  $totalPmfm = new BehaviorSubject<PmfmStrategy>(undefined);
  $packagingPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  amountDefinition: FormFieldDefinition;

  @Input() rankOrder: number;

  @Input() expenseType: string = 'UNKNOWN';

  @Input()
  set pmfms(pmfms: PmfmStrategy[]) {
    this.setPmfms(pmfms);
  }

  @Output() totalValueChanges = new EventEmitter<any>();

  get total(): number {
    return this.$totalPmfm.getValue() && this.form.get(this.$totalPmfm.getValue().pmfmId.toString()).value || 0;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: TypedExpenseValidatorService,
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected programRefService: ProgramRefService,
    protected platform: PlatformService
  ) {
    super(dateAdapter, validatorService, formBuilder, programRefService, settings, cd);
    this.mobile = platform.mobile;
    this.keepRankOrder = true;
  }

  ngOnInit() {
    super.ngOnInit();

    this.amountDefinition = {
      key: 'amount',
      label: `EXPENSE.${this.expenseType.toUpperCase()}.AMOUNT`,
      type: "double",
      minValue: 0,
      maximumNumberDecimals: 2
    };


    this.registerSubscription(filterNotNil(this.$pmfms).subscribe(pmfms => {

      // Wait form controls ready
      this.ready().then(() => {
        // dispatch pmfms
        this.parsePmfms(pmfms);
      });

    }));

    this.registerSubscription(filterNotNil(this.$totalPmfm).subscribe(totalPmfm => {
      this.form.get(totalPmfm.pmfmId.toString()).valueChanges.pipe(
        filter(() => this.totalValueChanges.observers.length > 0),
        debounceTime(250)
      ).subscribe(() => this.totalValueChanges.emit(this.form.get(totalPmfm.pmfmId.toString()).value))
    }))

    // type
    this.registerAutocompleteField('packaging', {
      showAllOnFocus: true,
      items: this.$packagingPmfms,
      attributes: ['unitLabel'],
      columnNames: ['REFERENTIAL.PMFM.UNIT']
    });

  }

  protected getValue(): Measurement[] {
    const values = super.getValue();

    // parse values
    const packagingPmfms: PmfmStrategy[] = this.$packagingPmfms.getValue() || []
    if (values && packagingPmfms.length) {
      packagingPmfms.forEach(packagingPmfm => {
        const value = values.find(v => v.pmfmId === packagingPmfm.pmfmId)
        if (value) {
          if (this.form.value.packaging && this.form.value.packaging.pmfmId === value.pmfmId) {
            value.numericalValue = this.form.value.amount;
          } else {
            value.numericalValue = undefined;
          }
        }
      });
    }

    // set rank order if provided
    if (this.rankOrder) {
      (values || []).forEach(value => value.rankOrder = this.rankOrder);
    }

    return values;
  }

  setValue(data: Measurement[], opts?: { emitEvent?: boolean; onlySelf?: boolean }) {

    // filter measurements on rank order if provided
    if (this.rankOrder) {
      data = (data || []).filter(value => value.rankOrder === this.rankOrder);
    }

    super.setValue(data, opts);

    // set packaging and amount value
    const packaging = (this.$packagingPmfms.getValue() || [])
      .find(pmfm => this.form.get(pmfm.pmfmId.toString()) && isNotNilOrNaN(this.form.get(pmfm.pmfmId.toString()).value));
    const amount = packaging && this.form.get(packaging.pmfmId.toString()).value || undefined;
    this.form.patchValue({amount, packaging});

  }

  parsePmfms(pmfms: PmfmStrategy[]) {
    if (isNotEmptyArray(pmfms)) {
      const remainingPmfms = pmfms.slice();
      this.$typePmfm.next(remove(remainingPmfms, this.isTypePmfm));
      this.$totalPmfm.next(remove(remainingPmfms, this.isTotalPmfm));
      this.$packagingPmfms.next(removeAll(remainingPmfms, this.isPackagingPmfm));
      if (remainingPmfms.length) {
        console.warn('[typed-expense] some pmfms have not been parsed', remainingPmfms);
      }

      // must update controls
      this.validatorService.updateFormGroup(this.form, {
        pmfms,
        typePmfm: this.$typePmfm.getValue(),
        totalPmfm: this.$totalPmfm.getValue()
      });

    }
  }

  isTypePmfm(pmfm: PmfmStrategy): boolean {
    return pmfm.label.endsWith('TYPE');
  }

  isPackagingPmfm(pmfm: PmfmStrategy): boolean {
    return pmfm.label.endsWith('WEIGHT') || pmfm.label.endsWith('COUNT');
  }

  isTotalPmfm(pmfm: PmfmStrategy): boolean {
    return pmfm.label.endsWith('COST');
  }

  protected markForCheck() {
    if (this.cd)
      this.cd.markForCheck();
    else
      console.warn('[typed-expense-form] ChangeDetectorRef is undefined');
  }

}

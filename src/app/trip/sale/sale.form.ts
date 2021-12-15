import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit } from '@angular/core';
import {SaleValidatorService} from "../services/validator/sale.validator";
import {Moment} from 'moment';
import {DateAdapter} from "@angular/material/core";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {Sale} from "../services/model/sale.model";
import {LocationLevelIds} from "../../referential/services/model/model.enum";
import {AppForm}  from "@sumaris-net/ngx-components";
import {referentialToString}  from "@sumaris-net/ngx-components";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {StatusIds}  from "@sumaris-net/ngx-components";
import { OnReady } from '@sumaris-net/ngx-components/public_api';

@Component({
  selector: 'form-sale',
  templateUrl: './sale.form.html',
  styleUrls: ['./sale.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaleForm extends AppForm<Sale> implements OnInit, OnReady {

  private _minDate: Moment = null;

  @Input() required = true;
  @Input() showError = true;
  @Input() showVessel = true;
  @Input() showEndDateTime = true;
  @Input() showComment = true;
  @Input() showButtons = true;

  @Input() set minDate(value: Moment) {
    if (value && (!this._minDate || !this._minDate.isSame(value))) {
      console.log('TODO received new date', value)
      this._minDate = value;
      if (!this._loading) this.updateFormGroup();
    }
  }

  get empty(): any {
    const value = this.value;
    return (!value.saleLocation || !value.saleLocation.id)
      && (!value.startDateTime)
      && (!value.endDateTime)
      && (!value.saleType || !value.saleType.id)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  constructor(
    injector: Injector,
    protected saleValidatorService: SaleValidatorService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef
  ) {
    super(injector, saleValidatorService.getFormGroup());
  }

  ngOnInit() {
    super.ngOnInit();

    // Combo: vessels (if need)
    if (this.showVessel) {
      // Combo: vessels
      this.vesselSnapshotService.getAutocompleteFieldOptions().then(opts =>
        this.registerAutocompleteField('vesselSnapshot', opts)
      );
    } else {
      this.form.get('vesselSnapshot').clearValidators();
    }

    // Combo: sale locations
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
      }
    });

    // Combo: sale types
    this.registerAutocompleteField('saleType', {
      service: this.referentialRefService,
      attributes: ['name'],
      filter: {
        entityName: 'SaleType'
      }
    });
  }

  ngOnReady() {
    this.updateFormGroup();
  }

  protected updateFormGroup(opts?: { emitEvent?: boolean; }) {
    console.info('[sale-form] Updating form group...');
    this.saleValidatorService.updateFormGroup(this.form, {
      required: this.required, // Set if required or not
      minDate: this._minDate
    });

    if (!opts || opts.emitEvent !== false) {
      this.form.updateValueAndValidity();
      this.markForCheck();
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  referentialToString = referentialToString;
}

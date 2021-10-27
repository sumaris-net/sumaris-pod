import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment';
import {DateAdapter} from '@angular/material/core';
import {
  AccountService,
  IReferentialRef,
  isNotNil,
  LocalSettingsService,
  PlatformService, ReferentialRef, SharedFormGroupValidators,
  UsageMode
} from '@sumaris-net/ngx-components';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {OperationGroup, PhysicalGear, Trip} from '../services/model/trip.model';
import {BehaviorSubject} from 'rxjs';
import {MetierService} from '../../referential/services/metier.service';
import {ReferentialRefService} from '../../referential/services/referential-ref.service';
import {MeasurementValuesForm} from '@app/trip/measurement/measurement-values.form.class';
import {ProgramRefService} from '@app/referential/services/program-ref.service';
import {BatchValidatorService} from '@app/trip/services/validator/batch.validator';
import {MeasurementsValidatorService} from '@app/trip/services/validator/measurement.validator';
import {referentialToString} from '@sumaris-net/ngx-components';
import {OperationGroupValidatorService} from '@app/trip/services/validator/operation-group.validator';
import {debounceTime, filter, first} from 'rxjs/operators';
import {Metier} from '@app/referential/services/model/taxon.model';
import {AcquisitionLevelCodes} from '@app/referential/services/model/model.enum';
import {environment} from '@environments/environment';
import {BatchUtils} from '@app/trip/services/model/batch.model';


@Component({
  selector: 'app-operation-group-form',
  templateUrl: './operation-group.form.html',
  styleUrls: ['./operation-group.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationGroupForm extends MeasurementValuesForm<OperationGroup> implements OnInit {

  private _metiersSubject = new BehaviorSubject<IReferentialRef[]>(undefined);
  protected $initialized = new BehaviorSubject<boolean>(false);
  displayAttributes: {
    [key: string]: string[]
  };

  mobile: boolean;
  gear: ReferentialRef;
  metier: Metier;

  @Input() tabindex: number;
  @Input() showComment = false;
  @Input() showError = true;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected platform: PlatformService,
    protected validatorService: OperationGroupValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService,
    protected metierService: MetierService,
    protected accountService: AccountService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programRefService, settings, cd,
      validatorService.getFormGroup(null, {
        withMeasurements: false
      }),
      {
        onUpdateControls: (form) => this.onUpdateControls(form)
      }
    );

    // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.OPERATION;

    this.debug = !environment.production;
  };

  ngOnInit() {
    console.debug('[operation-group.form] init form operation group form');
    super.ngOnInit();

    // Default values
    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;

    this.gear = this.data.physicalGear?.gear || this.data.metier?.gear;
    this.metier = this.data.metier;

    this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: ['taxonGroup.label', 'taxonGroup.name']
    };

    // Metier combo
    const metierAttributes = this.settings.getFieldDisplayAttributes('metier');

    this.registerAutocompleteField('metier', {
      mobile: this.mobile,
      items: this._metiersSubject,
      attributes: metierAttributes,
      columnSizes: metierAttributes.map(attr => attr === 'label' ? 3 : undefined),
      suggestFn: (value: any, options?: any) => this.metierService.suggest(value, options)
    });

    this.registerSubscription(
      this.form.get('metier').valueChanges
        .subscribe(metier => this.updateGearAndTargetSpecies(metier))
    );
  }
  setValue(data: OperationGroup, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; }) {
    super.setValue(data, opts);

    // This will cause update controls
    this.$initialized.next(true);
  }

  async updateGearAndTargetSpecies(metier: Metier) {

    console.debug('[operation-group.form] Update Gear and Target Species', metier);
    if (metier && metier.id) {

      this.data.metier = await this.metierService.load(metier.id);
      this.metier = this.data.metier;
      console.debug('[operation-group.form] Taxon group : ', this.metier.taxonGroup);

      if (!this.data.physicalGear.gear || this.data.physicalGear.gear.id !== this.data.metier.gear.id) {

        this.data.physicalGear = this.data.physicalGear || new PhysicalGear();
        this.data.physicalGear.gear = this.data.metier.gear;

        this.gear =  this.data.physicalGear.gear;
      }
    }
  }

  protected async onUpdateControls(form?: FormGroup): Promise<void> {
    form = form || this.form;

    // Wait end of ngInit()
    await this.onInitialized();

    // Add pmfms to form
    const measFormGroup = form.get('measurementValuesForm') as FormGroup;
    if (measFormGroup) {
      this.measurementValidatorService.updateFormGroup(measFormGroup, {pmfms: this.$pmfms.getValue()});
    }
  }

  /* -- protected methods -- */

  protected async onInitialized(): Promise<void> {
    // Wait end of setValue()
    if (this.$initialized.getValue() !== true) {
      await this.$initialized
        .pipe(
          filter((initialized) => initialized === true),
          first()
        ).toPromise();
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  referentialToString = referentialToString;
}

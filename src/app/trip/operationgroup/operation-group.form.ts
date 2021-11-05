import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment';
import {DateAdapter} from '@angular/material/core';
import {AccountService, IReferentialRef, isNotNil, LocalSettingsService, PlatformService, ReferentialRef, referentialToString} from '@sumaris-net/ngx-components';
import {FormBuilder, FormGroup} from '@angular/forms';
import {OperationGroup} from '../services/model/trip.model';
import {BehaviorSubject, Observable} from 'rxjs';
import {MetierService} from '@app/referential/services/metier.service';
import {ReferentialRefService} from '@app/referential/services/referential-ref.service';
import {MeasurementValuesForm} from '@app/trip/measurement/measurement-values.form.class';
import {ProgramRefService} from '@app/referential/services/program-ref.service';
import {MeasurementsValidatorService} from '@app/trip/services/validator/measurement.validator';
import {OperationGroupValidatorService} from '@app/trip/services/validator/operation-group.validator';
import {filter, first} from 'rxjs/operators';
import {AcquisitionLevelCodes} from '@app/referential/services/model/model.enum';
import {environment} from '@environments/environment';
import {Metier} from '@app/referential/services/model/metier.model';


@Component({
  selector: 'app-operation-group-form',
  templateUrl: './operation-group.form.html',
  styleUrls: ['./operation-group.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationGroupForm extends MeasurementValuesForm<OperationGroup> implements OnInit {

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
  @Input() metiers: Observable<ReferentialRef[]> | ReferentialRef[];

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
        onUpdateFormGroup: (form) => this.onUpdateFormGroup(form)
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

    this.gear = this.data.metier?.gear;
    this.metier = this.data.metier;

    this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: ['taxonGroup.label', 'taxonGroup.name']
    };

    // Metier combo
    const metierAttributes = this.settings.getFieldDisplayAttributes('metier');

    this.registerAutocompleteField('metier', {
      mobile: this.mobile,
      items: this.metiers,
      attributes: metierAttributes,
      columnSizes: metierAttributes.map(attr => attr === 'label' ? 3 : undefined),
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

      if (this.data.physicalGearId !== this.data.metier.gear.id) {

        this.data.physicalGearId = this.data.physicalGearId || null;
        this.gear = this.data.metier.gear;
      }
    }
  }

  protected async onUpdateFormGroup(form?: FormGroup): Promise<void> {
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

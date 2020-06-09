import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit} from '@angular/core';
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {isNil, PmfmStrategy} from "../services/model";
import {ValidatorService} from "angular4-material-table";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {environment} from "../../../environments/environment";
import {PmfmStrategyValidatorService} from "../services/validator/pmfm-strategy.validator";
import {AppInMemoryTable} from "../../core/table/memory-table.class";
import {filterNumberInput} from "../../shared/inputs";
import {ReferentialRefService} from "../services/referential-ref.service";
import {FormFieldDefinition} from "../../shared/form/field.model";
import {changeCaseToUnderscore} from "../../shared/functions";
import {BehaviorSubject} from "rxjs";
import {firstNotNilPromise} from "../../shared/observables";
import {PmfmService} from "../services/pmfm.service";
import {Pmfm} from "../services/model/pmfm.model";
import {IReferentialRef} from "../../core/services/model";

export class PmfmStrategyFilter {
  strategyId?: number;
  acquisitionLevel?: string;
}

@Component({
  selector: 'app-pmfm-strategies-table',
  templateUrl: './pmfm-strategies.table.html',
  styleUrls: ['./pmfm-strategies.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: PmfmStrategyValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PmfmStrategiesTable extends AppInMemoryTable<PmfmStrategy, PmfmStrategyFilter> implements OnInit {


  $acquisitionLevels = new BehaviorSubject<IReferentialRef[]>(undefined);
  $pmfms = new BehaviorSubject<Pmfm[]>(undefined);
  fieldDefinitions: FormFieldDefinition[] = [];

  @Input() canEdit = false;
  @Input() canDelete = false;

  @Input() title: string;

  constructor(
    protected injector: Injector,
    protected validatorService: ValidatorService,
    protected pmfmService: PmfmService,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef
  ) {
    super(injector,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'acquisitionLevel',
          'rankOrder',
          'pmfm',
          'isMandatory',
          'acquisitionNumber',
          'minValue',
          'maxValue'
        ])
        .concat(RESERVED_END_COLUMNS),
      PmfmStrategy,
      new InMemoryTableDataService<PmfmStrategy, PmfmStrategyFilter>(PmfmStrategy, {
        onLoad: (data) => this.onLoad(data),
        onSave: (data) => this.onSave(data)
      }),
      validatorService,
      null,
      {});

    this.i18nColumnPrefix = 'PROGRAM.STRATEGY.PMFM_STRATEGY.';
    this.inlineEdition = true;
    //this.confirmBeforeDelete = true;

    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Acquisition level
    this.registerFormField('acquisitionLevel', {
      type: 'entity',
      required: true,
      autocomplete: this.registerAutocompleteField('acquisitionLevel', {
        items: this.$acquisitionLevels,
        attributes: ['name'],
        showAllOnFocus: true
      })
    });

    // Rank order
    this.registerFormField('rankOrder', {
      type: 'integer',
      minValue: 1,
      defaultValue: 1,
      required: true
    });

    // Pmfm
    this.registerFormField('pmfm', {
      type: 'entity',
      required: true,
      autocomplete: this.registerAutocompleteField('pmfm', {
        items: this.$pmfms,
        attributes: ['label', 'parameter.name', 'method.name'],
        columnNames: ['REFERENTIAL.LABEL', 'REFERENTIAL.NAME', 'REFERENTIAL.PMFM.MATRIX', 'REFERENTIAL.PMFM.METHOD'],
        showAllOnFocus: false
      })
    });

    // Is mandatory
    this.registerFormField('isMandatory', {
      type: 'boolean',
      defaultValue: false,
      required: true
    });

    // Acquisition number
    this.registerFormField('acquisitionNumber', {
      type: 'integer',
      minValue: 1,
      defaultValue: 1,
      required: true
    });

    // Min / Max
    this.registerFormField('minValue', {
      type: 'double',
      required: false
    });
    this.registerFormField('maxValue', {
      type: 'double',
      required: false
    });

    this.loadReferential();
  }

  async onLoad(data: PmfmStrategy[]): Promise<PmfmStrategy[]> {

    await this.waitReady();

    const acquisitionLevels = this.$acquisitionLevels.getValue();
    const pmfms = this.$pmfms.getValue();
    return data.map(source => {
      const target = source.asObject();
      target.acquisitionLevel = acquisitionLevels.find(i => i.label === target.acquisitionLevel);
      target.pmfm = pmfms.find(i => i.id === target.pmfmId);
      return target;
    })
  }

  async onSave(data: PmfmStrategy[]): Promise<PmfmStrategy[]> {

    return data.map(source => {
      const target = source.asObject();
      target.acquisitionLevel = target.acquisitionLevel && target.acquisitionLevel.label;
      target.pmfmId = target.pmfm && target.pmfm.id;
      delete target.pmfm;
      return target;
    })
  }

  /* -- protected methods -- */

  protected registerFormField(fieldName: string, def: Partial<FormFieldDefinition>) {
    const definition = <FormFieldDefinition>{
      key: fieldName,
      label: this.i18nColumnPrefix + changeCaseToUnderscore(fieldName).toUpperCase(),
      ...def
    }
    this.fieldDefinitions.push(definition);
  }

  protected async loadReferential() {
    console.debug("[pmfm-strategy-table] Loading referential items...");
    this.markAsLoading();

    try {
      await Promise.all([
        this.loadAcquisitionLevels(),
        this.loadPmfms()
      ]);

      console.debug("[pmfm-strategy-table] Loaded referential items");
    } catch(err) {
      this.error = err && err.message || err;
      this.markForCheck();
    }
  }

  protected async waitReady(): Promise<void> {
    if (isNil(this.$acquisitionLevels.getValue()) || isNil(this.$pmfms.getValue())) {
        await Promise.all([
          firstNotNilPromise(this.$acquisitionLevels.asObservable()),
          firstNotNilPromise(this.$pmfms.asObservable())
        ]);
    }
  }

  protected async loadAcquisitionLevels() {
    const res = await this.referentialRefService.loadAll(0, 100, null, null, {
      entityName: 'AcquisitionLevel'
    }, {withTotal: false});
    this.$acquisitionLevels.next(res && res.data || undefined)
  }

  protected async loadPmfms() {
    const res = await this.pmfmService.loadAll(0, 1000, null, null, null,
      {
        withTotal: false,
        withDetails: true
      });
    this.$pmfms.next(res && res.data || undefined)
  }


  filterNumberInput = filterNumberInput;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

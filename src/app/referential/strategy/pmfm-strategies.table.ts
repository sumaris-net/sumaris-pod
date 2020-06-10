import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit} from '@angular/core';
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {fromDateISOString, isNil, isNotNil, PmfmStrategy, ReferentialRef} from "../services/model";
import {TableElement, ValidatorService} from "angular4-material-table";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {environment} from "../../../environments/environment";
import {PmfmStrategyValidatorService} from "../services/validator/pmfm-strategy.validator";
import {AppInMemoryTable} from "../../core/table/memory-table.class";
import {filterNumberInput} from "../../shared/inputs";
import {ReferentialRefService} from "../services/referential-ref.service";
import {FormFieldDefinition} from "../../shared/form/field.model";
import {Beans, changeCaseToUnderscore, isEmptyArray, isNotEmptyArray, KeysEnum} from "../../shared/functions";
import {BehaviorSubject} from "rxjs";
import {firstNotNilPromise} from "../../shared/observables";
import {PmfmService} from "../services/pmfm.service";
import {Pmfm} from "../services/model/pmfm.model";
import {IReferentialRef, ReferentialUtils} from "../../core/services/model";
import {AppTableDataSourceOptions} from "../../core/table/table-datasource.class";

export class PmfmStrategyFilter {

  static searchFilter<T extends PmfmStrategy>(f: PmfmStrategyFilter): (T) => boolean {
    if (this.isEmpty(f)) return undefined; // no filter need
    return (t: T) => {

      // DEBUG
      console.debug("Filtering pmfmStrategy: ", t, f);

      // Acquisition Level
      const acquisitionLevel = t.acquisitionLevel && t.acquisitionLevel instanceof ReferentialRef ? t.acquisitionLevel.label : t.acquisitionLevel;
      if (f.acquisitionLevel && (!acquisitionLevel || acquisitionLevel !== f.acquisitionLevel)) {
        return false;
      }

      // Locations

      // Gears

      // Taxon groups
      if (isNotEmptyArray(f.taxonGroupIds) && (isEmptyArray(t.taxonGroupIds) || t.taxonGroupIds.findIndex(id => f.taxonGroupIds.includes(id)) === -1)) {
        return false;
      }

      return true;
    };
  }

  static isEmpty(aFilter: PmfmStrategyFilter|any): boolean {
    return Beans.isEmpty(aFilter, PmfmStrategyFilterKeys, {
      blankStringLikeEmpty: true
    });
  }

  strategyId?: number;
  acquisitionLevel?: string;
  gearIds?: number[];
  locationIds?: number[];
  taxonGroupIds?: number[];
  taxonNameIds?: number[];
}
export const PmfmStrategyFilterKeys: KeysEnum<PmfmStrategyFilter> = {
  strategyId: true,
  acquisitionLevel: true,
  locationIds: true,
  gearIds: true,
  taxonGroupIds: true,
  taxonNameIds: true,
};

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
        onSave: (data) => this.onSave(data),
        onFilter: (data) => this.onFilter(data)
      }),
      validatorService,
      <AppTableDataSourceOptions<PmfmStrategy>>{
        prependNewElements: true,
        suppressErrors: true,
        onRowCreated: (row) => this.onRowCreated(row)
      },
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


  /* -- protected methods -- */

  protected async onLoad(data: PmfmStrategy[]): Promise<PmfmStrategy[]> {

    await this.waitReady();

    const acquisitionLevels = this.$acquisitionLevels.getValue();
    const pmfms = this.$pmfms.getValue();
    return data.map(source => {
      const target:any = source instanceof PmfmStrategy ? source.asObject() : source;
      target.acquisitionLevel = acquisitionLevels.find(i => i.label === target.acquisitionLevel);
      target.pmfm = pmfms.find(i => i.id === target.pmfmId);
      delete target.pmfmId;
      return target;
    })
  }

  protected async onSave(data: PmfmStrategy[]): Promise<PmfmStrategy[]> {

    return data.map(source => {
      const target:any = source instanceof PmfmStrategy ? source.asObject() : source;
      return target;
    });
  }


  protected async onFilter(data: PmfmStrategy[]): Promise<PmfmStrategy[]> {
    if (isEmptyArray(data)) return data; // Skip if empty

    const filterFn = PmfmStrategyFilter.searchFilter(this.filter)
    if (!filterFn) return data; // Skip if filter not need

    // Apply filter
    return (data||[]).filter(filterFn);
  }

  protected async onRowCreated(row: TableElement<PmfmStrategy>): Promise<void> {

    // Creating default values, from the current filter
    const filter = this.filter;
    const acquisitionLevelLabel = filter && filter.acquisitionLevel;
    const acquisitionLevel = acquisitionLevelLabel && (this.$acquisitionLevels.getValue() || []).find(item => item.label === acquisitionLevelLabel);

    let rankOrder:number = null;
    if (acquisitionLevel) {
      rankOrder = ((await this.getMaxRankOrder(acquisitionLevel)) || 0) + 1;
    }
    const defaultValues = {
      acquisitionLevel,
      rankOrder
    }

    // Applying defaults
    if (row.validator) {
      row.validator.patchValue(defaultValues);
    }
    else {
      Object.assign(row.currentData, defaultValues);
    }
  }

  protected async getMaxRankOrder(acquisitionLevel: IReferentialRef): Promise<number> {
    const rows = await this.dataSource.getRows();
    return rows
      .map(row => row.currentData)
      .filter(data => ReferentialUtils.equals(data.acquisitionLevel, acquisitionLevel))
      .reduce((res, data) => Math.max(res, data.rankOrder || 0), 0);
  }

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

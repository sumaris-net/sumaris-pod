import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, Output} from '@angular/core';
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {environment} from "../../../environments/environment";
import {PmfmStrategyValidatorService} from "../services/validator/pmfm-strategy.validator";
import {AppInMemoryTable} from "../../core/table/memory-table.class";
import {filterNumberInput} from "../../shared/inputs";
import {ReferentialRefService} from "../services/referential-ref.service";
import {FormFieldDefinition, FormFieldDefinitionMap} from "../../shared/form/field.model";
import {Beans, changeCaseToUnderscore, isEmptyArray, isNotEmptyArray, isNotNil, KeysEnum} from "../../shared/functions";
import {BehaviorSubject, Observable, of} from "rxjs";
import {firstFalsePromise} from "../../shared/observables";
import {PmfmService} from "../services/pmfm.service";
import {Pmfm} from "../services/model/pmfm.model";
import {
  IReferentialRef,
  ReferentialRef,
  referentialToString,
  ReferentialUtils
} from "../../core/services/model/referential.model";
import {AppTableDataSourceOptions} from "../../core/table/entities-table-datasource.class";
import {debounceTime, map, startWith, switchMap} from "rxjs/operators";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {PmfmValueUtils} from "../services/model/pmfm-value.model";
import {Program} from "../services/model/program.model";
import {SelectionChange} from "@angular/cdk/collections";

export class PmfmStrategyFilter {

  static searchFilter<T extends PmfmStrategy>(f: PmfmStrategyFilter): (PmfmStrategy) => boolean {
    if (PmfmStrategyFilter.isEmpty(f)) return undefined; // no filter need
    return (t) => {
      // DEBUG
      //console.debug("Filtering pmfmStrategy: ", t, f);

      // Acquisition Level
      const acquisitionLevel = t.acquisitionLevel && t.acquisitionLevel instanceof ReferentialRef ? t.acquisitionLevel.label : t.acquisitionLevel;
      if (f.acquisitionLevel && (!acquisitionLevel || acquisitionLevel !== f.acquisitionLevel)) {
        return false;
      }

      // Locations
      //if (isNotEmptyArray(f.locationIds) && (isEmptyArray(t.gears) || t.gears.findIndex(id => f.gearIds.includes(id)) === -1)) {
      //    return false;
      //}

      // Gears
      if (isNotEmptyArray(f.gearIds) && (isEmptyArray(t.gearIds) || t.gearIds.findIndex(id => f.gearIds.includes(id)) === -1)) {
        return false;
      }

      // Taxon groups
      if (isNotEmptyArray(f.taxonGroupIds) && (isEmptyArray(t.taxonGroupIds) || t.taxonGroupIds.findIndex(id => f.taxonGroupIds.includes(id)) === -1)) {
        return false;
      }

      // Taxon names
      if (isNotEmptyArray(f.referenceTaxonIds) && (isEmptyArray(t.referenceTaxonIds) || t.referenceTaxonIds.findIndex(id => f.referenceTaxonIds.includes(id)) === -1)) {
        return false;
      }

      return true;
    }
  }

  static isEmpty(aFilter: PmfmStrategyFilter|any): boolean {
    return Beans.isEmpty<PmfmStrategyFilter>(aFilter, PmfmStrategyFilterKeys, {
      blankStringLikeEmpty: true
    });
  }

  strategyId?: number;
  acquisitionLevel?: string;
  gearIds?: number[];
  locationIds?: number[];
  taxonGroupIds?: number[];
  referenceTaxonIds?: number[];
}

export const PmfmStrategyFilterKeys: KeysEnum<PmfmStrategyFilter> = {
  strategyId: true,
  acquisitionLevel: true,
  locationIds: true,
  gearIds: true,
  taxonGroupIds: true,
  referenceTaxonIds: true
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


  $selectedPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  $acquisitionLevels = new BehaviorSubject<IReferentialRef[]>(undefined);
  $pmfms = new BehaviorSubject<Pmfm[]>(undefined);
  $pmfmsParameters = new BehaviorSubject<IReferentialRef[]>(undefined);
  $pmfmsMatrix = new BehaviorSubject<Pmfm[]>(undefined);
  $pmfmsFractions = new BehaviorSubject<Pmfm[]>(undefined);
  $pmfmsMethods = new BehaviorSubject<Pmfm[]>(undefined);
  $gears = new BehaviorSubject<IReferentialRef[]>(undefined);
  $loadingReferential = new BehaviorSubject<boolean>(true);
  $qualitativeValues = new BehaviorSubject<IReferentialRef[]>(undefined);

  fieldDefinitionsMap: FormFieldDefinitionMap = {};
  fieldDefinitions: FormFieldDefinition[] = [];

  @Input() canEdit = false;
  @Input() canDelete = false;
  @Input() sticky = false;
  @Input()
  set showPMFMDetailsColumns(value: boolean) {
    // Display PMFM details or other columns
    this.setShowColumn('parameter', value);
    this.setShowColumn('matrix', value);
    this.setShowColumn('fraction', value);
    this.setShowColumn('method', value);

    this.setShowColumn('acquisitionLevel', !value);
    this.setShowColumn('rankOrder', !value);
    this.setShowColumn('isMandatory', !value);
    this.setShowColumn('acquisitionNumber', !value);
    this.setShowColumn('minValue', !value);
    this.setShowColumn('maxValue', !value);
    this.setShowColumn('defaultValue', !value);
  }


  @Input() title: string;

  @Input() program: Program;

  @Output() get selectionChanges(): Observable<TableElement<PmfmStrategy>[]> {
    return this.selection.changed.pipe(
      map(_ => this.selection.selected)
    );
  }

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
          'parameter',
          'matrix',
          'fraction',
          'method',
          'isMandatory',
          'acquisitionNumber',
          'minValue',
          'maxValue',
          'defaultValue'
        ])
        .concat(RESERVED_END_COLUMNS),
      PmfmStrategy,
      new InMemoryEntitiesService<PmfmStrategy, PmfmStrategyFilter>(PmfmStrategy, {
        onLoad: (data) => this.onLoad(data),
        onSave: (data) => this.onSave(data),
        filterFnFactory: PmfmStrategyFilter.searchFilter
      }),
      validatorService,
      <AppTableDataSourceOptions<PmfmStrategy>>{
        prependNewElements: true,
        suppressErrors: true,
        onRowCreated: (row) => this.onRowCreated(row)
      },
      new PmfmStrategyFilter());

    this.i18nColumnPrefix = 'PROGRAM.STRATEGY.PMFM_STRATEGY.';
    this.inlineEdition = true;
    //this.confirmBeforeDelete = true;

    this.debug = !environment.production;

    // Loading referential items
    this.loadReferential();
  }


  protected getDisplayColumns(): string[] {
    let userColumns = this.getUserColumns();

    // No user override: use defaults
    if (!userColumns) return this.columns;

    // Get fixed start columns
    const fixedStartColumns = this.columns.filter(c => RESERVED_START_COLUMNS.includes(c));

    // Remove end columns
    const fixedEndColumns = this.columns.filter(c => RESERVED_END_COLUMNS.includes(c));

    // Remove fixed columns from user columns
    userColumns = userColumns.filter(c => (!fixedStartColumns.includes(c) && !fixedEndColumns.includes(c) && this.columns.includes(c)));

    return fixedStartColumns
      .concat(userColumns)
      .concat(fixedEndColumns)
      // Remove columns to hide
      .filter(column => !this.excludesColumns.includes(column));
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
        showAllOnFocus: true,
        class: 'mat-autocomplete-panel-large-size'
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
    const basePmfmAttributes = this.settings.getFieldDisplayAttributes('pmfm', ['label', 'name']);
    const pmfmAttributes = basePmfmAttributes
      .map(attr => attr === 'name' ? 'parameter.name' : attr)
      .concat(['unit.label', 'matrix.name', 'fraction.name', 'method.name']);
    const pmfmColumnNames = basePmfmAttributes.map(attr => 'REFERENTIAL.' + attr.toUpperCase())
      .concat(['REFERENTIAL.PMFM.UNIT', 'REFERENTIAL.PMFM.MATRIX', 'REFERENTIAL.PMFM.FRACTION', 'REFERENTIAL.PMFM.METHOD']);
    this.registerFormField('pmfm', {
      type: 'entity',
      required: true,
      autocomplete: this.registerAutocompleteField('pmfm', {
        items: this.$pmfms,
        attributes: pmfmAttributes,
        columnSizes: pmfmAttributes.map(attr => {
          switch(attr) {
            case 'label':
              return 2;
            case 'name':
              return 3;
            case 'unit.label':
              return 1;
            case 'method.name':
              return 4;
            default: return undefined;
          }
        }),
        columnNames: pmfmColumnNames,
        showAllOnFocus: false,
        class: 'mat-autocomplete-panel-full-size'
      })
    });

    // PMFM.PARAMETER
    const pmfmParameterAttributes = ['label', 'name'];
    this.registerFormField('parameter', {
      type: 'entity',
      required: true,
      autocomplete: this.registerAutocompleteField('parameter', {
        items: this.$pmfmsParameters,
        attributes: pmfmParameterAttributes,
        columnSizes: pmfmParameterAttributes.map(attr => {
          switch(attr) {
            case 'code':
              return 3;
            case 'label':
              return 3;
            case 'name':
              return 4;
            default: return undefined;
          }
        }),
        columnNames: ['REFERENTIAL.PARAMETER.CODE', 'REFERENTIAL.PARAMETER.NAME'],
        showAllOnFocus: false,
        class: 'mat-autocomplete-panel-full-size'
      })
    });

    // PMFM.MATRIX
    const pmfmMatrixAttributes = ['matrix.name', 'matrix.description'];
    this.registerFormField('matrix', {
      type: 'entity',
      required: true,
      autocomplete: this.registerAutocompleteField('matrix', {
        items: this.$pmfmsMatrix,
        attributes: pmfmMatrixAttributes,
        columnSizes: pmfmMatrixAttributes.map(attr => {
          switch(attr) {
            case 'matrix.name':
              return 3;
            case 'matrix.description':
              return 4;
            default: return undefined;
          }
        }),
        showAllOnFocus: false,
        class: 'mat-autocomplete-panel-full-size'
      })
    });

    // PMFM.FRACTION
    const pmfmFractionAttributes = ['fraction.name', 'fraction.description'];
    this.registerFormField('fraction', {
      type: 'entity',
      required: true,
      autocomplete: this.registerAutocompleteField('fraction', {
        items: this.$pmfmsFractions,
        attributes: pmfmFractionAttributes,
        columnSizes: pmfmFractionAttributes.map(attr => {
          switch(attr) {
            case 'fraction.name':
              return 3;
            case 'fraction.description':
              return 4;
            default: return undefined;
          }
        }),
        showAllOnFocus: false,
        class: 'mat-autocomplete-panel-full-size'
      })
    });

    // PMFM.METHOD
    const pmfmMethodAttributes = ['method.name', 'method.description'];
    this.registerFormField('method', {
      type: 'entity',
      required: true,
      autocomplete: this.registerAutocompleteField('method', {
        items: this.$pmfmsMethods,
        attributes: pmfmMethodAttributes,
        columnSizes: pmfmMethodAttributes.map(attr => {
          switch(attr) {
            case 'method.name':
              return 3;
            case 'method.description':
              return 4;
            default: return undefined;
          }
        }),
        showAllOnFocus: false,
        class: 'mat-autocomplete-panel-full-size'
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
    this.registerFormField('defaultValue', {
      type: 'double',
      required: false
    }, true);

    const qvAttributes = this.settings.getFieldDisplayAttributes('qualitativeValue', ['label', 'name']);
    this.registerFormField('defaultQualitativeValue', {
      type: 'entity',
      autocomplete: {
        attributes: qvAttributes,
        items: this.onStartEditingRow
          .pipe(
            switchMap(row => {
              const control = row.validator && row.validator.get('pmfm');
              if (control) {
                return control.valueChanges.pipe(startWith(control.value))
              } else {
                return of(row.currentData.pmfm);
              }
            }),
            debounceTime(200),
            map(pmfm => isNotEmptyArray(pmfm && pmfm.qualitativeValues) ? pmfm.qualitativeValues : (pmfm.parameter && pmfm.parameter.qualitativeValues || []))
          ),
        showAllOnFocus: false,
        class: 'mat-autocomplete-panel-large-size'
      },
      required: false
    }, true);


    // Listen PmfmsParameterZs, to change pmfm lists
    //this.registerSubscription(
    //  this.editedRow.validator.get('pmfmsUnits').value.valueChanges
    //    /*.pipe(
    //      distinctUntilChanged((o1, o2) => EntityUtils.equals(o1, o2, 'id'))
    //    )*/
    //    .subscribe((pmfmsUnits) => this.onPmfmsUnitsChanged(pmfmsUnits))
    //);

  }


  /* -- protected methods -- */

    protected async onPmfmsParametersChanged(pmfmsUnits) {
    console.debug("onPmfmsParametersChanged(pmfmsUnits");
/**      const metierControl = this.form.get('metier');
      const physicalGearControl = this.form.get('physicalGear');

      const hasPhysicalGear = EntityUtils.isNotEmpty(physicalGear, 'id');
      const gears = this._physicalGearsSubject.getValue() || this._trip && this._trip.gears;
      // Use same trip's gear Object (if found)
      if (hasPhysicalGear && isNotEmptyArray(gears)) {
        physicalGear = (gears || []).find(g => g.id === physicalGear.id);
        physicalGearControl.patchValue(physicalGear, {emitEvent: false});
      }

      // Change metier status, if need
      const enableMetier = hasPhysicalGear && this.form.enabled && isNotEmptyArray(gears);
      if (enableMetier) {
        if (metierControl.disabled) metierControl.enable();
      }
      else {
        if (metierControl.enabled) metierControl.disable();
      }

      if (hasPhysicalGear) {

        // Refresh metiers
        const metiers = await this.loadMetiers(physicalGear);
        this._metiersSubject.next(metiers);

        const metier = metierControl.value;
        if (ReferentialUtils.isNotEmpty(metier)) {
          // Find new reference, by ID
          let updatedMetier = (metiers || []).find(m => m.id === metier.id);

          // If not found : retry using the label (WARN: because of searchJoin, label = taxonGroup.label)
          updatedMetier = updatedMetier || (metiers || []).find(m => m.label === metier.label);

          // Update the metier, if not found (=reset) or ID changed
          if (!updatedMetier || !ReferentialUtils.equals(metier, updatedMetier)) {
            metierControl.setValue(updatedMetier);
          }
        }
      }*/
    }

  protected async onLoad(data: PmfmStrategy[]): Promise<PmfmStrategy[]> {

    await this.waitReferentialReady();

    console.debug("[pmfm-strategy-table] Adapt loaded data to table...");

    const acquisitionLevels = this.$acquisitionLevels.getValue();
    const pmfms = this.$pmfms.getValue();
    //const gears = this.$gears.getValue();
    return data.map(source => {
      const target:any = source instanceof PmfmStrategy ? source.asObject() : source;
      target.acquisitionLevel = acquisitionLevels.find(i => i.label === target.acquisitionLevel);

      const pmfm = pmfms.find(i => i.id === target.pmfmId);
      target.pmfm = pmfm;
      delete target.pmfmId;

      if (isNotNil(target.defaultValue)) console.log("TODO check reading default value", target.defaultValue);
      target.defaultValue = PmfmValueUtils.fromModelValue(target.defaultValue, pmfm);

      return target;
    })
  }

  protected async onSave(data: PmfmStrategy[]): Promise<PmfmStrategy[]> {
    console.debug("[pmfm-strategy-table] Saving...");

    // Convert to JSON
    //return data.map(source => source instanceof PmfmStrategy ? source.asObject() : source);

    return data;
  }

  setFilter(source: Partial<PmfmStrategyFilter>, opts?: { emitEvent: boolean }) {
    const target = new PmfmStrategyFilter();
    Object.assign(target, source)
    super.setFilter(target, opts);
  }

  protected async onRowCreated(row: TableElement<PmfmStrategy>): Promise<void> {

    // Creating default values, from the current filter
    const filter = this.filter;
    const acquisitionLevelLabel = filter && filter.acquisitionLevel;
    const acquisitionLevel = acquisitionLevelLabel && (this.$acquisitionLevels.getValue() || []).find(item => item.label === acquisitionLevelLabel);
    const gearIds = filter && filter.gearIds;
    const taxonGroupIds = filter && filter.taxonGroupIds;
    const referenceTaxonIds = filter && filter.referenceTaxonIds;

    let rankOrder:number = null;
    if (acquisitionLevel) {
      rankOrder = ((await this.getMaxRankOrder(acquisitionLevel)) || 0) + 1;
    }
    const defaultValues = {
      acquisitionLevel,
      rankOrder,
      gearIds,
      taxonGroupIds,
      referenceTaxonIds
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

  protected registerFormField(fieldName: string, def: Partial<FormFieldDefinition>, intoMap?: boolean) {
    const definition = <FormFieldDefinition>{
      key: fieldName,
      label: this.i18nColumnPrefix + changeCaseToUnderscore(fieldName).toUpperCase(),
      ...def
    }
    if (intoMap === true) {
      this.fieldDefinitionsMap[fieldName] = definition;
    }
    else {
      this.fieldDefinitions.push(definition);
    }
  }


  protected async waitReferentialReady(): Promise<void> {
    // Wait end of loading referential
    if (this.$loadingReferential.getValue()) {
      await firstFalsePromise(this.$loadingReferential);
    }
  }

  protected async loadReferential() {
    console.debug("[pmfm-strategy-table] Loading referential items...");
    this.markAsLoading();

    this.$loadingReferential.next(true);

    try {
      await Promise.all([
        this.loadAcquisitionLevels(),
        this.loadPmfms(),
        this.loadPmfmsParameters(),
        this.loadPmfmsMatrix(),
        this.loadPmfmsFractions(),
        this.loadPmfmsMethods(),
        //this.loadGears(),
      ]);

      console.debug("[pmfm-strategy-table] Loaded referential items");
    } catch(err) {
      this.error = err && err.message || err;
      this.markForCheck();
    }
    finally {
      this.$loadingReferential.next(false);
    }
  }

  protected async loadAcquisitionLevels() {
    const res = await this.referentialRefService.loadAll(0, 100, null, null, {
      entityName: 'AcquisitionLevel'
    }, {withTotal: false});
    this.$acquisitionLevels.next(res && res.data || undefined)
  }

  protected async loadPmfms() {
    const res = await this.pmfmService.loadAll(0, 1000, null, null, null,
      {
        withTotal: false,
        withDetails: true
      });
    this.$pmfms.next(res && res.data || [])
  }

    protected async loadPmfmsMatrix() {
        const res = await this.pmfmService.loadAllPmfmsMatrix(0, 1000, null, null, null,
          {
            withTotal: false,
            withDetails: true
          });
        this.$pmfmsMatrix.next(res && res.data || [])
      }

      protected async loadPmfmsFractions() {
          const res = await this.pmfmService.loadAllPmfmsFractions(0, 1000, null, null, null,
            {
              withTotal: false,
              withDetails: true
            });
          this.$pmfmsFractions.next(res && res.data || [])
        }

        protected async loadPmfmsMethods() {
            const res = await this.pmfmService.loadAllPmfmsMethods(0, 1000, null, null, null,
              {
                withTotal: false,
                withDetails: true
              });
            this.$pmfmsMethods.next(res && res.data || [])
          }

  protected async loadPmfmsParameters() {
      const res = await this.referentialRefService.loadAll(0, 1000, null, null, {
          entityName: 'Parameter'
        },
        {
          withTotal: false
        });
      this.$pmfmsParameters.next(res && res.data || []);
    }

  protected async loadGears() {
    const res = await this.referentialRefService.loadAll(0, 1000, null, null, {
        entityName: 'Gear',
        levelId: this.program && this.program.gearClassification && this.program.gearClassification.id
      },
      {
        withTotal: false
      });
    this.$gears.next(res && res.data || []);
  }

  protected startEditingRow() {
    console.log("TODO start edit")
  }

  filterNumberInput = filterNumberInput;
  referentialToString = referentialToString;
  pmfmValueToString = PmfmValueUtils.valueToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

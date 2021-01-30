import {ChangeDetectionStrategy, Component, Injector} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {SampleValidatorService} from "../../services/validator/sample.validator";
import {Sample} from "../../services/model/sample.model";
import {ParameterLabelGroups} from "../../../referential/services/model/model.enum";
import {FormFieldDefinition} from "../../../shared/form/field.model";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../../core/table/table.class";
import {environment} from "../../../../environments/environment";
import {TableAddPmfmsComponent} from "../table-add-pmfms.component";
import {ProgramService} from "../../../referential/services/program.service";
import {StrategyService} from "../../../referential/services/strategy.service";
import {LandingEditorOptions, SamplesTable} from "../samples.table";

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}
export const SAMPLE2_RESERVED_START_COLUMNS: string[] = ['label', 'morseCode', 'comment' /*,'weight','totalLenghtCm','totalLenghtMm','indexGreaseRate'*/];
export const SAMPLE2_RESERVED_END_COLUMNS: string[] = [];

declare interface ColumnDefinition extends FormFieldDefinition {
  computed: boolean;
  unitLabel?: string;
  rankOrder: number;
  qvIndex: number;
}


@Component({
  selector: 'app-biological-sampling-samples-table',
  templateUrl: 'biological-sampling-samples.table.html',
  styleUrls: ['biological-sampling-samples.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: SampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BiologicalSamplingSamplesTable extends SamplesTable {

  dynamicColumns: ColumnDefinition[];

  constructor(
    protected injector: Injector,
    protected programService: ProgramService,
    protected strategyService: StrategyService
  ) {
    super(injector,
      <LandingEditorOptions>{
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: SAMPLE2_RESERVED_START_COLUMNS,
        reservedEndColumns: SAMPLE2_RESERVED_END_COLUMNS
      }
    );
  }

  /**
   * Use in ngFor, for trackBy
   * @param index
   * @param column
   */
  trackColumnDef(index: number, column: ColumnDefinition) {
    return column.rankOrder;
  }

  isQvEven(column: ColumnDefinition) {
    return (column.qvIndex % 2 === 0);
  }

  isQvOdd(column: ColumnDefinition) {
    return (column.qvIndex % 2 !== 0);
  }

  getFlexSize(columns: ColumnDefinition[], column: ColumnDefinition) {
    let columnSize = 0;
    let columnDisplayLabel = false;
    columns.forEach(colIter => {
      if (colIter.defaultValue === column.defaultValue)
      {
        columnSize = columnSize + 1;
        if ((columnSize === 1) && colIter === column) {
          columnDisplayLabel = true;
        }
      }
    });
    if (columnDisplayLabel)
    {
      return columnSize;
    }
    return 0;
  }

  protected getDisplayColumns(): string[] {

    const pmfms = this.$pmfms.getValue();
    if (!pmfms) return this.columns;

    const userColumns = this.getUserColumns();

    const dynamicColumnNames = [];
    const dynamicWeightColumnNames = [];
    const dynamicSizeColumnNames = [];
    const dynamicMaturityColumnNames = [];
    const dynamicSexColumnNames = [];
    const dynamicAgeColumnNames = [];
    const dynamicOthersColumnNames = [];

    // FIXME CLT WIP
    // filtrer sur les pmfms pour les mettres dans les différents tableaux
    (pmfms || []).map(pmfmStrategy => {
      const pmfm = pmfmStrategy.pmfm;
      if (pmfm) {
        if (pmfm.parameter && pmfm.parameter.label) {
          const label = pmfm.parameter.label;
          if (ParameterLabelGroups.AGE.includes(label)) {
            dynamicAgeColumnNames.push(pmfmStrategy.pmfmId.toString());
          }
          else if (ParameterLabelGroups.SEX.includes(label)) {
            dynamicSexColumnNames.push(pmfmStrategy.pmfmId.toString());
          }
          else if (ParameterLabelGroups.WEIGHT.includes(label)) {
            dynamicWeightColumnNames.push(pmfmStrategy.pmfmId.toString());
          }
          else if (ParameterLabelGroups.LENGTH.includes(label)) {
            dynamicSizeColumnNames.push(pmfmStrategy.pmfmId.toString());
          }
          else if (ParameterLabelGroups.MATURITY.includes(label)) {
            dynamicMaturityColumnNames.push(pmfmStrategy.pmfmId.toString());
          }
          else
          {
            // Filter on type. Fractions pmfm doesn't provide type.
            if (pmfmStrategy.type)
            {
              dynamicOthersColumnNames.push(pmfmStrategy.pmfmId.toString());
            }
          }
        }
          else {
          // Display pmfm without parameter label like fractions ?
            // Filter on type. Fractions pmfm doesn't provide type.
            if (pmfmStrategy.type)
            {
              dynamicOthersColumnNames.push(pmfmStrategy.pmfmId.toString());
            }
          }
        }
        else {
          // Display pmfm without parameter label like fractions ?
        // Filter on type. Fractions pmfm doesn't provide type.
        if (pmfmStrategy.type)
        {
          dynamicOthersColumnNames.push(pmfmStrategy.pmfmId.toString());
        }

      }
    });

    //const pmfmColumnNames = pmfms
    // .filter(p => p.isMandatory || !userColumns || userColumns.includes(p.pmfmId.toString()))
    // .map(p => p.pmfmId.toString());

    const startColumns = (this.options && this.options.reservedStartColumns || []).filter(c => !userColumns || userColumns.includes(c));
    const endColumns = (this.options && this.options.reservedEndColumns || []).filter(c => !userColumns || userColumns.includes(c));

    this.dynamicColumns = [];
    let idx = 1;
    let rankOrderIdx = 1;
    dynamicWeightColumnNames.forEach(pmfmColumnName => {
      const col = <ColumnDefinition>{
      key: pmfmColumnName,
      label: this.i18nFieldPrefix + 'WEIGHT',
      defaultValue: "WEIGHT",
      type: 'string',
      computed : false,
      qvIndex : idx,
      rankOrder : rankOrderIdx,
      disabled : false
    };
        dynamicColumnNames.push(pmfmColumnName);
        this.dynamicColumns.push(col);
    });
    if (dynamicWeightColumnNames && dynamicWeightColumnNames.length)
    {
      idx = idx + 1;
    }
    dynamicSizeColumnNames.forEach(pmfmColumnName => {
      const col = <ColumnDefinition>{
        key: pmfmColumnName,
        label: this.i18nFieldPrefix + 'SIZE',
        defaultValue: "SIZE",
        type: 'string',
        computed : false,
        qvIndex : idx,
        rankOrder : rankOrderIdx,
        disabled : false
      };
      rankOrderIdx = rankOrderIdx + 1;
      dynamicColumnNames.push(pmfmColumnName);
      this.dynamicColumns.push(col);
    });
    if (dynamicSizeColumnNames && dynamicSizeColumnNames.length)
    {
      idx = idx + 1;
    }
    dynamicMaturityColumnNames.forEach(pmfmColumnName => {
      const col = <ColumnDefinition>{
        key: pmfmColumnName,
        label: this.i18nFieldPrefix + 'MATURITY',
        defaultValue: "MATURITY",
        type: 'string',
        computed : false,
        qvIndex : idx,
        rankOrder : rankOrderIdx,
        disabled : false
      };
      rankOrderIdx = rankOrderIdx + 1;
      dynamicColumnNames.push(pmfmColumnName);
      this.dynamicColumns.push(col);
    });
    if (dynamicMaturityColumnNames && dynamicMaturityColumnNames.length) {
      idx = idx + 1;
    }
    dynamicSexColumnNames.forEach(pmfmColumnName => {
      const col = <ColumnDefinition>{
        key: pmfmColumnName,
        label: this.i18nFieldPrefix + 'SEX',
        defaultValue: "SEX",
        type: 'string',
        computed : false,
        qvIndex : idx,
        rankOrder : rankOrderIdx,
        disabled : false
      };
      rankOrderIdx = rankOrderIdx + 1;
      dynamicColumnNames.push(pmfmColumnName);
      this.dynamicColumns.push(col);
    });
    if (dynamicSexColumnNames && dynamicSexColumnNames.length)
    {
      idx = idx + 1;
    }
    dynamicAgeColumnNames.forEach(pmfmColumnName => {
      const col = <ColumnDefinition>{
        key: pmfmColumnName,
        label: this.i18nFieldPrefix + 'AGE',
        defaultValue: "AGE",
        type: 'string',
        computed : false,
        qvIndex : idx,
        rankOrder : rankOrderIdx,
        disabled : false
      };
      rankOrderIdx = rankOrderIdx + 1;
      dynamicColumnNames.push(pmfmColumnName);
      this.dynamicColumns.push(col);
    });
    if (dynamicAgeColumnNames && dynamicAgeColumnNames.length)
    {
      idx = idx + 1;
    }
    dynamicOthersColumnNames.forEach(pmfmColumnName => {
      const col = <ColumnDefinition>{
        key: pmfmColumnName,
        label: this.i18nFieldPrefix + 'OTHER',
        defaultValue: "OTHER",
        type: 'string',
        computed : false,
        qvIndex : idx,
        rankOrder : rankOrderIdx,
        disabled : false
      };
      rankOrderIdx = rankOrderIdx + 1;
      dynamicColumnNames.push(pmfmColumnName);
      this.dynamicColumns.push(col);
    });

    return RESERVED_START_COLUMNS
      .concat(startColumns)
      .concat(dynamicColumnNames)
      .concat(endColumns)
      .concat(RESERVED_END_COLUMNS)
      // Remove columns to hide
      .filter(column => !this.excludesColumns.includes(column));
  }

  /* -- protected methods -- */

  protected async addRowToTable(): Promise<TableElement<Sample>> {
    this.focusFirstColumn = true;
    await this._dataSource.asyncCreateNew();
    this.editedRow = this._dataSource.getRow(-1);
    const sample = this.editedRow.currentData;
    // Initialize default parameters
    await this.onNewEntity(sample);
    // Update row
    await this.updateEntityToTable(sample, this.editedRow);

    // Emit start editing event
    this.onStartEditingRow.emit(this.editedRow);
    this._dirty = true;
    this.resultsLength++;
    this.visibleRowCount++;
    this.markForCheck();
    return this.editedRow;
  }


  async openAddPmfmsModal(event?: UIEvent): Promise<any> {
    //const columns = this.displayedColumns;
    const pmfms = this.$pmfms.getValue();

    const modal = await this.modalCtrl.create({
      component: TableAddPmfmsComponent,
      componentProps: {pmfms: pmfms, programService: this.programService, strategyService: this.strategyService}
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED

    // Apply new pmfm
    this.displayedColumns = this.getDisplayColumns();
    this.markForCheck();

  }

  async openChangePmfmsModal(event?: UIEvent): Promise<any> {
    const modal = await this.modalCtrl.create({
      component: TableAddPmfmsComponent,
      componentProps: {}
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED

    // Apply new pmfm
    this.markForCheck();
  }
}

import {Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {TranslateService} from '@ngx-translate/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {isNil, isNotNil} from '../../shared/shared.module';
import {TableDataSource} from "angular4-material-table";
import {ExtractionColumn, ExtractionResult, ExtractionRow, ExtractionType} from "../services/extraction.model";
import {ExtractionFilter, ExtractionFilterCriterion, ExtractionService} from "../services/extraction.service";
import {AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {MatExpansionPanel, MatPaginator, MatSort, MatTable} from "@angular/material";
import {merge} from "rxjs/observable/merge";
import {TableSelectColumnsComponent} from "../../core/table/table-select-columns.component";
import {SETTINGS_DISPLAY_COLUMNS} from "../../core/table/table.class";
import {ModalController} from "@ionic/angular";
import {AccountService} from "../../core/services/account.service";
import {Location} from "@angular/common";
import {trimEmptyToNull} from "../../shared/functions";
import {distinct, first, map} from "rxjs/operators";

export const DEFAULT_PAGE_SIZE = 20;
export const DEFAULT_CRITERION_OPERATOR = '=';

@Component({
  selector: 'extract-table',
  templateUrl: './extract-table.component.html',
  styleUrls: ['./extract-table.component.scss']
})
export class ExtractTable implements OnInit {

  data: ExtractionResult;
  extractionType: ExtractionType;
  loading: boolean = true;
  filterForm: FormGroup;
  $extractionTypes: Observable<ExtractionType[]>;
  $sheetNames: Observable<string[]|undefined>;
  error: string;
  $title = new Subject<string>();
  columns: string[];
  displayedColumns: string[];
  $columns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  dataSource: TableDataSource<ExtractionRow>;
  onRefresh = new EventEmitter<any>();
  settingsId: string;
  operators: {symbol: String; name?: String;}[] = [
    {symbol:'='},
    {symbol:'!='},
    {symbol:'>'},
    {symbol:'>='},
    {symbol:'<'},
    {symbol:'<='},
    {symbol:'BETWEEN', name: "EXTRACTION.FILTER.BETWEEN"}
  ];
  showHelp = true;

  @ViewChild(MatTable) table: MatSort;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatExpansionPanel) filterExpansionPanel: MatExpansionPanel;

  get sheetName(): string {
    return this.filterForm.get('sheetName').value;
  }

  get criteriaForm(): FormArray {
    return this.filterForm.get('sheets').get(this.sheetName) as FormArray;
  }

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected location: Location,
    protected translate: TranslateService,
    protected accountService: AccountService,
    protected service: ExtractionService,
    protected formBuilder: FormBuilder,
    protected modalCtrl: ModalController
  ) {

    this.displayedColumns = []
    this.dataSource = new TableDataSource<ExtractionRow>([], ExtractionRow);
    this.filterForm = formBuilder.group({
      'extractionType': [null, Validators.required],
      'sheetName': [null],
      'sheets': formBuilder.group({})
    });

    // Load types
    this.$extractionTypes = this.service.loadTypes().first();

    // Listen route parameters
    this.route.params.subscribe(({category, label}) => {
      const extractionType  = {
        category,
        label
      };

      // If not type found in params, redirect to first one
      if (isNil(extractionType.category) || isNil(extractionType.label)) {
        return this.$extractionTypes.first().subscribe(types => {
          // no types
          if (!types || !types.length) {
            console.warn("[extract-table] No extraction types loaded !");
            return;
          }
          const selectedType = types[0];
          return this.router.navigate(['extraction', selectedType.category, selectedType.label], {
            skipLocationChange: false
          });
        })
      }

      this.$extractionTypes.first().subscribe(types => {
        // Select the exact type object in the filter form
        const selectedType = types.find(type => type.label === extractionType.label && type.category === extractionType.category);

        this.route.queryParams.first().subscribe(({sheet, q}) =>  {
          this.filterForm.get('extractionType').setValue(selectedType);

          const sheetName = sheet || (selectedType && selectedType.sheetNames && selectedType.sheetNames.length && selectedType.sheetNames[0]);
          this.filterForm.get('sheetName').setValue(sheetName);

          // Reset criteria
          this.$sheetNames = this.$sheetNames && selectedType && Observable.of(selectedType.sheetNames) || Observable.empty();
          this.resetFilterCriteria();

          if (q) {
            console.log("TODO: parse queryParams:", q);
          }

          // Load from extraction type
          return this.load(selectedType);
        });
      });
    });

  }

  async ngOnInit() {

    // If the user changes the sort order, reset back to the first page.
    this.sort && this.paginator && this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(
      this.sort && this.sort.sortChange || EventEmitter.empty(),
      this.paginator && this.paginator.page || EventEmitter.empty(),
      this.onRefresh
    )
      .subscribe(() => {
        if (this.loading || isNil(this.extractionType)) return; // avoid multiple load
        console.debug('[extract-table] Refreshing...');
        return this.load(this.extractionType);
      });

    this.$sheetNames = this.filterForm.get('extractionType').valueChanges
      .pipe(
        map((extractionType: any) => {
          if (isNotNil(extractionType) && extractionType.sheetNames) {
            return (extractionType as ExtractionType).sheetNames || [];
          }
          return [];
        })
      );

    this.filterForm.get('extractionType').valueChanges
      .pipe(
        distinct()
      )
      .subscribe(()=> this.onExtractionTypeChange());


    // this.$criteria = this.filterForm.get('sheetName').valueChanges
    //   .pipe(
    //     map((_) => (this.filterForm.get('criteria') as FormArray).controls),
    //     filter(controls => control.value )
    //   )
  }

  async load(extractionType?: ExtractionType) {

    this.loading = true;
    this.extractionType = extractionType || this.filterForm.get('extractionType').value;

    this.settingsId = this.generateTableId();
    this.error = null;
    console.debug(`[extract-table] Loading ${this.extractionType.category} ${this.extractionType.label}`);

    const filter = this.getFilterValue();
    this.filterForm.disable();

    try {
      // Load rows
      const data = await this.service.loadRows(this.extractionType,
        this.paginator && this.paginator.pageIndex * this.paginator.pageSize,
        this.paginator && this.paginator.pageSize || DEFAULT_PAGE_SIZE,
        this.sort && this.sort.active,
        this.sort && this.sort.direction,
        filter
      );

      // Update the view
      await this.updateView(data)
    }
    catch(err) {
      console.error(err);
      this.error = err && err.message || err;
      this.loading = false;
      this.filterForm.enable();
      this.filterForm.markAsDirty();
    }
  }

  async updateView(data: ExtractionResult) {

    this.data = data;

    // Update columns
    this.columns = data.columns.slice()
      // Sort by rankOder
      .sort((col1, col2) => col1.rankOrder - col2.rankOrder)
      .map(col => col.name);
    this.displayedColumns = this.columns
      .filter(columnName => columnName != "id"); // Remove id
    this.$columns.next(data.columns);

    // Update rows
    this.dataSource.updateDatasource(data.rows || []);

    // Update title
    await this.updateTitle();

    this.dataSource.connect().first().subscribe(() => {
      this.loading = false;
      this.filterForm.enable();
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
    });

  }

  async onExtractionTypeChange(extractionType?: ExtractionType): Promise<any> {
    if (this.loading) return; // skip

    const json = this.filterForm.value;

    extractionType = extractionType || (json.extractionType as ExtractionType);

    // Skip empty or same
    if (!extractionType || (this.extractionType
      && extractionType.category == this.extractionType.category
      && extractionType.label == this.extractionType.label)) return;

    return this.router.navigate([extractionType.category, extractionType.label], {
      relativeTo: this.route.parent.parent,
      queryParams: {}
    });
  }

  onSheetTabChange(sheetName: string) {
    if (this.loading || isNil(sheetName)) return; // skip

    // Skip if same
    if (this.filterForm.get('sheetName').value === sheetName) return;


    const sheetsForm = this.filterForm.get('sheets') as FormGroup;
    let criteriaForm = sheetsForm.get(sheetName);
    if (!criteriaForm) {
      criteriaForm = this.formBuilder.array([]);
      sheetsForm.addControl(sheetName, criteriaForm);
    }

    // Set sheet name
    this.filterForm.get('sheetName').setValue(sheetName);

    setTimeout(() => {
      this.router.navigate(['.'], {
        relativeTo: this.route,
        skipLocationChange: false,
        queryParams: this.getFilterAsQueryParams()
      });
    }, 500);

    this.onRefresh.emit();
  }

  async downloadAsFile() {
    if (this.loading) return;

    this.loading = true;
    const extractionType = this.filterForm.get('extractionType').value;

    this.error = null;
    console.debug(`[extract-table] Downloading ${this.extractionType.category} ${this.extractionType.label}...`);

    const filter = this.getFilterValue();
    delete filter.sheetName; // Force to download all sheets

    this.filterForm.disable();

    try {
      // Download file
      const file = await this.service.downloadFile(extractionType, filter);
      if (isNotNil((file))) {
            window.open(file);
      }

    }
    catch(err) {
      console.error(err);
      this.error = err && err.message || err;
    }
    finally {
      this.loading = false;
      this.filterForm.enable();
    }
  }

  public async openSelectColumnsModal(event: any): Promise<any> {
    var columns = this.columns
      .map((name, index) => {
        return {
          name,
          label: this.getI18nColumnName(name),
          visible: this.displayedColumns.indexOf(name) != -1
        }
      });

    const modal = await this.modalCtrl.create({ component: TableSelectColumnsComponent, componentProps: { columns: columns } });

    // On dismiss
    modal.onDidDismiss()
      .then(res => {
        if (!res) return; // CANCELLED

        // Apply columns
        this.displayedColumns = columns && columns.filter(c => c.visible).map(c => c.name) || [];

        // Update user settings
        return this.accountService.savePageSetting(this.settingsId, this.displayedColumns, SETTINGS_DISPLAY_COLUMNS);
      });
    return modal.present();
  }

  public getI18nColumnName(columnName: string): string {
    let key = `EXTRACTION.TABLE.${this.extractionType.category.toUpperCase()}.${columnName.toUpperCase()}`;
    let message = this.translate.instant(key);

    // No I18n translation
    if (message === key) {

      // Try to get common translation
      key = `EXTRACTION.COMMON.${columnName.toUpperCase()}`;
      message = this.translate.instant(key);

      // Or split column name
      if (message === key) {
        // Replace underscore with space
        message = columnName.replace(/[_-]+/g, " ").toUpperCase();
        if (message.length > 1) {
          // First letter as upper case
          message = message.substring(0,1) + message.substring(1).toLowerCase();
        }
      }
    }
    return message;
  }

  public addFilterCriterion(criterion?: ExtractionFilterCriterion, options?: {appendValue?: boolean; }): boolean {
    options = options || {};
    options.appendValue = isNotNil(options.appendValue) ? options.appendValue : false;
    console.debug("[extract-table] Adding filter criterion");

    let hasChanged = false;
    let existingCriterionIndex = -1;

    const sheetName = criterion && criterion.sheetName || this.sheetName;
    const sheetsForm = this.filterForm.get('sheets') as FormGroup;
    let criteriaControl = sheetsForm.get(sheetName) as FormArray;
    if (!criteriaControl) {
      criteriaControl = this.formBuilder.array([]);
      sheetsForm.addControl(sheetName, criteriaControl);
    }
    else {

      // Search by name on existing criteria
      if (criterion && isNotNil(criterion.name)) {
        existingCriterionIndex = (criteriaControl.value || []).findIndex(c => (c.name === criterion.name));
      }

      // If last criterion has no value: use it
      if (existingCriterionIndex == -1 && criteriaControl.length){
        // Find last criterion (so reverse array order)
        const lastCriterion = criteriaControl.at(criteriaControl.length-1).value as ExtractionFilterCriterion;
        existingCriterionIndex = isNil(lastCriterion.name) && isNil(lastCriterion.value) ? criteriaControl.length-1 : -1;
      }
    }

    // Replace the existing criterion
    if (existingCriterionIndex >= 0) {
      if (criterion && criterion.name) {
        const criterionForm = criteriaControl.at(existingCriterionIndex) as FormGroup;
        const existingCriterion = criterionForm.value as ExtractionFilterCriterion;
        options.appendValue = options.appendValue && isNotNil(criterion.value) && isNotNil(existingCriterion.value)
          && (existingCriterion.operator == '=' || existingCriterion.operator == '!=');

        // Append value to existing value
        if (options.appendValue){
          existingCriterion.value += ", " + criterion.value;
          this.setCriterionValue(criterionForm, existingCriterion);
        }

        // Replace existing criterion value
        else {
          this.setCriterionValue(criterionForm, criterion);
        }
        hasChanged = true;
      }
    }

    // Add a new criterion
    else {
      criteriaControl.push(this.formBuilder.group({
        name: [criterion && criterion.name || null],
        operator: [criterion && criterion.operator || '=', Validators.required],
        value: [criterion && criterion.value || null],
        endValue: [criterion && criterion.endValue || null],
        sheetName: [criterion && criterion.sheetName || this.sheetName]
      }));
      hasChanged = true;
    }

    // Mark filter form as dirty (if need)
    if (hasChanged && criterion && criterion.value) {
      this.filterForm.markAsDirty({onlySelf: true});
    }

    return hasChanged;
  }

  public hasFilterCriteria(sheetName?: string): boolean {
    if (isNil(sheetName)) {
      const control = this.filterForm.get('sheets').get(this.sheetName) as FormArray;
      if (control && control.length === 1) {
        const criterion = control.at(0).value as ExtractionFilterCriterion;
        return trimEmptyToNull(criterion.value) && true;
      }
      return control && control.length > 1;
    }
    else {
      const control = this.filterForm.get('sheets').get(sheetName) as FormArray;
      return control && control.controls
        .map(c => c.value)
        .findIndex(criterion => trimEmptyToNull(criterion.value) && true) >= 0;
    }
  }

  public removeFilterCriterion($event: MouseEvent, index) {
    const control = this.filterForm.get('sheets').get(this.sheetName) as FormArray;

    // Do not remove if last criterion
    if (control.length == 1) {
      this.clearFilterCriterion($event, index);
      return;
    }

    control.removeAt(index);

    if (!$event.ctrlKey) {
      this.onRefresh.emit();
    }
    else {
      this.filterForm.markAsDirty();
    }
  }

  public clearFilterCriterion($event: MouseEvent, index): boolean {
    const control = this.filterForm.get('sheets').get(this.sheetName) as FormArray;

    const oldValue = control.at(index).value;
    let needClear = (isNotNil(oldValue.name) || isNotNil(oldValue.value));
    if (!needClear) return false;

    this.setCriterionValue(control.at(index), null);

    if (!$event.ctrlKey) {
      this.onRefresh.emit();
    }
    else {
      this.filterForm.markAsDirty();
    }
    return false;
  }

  public resetFilterCriteria() {

    // Close the filter panel
    if (this.filterExpansionPanel && this.filterExpansionPanel.expanded) {
      this.filterExpansionPanel.close();
    }

    // Remove all criterion
    const control = this.filterForm.get('sheets') as FormGroup;
    Object.getOwnPropertyNames(control.controls).forEach(sheetName => control.removeControl(sheetName));

    // Add the default (empty)
    this.$sheetNames && this.$sheetNames.pipe(first()).subscribe(sheetNames => {
     (sheetNames||[]).forEach(sheetName => this.addFilterCriterion({name: null, operator: '=', sheetName: sheetName}));
    });

  }

  public onCellValueClick($event: MouseEvent, column: ExtractionColumn, value: string) {
    const hasChanged = this.addFilterCriterion({
      name: column.name,
      operator: DEFAULT_CRITERION_OPERATOR,
      value: value,
      sheetName: this.sheetName
    }, {
      appendValue : $event.ctrlKey
    });
    if (!hasChanged) return;

    if (!this.filterExpansionPanel.expanded) {
      this.filterExpansionPanel.open();
    }

    if (!$event.ctrlKey) {
      this.onRefresh.emit();
    }
  }

  public getI18nExtractionTypeName(type? : ExtractionType, self?: ExtractTable):string {
    self = self || this;
    if (isNil(type)) return undefined;
    let key = `EXTRACTION.${type.category.toUpperCase()}.${type.label.toUpperCase()}.TITLE`;
    let message = self.translate.instant(key);

    // No I18n translation
    if (message === key) {

      // Replace underscore with space
      message = type.label.replace(/[_-]+/g, " ").toUpperCase();
      if (message.length > 1) {
        // First letter as upper case
        message = message.substring(0, 1) + message.substring(1).toLowerCase();
      }
    }
    return message;
  }

  public getI18nSheetName(sheetName?: string, self?: ExtractTable):string {
    self = self || this;
    if (isNil(sheetName) || isNil(self.extractionType)) return undefined;
    const type = self.filterForm.controls['extractionType'].value;
    let key = `EXTRACTION.${type.category.toUpperCase()}.${type.label.toUpperCase()}.SHEET.${sheetName}`;
    let message = self.translate.instant(key);

    // No I18n translation
    if (message === key) {

      // Replace underscore with space
      message = sheetName.replace(/[_-]+/g, " ").toUpperCase();
    }
    return message;
  }

  public newI18nExtractionTypeNameFn(): (type? : ExtractionType) => string {
    const self= this;
    return function(type? : ExtractionType): string {
      return self.getI18nExtractionTypeName(type);
    }
  }

  /* -- private method -- */

  private async updateTitle() {
    const key = `EXTRACTION.CATEGORY.${this.extractionType.category.toUpperCase()}`;
    let title = await this.translate.get(key).toPromise();
    if (title === key) {
      console.warn("Missing i18n key '" + key + "'");
      this.$title.next("");
    }
    else {
      this.$title.next(title);
    }
  }

  private generateTableId() {
    const id = this.location.path(true).replace(/[?].*$/g, '').replace(/\/[\d]+/g, '_id') + "_" + this.constructor.name;
    return id;
  }

  private setCriterionValue(control: AbstractControl, criterion?: ExtractionFilterCriterion) {
    const sheetName = this.filterForm.get('sheetName').value;
    control.setValue({
      name: criterion && criterion.name || null,
      operator: criterion && criterion.operator || DEFAULT_CRITERION_OPERATOR,
      value: criterion && criterion.value || null,
      endValue: criterion && criterion.endValue || null,
      sheetName: criterion && sheetName || null
    });
  }

  private getFilterValue(): ExtractionFilter {
    const filter = this.filterForm.value;
    if (!filter) return undefined;
    filter.criteria = filter.sheets && Object.getOwnPropertyNames(filter.sheets).reduce((res, sheetName) => {
      return res.concat(filter.sheets[sheetName]);
    }, []);
    delete filter.sheets;
    return this.service.prepareFilter(filter);
  }

  private getFilterAsQueryParams(): any {
    const filter = this.getFilterValue();
    const params: any = {};
    if (filter.sheetName) {
      params.sheet=filter.sheetName;
    }
    if (filter.criteria && filter.criteria.length) {
      params.q = filter.criteria.reduce((res, criterion)=> {
        if (criterion.endValue) {
          return res.concat(`${criterion.name}${criterion.operator}${criterion.value}:${criterion.endValue}`);
        }
        else {
          return res.concat(`${criterion.name}${criterion.operator}${criterion.value}`);
        }
      }, []).join(";");
    }
    return params;
  }

}

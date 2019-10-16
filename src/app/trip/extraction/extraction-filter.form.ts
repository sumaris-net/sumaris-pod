import {EventEmitter, Injector, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {TranslateService} from '@ngx-translate/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {DateFormatPipe, isNil, isNotNil, isNotNilOrBlank} from '../../shared/shared.module';
import {AggregationType, ExtractionType} from "../services/extraction.model";
import {ExtractionFilter, ExtractionFilterCriterion, ExtractionService} from "../services/extraction.service";
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {trimEmptyToNull} from "../../shared/functions";
import {debounceTime, distinctUntilChanged, filter, first, map, mergeMap} from "rxjs/operators";
import {AccountService, AppForm, LocalSettingsService} from "../../core/core.module";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {filterNotNil, firstNotNil, firstNotNilPromise} from "../../shared/observables";


export const DEFAULT_CRITERION_OPERATOR = '=';

export abstract class ExtractionForm<T extends ExtractionType | AggregationType> extends AppForm<any> implements OnInit {

  protected routePath = 'extraction';

  protected dateAdapter: DateAdapter<Moment>;
  protected formBuilder: FormBuilder;
  protected route: ActivatedRoute;
  protected router: Router;
  protected translate: TranslateService;
  protected service: ExtractionService;
  protected accountService: AccountService;

  canEdit = false;
  loading = true;
  type: T;

  typesPopoverOptions: any = {
    showBackdrop: true
  };

  operators: { symbol: String; name?: String; }[] = [
    {symbol: '='},
    {symbol: '!='},
    {symbol: '>'},
    {symbol: '>='},
    {symbol: '<'},
    {symbol: '<='},
    {symbol: 'BETWEEN', name: "EXTRACTION.FILTER.BETWEEN"}
  ];
  onRefresh = new EventEmitter<any>();

  $types = new BehaviorSubject<T[]>(undefined);
  $sheetNames: Observable<string[]>;

  get sheetName(): string {
    return this.form.controls.sheetName.value;
  }

  get sheetCriteriaForm(): FormArray {
    return this.form.get('sheets.' + this.sheetName) as FormArray;
  }

  protected constructor(
    protected injector: Injector,
    form?: FormGroup
  ) {
    super(injector.get(DateFormatPipe), form, injector.get(LocalSettingsService));
    this.formBuilder = injector.get(FormBuilder);
    this.route = injector.get(ActivatedRoute);
    this.router = injector.get(Router);
    this.translate = injector.get(TranslateService);
    this.service = injector.get(ExtractionService);
    this.accountService = injector.get(AccountService);
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this.translate.get('EXTRACTION.TYPE').subscribe(msg => {
        this.typesPopoverOptions.header = msg;
      }));

    // Load types
    this.registerSubscription(
      this.watchTypes()
        .subscribe(types => this.$types.next(types))
    );

    // Listen route parameters
    this.registerSubscription(
      this.route.queryParams
        .pipe(
          // Convert query params into a valid type
          mergeMap(async ({category, label, sheet}) => {
          const paramType = this.fromObject({category, label});

          // Read type
          const types = await firstNotNilPromise(this.$types);
          let selectedType;

          // If not type found in params, redirect to first one
          if (isNil(paramType.category) || isNil(paramType.label)) {
            selectedType = types && types[0];
          }

          // Select the exact type object in the filter form
          else {
            selectedType = types.find(t => ExtractionType.equals(t, paramType)) || paramType;
          }

          const selectedSheetName = sheet || (selectedType && selectedType.sheetNames && selectedType.sheetNames.length && selectedType.sheetNames[0]);
          if (selectedSheetName && selectedType && !selectedType.sheetNames) {
            selectedType.sheetNames = [selectedSheetName];
          }

          return {selectedType, selectedSheetName};
        }))

        .subscribe(async ({selectedType, selectedSheetName}) => {
          // Set the type
          await this.setType(selectedType, {
            sheetName: selectedSheetName,
            emitEvent: false,
            skipLocationChange: true // Here, we not need an update of the location
          });

          // Execute the first load
          await this.load(this.type);

        }));

    this.$sheetNames = this.form.get('type').valueChanges
      .pipe(
        map((type: any) => {
          if (isNotNil(type) && type.sheetNames) {
            return (type as T).sheetNames || [];
          }
          return [];
        })
      );

  }

  protected async abstract load(type?: T);

  protected abstract watchTypes(): Observable<T[]>;

  protected abstract fromObject(type?: any): T;

  compareWith = ExtractionType.equals;

  onSelectTypeChange(event: CustomEvent<{ value: T }>) {
    const type = event.detail.value;
    if (!type) return; // skip
    this.setType(type);
  }

  async setType(type: T, opts?: { emitEvent?: boolean; skipLocationChange?: boolean; sheetName?: string; }) {
    opts = opts || {};
    opts.emitEvent = isNotNil(opts.emitEvent) ? opts.emitEvent : true;
    opts.skipLocationChange = isNotNil(opts.skipLocationChange) ? opts.skipLocationChange : false;

    // If empty or same: skip
    if (!type || ExtractionType.equals(type, this.type)) return;

    const types = await firstNotNilPromise(this.$types);
    const selectType = types.find(t => ExtractionType.equals(t, type));

    if (!selectType) {
      console.warn("[extraction-form] Type not found:", type);
      return;
    }

    console.debug(`[extraction-form] Set type to {${selectType.label}}`, selectType);
    this.type = selectType;
    this.form.get('type').patchValue(type, opts);

    // Check if user can edit (admin or supervisor in the rec department)
    this.canEdit = this.canUserWrite(selectType);

    // Select the given sheet, or the first one
    const sheetName = opts.sheetName || (selectType.sheetNames && selectType.sheetNames[0]);
    this.onSheetChange(sheetName || null,
      {
        emitEvent: false,
        skipLocationChange: true
      });

    // Reset criteria
    this.resetFilterCriteria();

    // Update the window location
    if (opts.skipLocationChange === false) {
      setTimeout(() => this.updateLocationParams(type), 500);
    }

    // Refresh data
    if (opts.emitEvent === true) {
      this.onRefresh.emit();
    }
  }

  /**
   * Update the URL
   */
  updateLocationParams(type: T) {
    console.debug('[extraction-form] Updating query params', type);

    this.router.navigate(['.'], {
      relativeTo: this.route,
      skipLocationChange: false,
      queryParams: this.getFilterAsQueryParams()
    });
  }

  onSheetChange(sheetName: string, opts?: { emitEvent?: boolean; skipLocationChange?: boolean; }) {
    opts = opts || {emitEvent: !this.loading};
    // Skip if same, or loading
    if (isNil(sheetName) || this.sheetName === sheetName) return;

    const sheetsForm = this.form.get('sheets') as FormGroup;
    let criteriaForm = sheetsForm.get(sheetName);
    if (!criteriaForm) {
      criteriaForm = this.formBuilder.array([]);
      sheetsForm.addControl(sheetName, criteriaForm);
    }

    // Set sheet name
    this.form.get('sheetName').patchValue(sheetName, opts);

    if (opts.skipLocationChange !== true) {
      setTimeout(() => this.updateLocationParams(this.type), 500);
    }

    if (opts.emitEvent !== false) {
      this.onRefresh.emit();
    }
  }

  public async downloadAsFile() {
    if (this.loading) return;

    this.loading = true;
    const type = this.form.get('type').value;

    this.error = null;
    console.debug(`[extraction-form] Downloading ${type.category} ${type.label}...`);

    const filter = this.getFilterValue();
    delete filter.sheetName; // Force to download all sheets

    this.disable();

    try {
      // Download file
      const file = await this.service.downloadFile(type, filter);
      if (isNotNil((file))) {
        window.open(file);
      }

    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
    } finally {
      this.loading = false;
      this.enable();
    }
  }

  public addFilterCriterion(criterion?: ExtractionFilterCriterion, options?: { appendValue?: boolean; }): boolean {
    options = options || {};
    options.appendValue = isNotNil(options.appendValue) ? options.appendValue : false;
    console.debug("[extraction-form] Adding filter criterion");

    let hasChanged = false;
    let index = -1;

    const sheetName = criterion && criterion.sheetName || this.sheetName;
    const sheetsForm = this.form.get('sheets') as FormGroup;
    let arrayControl = sheetsForm.get(sheetName) as FormArray;
    if (!arrayControl) {
      arrayControl = this.formBuilder.array([]);
      sheetsForm.addControl(sheetName, arrayControl);
    } else {

      // Search by name on existing criteria
      if (criterion && isNotNil(criterion.name)) {
        index = (arrayControl.value || []).findIndex(c => (c.name === criterion.name));
      }

      // If last criterion has no value: use it
      if (index === -1 && arrayControl.length) {
        // Find last criterion (so reverse array order)
        const lastCriterion = arrayControl.at(arrayControl.length - 1).value as ExtractionFilterCriterion;
        index = isNil(lastCriterion.name) && isNil(lastCriterion.value) ? arrayControl.length - 1 : -1;
      }
    }

    // Replace the existing criterion value
    if (index >= 0) {
      if (criterion && criterion.name) {
        const criterionForm = arrayControl.at(index) as FormGroup;
        const existingCriterion = criterionForm.value as ExtractionFilterCriterion;
        options.appendValue = options.appendValue && isNotNil(criterion.value) && isNotNil(existingCriterion.value)
          && (existingCriterion.operator === '=' || existingCriterion.operator === '!=');

        // Append value to existing value
        if (options.appendValue) {
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

    // Add a new criterion (formGroup + value)
    else {
      const criterionForm = this.formBuilder.group({
        name: [criterion && criterion.name || null],
        operator: [criterion && criterion.operator || '=', Validators.required],
        value: [criterion && criterion.value || null],
        endValue: [criterion && criterion.endValue || null],
        sheetName: [criterion && criterion.sheetName || this.sheetName]
      });
      const criterionValueControl = criterionForm.controls.value;
      criterionForm.controls.name.valueChanges
        .pipe(
          debounceTime(250),
          distinctUntilChanged()
        )
        .subscribe(value => {
          if (isNotNilOrBlank(value)) {
            if (criterionValueControl.disabled) {
              criterionValueControl.enable();
            }
          }
          else if (criterionValueControl.enabled) {
            criterionValueControl.disable();
          }
        });
      arrayControl.push(criterionForm);
      hasChanged = true;
    }

    // Mark filter form as dirty (if need)
    if (hasChanged && criterion && criterion.value) {
      this.form.markAsDirty({onlySelf: true});
    }

    return hasChanged;
  }

  public hasFilterCriteria(sheetName?: string): boolean {
    if (isNil(sheetName)) {
      const arrayControl = this.sheetCriteriaForm;
      if (arrayControl && arrayControl.length === 1) {
        const criterion = arrayControl.at(0).value as ExtractionFilterCriterion;
        return isNotNilOrBlank(criterion.value);
      }
      return arrayControl && arrayControl.length > 1;
    } else {
      const control = this.form.get('sheets.' + sheetName) as FormArray;
      return control && control.controls
        .map(c => c.value)
        .findIndex(criterion => criterion && isNotNilOrBlank(criterion.value)) !== -1;
    }
  }

  public removeFilterCriterion($event: MouseEvent, index: number) {
    const arrayControl = this.form.get('sheets').get(this.sheetName) as FormArray;

    // Do not remove if last criterion
    if (arrayControl.length === 1) {
      this.clearFilterCriterion($event, index);
      return;
    }

    arrayControl.removeAt(index);

    if (!$event.ctrlKey) {
      this.onRefresh.emit();
    } else {
      this.form.markAsDirty();
    }
  }

  public clearFilterCriterion($event: MouseEvent, index: number): boolean {
    const arrayControl = this.form.get('sheets').get(this.sheetName) as FormArray;

    const oldValue = arrayControl.at(index).value;
    const needClear = (isNotNil(oldValue.name) || isNotNil(oldValue.value));
    if (!needClear) return false;

    this.setCriterionValue(arrayControl.at(index), null);

    if (!$event.ctrlKey) {
      this.onRefresh.emit();
    } else {
      this.form.markAsDirty();
    }
    return false;
  }

  public resetFilterCriteria() {
    // Remove all criterion
    const control = this.form.get('sheets') as FormGroup;
    Object.getOwnPropertyNames(control.controls).forEach(sheetName => control.removeControl(sheetName));

    // Add the default (empty)
    this.$sheetNames.pipe(first()).subscribe(sheetNames => {
      (sheetNames || []).forEach(sheetName => this.addFilterCriterion({
        name: null,
        operator: '=',
        sheetName: sheetName
      }));
    });

  }

  public getI18nTypeName(type?: T, self?: ExtractionForm<T>): string {
    self = self || this;
    if (isNil(type)) return undefined;
    const key = `EXTRACTION.${type.category}.${type.format}.TITLE`.toUpperCase();
    let message = self.translate.instant(key);

    if (message !== key) return message;
    // No I18n translation: continue

    // Use name, or label (but replace underscore with space)
    message = type.name || type.label.replace(/[_-]+/g, " ").toUpperCase();
    // First letter as upper case
    if (message.length > 1) {
      return message.substring(0, 1) + message.substring(1).toLowerCase();
    }
    return message;
  }

  public getI18nSheetName(sheetName?: string, self?: ExtractionForm<T>): string {
    self = self || this;
    const type = self.type;
    sheetName = sheetName || this.sheetName;
    if (isNil(sheetName) || isNil(type)) return undefined;

    // Try from specific translation
    let key = `EXTRACTION.${type.category.toUpperCase()}.${type.label.toUpperCase()}.SHEET.${sheetName}`;
    let message = self.translate.instant(key);
    if (message !== key) return message;

    // Try from generic translation
    key = `EXTRACTION.SHEET.${sheetName}`;
    message = self.translate.instant(key);
    if (message !== key) return message;

    // No translation found: replace underscore with space
    return sheetName.replace(/[_-]+/g, " ").toUpperCase();
  }

  public getI18nColumnName(columnName: string, self?: ExtractionForm<T>): string {
    self = self || this;
    const type = self.form.controls['type'].value;
    let key = `EXTRACTION.TABLE.${type.category.toUpperCase()}.${columnName.toUpperCase()}`;
    let message = self.translate.instant(key);

    // No I18n translation
    if (message === key) {

      // Try to get common translation
      key = `EXTRACTION.COMMON.${columnName.toUpperCase()}`;
      message = self.translate.instant(key);

      // Or split column name
      if (message === key) {
        // Replace underscore with space
        message = columnName.replace(/[_-]+/g, " ").toUpperCase();
        if (message.length > 1) {
          // First letter as upper case
          message = message.substring(0, 1) + message.substring(1).toLowerCase();
        }
      }
    }
    return message;
  }

  /* -- protected method -- */

  protected setCriterionValue(control: AbstractControl, criterion?: ExtractionFilterCriterion) {
    const sheetName = this.form.get('sheetName').value;
    control.setValue({
      name: criterion && criterion.name || null,
      operator: criterion && criterion.operator || DEFAULT_CRITERION_OPERATOR,
      value: criterion && criterion.value || null,
      endValue: criterion && criterion.endValue || null,
      sheetName: criterion && sheetName || null
    });
  }

  protected getFilterValue(): ExtractionFilter {
    if (!this._enable) this.form.enable({emitEvent: false});
    const filter = this.form.value;
    if (!this._enable) this.form.disable({emitEvent: false});
    if (!filter) return undefined;
    filter.criteria = filter.sheets && Object.getOwnPropertyNames(filter.sheets).reduce((res, sheetName) => {
      return res.concat(filter.sheets[sheetName]);
    }, []);
    delete filter.sheets;
    return this.service.prepareFilter(filter);
  }

  protected getFilterAsQueryParams(): any {
    const params: any = {
      category: this.type && this.type.category,
      label: this.type && this.type.label
    };
    const filter = this.getFilterValue();
    if (filter.sheetName) {
      params.sheet = filter.sheetName;
    }
    if (filter.criteria && filter.criteria.length) {
      params.q = filter.criteria.reduce((res, criterion) => {
        if (criterion.endValue) {
          return res.concat(`${criterion.name}${criterion.operator}${criterion.value}:${criterion.endValue}`);
        } else {
          return res.concat(`${criterion.name}${criterion.operator}${criterion.value}`);
        }
      }, []).join(";");
    }
    return params;
  }

  protected canUserWrite(type: ExtractionType): boolean {
    return type.category === "product" && (
      this.accountService.isAdmin()
      || (this.accountService.isSupervisor() && this.accountService.canUserWriteDataForDepartment(type.recorderDepartment)))
  }
}

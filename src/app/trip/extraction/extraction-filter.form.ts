import {EventEmitter, OnInit} from '@angular/core';
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {TranslateService} from '@ngx-translate/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {isNil, isNotNil} from '../../shared/shared.module';
import {ExtractionType} from "../services/extraction.model";
import {ExtractionFilter, ExtractionFilterCriterion, ExtractionService} from "../services/extraction.service";
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {trimEmptyToNull} from "../../shared/functions";
import {distinct, filter, first, map, switchMap, zip} from "rxjs/operators";
import {AppForm} from "../../core/core.module";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";


export const DEFAULT_CRITERION_OPERATOR = '=';

export abstract class ExtractionForm<T extends ExtractionType> extends AppForm<any> implements OnInit {

  protected routePath = 'extraction';

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

  $types: Observable<T[]>;
  $sheetNames: Observable<string[]>;

  get sheetName(): string {
    return this.form.get('sheetName').value;
  }

  get criteriaForm(): FormArray {
    return this.form.get('sheets').get(this.sheetName) as FormArray;
  }

  protected constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected route: ActivatedRoute,
    protected router: Router,
    protected translate: TranslateService,
    protected service: ExtractionService,
    public form?: FormGroup
  ) {
    super(dateAdapter, form);

  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this.translate.get('EXTRACTION.TYPE').subscribe(msg => {
        this.typesPopoverOptions.header = msg;
      }));

    // Load types
    this.$types = this.loadTypes();

    // Listen route parameters
    this.registerSubscription(
      this.route.queryParams
        .subscribe(async ({category, label, sheet}) => {
          const paramType = this.fromObject({category, label});

          const types = await this.$types.toPromise();
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
          if (!selectedType.sheetNames && selectedSheetName) {
            selectedType.sheetNames = [selectedSheetName];
          }

          await this.setType(selectedType, {emitEvent: false, skipLocationChange: true});
          this.form.get('sheetName').patchValue(selectedSheetName, {emitEvent: false});
          this.markForCheck();

          // Reset criteria
          this.resetFilterCriteria();

          // TODO: parse queryParams: convert into criterion?

          // Load from extraction type
          return this.load(selectedType);
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

    // this.form.get('type').valueChanges
    //   .pipe(
    //     distinct()
    //   )
    //   .subscribe(() => this.onTypeChange());

  }

  protected async abstract load(type?: T);

  protected abstract loadTypes(): Observable<T[]>;

  protected abstract fromObject(type?: any): T;

  compareWith = ExtractionType.equals;

  onSelectTypeChange(event: CustomEvent<{ value: T }>) {
    const type = event.detail.value;
    if (!type) return; // skip

    this.setType(type);

    // Reset criteria
    this.resetFilterCriteria();
  }

  async setType(type: T, opts?: { emitEvent?: boolean; skipLocationChange?: boolean; }) {
    opts = opts || {emitEvent: true, skipLocationChange: false};

    // If empty or same: skip
    if (!type || ExtractionType.equals(type, this.type)) return;

    const types = await this.$types.pipe(first()).toPromise();
    const selectType = types.find(t => ExtractionType.equals(t, type));

    if (!selectType) {
      console.warn("[extraction-form] Type not found:", type);
      return;
    }

    console.debug(`[extraction-form] Set type to {${selectType.label}}`, selectType);
    this.type = selectType;
    this.form.get('type').patchValue(type, opts);

    // Refresh data (default: true)
    if (opts.emitEvent !== false) {
      this.onRefresh.emit();
    }

    // Update the window location (default: true)
    if (opts.skipLocationChange !== true) {
      this.updateLocationParams(type);
    }
  }

  /**
   * Update the URL
   */
  updateLocationParams(type: T) {
    console.debug('[extraction-form] Updating query params', type);
    this.router.navigate([this.routePath], {
      queryParams: {
        category: type.category,
        label: type.label
      }
    });
  }

  onSheetChange(sheetName: string) {
    // Skip if same, or loading
    if (this.loading || isNil(sheetName) || this.sheetName === sheetName) return;

    const sheetsForm = this.form.get('sheets') as FormGroup;
    let criteriaForm = sheetsForm.get(sheetName);
    if (!criteriaForm) {
      criteriaForm = this.formBuilder.array([]);
      sheetsForm.addControl(sheetName, criteriaForm);
    }

    // Set sheet name
    this.form.get('sheetName').setValue(sheetName);

    setTimeout(() => {
      this.router.navigate(['.'], {
        relativeTo: this.route,
        skipLocationChange: false,
        queryParams: this.getFilterAsQueryParams()
      });
    }, 500);

    this.onRefresh.emit();
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

    // Replace the existing criterion
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

    // Add a new criterion
    else {
      arrayControl.push(this.formBuilder.group({
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
      this.form.markAsDirty({onlySelf: true});
    }

    return hasChanged;
  }

  public hasFilterCriteria(sheetName?: string): boolean {
    if (isNil(sheetName)) {
      const arrayControl = this.form.get('sheets').get(this.sheetName) as FormArray;
      if (arrayControl && arrayControl.length === 1) {
        const criterion = arrayControl.at(0).value as ExtractionFilterCriterion;
        return trimEmptyToNull(criterion.value) && true;
      }
      return arrayControl && arrayControl.length > 1;
    } else {
      const control = this.form.get('sheets').get(sheetName) as FormArray;
      return control && control.controls
        .map(c => c.value)
        .findIndex(criterion => trimEmptyToNull(criterion.value) && true) >= 0;
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

    // Replace underscore with space
    message = type.label.replace(/[_-]+/g, " ").toUpperCase();
    if (message.length > 1) {
      // First letter as upper case
      return message.substring(0, 1) + message.substring(1).toLowerCase();
    }
    return message;
  }

  public getI18nSheetName(sheetName?: string, self?: ExtractionForm<T>): string {
    self = self || this;
    if (isNil(sheetName) || isNil(self.type)) return undefined;
    const type = self.form.controls['type'].value;
    const key = `EXTRACTION.${type.category.toUpperCase()}.${type.label.toUpperCase()}.SHEET.${sheetName}`;
    let message = self.translate.instant(key);

    // No I18n translation
    if (message === key) {

      // Replace underscore with space
      message = sheetName.replace(/[_-]+/g, " ").toUpperCase();
    }
    return message;
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
    const filter = this.getFilterValue();
    const params: any = {};
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

}

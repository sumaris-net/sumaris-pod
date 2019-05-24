import {EventEmitter, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {TranslateService} from '@ngx-translate/core';
import {Observable} from 'rxjs';
import {isNil, isNotNil} from '../../shared/shared.module';
import {ExtractionType} from "../services/extraction.model";
import {ExtractionFilter, ExtractionFilterCriterion, ExtractionService} from "../services/extraction.service";
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {trimEmptyToNull} from "../../shared/functions";
import {distinct, first, map} from "rxjs/operators";
import {AppForm} from "../../core/core.module";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";


export const DEFAULT_CRITERION_OPERATOR = '=';

export abstract class ExtractionForm<T extends ExtractionType> extends AppForm<any> implements OnInit {

  protected routePath = 'extraction';

  loading = true;
  type: T;

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
  $sheetNames: Observable<string[] | undefined>;

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

    // Load types
    this.$types = this.loadTypes();

    // Listen route parameters
    this.route.params.subscribe(({category, label}) => {
      const pathType = {
        category,
        label
      };

      // If not type found in params, redirect to first one
      if (isNil(pathType.category) || isNil(pathType.label)) {
        return this.$types.first().subscribe(types => {
          // no types
          if (!types || !types.length) {
            console.warn("[extraction-form] No extraction types loaded !");
            return;
          }
          const selectedType = types[0];
          return this.router.navigate([this.routePath, selectedType.category, selectedType.label], {
            skipLocationChange: false
          });
        });
      }

      this.$types.first().subscribe(types => {
        // Select the exact type object in the filter form
        const selectedType = types.find(t => t.label === pathType.label && t.category === pathType.category);

        this.route.queryParams.first().subscribe(({sheet, q}) => {
          this.form.get('type').setValue(selectedType);

          const sheetName = sheet || (selectedType && selectedType.sheetNames && selectedType.sheetNames.length && selectedType.sheetNames[0]);
          this.form.get('sheetName').setValue(sheetName);

          // Reset criteria
          this.$sheetNames = this.$sheetNames && selectedType && Observable.of(selectedType.sheetNames) || Observable.empty();
          this.resetFilterCriteria();

          // TODO: parse queryParams: convert into criterion?

          // Load from extraction type
          return this.load(selectedType);
        });
      });
    });

  }

  ngOnInit() {
    super.ngOnInit();

    this.$sheetNames = this.form.get('type').valueChanges
      .pipe(
        map((type: any) => {
          if (isNotNil(type) && type.sheetNames) {
            return (type as T).sheetNames || [];
          }
          return [];
        })
      );

    this.form.get('type').valueChanges
      .pipe(
        distinct()
      )
      .subscribe(() => this.onTypeChange());

  }

  protected async abstract load(type?: T);

  protected abstract loadTypes(): Observable<T[]>;

  async onTypeChange(type?: T): Promise<any> {
    if (this.loading) return; // skip

    const json = this.form.value;

    type = type || (json.type as T);

    // If empty or same: skip
    if (!type || (this.type
      && type.category === this.type.category
      && type.label === this.type.label)) return;

    // Update the URL
    return this.router.navigate([this.routePath, type.category, type.label], {
      skipLocationChange: false,
      queryParams: {}
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
    console.debug(`[extraction-form] Downloading ${this.type.category} ${this.type.label}...`);

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
    let existingCriterionIndex = -1;

    const sheetName = criterion && criterion.sheetName || this.sheetName;
    const sheetsForm = this.form.get('sheets') as FormGroup;
    let criteriaControl = sheetsForm.get(sheetName) as FormArray;
    if (!criteriaControl) {
      criteriaControl = this.formBuilder.array([]);
      sheetsForm.addControl(sheetName, criteriaControl);
    } else {

      // Search by name on existing criteria
      if (criterion && isNotNil(criterion.name)) {
        existingCriterionIndex = (criteriaControl.value || []).findIndex(c => (c.name === criterion.name));
      }

      // If last criterion has no value: use it
      if (existingCriterionIndex === -1 && criteriaControl.length) {
        // Find last criterion (so reverse array order)
        const lastCriterion = criteriaControl.at(criteriaControl.length - 1).value as ExtractionFilterCriterion;
        existingCriterionIndex = isNil(lastCriterion.name) && isNil(lastCriterion.value) ? criteriaControl.length - 1 : -1;
      }
    }

    // Replace the existing criterion
    if (existingCriterionIndex >= 0) {
      if (criterion && criterion.name) {
        const criterionForm = criteriaControl.at(existingCriterionIndex) as FormGroup;
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
      this.form.markAsDirty({onlySelf: true});
    }

    return hasChanged;
  }

  public hasFilterCriteria(sheetName?: string): boolean {
    if (isNil(sheetName)) {
      const control = this.form.get('sheets').get(this.sheetName) as FormArray;
      if (control && control.length === 1) {
        const criterion = control.at(0).value as ExtractionFilterCriterion;
        return trimEmptyToNull(criterion.value) && true;
      }
      return control && control.length > 1;
    } else {
      const control = this.form.get('sheets').get(sheetName) as FormArray;
      return control && control.controls
        .map(c => c.value)
        .findIndex(criterion => trimEmptyToNull(criterion.value) && true) >= 0;
    }
  }

  public removeFilterCriterion($event: MouseEvent, index) {
    const control = this.form.get('sheets').get(this.sheetName) as FormArray;

    // Do not remove if last criterion
    if (control.length === 1) {
      this.clearFilterCriterion($event, index);
      return;
    }

    control.removeAt(index);

    if (!$event.ctrlKey) {
      this.onRefresh.emit();
    } else {
      this.form.markAsDirty();
    }
  }

  public clearFilterCriterion($event: MouseEvent, index): boolean {
    const control = this.form.get('sheets').get(this.sheetName) as FormArray;

    const oldValue = control.at(index).value;
    let needClear = (isNotNil(oldValue.name) || isNotNil(oldValue.value));
    if (!needClear) return false;

    this.setCriterionValue(control.at(index), null);

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
    this.$sheetNames && this.$sheetNames.pipe(first()).subscribe(sheetNames => {
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

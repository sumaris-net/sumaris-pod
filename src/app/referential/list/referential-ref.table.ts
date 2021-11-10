import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {StatusList}  from "@sumaris-net/ngx-components";
import {AbstractControl, FormBuilder, FormGroup} from "@angular/forms";
import {debounceTime, filter} from "rxjs/operators";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS}  from "@sumaris-net/ngx-components";
import {environment} from "../../../environments/environment";
import {Entity}  from "@sumaris-net/ngx-components";
import {ReferentialFilter} from "../services/filter/referential.filter";
import { StatusById } from '@sumaris-net/ngx-components/src/app/core/services/model/referential.model';


@Component({
  selector: 'app-referential-ref-table',
  templateUrl: './referential-ref.table.html',
  styleUrls: ['./referential-ref.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReferentialRefTable<T extends Entity<T>, F extends ReferentialFilter> extends AppTable<T, F> {

  readonly statusList = StatusList;
  readonly statusById = StatusById;

  filterForm: FormGroup;

  @Input() showToolbar = false;
  @Input() showFilter = true;

  @Input() set entityName(entityName: string) {
    this.setFilter({
      ...this.filter,
      entityName
    });
  }

  get entityName(): string {
    return this.filter.entityName;
  }

  constructor(
    protected injector: Injector,
    formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef,
  ) {
    super(injector.get(ActivatedRoute),
      injector.get(Router),
      injector.get(Platform),
      injector.get(Location),
      injector.get(ModalController),
      injector.get(LocalSettingsService),
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'label',
          'name',
          'description',
          'status',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      null,
      null,
      injector);

    this.i18nColumnPrefix = 'REFERENTIAL.';
    this.inlineEdition = false;
    this.autoLoad = false; // waiting dataSource to be set

    this.filterForm = formBuilder.group({
      'searchText': [null]
    });

    // Update filter when changes
    this.registerSubscription(
      this.filterForm.valueChanges
        .pipe(
          debounceTime(250),
          filter(() => this.filterForm.valid)
        )
        // Applying the filter
        .subscribe((json) => this.setFilter({
          ...this.filter, // Keep previous filter
          ...json},
          {emitEvent: this.mobile}))
    );

    this.debug = !environment.production;
  }

  clearControlValue(event: UIEvent, formControl: AbstractControl): boolean {
    if (event) event.stopPropagation(); // Avoid to enter input the field
    formControl.setValue(null);
    return false;
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}


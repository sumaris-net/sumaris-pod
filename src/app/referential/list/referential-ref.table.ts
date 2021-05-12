import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {DefaultStatusList} from "../../core/services/model/referential.model";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";
import {AbstractControl, FormBuilder, FormGroup} from "@angular/forms";
import {debounceTime, filter} from "rxjs/operators";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {environment} from "../../../environments/environment";
import {Entity} from "../../core/services/model/entity.model";
import {ReferentialFilter} from "../services/referential.service";


@Component({
  selector: 'app-referential-ref-table',
  templateUrl: './referential-ref.table.html',
  styleUrls: ['./referential-ref.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReferentialRefTable<T extends Entity<T>, F extends ReferentialFilter> extends AppTable<T, F> {

  statusList = DefaultStatusList;
  statusById: any;
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
    this.autoLoad = false; // waiting dataSource to be set
    this.inlineEdition = false;


    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

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


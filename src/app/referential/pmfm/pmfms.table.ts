import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input} from '@angular/core';
import {AbstractControl, FormBuilder, FormGroup} from '@angular/forms';
import {AppTable, DefaultStatusList, LocalSettingsService, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from '@sumaris-net/ngx-components';
import {PmfmFilter} from '../services/pmfm.service';
import {ActivatedRoute, Router} from '@angular/router';
import {ModalController, Platform} from '@ionic/angular';
import {Location} from '@angular/common';
import {debounceTime, filter} from 'rxjs/operators';
import {environment} from '@environments/environment';
import {Pmfm} from '../services/model/pmfm.model';


@Component({
  selector: 'app-pmfms-table',
  templateUrl: './pmfms.table.html',
  styleUrls: ['./pmfms.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PmfmsTable extends AppTable<Pmfm, PmfmFilter> {
  statusList = DefaultStatusList;
  statusById: any;
  filterForm: FormGroup;

  @Input() showToolbar = false;
  @Input() showFilter = true;

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
          'name',
          'unit',
          'matrix',
          'fraction',
          'method',
          'status'])
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
      searchText: [null]
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


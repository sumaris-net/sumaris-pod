import { Component, OnInit, ViewChild, OnDestroy } from "@angular/core";
import { Observable } from 'rxjs';
import { map, debounceTime } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource, AppTable, AccountService } from "../../core/core.module";
import { PhysicalGearValidatorService } from "../services/physicalgear.validator";
import { referentialToString, PhysicalGear } from "../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { ReferentialService } from "../../referential/referential.module";
import { DataService } from "../../core/services/data-service.class";
import { PhysicalGearForm } from "./physicalgear.form";


@Component({
  selector: 'table-physical-gears',
  templateUrl: 'physicalgears.table.html',
  styleUrls: ['physicalgears.table.scss'],
  providers: [
    { provide: ValidatorService, useClass: PhysicalGearValidatorService }
  ],
})
export class PhysicalGearTable extends AppTable<PhysicalGear, any> implements OnInit, OnDestroy, DataService<PhysicalGear, any> {

  private data: PhysicalGear[];

  detailMeasurements: Observable<string[]>;

  @ViewChild('gearForm') gearForm: PhysicalGearForm;

  set value(data: PhysicalGear[]) {
    this.data = data;
    this.onRefresh.emit();
  }

  get value(): PhysicalGear[] {
    return this.data;
  }

  get dirty(): boolean {
    return this._dirty && this.gearForm.dirty;
  }
  get invalid(): boolean {
    return !this.valid;
  }
  get valid(): boolean {
    if (this.selectedRow && this.gearForm.dirty) {
      return this.gearForm.valid;
    }
    return true;
  }

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: PhysicalGearValidatorService,
    protected referentialService: ReferentialService
  ) {
    super(route, router, platform, location, modalCtrl, accountService, validatorService,
      null,
      ['select',
        'rankOrder',
        'gear',
        'comments'],
      {} // filter
    );
    this.i18nColumnPrefix = 'TRIP.PHYSICAL_GEAR.LIST.';
    this.autoLoad = false;

    this.setDatasource(new AppTableDataSource<PhysicalGear, any>(PhysicalGear, this, this.validatorService));
  };


  ngOnInit() {

    super.ngOnInit();

    // Listen detail form, to update the table
    this.gearForm.valueChanges
      .subscribe(value => {
        if (!this.selectedRow) return;
        this.selectedRow.currentData.fromObject(value);
        this._dirty = true;
      });

    this.detailMeasurements = this.gearForm
      .valueChanges
      .pipe(
        debounceTime(300),
        map(value => {
          return (value.measurements || [])
            .map(m => {
              let res: string = m.id ? ('id=' + m.id) : 'id=..';
              res += ' | ';
              res += m.pmfmId;
              res += ' | ';
              res += m.numericalValue || m.alphanumericalValue || (m.qualitativeValue && m.qualitativeValue.label) || '';
              return res;
            })
        })
      );
  }


  loadAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: any,
    options?: any
  ): Observable<PhysicalGear[]> {
    if (!this.data) return Observable.empty(); // Not initialized
    sortBy = sortBy || 'rankOrder';

    //console.debug("[table-physical-gear] Sorting... ", sortBy, sortDirection);
    const res = this.data.slice(0); // Copy the array
    const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
    res.sort((a, b) =>
      a[sortBy] === b[sortBy] ?
        0 : (a[sortBy] > b[sortBy] ?
          after : (-1 * after)
        )
    );
    return Observable.of(res);
  }

  saveAll(data: PhysicalGear[], options?: any): Promise<PhysicalGear[]> {
    if (!this.data) throw new Error("[table-physical-gears] Could not save table: value not set yet");

    if (this.selectedRow) {
      this.selectedRow.currentData = this.gearForm.value;
    }

    this.data = data;
    return Promise.resolve(this.data);
  }

  deleteAll(dataToRemove: PhysicalGear[], options?: any): Promise<any> {
    console.debug("[table-physical-gear] Remove data", dataToRemove);
    this.data = this.data.filter(gear => !dataToRemove.find(g => g === gear || g.id === gear.id))
    return Promise.resolve();
  }

  public onRowClick(event: MouseEvent, row: TableElement<PhysicalGear>): boolean {
    if (!row.currentData || event.defaultPrevented) return false;

    if (this.selectedRow && this.selectedRow.validator.invalid) {
      event.stopPropagation();
      return false;
    }

    //row.startEdit();
    this.selectedRow = row;
    this.gearForm.value = row.currentData;

    return true;
  }

  addRow() {
    // Skip if error in previous row
    if (this.selectedRow && this.selectedRow.validator.invalid) return;

    // Create new row
    this.createNew();
    const row = this.dataSource.getRow(-1);
    this.data.push(row.currentData);
    this.selectedRow = row;
    this.resultsLength++;
    row.currentData.rankOrder = this.resultsLength;
    this.gearForm.value = row.currentData;
  }

  deleteSelection() {
    super.deleteSelection();
    this.selectedRow = undefined;
    this.markAsUntouched();
    this.markAsPristine();
  }

  isDetailRow(row: TableElement<PhysicalGear>): boolean {
    return this.selectedRow && (this.selectedRow == row || this.selectedRow.currentData.id == row.currentData.id);
  }

  markAsPristine() {
    super.markAsPristine();
    this.gearForm.markAsPristine();
  }

  markAsUntouched() {
    super.markAsUntouched();
    this.gearForm.markAsUntouched();
  }

  referentialToString = referentialToString;
}



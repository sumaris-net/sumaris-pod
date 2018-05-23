import {Component, EventEmitter, OnInit, Output, ViewChild, OnDestroy} from "@angular/core";
import {MatPaginator, MatSort} from "@angular/material";
import {merge} from "rxjs/observable/merge";
import {startWith, switchMap} from "rxjs/operators";
import {ValidatorService, TableElement} from "angular4-material-table";
import {AppTableDataSource} from "../../../app/material/material.table";
import {VesselValidatorService} from "../validator/validators";
import {VesselService, VesselFilter} from "../../../services/vessel-service";
import {SelectionModel} from "@angular/cdk/collections";
import {VesselModal} from "../modal/modal-vessel";
import {VesselFeatures, Referential} from "../../../services/model";
import {Subscription} from "rxjs";
import { ModalController, Platform } from "ionic-angular";
import { Router, ActivatedRoute } from "@angular/router";

@Component({
  selector: 'page-vessels',
  templateUrl: 'vessels.html',
  providers: [
    {provide: ValidatorService, useClass: VesselValidatorService }
  ],
})
export class VesselsPage implements OnInit, OnDestroy {

  any: any;
  inlineEdition: boolean = false;
  subscriptions: Subscription[] = [];
  displayedColumns = ['select', 
    'id', 'exteriorMarking',
    'name',
    'basePortLocation',
    'comments'];
  dataSource:AppTableDataSource<VesselFeatures, VesselFilter>;
  resultsLength = 0;
  loading = true;
  focusFirstColumn = false;
  error: string;
  showFilter = false;
  dirty = false;
  isRateLimitReached = false;
  locations: Referential[] = [
    new Referential({id: 1, label: 'XBR', name: 'Brest'}),
    new Referential({id: 2, label: 'XBL', name: 'Boulogne'})
  ];
  selection = new SelectionModel<TableElement<VesselFeatures>>(true, []);
  selectedRow: TableElement<VesselFeatures> = undefined;
  onRefresh: EventEmitter<any> = new EventEmitter<any>();
  filter: VesselFilter = {
    date: null,
    searchText: null
  };

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  @Output()
  listChange = new EventEmitter<VesselFeatures[]>();

  constructor(
              private vesselValidatorService: VesselValidatorService,
              private vesselService: VesselService,
              private modalCtrl: ModalController,
              private route: ActivatedRoute,
              private router: Router,
              private platform: Platform
  ) {
    this.dataSource = new AppTableDataSource<VesselFeatures, VesselFilter>(VesselFeatures, this.vesselService, this.vesselValidatorService);
  };

  ngOnInit() {

    // If the user changes the sort order, reset back to the first page.
    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(
      this.sort.sortChange,
      this.paginator.page,
      this.onRefresh
    )
      .pipe(
        startWith({}),
        switchMap((any:any) => {
          this.dirty = false;
          this.selection.clear();
          this.selectedRow = null;
          return this.dataSource.load(
            this.paginator.pageIndex * this.paginator.pageSize,
            this.paginator.pageSize || 10,
            this.sort.active,
            this.sort.direction,
            this.filter
          );
        })
      )
      .subscribe(data => {
        if (data) {
          this.isRateLimitReached = data.length < this.paginator.pageSize;
          this.resultsLength = this.paginator.pageIndex * this.paginator.pageSize + data.length;
          console.debug('[vessels] Loaded ' + data.length + ' vessels: ', data);
        }
        else {
          console.debug('[vessels] Loaded NO vessels');
          this.isRateLimitReached = true;
          this.resultsLength = 0;
        }
      });

    // Subscriptions:
    this.subscriptions.push(this.dataSource.onLoading.subscribe(loading => this.loading = loading));
    this.subscriptions.push(this.dataSource.datasourceSubject.subscribe(data => this.listChange.emit(data)));
    this.subscriptions.push(this.listChange.subscribe(event => this.onDataChanged(event)));
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.subscriptions = [];
  }

  confirmAndAddRow(row: TableElement<VesselFeatures>) {
    // create
    var valid = false;
    if (row.id<0) {
      valid = this.dataSource.confirmCreate(row);
    }
    // update
    else {
      valid = this.dataSource.confirmEdit(row);
    }
    if (!valid) {
      return false;
    }

    // Add new row
    this.dataSource.createNew();
    this.resultsLength++;
    return true;
  }

  cancelOrDelete(row) {
    // create
    this.resultsLength--;
    row.cancelOrDelete();
  }

  addRow() {
    // Use modal if not expert mode, or if small screen
    if (this.platform.is('mobile') || !this.inlineEdition) {
      return this.openVesselModal();
    }

    // Add new row
    this.focusFirstColumn = true;
    this.dataSource.createNew();
    var subscription = this.dataSource.connect().first().subscribe(rows => {
      console.log("TODO: select new row");
      //this.selectedRow = rows[3];
    });
    this.dirty = true;
    this.resultsLength++;
    //this.selectedRow = null;
  }

  editRow(row) {
    if (!row.editing) {
      console.log(row);
      row.startEdit();
    }
  }

  createVessel() {
    var vessel = new VesselFeatures();
    return vessel;
  }

  onDataChanged(data: VesselFeatures[]) {
    this.error = undefined;
    data.forEach(t => {
      if (!t.id && !t.dirty) {
        t.dirty = true;
      }
    });
  }

  save() {
    this.error = undefined;
    if (this.selectedRow && this.selectedRow.editing) {
      var confirm = this.selectedRow.confirmEditCreate();
      if (!confirm) return;
    }
    console.log("[vessels] Saving...");
    this.dataSource.save()
      .then(res => {
        if (res) this.dirty = false;
      })
      .catch(err => {
        this.error = err && err.message || err;
      });
  }

  displayReferentialFn(ref?: Referential): string | undefined {
    return ref && ref.label ? (ref.label + ' - ' + ref.name) : undefined;
  }

  /** Whether the number of selected elements matches the total number of rows. */
  isAllSelected() {
    const numSelected = this.selection.selected.length;
    return numSelected == this.resultsLength;
  }

  /** Selects all rows if they are not all selected; otherwise clear selection. */
  masterToggle () {
    if (this.loading) return;
    this.isAllSelected() ?
      this.selection.clear() :
      this.dataSource.connect().subscribe(rows =>
        rows.forEach(row => this.selection.select(row))
      );
  }

  deleteSelection() {
    if (this.loading) return;
    this.selection.selected.forEach(row => {
      if (row.currentData && row.currentData.id >= 0) {
       row.delete();
        this.selection.deselect(row);
        this.resultsLength--;
      }
    });
    //this.selection.clear();
    this.selectedRow = null;
  }

  onEditRow(event, row) {
    if (this.selectedRow && this.selectedRow === row) return;
    if (this.selectedRow && this.selectedRow !== row && this.selectedRow.editing) {
      var confirm = this.selectedRow.confirmEditCreate();
      if (!confirm) {
        return;
      }
    }
    if (!row.editing && !this.loading) {
      row.startEdit();
      row.currentData.dirty = true;
    }
    this.selectedRow = row;
    this.dirty = true;
  }

  onOpenRowDetail(event, row) {
    if (!row.currentData.id || row.editing) return;

    // Open the detail page (if not editing)
    if (!this.dirty && !this.inlineEdition) {
      return this.router.navigate([row.currentData.id], { 
        relativeTo: this.route
      });
    }

    this.onEditRow(event, row);    
  }

  openVesselModal(): Promise<any> {
    if (this.loading) return;

    let modal = this.modalCtrl.create(VesselModal);
    modal.onDidDismiss(res => {
      // if new vessel added, refresh the table
      if (res) this.onRefresh.emit();
    });
    return modal.present();
  }

  toggleFilter() {
    this.showFilter = !this.showFilter;
  }
}


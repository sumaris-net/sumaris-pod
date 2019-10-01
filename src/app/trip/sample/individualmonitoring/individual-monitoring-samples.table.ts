import {ChangeDetectionStrategy, Component, Injector, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {isNotNil, referentialToString} from "../../../core/core.module";
import {Sample} from "../../services/trip.model";
import {SubSampleValidatorService} from "../../services/sub-sample.validator";
import {FormGroup, Validators} from "@angular/forms";
import {AcquisitionLevelCodes, PmfmIds} from "../../../referential/services/model";
import {filter} from "rxjs/operators";
import {SubSamplesTable} from "../sub-samples.table";


@Component({
  selector: 'app-individual-monitoring-table',
  templateUrl: '../sub-samples.table.html',
  styleUrls: ['../sub-samples.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: SubSampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IndividualMonitoringSubSamplesTable extends SubSamplesTable implements OnInit {

  protected hasIsDeadPmfm = false;

  constructor(
    injector: Injector
  ) {
    super(injector);
    this.acquisitionLevel = AcquisitionLevelCodes.INDIVIDUAL_MONITORING;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this.$pmfms
        .pipe(filter(isNotNil))
        .subscribe(pmfms => {

          // Listening on column 'IS_DEAD' value changes
          this.hasIsDeadPmfm = pmfms.findIndex(p => p.pmfmId === PmfmIds.IS_DEAD) !== -1;
          if (this.hasIsDeadPmfm) {
            this.registerCellValueChanges('isDead', "measurementValues." + PmfmIds.IS_DEAD.toString())
              .subscribe((isDeadValue) => {
                if (!this.editedRow) return; // Should never occur
                const row = this.editedRow;
                const controls = (row.validator.controls['measurementValues'] as FormGroup).controls;
                if (isDeadValue) {
                  if (controls[PmfmIds.DEATH_TIME]) {
                    if (row.validator.enabled) {
                      controls[PmfmIds.DEATH_TIME].enable();
                    }
                    controls[PmfmIds.DEATH_TIME].setValidators(Validators.required);
                  }
                  if (controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS]) {
                    if (row.validator.enabled) {
                      controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].enable();
                    }
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValidators(Validators.required);
                  }
                } else {
                  if (controls[PmfmIds.DEATH_TIME]) {
                    controls[PmfmIds.DEATH_TIME].disable();
                    controls[PmfmIds.DEATH_TIME].setValue(null);
                    controls[PmfmIds.DEATH_TIME].setValidators([]);
                  }
                  if (controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS]) {
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValue(null);
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValidators([]);
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].disable();
                  }
                }
              });
          }
        }));
  }

  /* -- protected methods -- */

  protected startListenRow(row: TableElement<Sample>) {
    super.startListenRow(row);

    // Listening IS_DEAD cell
    if (this.hasIsDeadPmfm) {
      this.startCellValueChanges('isDead', row);
    }
  }

  parentToString(parent: Sample) {
    if (!parent) return null;
    return parent.measurementValues && parent.measurementValues[PmfmIds.TAG_ID] || `#${parent.rankOrder}`;
  }

}


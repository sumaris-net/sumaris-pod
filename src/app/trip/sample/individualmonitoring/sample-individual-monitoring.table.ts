import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AccountService, isNotNil} from "../../../core/core.module";
import {Sample} from "../../services/trip.model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ProgramService, ReferentialRefService} from "../../../referential/referential.module";
import {SubSampleValidatorService} from "../../services/sub-sample.validator";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {TranslateService} from '@ngx-translate/core';
import {AcquisitionLevelCodes} from "../../../core/services/model";
import {MeasurementsValidatorService} from "../../services/trip.validators";
import {PmfmIds} from "../../../referential/services/model";
import {SubSamplesTable} from "../sub-samples.table";
import {filter, first} from "rxjs/operators";


@Component({
  selector: 'table-individual-monitoring',
  templateUrl: '../sub-samples.table.html',
  styleUrls: ['../sub-samples.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: SubSampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IndividualMonitoringTable extends SubSamplesTable implements OnInit {

  protected hasIsDeadPmfm = false;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: SubSampleValidatorService,
    protected measurementsValidatorService: MeasurementsValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected programService: ProgramService,
    protected translate: TranslateService,
    protected formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
      validatorService, measurementsValidatorService, referentialRefService,
      programService, translate, formBuilder, cd
    );
    this.acquisitionLevel = AcquisitionLevelCodes.INDIVIDUAL_MONITORING;
  };

  async ngOnInit(): Promise<void> {
    await super.ngOnInit();

    this.pmfms
      .pipe(filter(isNotNil), first())
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
      });
  }

  /* -- protected methods -- */

  protected startListenRow(row: TableElement<Sample>) {
    super.startListenRow(row);

    // Listening IS_DEAD cell
    if (this.hasIsDeadPmfm) {
      this.startCellValueChanges('isDead', row);
    }
  }

}


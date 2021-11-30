import { ChangeDetectionStrategy, Component, Injector, OnInit } from '@angular/core';
import { ValidatorService } from '@e-is/ngx-material-table';
import { SubSampleValidatorService } from '../../services/validator/sub-sample.validator';
import { FormGroup, Validators } from '@angular/forms';
import { AcquisitionLevelCodes, PmfmIds } from '../../../referential/services/model/model.enum';
import { filter } from 'rxjs/operators';
import { SubSamplesTable } from '../sub-samples.table';
import { isNotNil } from '@sumaris-net/ngx-components';
import { Sample } from '@app/trip/services/model/sample.model';
import { SamplingStrategyService } from '@app/referential/services/sampling-strategy.service';


@Component({
  selector: 'app-individual-monitoring-table',
  templateUrl: '../sub-samples.table.html',
  styleUrls: ['../sub-samples.table.scss', 'individual-monitoring-samples.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: SubSampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IndividualMonitoringSubSamplesTable extends SubSamplesTable implements OnInit {

  protected currentSample: Sample; // require to preset presentation on new row

  constructor(
    injector: Injector,
    protected samplingStrategyService: SamplingStrategyService
  ) {
    super(injector);
    this.acquisitionLevel = AcquisitionLevelCodes.INDIVIDUAL_MONITORING;
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    this.registerSubscription(
      this.$pmfms
        .pipe(
          filter(isNotNil),
          // DEBUG
          //tap(pmfms => console.debug("[individual-monitoring-samples] Pmfms:", pmfms))
        )
        .subscribe(pmfms => {

          // Listening on column 'IS_DEAD' value changes
          const hasIsDeadPmfm = pmfms.findIndex(p => p.id === PmfmIds.IS_DEAD) !== -1;
          if (hasIsDeadPmfm) {
            this.registerSubscription(
              this.registerCellValueChanges('isDead', `measurementValues.${PmfmIds.IS_DEAD}`)
              .subscribe((isDeadValue) => {
                if (!this.editedRow) return; // Should never occur
                const row = this.editedRow;
                const controls = (row.validator.get('measurementValues') as FormGroup).controls;
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
                    controls[PmfmIds.DEATH_TIME].setValidators(null);
                  }
                  if (controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS]) {
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValue(null);
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].setValidators(null);
                    controls[PmfmIds.VERTEBRAL_COLUMN_ANALYSIS].disable();
                  }
                }
              }));
          }
        }));
  }
}


import {ChangeDetectionStrategy, Component, Injector, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AppFormUtils, isNotNil} from "../../../core/core.module";
import {PmfmStrategy, Sample} from "../../services/trip.model";
import {AbstractControl, AsyncValidatorFn, FormGroup} from "@angular/forms";
import {AcquisitionLevelCodes, PmfmIds, QualitativeLabels} from "../../../referential/services/model";
import {debounceTime, filter} from "rxjs/operators";
import {SamplesTable} from "../samples.table";
import {SampleValidatorService} from "../../services/sample.validator";
import {isNotNilOrBlank} from "../../../shared/functions";


@Component({
  selector: 'app-sample-auction-control-table',
  templateUrl: '../samples.table.html',
  styleUrls: ['./auction-control-samples.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: SampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuctionControlSamplesTable extends SamplesTable implements OnInit {

  protected weightPmfm: PmfmStrategy;
  protected outOfSizePmfm: PmfmStrategy;
  protected vivacityPmfm: PmfmStrategy;
  protected computedPmfmPaths: string[];

  // @Input()
  // set value(data: Sample[]) {
  //
  //   // Remove generated label
  //   const generatedPrefix = (this.acquisitionLevel || 'SAMPLE') + '#';
  //   (data||[])
  //     .filter(s => s.label.startsWith(generatedPrefix))
  //     .forEach(s => s.label = null);
  //
  //   this.memoryDataService.value = data;
  // }

  constructor(
    injector: Injector
  ) {
    super(injector);
    this.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
  }

  ngOnInit() {
    if (this.debug) console.debug('[auction-control-samples] Init samples table...');

    super.ngOnInit();

    this.$pmfms
      .pipe(filter(isNotNil))
      .subscribe(pmfms => {

        this.computedPmfmPaths =  pmfms
          .filter(p => p.isComputed)
          .map(p => 'measurementValues.' + p.pmfmId);

        this.weightPmfm = pmfms.find(p => p.label.endsWith('_WEIGHT'));
        this.outOfSizePmfm = pmfms.find(p => p.pmfmId === PmfmIds.OUT_OF_SIZE);

        if (this.weightPmfm && this.outOfSizePmfm) {

          // Compute out of size percentage
          this.registerCellValueChanges('outOfSize', "measurementValues." + this.outOfSizePmfm.pmfmId)
            .pipe(debounceTime(250))
            .subscribe((outOfSize) => {
              if (!this.editedRow) return; // Should never occur

              const row = this.editedRow;
              const controls = (row.validator.controls['measurementValues'] as FormGroup).controls;
              const weight = controls[this.weightPmfm.pmfmId].value;

              if (isNotNilOrBlank(weight) && isNotNilOrBlank(outOfSize)
                && controls[PmfmIds.OUT_OF_SIZE_PCT]
                && row.validator.enabled
                && outOfSize <= weight) {
                // Compute out of size percentage
                const pct = Math.trunc(10000 * outOfSize / weight) / 100;
                controls[PmfmIds.OUT_OF_SIZE_PCT].setValue(pct);
              } else if (controls[PmfmIds.OUT_OF_SIZE_PCT]) {
                // Reset out of size percentage
                controls[PmfmIds.OUT_OF_SIZE_PCT].setValue(null);
              }
            });

        }

        // Listen vitality
        this.vivacityPmfm = pmfms.find(p => p.pmfmId === PmfmIds.VIVACITY);

        if (this.vivacityPmfm) {
          this.registerCellValueChanges('vivacity', "measurementValues." + this.vivacityPmfm.pmfmId)
            .pipe(debounceTime(250))
            .subscribe((vivacity) => {
              if (vivacity && vivacity.label === QualitativeLabels.VIVACITY.DEAD) {
                console.log('TODO: MORTE -> change other PMFM !!');
              }
            });
        }
      });
  }

  /* -- protected methods -- */

  protected startListenRow(row: TableElement<Sample>) {
    super.startListenRow(row);

    // Always disable computed pmfms
    if (row.validator) {

      // Disable computed pmfms
      AppFormUtils.disableControls(row.validator, this.computedPmfmPaths);

      // Label: remove required validator
      row.validator.get('label').setValidators([]);

      // 'out of size'
      if (this.outOfSizePmfm) {
        // Start listen
        this.startCellValueChanges('outOfSize', row);

        // Add a max validator (use async validator, to keep default validator)
        AppFormUtils.getControlFromPath(row.validator, 'measurementValues.' + this.outOfSizePmfm.pmfmId)
          .setAsyncValidators(this.outOfSizeMax(row));
      }

      // 'vivacity'
      if (this.vivacityPmfm) {
        this.startCellValueChanges('vivacity', row);
      }
    }
  }

  protected disableControls(row: TableElement<Sample>, paths: string[]) {
    (paths || []).forEach(path => {
      const control = AppFormUtils.getControlFromPath(row.validator, path);
      if (control) {
        control.disable();
      }
    });
  }

  protected outOfSizeMax(row: TableElement<Sample>): AsyncValidatorFn {
    return async (control: AbstractControl) => {
      if (!row.validator.enabled) return;
      const outOfSize = +control.value;
      const weight = +AppFormUtils.getControlFromPath(row.validator, 'measurementValues.' + this.weightPmfm.pmfmId).value;
      if (isNotNilOrBlank(outOfSize) && isNotNilOrBlank(weight) && outOfSize > weight) {
        return {max: {actual: outOfSize, max: weight}};
      }
    };
  }
}


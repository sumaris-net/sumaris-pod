import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild } from '@angular/core';
import { Batch, BatchUtils } from '../../services/model/batch.model';
import { AppFormUtils, LocalSettingsService, PlatformService, toBoolean } from '@sumaris-net/ngx-components';
import { IonContent, ModalController } from '@ionic/angular';
import { BehaviorSubject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { AcquisitionLevelCodes } from '../../../referential/services/model/model.enum';
import { PmfmStrategy } from '../../../referential/services/model/pmfm-strategy.model';
import { SubBatchForm } from '../form/sub-batch.form';
import { SubBatch } from '../../services/model/subbatch.model';
import { IBatchModalOptions } from '@app/trip/batch/modal/batch.modal';
import { BatchGroup } from '@app/trip/services/model/batch-group.model';


export interface ISubBatchModalOptions extends IBatchModalOptions<SubBatch> {

  showParent: boolean;
  availableParents: BatchGroup[];
}

@Component({
  selector: 'app-sub-batch-modal',
  templateUrl: 'sub-batch.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchModal implements OnInit {

  debug = false;
  loading = false;
  mobile: boolean;
  $title = new BehaviorSubject<string>(undefined);

  @Input() disabled: boolean;
  @Input() isNew: boolean;
  @Input() data: SubBatch;
  @Input() acquisitionLevel: string;
  @Input() programLabel: string;

  @Input() showParent = true;
  @Input() showTaxonName = true;
  @Input() showIndividualCount = false;
  @Input() qvPmfm: PmfmStrategy;
  @Input() availableParents: BatchGroup[];

  @ViewChild('form', { static: true }) form: SubBatchForm;
  @ViewChild(IonContent) content: IonContent;

  get dirty(): boolean {
    return this.form.dirty;
  }

  get invalid(): boolean {
    return this.form.invalid;
  }

  get valid(): boolean {
    return this.form.valid;
  }

  constructor(
    protected injector: Injector,
    protected viewCtrl: ModalController,
    protected platform: PlatformService,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {
    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;
    this.mobile = platform.mobile;

    // TODO: for DEV only
    //this.debug = !environment.production;
  }


  async ngOnInit() {
    this.disabled = toBoolean(this.disabled, false);
    this.isNew = toBoolean(this.isNew, false);

    this.data = this.data || new SubBatch();

    // Compute the title
    this.computeTitle();

    await this.form.waitIdle();
    this.form.value = this.data;

    if (!this.isNew) {
      // Update title each time value changes
      this.form.valueChanges.subscribe(batch => this.computeTitle(batch));
    }

    if (!this.disabled) {
      this.form.enable({emitEvent: false});
    }

  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async close(event?: UIEvent) {
    if (this.loading) return; // avoid many call

    if (this.invalid) {
      if (this.debug) AppFormUtils.logFormErrors(this.form.form, "[sub-batch-modal] ");
      this.form.error = "COMMON.FORM.HAS_ERROR";
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;

    // Save table content
    const data = this.form.value;

    await this.viewCtrl.dismiss(data);
  }

  /* -- protected methods -- */

  protected async computeTitle(data?: SubBatch) {
    data = data || this.data;
    if (this.isNew || !data) {
      this.$title.next(await this.translate.get('TRIP.SUB_BATCH.NEW.TITLE').toPromise());
    }
    else {
      const label = BatchUtils.parentToString(data);
      this.$title.next(await this.translate.get('TRIP.SUB_BATCH.EDIT.TITLE', {label}).toPromise());
    }
  }

  async scrollToTop() {
    return this.content.scrollToTop();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

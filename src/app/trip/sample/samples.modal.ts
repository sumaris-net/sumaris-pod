import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import { AppFormUtils, LocalSettingsService, waitWhilePending } from '@sumaris-net/ngx-components';
import {environment} from "../../../environments/environment";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject, Observable} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {AcquisitionLevelCodes, AcquisitionLevelType} from "../../referential/services/model/model.enum";
import {toBoolean} from "@sumaris-net/ngx-components";
import {PlatformService}  from "@sumaris-net/ngx-components";
import {Sample} from "../services/model/sample.model";
import {SamplesTable} from "./samples.table";
import {Moment} from "moment";
import {ReferentialRef}  from "@sumaris-net/ngx-components";
import {IPmfm} from "../../referential/services/model/pmfm.model";

@Component({
  selector: 'app-samples-modal',
  templateUrl: 'samples.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplesModal implements OnInit {

  debug = false;
  loading = false;
  mobile: boolean;
  data: Sample[];
  $title = new BehaviorSubject<string>(undefined);

  @Input() acquisitionLevel: AcquisitionLevelType;
  @Input() programLabel: string;
  @Input() canEdit: boolean;
  @Input() disabled: boolean;
  @Input() isNew = false;
  @Input() defaultSampleDate: Moment;
  @Input() defaultTaxonGroup: ReferentialRef;
  @Input() showTaxonGroup = true;
  @Input() showTaxonName = true;
  @Input() showLabel = false;
  @Input() title: string;
  @Input() onReady: (modal: SamplesModal) => void;

  @Input()
  set value(value: Sample[]) {
    this.data = value;
  }

  @ViewChild('table', { static: true }) table: SamplesTable;

  get dirty(): boolean {
    return this.table.dirty;
  }

  get invalid(): boolean {
    return this.table.invalid;
  }

  get valid(): boolean {
    return this.table.valid;
  }

  get $pmfms(): Observable<IPmfm[]> {
    return this.table.$pmfms;
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
    this.acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
    this.mobile = platform.mobile;

    // TODO: for DEV only
    this.debug = !environment.production;
  }


  ngOnInit() {
    this.canEdit = toBoolean(this.canEdit, !this.disabled);
    this.disabled = !this.canEdit || toBoolean(this.disabled, true);

    if (this.disabled) {
      this.table.disable();
    }

    this.table.value = this.data || [];

    // Compute the title
    this.$title.next(this.title || '');

    // Add callback
    this.ready().then(() => {
      if (this.onReady) this.onReady(this);
      this.markForCheck();
    });
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async ready(): Promise<void>{
    await this.table.ready();
  }

  async onSubmit(event?: UIEvent) {
    if (this.loading) return; // avoid many call

    await AppFormUtils.waitWhilePending(this.table);

    if (this.invalid) {
      // if (this.debug) AppFormUtils.logFormErrors(this.table.table., "[sample-modal] ");
      this.table.error = "COMMON.FORM.HAS_ERROR";
      this.table.markAllAsTouched();
      return;
    }

    this.loading = true;

    // Save table content
    await this.table.save();
    const data = this.table.value;

    await this.viewCtrl.dismiss(data);
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  onNewFabButtonClick(event: UIEvent){
    this.table.addRow(event);
  }

}

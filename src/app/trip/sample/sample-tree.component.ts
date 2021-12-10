import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, ViewChild } from '@angular/core';
import { AppTabEditor, AppTable, Entity, IconRef, InMemoryEntitiesService, isNil, isNotNil, isNotNilOrBlank, PlatformService, UsageMode, WaitForOptions } from '@sumaris-net/ngx-components';
import { Sample, SampleUtils } from '@app/trip/services/model/sample.model';
import { ActivatedRoute, Router } from '@angular/router';
import { AlertController, ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { SamplesTable } from '@app/trip/sample/samples.table';
import { IndividualMonitoringTable } from '@app/trip/sample/individualmonitoring/individual-monitoring.table';
import { IndividualReleasesTable } from '@app/trip/sample/individualrelease/individual-releases.table';
import { ProgramRefService } from '@app/referential/services/program-ref.service';
import { BehaviorSubject } from 'rxjs';
import { Program } from '@app/referential/services/model/program.model';
import { Moment } from 'moment';
import { environment } from '@environments/environment';
import { SampleFilter } from '@app/trip/services/filter/sample.filter';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { ProgramProperties } from '@app/referential/services/config/program.config';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { BatchUtils } from '@app/trip/services/model/batch.model';

export interface SampleTabDefinition {
  iconRef: IconRef;
  label: string;
}

@Component({
  selector: 'app-sample-tree',
  templateUrl: './sample-tree.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SampleTreeComponent extends AppTabEditor<Sample[]> {

  private _subSamplesService: InMemoryEntitiesService<Sample, SampleFilter>;


  data: Sample[];
  $programLabel = new BehaviorSubject<string>(null);
  $program = new BehaviorSubject<Program>(null);
  listenProgramChanges = true;

  @Input() debug: boolean;
  @Input() mobile: boolean;
  @Input() usageMode: UsageMode;
  @Input() showLabelColumn = false;
  @Input() defaultSampleDate: Moment;
  @Input() sampleTabDef: SampleTabDefinition = {
    iconRef: {matSvgIcon: 'fish-oblique'},
    label: 'TRIP.OPERATION.EDIT.TAB_SAMPLES'
  };
  @Input() requiredStrategy = false;

  @Input()
  set programLabel(value: string) {
    if (this.$programLabel.value !== value) {
      this.$programLabel.next(value);
    }
  }

  get programLabel(): string {
    return this.$programLabel.value;
  }

  @Input()
  set program(value: Program) {
    this.listenProgramChanges = false; // Avoid to watch program changes, when program is given by parent component
    this.$program.next(value);
  }

  @Input()
  set value(value: Sample[]) {
    this.setValue(value);
  }

  get value(): Sample[] {
    return this.getValue();
  }

  get dirty(): boolean {
    return super.dirty || (this._subSamplesService?.dirty) || false;
  }

  set selectedSubTabIndex(value: number) {
    this.setSelectedTabIndex(value);
  }

  @ViewChild('samplesTable', {static: true}) samplesTable: SamplesTable;
  @ViewChild('individualMonitoringTable', {static: true}) individualMonitoringTable: IndividualMonitoringTable;
  @ViewChild('individualReleaseTable', {static: true}) individualReleasesTable: IndividualReleasesTable;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected programRefService: ProgramRefService,
    protected modalCtrl: ModalController,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, alertCtrl, translate,
      {
        tabCount: platform.mobile ? 1 : 3
      });

    // Defaults
    this.mobile = platform.mobile;
    this.debug = !environment.production
    this.i18nContext = {
      prefix: '',
      suffix: ''
    }
  }

  ngOnInit() {
    // Set defaults
    this.tabCount = this.mobile ? 1 : 3;

    // Init a service, to store sub-samples (mobile)
    this._subSamplesService = this.mobile
      ? new InMemoryEntitiesService(Sample, SampleFilter, {
        equals: Sample.equals
      })
      : null;

    super.ngOnInit();

    this.registerForms();
  }

  ngAfterViewInit() {
    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.$programLabel
        .pipe(
          filter(() => this.listenProgramChanges), // Avoid to watch program, if was already set
          filter(isNotNilOrBlank),
          distinctUntilChanged(),
          switchMap(programLabel => this.programRefService.watchByLabel(programLabel))
        )
        .subscribe(program => this.$program.next(program))
    );

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.$program
        .pipe(
          distinctUntilChanged((p1, p2) => p1 && p2 && p1.label === p2.label && p1.updateDate.isSame(p2.updateDate)),
          filter(isNotNil)
        )
        .subscribe(program => this.setProgram(program))
    );

    // Update available parent on sub-sample table, when samples changes
    this.registerSubscription(
      this.samplesTable.dataSource.datasourceSubject
        .pipe(
          debounceTime(500),
          filter(() => !this.loading) // skip if loading
        )
        .subscribe(samples => {
          // Will refresh the tables (inside the setter):
          this.individualMonitoringTable.availableParents = samples;
          this.individualReleasesTable.availableParents = samples;

          // TODO: remove this
          this.samplesTable.setIndividualReleaseModalOption('availableParents', samples);
        }));

    // TODO: remove this
    // Update available releases on sample table, when sub-samples changes
    this.registerSubscription(
      this.individualReleasesTable.dataSource.datasourceSubject
        .pipe(
          debounceTime(500),
          // skip if loading
          filter(() => !this.loading)
        )
        .subscribe(samples => {
          if (this.loading) return; // skip during loading

          this.samplesTable.availableReleases = (samples || [])
            .filter(s => isNotNil(s.parent));
        }));
  }

  get isNewData(): boolean {
    return false;
  }

  async ready(opts?: WaitForOptions): Promise<void> {
    await super.ready(opts);
  }

  async setValue(data: Sample[]) {
    console.debug('[sample-tree] Setting value', data);

    await this.ready();

    // Get all samples
    const samples = (data || []).reduce((res, sample) => !sample.children ? res.concat(sample) : res.concat(sample).concat(sample.children), []);

    // Set root samples
    const rootSamples = samples.filter(s => s.label && s.label.startsWith(this.samplesTable.acquisitionLevel + '#'));
    this.samplesTable.value = rootSamples;

    // Set sub-samples (individual monitoring)
    this.individualMonitoringTable.availableParents = rootSamples;
    this.individualMonitoringTable.value = samples.filter(s => s.label && s.label.startsWith(this.individualMonitoringTable.acquisitionLevel + '#'));

    // Set sub-samples (individual release)
    this.individualReleasesTable.availableParents = rootSamples;
    this.individualReleasesTable.value = samples.filter(s => s.label && s.label.startsWith(this.individualReleasesTable.acquisitionLevel + '#'));

  }

  getValue(): Sample[] {
    return this.data;
  }


  async save(event?: Event, options?: any): Promise<boolean> {
    console.debug('[sample-tree] Saving samples...');

    // Save batch groups and sub batches
    const [rootSamples, subSamples] = await Promise.all([
      this.getTableValue(this.samplesTable),
      this.getSubSamples()
    ]);

    // Prepare subSamples for model
    const target = (rootSamples || [])
      .map(sample => {
        sample.children = subSamples.filter(childSample => childSample.parent && sample.equals(childSample.parent));
        return sample;
      });

    // DEBUG
    if (this.debug) SampleUtils.logTree(target);

    this.data = target;

    return true;
  }

  realignInkBar() {
    if (this.tabGroup) {
      //this.tabGroup.selectedIndex = this.selectedTabIndex;
      this.tabGroup.realignInkBar();
    }
  }

  addRow(event: UIEvent) {
    switch (this.selectedTabIndex) {
      case 0:
        this.samplesTable.addRow(event);
        break;
      case 1:
        this.individualMonitoringTable.addRow(event);
        break;
      case 2:
        this.individualReleasesTable.addRow(event);
        break;
    }
  }

  load(id?: number, options?: any): Promise<void> {
    return Promise.resolve(undefined);
  }

  reload(): Promise<void> {
    return Promise.resolve(undefined);
  }

  /* -- -- */

  protected registerForms() {
    this.addChildForms([
      this.samplesTable,
      this.individualMonitoringTable,
      this.individualReleasesTable
    ]);
  }

  async onIndividualReleaseChanges(subSample: Sample) {
    if (isNil(subSample)) return; // user cancelled

    if (this.individualReleasesTable) {
      await this.individualReleasesTable.addRowFromValue(subSample);
    }
  }
  async onIndividualReleaseDelete(subSample: Sample) {
    if (isNil(subSample)) return; // user cancelled

    if (this.individualReleasesTable) {
      await this.individualReleasesTable.deleteEntity(null, subSample);
    }

  }

  async getSubSamples(): Promise<Sample[]> {

    if (this.individualMonitoringTable && this.individualReleasesTable) {
      const [subSamples1, subSamples2] = await Promise.all([
        this.getTableValue(this.individualMonitoringTable),
        this.getTableValue(this.individualReleasesTable)
      ])

      return (subSamples1 || []).concat(subSamples2 || []);
    } else {
      return this._subSamplesService.value;
    }
  }

  onTabChange(event: MatTabChangeEvent, queryTabIndexParamName?: string) {
    const result = super.onTabChange(event, queryTabIndexParamName);

    // On each tables, confirm the current editing row
    if (!this.loading) {
      this.samplesTable.confirmEditCreate();
      this.individualMonitoringTable?.confirmEditCreate();
      this.individualReleasesTable?.confirmEditCreate();
    }

    return result;
  }

  protected setProgram(program: Program) {
    if (!program) return; // Skip
    if (this.debug) console.debug(`[sample-tree] Program ${program.label} loaded, with properties: `, program.properties);

    let i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    i18nSuffix = i18nSuffix !== 'legacy' ? i18nSuffix : '';
    this.i18nContext.suffix = i18nSuffix;

    this.samplesTable.showTaxonGroupColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_SAMPLE_TAXON_GROUP_ENABLE);
    this.samplesTable.showTaxonNameColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_SAMPLE_TAXON_NAME_ENABLE);
    this.samplesTable.showSampleDateColumn  = program.getPropertyAsBoolean(ProgramProperties.TRIP_SAMPLE_DATE_ENABLE);
    this.samplesTable.programLabel = program.label;
    this.samplesTable.i18nColumnSuffix = i18nSuffix;

    const defaultLatitudeSign: '+' | '-' = program.getProperty(ProgramProperties.TRIP_LATITUDE_SIGN);
    const defaultLongitudeSign: '+' | '-' = program.getProperty(ProgramProperties.TRIP_LONGITUDE_SIGN);

    // TODO: remove this
    this.samplesTable.setIndividualReleaseModalOption('defaultLatitudeSign', defaultLatitudeSign);
    this.samplesTable.setIndividualReleaseModalOption('defaultLongitudeSign', defaultLongitudeSign);

    this.individualMonitoringTable.i18nColumnSuffix = IndividualMonitoringTable.DEFAULT_I18N_SUFFIX + i18nSuffix;

    this.individualReleasesTable.setModalOption('defaultLatitudeSign', defaultLatitudeSign);
    this.individualReleasesTable.setModalOption('defaultLongitudeSign', defaultLongitudeSign);
    this.individualReleasesTable.i18nColumnSuffix = IndividualReleasesTable.DEFAULT_I18N_SUFFIX + i18nSuffix;

    // Propagate to observables
    if (this.$programLabel.value !== program?.label) this.$programLabel.next(program?.label);
  }

  protected async getTableValue<T extends Entity<T>>(table: AppTable<T> & { value: T[]}): Promise<T[]> {
    if (table.dirty) {
      await table.save();

      // Remember dirty state
      this.markAsDirty({emitEvent: false});
    }

    return table.value;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

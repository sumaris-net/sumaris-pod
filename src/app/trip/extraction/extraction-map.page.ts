import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {PlatformService} from "../../core/services/platform.service";
import {AggregationTypeFilter, CustomAggregationStrata, ExtractionService} from "../services/extraction.service";
import {BehaviorSubject, Observable, Subject, Subscription, timer} from "rxjs";
import {
  arraySize,
  isEmptyArray,
  isNil,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  toBoolean
} from "../../shared/functions";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {
  AggregationStrata,
  AggregationType,
  ExtractionColumn,
  ExtractionFilter,
  ExtractionFilterCriterion,
  ExtractionUtils
} from "../services/model/extraction.model";
import {Location} from "@angular/common";
import {Color, ColorScale, fadeInAnimation, fadeInOutAnimation} from "../../shared/shared.module";
import {ColorScaleLegendItem} from "../../shared/graph/graph-colors";
import * as L from 'leaflet';
import {CRS, DomUtil} from 'leaflet';
import {Feature} from "geojson";
import {debounceTime, filter, map, switchMap, tap, throttleTime} from "rxjs/operators";
import {AlertController, ModalController, ToastController} from "@ionic/angular";
import {AggregationTypeSelectModal} from "./aggregation-type-select.modal";
import {AccountService} from "../../core/services/account.service";
import {ExtractionAbstractPage} from "./extraction-abstract.page";
import {ActivatedRoute, Router} from "@angular/router";
import {TranslateService} from "@ngx-translate/core";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AggregationTypeValidatorService} from "../services/validator/aggregation-type.validator";
import {AppFormUtils} from "../../core/core.module";
import {MatExpansionPanel} from "@angular/material/expansion";
import {Label, SingleOrMultiDataSet} from "ng2-charts";
import {ChartOptions, ChartType} from "chart.js";
import {DEFAULT_CRITERION_OPERATOR} from "./extraction-data.page";

declare interface LegendOptions {
  min: number;
  max: number;
  startColor: string;
  endColor: string;
}
declare type TechChartOptions = ChartOptions & {
  legend: boolean;
  type: ChartType
  sortByLabel: boolean;
  //sortByLabel?: boolean;
  displayAllLabels?: boolean;
}


@Component({
  selector: 'app-extraction-map-page',
  templateUrl: './extraction-map.page.html',
  styleUrls: ['./extraction-map.page.scss'],
  animations: [fadeInAnimation, fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExtractionMapPage extends ExtractionAbstractPage<AggregationType> implements OnInit, OnDestroy {

  // -- Map Layers --
  osmBaseLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: '<a href=\'https://www.openstreetmap.org\'>Open Street Map</a>'
  });
  sextantBaseLayer = L.tileLayer(
    'https://sextant.ifremer.fr/geowebcache/service/wmts?Service=WMTS&Layer=sextant&Style=&TileMatrixSet=EPSG:3857&Request=GetTile&Version=1.0.0&Format=image/png&TileMatrix=EPSG:3857:{z}&TileCol={x}&TileRow={y}',
    {maxZoom: 18, attribution: "<a href='https://sextant.ifremer.fr'>Sextant</a>"});
  sextantGraticuleLayer = L.tileLayer.wms('https://www.ifremer.fr/services/wms1', {
    maxZoom: 18,
    version: '1.3.0',
    crs: CRS.EPSG4326,
    format: "image/png",
    transparent: true
  }).setParams({
    layers: "graticule_4326",
    service: 'WMS'
  });

  ready = false;
  options = {
    layers: [this.sextantBaseLayer],
    maxZoom: 10, // max zoom to sextant layer
    zoom: 5,
    center: L.latLng(46.879966, -10) // Atlantic centric
  };
  baseLayer: L.Layer = this.sextantBaseLayer;
  baseLayers = [
    {title: 'Sextant (Ifremer)', layer: this.sextantBaseLayer},
    {title: 'Open Street Map', layer: this.osmBaseLayer}
  ];

  data = {
    total: 0,
    min: 0,
    max: 0
  };

  showLegend = false;
  legendForm: FormGroup;
  showLegendForm = false;
  customLegendOptions: Partial<LegendOptions> = undefined;
  legendStyle = {};

  columnNames = {};
  map: L.Map;
  typesFilter: AggregationTypeFilter;
  showGraticule = false;

  $title = new BehaviorSubject<string>(undefined);
  $layers = new BehaviorSubject<L.GeoJSON<L.Polygon>[]>(null);
  $legendItems = new BehaviorSubject<ColorScaleLegendItem[] | undefined>([]);
  $onOverFeature = new Subject<Feature>();
  $selectedFeature = new BehaviorSubject<Feature | undefined>(undefined);
  $noData = new BehaviorSubject<boolean>(false);

  $sheetNames = new BehaviorSubject<String[]>(undefined);
  $timeColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $spatialColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $aggColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $techColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $criteriaColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);


  $details = new Subject<{ title: string; properties: { name: string; value: string }[]; }>();
  $tech = new Subject<{ title: string; titleParams?: any, labels: Label[]; data: SingleOrMultiDataSet }>();
  $years = new BehaviorSubject<number[]>(undefined);

  techChartOptions: TechChartOptions = {
    type: 'bar',
    responsive: true,
    legend: false,
    sortByLabel: true,
    displayAllLabels: false,
  };
  chartTypes: ChartType[] = ['pie', 'bar', 'doughnut'];

  formatNumberLocale: string;
  animation: Subscription;

  @ViewChild('filterExpansionPanel', { static: true }) filterExpansionPanel: MatExpansionPanel;
  @ViewChild('aggExpansionPanel', { static: true }) aggExpansionPanel: MatExpansionPanel;

  get year(): number {
    return this.form.get('year').value;
  }

  get aggColumnName(): string {
    return this.form.get('strata.aggColumnName').value;
  }

  get techColumnName(): string {
    return this.form.get('strata.techColumnName').value;
  }

  get hasData(): boolean {
    return this.ready && this.data && this.data.total > 0;
  }

  get legendStartColor(): string {
    return this.legendForm.get('startColor').value;
  }

  set legendStartColor(value: string) {
    this.legendForm.get('startColor')
      .patchValue(value, {emitEvent: false});
  }

  get legendEndColor(): string {
    return this.legendForm.get('endColor').value;
  }

  set legendEndColor(value: string) {
    this.legendForm.get('endColor')
      .patchValue(value, {emitEvent: false});
  }

  get dirty(): boolean {
    return this.form.dirty || this.criteriaForm.dirty;
  }

  markAsPristine(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.markAsPristine(opts);
    this.form.markAsPristine(opts);
  }

  markAsTouched(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.markAsTouched(opts);
    AppFormUtils.markAsTouched(this.form);
  }
  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected alertCtrl: AlertController,
    protected toastController: ToastController,
    protected translate: TranslateService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected service: ExtractionService,
    protected settings: LocalSettingsService,
    protected formBuilder: FormBuilder,
    protected platform: PlatformService,
    protected zone: NgZone,
    protected aggregationStrataValidator: AggregationTypeValidatorService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, alertCtrl, toastController, translate, accountService, service, settings, formBuilder, platform);

    // Add controls to form
    this.form.addControl('strata', this.aggregationStrataValidator.getStrataFormGroup());
    this.form.addControl('year', this.formBuilder.control(null, Validators.required));
    this.form.addControl('month', this.formBuilder.control(null));
    this.form.addControl('quarter', this.formBuilder.control(null));

    this._enabled = true; // enable the form

    // If supervisor, allow to see all aggregations types
    this.typesFilter = {
      statusIds: this.accountService.hasMinProfile("SUPERVISOR") ? [0, 1, 2] : [1],
      isSpatial: true
    };

    // TODO: restored from settings ?
    const legendStartColor = new Color([255, 255, 190], 1);
    const legendEndColor = new Color([150, 30, 30], 1);
    this.legendForm = formBuilder.group({
      count: [10, Validators.required],
      min: [0, Validators.required],
      max: [1000, Validators.required],
      startColor: [legendStartColor.rgba(), Validators.required],
      endColor: [legendEndColor.rgba(), Validators.required]
    });

    this.loading = false;
    const account = this.accountService.account;
    this.formatNumberLocale = account && account.settings.locale || 'en-US';
    this.formatNumberLocale = this.formatNumberLocale.replace(/_/g, '-');

    this.platform.ready().then(() => {
      setTimeout(async () => {
        this.ready = true;
        if (!this.loading) return this.start();
      }, 500);
    });

    this.registerSubscription(
      this.onRefresh.pipe(
        // avoid multiple load)
        filter(() => this.ready && isNotNil(this.type) && (!this.loading || !!this.animation)),
        switchMap(() => {
          console.debug('[extraction-map] Refreshing...');
          return this.loadData();
        })
      ).subscribe(() => this.markAsPristine()));

  }

  ngOnInit() {
    super.ngOnInit();

    this.addChildForm(this.criteriaForm);

    this.registerSubscription(
      this.$onOverFeature
        .pipe(
          throttleTime(200),
          tap(feature => this.openFeatureDetails(feature))
        ).subscribe());

    this.registerSubscription(
      this.criteriaForm.form.valueChanges
        .pipe(
          filter(() => this.ready && !this.loading),
          debounceTime(250)
        ).subscribe(() => this.markForCheck())
    );
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  onMapReady(leafletMap: L.Map) {
    this.map = leafletMap;
    this.zone.run(() => {
      this.start.bind(this);
    });
  }

  protected watchTypes(): Observable<AggregationType[]> {
    return this.service.watchAggregationTypes(this.typesFilter)
      .pipe(
        map(types => {
          // Compute name, if need
          types.forEach(t => t.name = t.name || this.getI18nTypeName(t));
          // Sort by name
          types.sort((t1, t2) => t1.name > t2.name ? 1 : (t1.name < t2.name ? -1 : 0) );

          return types;
        })
      );
  }

  protected fromObject(json: any): AggregationType {
    return AggregationType.fromObject(json);
  }

  protected async start() {
    if (!this.ready || this.loading) return; // skip

    const hasData = await this.tryLoadByYearIterations();

    // No data found: open the select modal
    if (!hasData) {
      this.openSelectTypeModal();
    }
  }

  async setType(type: AggregationType, opts?: { emitEvent?: boolean; skipLocationChange?: boolean; sheetName?: string }): Promise<boolean> {
    const changed = await super.setType(type, opts);

    if (changed) {

      // Update the title
      const typeName = this.getI18nTypeName(this.type);
      this.$title.next(typeName);

      // Stop animation
      this.stopAnimation();

      // Update sheet names
      this.updateSheetNames();
    }
    else {
      // Force refresh
      await this.updateColumns(opts);
      this.applyDefaultStrata(opts);
    }

    return changed;
  }

  setSheetName(sheetName: string, opts?: { emitEvent?: boolean; skipLocationChange?: boolean; }) {
    const changed = this.sheetName != sheetName;

    // Reset min/max of the custom legend (if exists)
    if (changed) {
      if (this.customLegendOptions) {
        this.customLegendOptions.min = 0;
        this.customLegendOptions.max = undefined;
      }
      this.stopAnimation();

      this.$timeColumns.next(null);
      this.$spatialColumns.next(null);
      this.$aggColumns.next(null);
      this.$techColumns.next(null);
      this.hideTechChart();
    }

    super.setSheetName(sheetName, {
      emitEvent: false,
      ...opts
    });

    if (changed) {
      this.applyDefaultStrata(opts);
      this.updateColumns(opts)
        .then(() => {
          if (!this.loading && (!opts || opts.emitEvent !== false)) {
            return this.loadData();
          }
        });
    }
  }

  setAggStrata(aggColumnName: string, opts?: {emitEVent?: boolean; }) {
    const changed = this.aggColumnName !== aggColumnName;

    if (!changed) return; // Skip

    this.form.get('strata').patchValue({
      aggColumnName
    }, opts);

    if (!opts || opts.emitEVent !== false) {
      this.onRefresh.emit();
    }
  }

  setTechStrata(techColumnName: string, opts?: {emitEVent?: boolean; }) {
    this.form.get('strata').patchValue({
      techColumnName
    }, opts);

    if (!opts || opts.emitEVent !== false) {
      this.onRefresh.emit();
    }
  }

  hideTechChart() {
    this.$tech.next(null);
  }

  getI18nSheetName(sheetName?: string, type?: AggregationType, self?: ExtractionAbstractPage<any>): string {
    const str = super.getI18nSheetName(sheetName, type, self);
    return str.replace(/\([A-Z]+\)$/, '');
  }

  /* -- protected method -- */

  protected async updateColumns(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }) {
    if (!this.type) return;

    // Update filter columns
    const sheetName = this.sheetName;
    const columns = sheetName && (await this.service.loadColumns(this.type, sheetName)) || [];

    // Translate names
    this.translateColumns(columns);

    // Convert to a map, by column name
    this.columnNames = columns.reduce((res, c) => {
      res[c.columnName] = c.name;
      return res;
    }, {});

    const columnsMap = ExtractionUtils.dispatchColumns(columns);
    console.debug("[extraction-map] dispatched columns: ", columnsMap);

    this.$aggColumns.next(columnsMap.aggColumns);
    this.$techColumns.next(columnsMap.techColumns);
    this.$spatialColumns.next(columnsMap.spatialColumns);
    this.$timeColumns.next(ExtractionUtils.filterWithValues(columnsMap.timeColumns));
    this.$criteriaColumns.next(ExtractionUtils.filterValuesMinSize(columnsMap.criteriaColumns, 2));

    const yearColumn = (columns || []).find(c => c.columnName === 'year');
    const years = (yearColumn && yearColumn.values || []).map(s => parseInt(s));
    this.$years.next(years);
  }

  protected updateSheetNames() {
    // Filter sheet name on existing stratum
    let sheetNames = this.type && this.type.sheetNames || null;
    if (sheetNames && this.type.stratum) {
      sheetNames = this.type.stratum.map(s => s.sheetName)
        .filter(sheetName => isNotNil(sheetName) && sheetNames.includes(sheetName));
    }
    this.$sheetNames.next(sheetNames);
  }

  protected applyDefaultStrata(opts?: { emitEvent?: boolean; }) {
    const sheetName = this.sheetName;
    if (!this.type || !sheetName) return;

    const defaultStrata = sheetName && (this.type.stratum || []).find(s => s.isDefault || s.sheetName === sheetName);
    console.debug('[extraction-map] Applying default strata: ', defaultStrata);

    if (defaultStrata) {
      this.form.patchValue({
        strata: defaultStrata
      }, opts);
    }
  }

  protected async tryLoadByYearIterations(
    type?: AggregationType,
    startYear?: number,
    endYear?: number
  ) {

    type = type || this.type;
    startYear = isNotNil(startYear) ? startYear : new Date().getFullYear();
    endYear = isNotNil(endYear) && endYear < startYear ? endYear : startYear - 10;

    const sheetName = this.sheetName || (type && type.sheetNames && type.sheetNames[0]) || null;
    const strata: any = (type && type.stratum || []).find(s => s && (s.isDefault || sheetName == s.sheetName));

    if (!strata) return false; // Skip

    let year = startYear;
    let hasData = false;
    do {
      this.loading = true;

      // Set default filter
      this.form.patchValue({
        year: year--,
        strata
      });

      await this.loadData();

      hasData = this.hasData;
    }
    while (!hasData && year >= endYear);

    return hasData;
  }

  async loadData() {
    if (!this.ready) return;
    if (!this.type || !this.type.category || !this.type.label) {
      this.loading = false;
      return;
    }

    this.loading = true;
    this.$details.next(); // hide details
    this.error = null;

    const isAnimated = !!this.animation;
    const strata = this.getStrataValue();
    const filter = this.getFilterValue(strata);
    this.disable();

    const now = Date.now();
    console.debug(`[extraction-map] Loading layer ${this.type.category} ${this.type.label}`, filter, strata);

    try {
      let hasMore = true;
      let offset = 0;
      const size = 3000;

      const layer = L.geoJSON(null, {
        onEachFeature: this.onEachFeature.bind(this)
      });
      let total = 0;
      const aggColumnName = strata.aggColumnName;
      let maxValue = 0;

      while (hasMore) {

        // Get geo json using slice
        const geoJson = await this.service.loadAggregationGeoJson(this.type,
          strata,
          offset, size,
          null, null,
          filter, {
          fetchPolicy: isAnimated ? "cache-first" : undefined /*default*/
          });

        const hasData = isNotNil(geoJson) && geoJson.features && geoJson.features.length || false;

        if (hasData) {
          // Add data to layer
          layer.addData(geoJson);

          // Compute max value (need for legend)
          maxValue = geoJson.features
            .map(feature => feature.properties[aggColumnName] as number)
            .reduce((max, value) => Math.max(max, value), maxValue);

          offset += size;
          total += geoJson.features.length;
        }

        hasMore = hasData && geoJson.features.length >= size;
      }

      this.data.total = total;
      this.data.max = maxValue;

      if (total === 0) {
        console.debug(`[extraction-map] No data found, in ${Date.now() - now}ms`);

        // Refresh layer
        this.$layers.next([]);
      } else {

        // Prepare legend options
        const legendOptions = {
          ...this.legendForm.value,
          ...this.customLegendOptions
        }
        if (!this.customLegendOptions || isNil(legendOptions.max)) {
          legendOptions.max  = Math.max(10, Math.round(maxValue + 0.5));
        }
        this.legendForm.patchValue(legendOptions, {emitEvent: false});

        // Create scale legend
        const scale = this.createLegendScale(legendOptions);
        layer.setStyle(this.getFeatureStyleFn(scale, aggColumnName));
        this.updateLegendStyle(scale);

        // Remove existing layers
        (this.$layers.getValue() || []).forEach(layer => layer.remove());

        // Refresh layers
        this.$layers.next([layer]);
        console.debug(`[extraction-map] ${total} geometries loaded in ${Date.now() - now}ms (${Math.floor(offset / size)} slices)`);

        // Load tech data (wait end if animation is running)
        const techDataPromise = this.loadTechData(this.type, strata, filter);
        if (techDataPromise && isAnimated) await techDataPromise;

        // TODO fit to scale
        /*map.fitBounds(this.lalayersyer.getBounds(), {
          padding: point(24, 24),
          maxZoom: 12,
          animate: true
        });*/

      }

    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
      this.showLegend = false;
    } finally {
      this.showLegend = isNotNilOrBlank(strata.aggColumnName);
      this.$noData.next(!this.hasData);
      if (!isAnimated) {
        this.loading = false;
        this.enable();
      }
    }
  }

  async loadTechData(type?: AggregationType, strata?: CustomAggregationStrata, filter?: ExtractionFilter) {
    type = type || this.type;
    strata = type && (strata || this.getStrataValue());
    filter = strata && (filter || this.getFilterValue(strata));

    if (!type || !strata || !strata.techColumnName || !strata.aggColumnName) return // skip;

    const isAnimated = !!this.animation;
    const techColumnName = strata.techColumnName;

    try {
      const map = await this.service.loadAggregationTech(type, strata, filter, {
        fetchPolicy: isAnimated ? 'cache-first' : undefined /*default*/
      });

      // Keep data without values for this year
      if (this.techChartOptions.displayAllLabels) {
        // Find the column
        const column = this.$techColumns.getValue().find(c => c.columnName === techColumnName);

        // Add missing values
        (column.values || [])
          .filter(key => isNil(map[key]))
          .forEach(key => map[key] = 0);
      }

      let entries = Object.entries(map);

      const firstEntry = entries.length ? entries[0] : undefined;
      // If label are number: always sort by value (ASC)
      if (firstEntry && (firstEntry[0] !== 'string')) {
        entries = entries.sort((a, b) => a[0] > b[0] ? -1 : 1);
      }
      // Sort by label (ASC)
      else if (this.techChartOptions.sortByLabel) {
        entries = entries.sort((a, b) => a[0] > b[0] ? 1 : -1);
      }
      // Sort by value (DESC)
      else {
        entries = entries.sort((a, b) => a[1] > b[1] ? -1 : 1);
      }

      // Round values
      const data = entries.map(item => item[1])
        .map(value => isNil(value) ? 0 : Math.round(value * 100) / 100);
      const labels = entries.map(item => item[0]);

      this.$tech.next({
        title: 'EXTRACTION.MAP.TECH_CHART_TITLE',
        titleParams: {
          aggColumnName: this.columnNames[strata.aggColumnName],
          techColumnName: this.columnNames[strata.techColumnName]
        },
        labels: labels,
        data: data
      });
    }
    catch(error) {
      console.error("Cannot load tech values:", error);
      // Reset tech, then continue
      this.$tech.next(undefined);
    }

  }

  setYear(year: number, opts?: {emitEvent?: boolean; }) {
    const changed = this.year !== year;

    // Skip if same and emitEvent not forced
    if (!changed && (!opts || opts.emitEvent === false)) return;

    this.form.patchValue({
      year
    }, opts);

    // Refresh
    if (!opts || opts.emitEvent !== false) {
      this.onRefresh.emit();
    }
  }

  onRefreshClick(event?: UIEvent) {
    this.stopAnimation();
    this.onRefresh.emit(event);
  }

  protected onEachFeature(feature: Feature, layer: L.Layer) {
    layer.on('mouseover', (_) => this.zone.run(() => this.$onOverFeature.next(feature)));
    layer.on('mouseout', (_) => this.zone.run(() => this.closeFeatureDetails(feature)));
  }

  protected openFeatureDetails(feature: Feature) {
    if (this.$selectedFeature.getValue() === feature) return; // skip if already selected
    const strata = this.getStrataValue();
    const properties = Object.getOwnPropertyNames(feature.properties)
      .filter(key => !strata.aggColumnName || key !== strata.aggColumnName)
      .map(key => {
        return {
          name: this.columnNames[key],
          value: feature.properties[key]
        };
      });
    const aggValue = this.formatNumber(feature.properties[strata.aggColumnName]);
    feature.properties[strata.aggColumnName] = aggValue;

    const title = isNotNilOrBlank(strata.aggColumnName) ? `${this.columnNames[strata.aggColumnName]}: <b>${aggValue}</b>` : undefined;

    // Emit events
    this.$details.next({title, properties});
    this.$selectedFeature.next(feature);
  }

  closeFeatureDetails(feature: Feature, force?: boolean) {
    if (this.$selectedFeature.getValue() !== feature) return; // skip is not the selected feature

    // Close now, of forced (already wait 5s)
    if (force) {
      this.$selectedFeature.next(undefined);
      this.$details.next(); // Hide details
      return;
    }

    // Wait 5s before closing
    return setTimeout(() => this.closeFeatureDetails(feature, true), 4000);
  }


  openLegendForm(event: UIEvent) {
    this.showLegendForm = true;
  }

  cancelLegendForm(event: UIEvent) {
    this.showLegendForm = false;

    // Reset legend color
    //const color = this.legendForm.get('color').value;
    //this.legendStartColor = this.scale.endColor;
  }

  applyLegendForm(event: UIEvent) {
    this.showLegendForm = false;
    this.customLegendOptions = this.legendForm.value;
    this.onRefresh.emit();
  }

  async openSelectTypeModal(event?: UIEvent) {
    if (event) {
      event.preventDefault();
    }
    // If supervisor, allow to see all aggregations types
    const filter: AggregationTypeFilter = {
      statusIds: this.accountService.hasMinProfile("SUPERVISOR") ? [0, 1, 2] : [1],
      isSpatial: true
    };
    const modal = await this.modalCtrl.create({
      component: AggregationTypeSelectModal,
      componentProps: {
        filter: filter
      }, keyboardClose: true
    });

    // Open the modal
    modal.present();

    // Wait until closed
    const res = await modal.onDidDismiss();

    // If new vessel added, use it
    if (res && res.data instanceof AggregationType) {
      const type = res.data as AggregationType;
      await this.setType(type, {emitEvent: false});

      const hasData = await this.tryLoadByYearIterations(type);

      // If no data: loop
      if (!hasData) {
        this.filterExpansionPanel.open();
      }
    }
  }

  toggleAnimation(event?: UIEvent) {
    if (event && event.defaultPrevented) return;
    // Avoid to expand the filter section
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    // Stop existing animation
    if (this.animation) {
      this.stopAnimation();
    }
    else {
      this.startAnimation();
    }
  }

  toggleGraticule() {

    // Make sure value is correct
    this.showGraticule = this.showGraticule && this.map.hasLayer(this.sextantGraticuleLayer);

    // Inverse value
    this.showGraticule = !this.showGraticule;

    // Add or remove layer
    if (this.showGraticule) {
      this.sextantGraticuleLayer.addTo(this.map);
      this.showGraticule = true;
    }
    else {
      this.map.removeLayer(this.sextantGraticuleLayer);
    }
  }

  setTechChartOption(value: Partial<TechChartOptions>, opts?: { emitEvent?: boolean; }) {
    this.techChartOptions = {
      ...this.techChartOptions,
      ...value
    };

    // Refresh (but skip if animation running)
    if (!this.animation && (!opts || opts.emitEvent !== false)) {
      this.loadTechData();
    }
  }

  onChartClick({event, active}) {
    if (!active) return; // Skip

    // Retrieve clicked values
    const values = active
      .map(element => element && element._model && element._model.label)
      .filter(isNotNil);
    if (isEmptyArray(values)) return; // Skip if empty

    const value = values[0];

    const hasChanged = this.criteriaForm.addFilterCriterion({
      name: this.techColumnName,
      operator: DEFAULT_CRITERION_OPERATOR,
      value: value,
      sheetName: this.sheetName
    }, {
      appendValue: event.ctrlKey
    });
    if (!hasChanged) return; // Skip if already added

    if (this.filterExpansionPanel && !this.filterExpansionPanel.expanded) {
      this.filterExpansionPanel.open();
    }

    if (!event.ctrlKey) {
      this.onRefresh.emit();
    }
  }


  setBaseLayer(layer: L.Layer) {

    if (this.map.hasLayer(layer)) return; // Skip

    this.baseLayers.forEach(l => {
      if (l.layer === layer) {
        this.map.addLayer(l.layer);
        this.baseLayer = layer;
      }
      else {
        this.map.removeLayer(l.layer);
      }
    });
  }


  /* -- protected methods -- */

  protected startAnimation() {
    const years = this.$years.getValue();

    // Pre loading data
    console.info("[extraction-map] Preloading data for animation...");


    console.info("[extraction-map] Starting animation...");
    this.animation = isNotEmptyArray(years) && timer(500, 500)
      .pipe(
        throttleTime(450)
      )
      .subscribe(index => {
        const year = years[index % arraySize(years)];
        console.info("[extraction-map] Rendering animation for year {" + year + "}...");
        this.setYear(year, {emitEvent: true /*force refresh if same*/});
      });

    this.animation.add(() => {
      console.info("[extraction-map] Animation stopped");
    });

    this.registerSubscription(this.animation);
  }

  protected stopAnimation() {
    if (this.animation) {
      this.unregisterSubscription(this.animation);
      this.animation.unsubscribe();
      this.animation = null;

      if (this.disabled) {
        this.enable();
        this.loading = false;
      }
    }
  }

  protected getFeatureStyleFn(scale: ColorScale, propertyName: string): L.StyleFunction<any> | null {
    if (isNil(propertyName)) return;

    return (feature) => {

      const value = feature.properties[propertyName];
      const color = scale.getValueColor(value);

      //console.debug(`${options.propertyName}=${value} | color=${color} | ${feature.properties['square']}`);

      return {
        fillColor: color,
        weight: 0,
        opacity: 0,
        color: color,
        fillOpacity: 1
      };
    };
  }

  protected createLegendScale(opts?: Partial<LegendOptions>): ColorScale {
    opts = opts || this.legendForm.value;
    const min = opts.min || 0;
    const max = opts.max || 1000;
    const startColor = Color.parseRgba(opts.startColor);
    const mainColor = Color.parseRgba(opts.endColor);
    const endColor = Color.parseRgba('rgb(0,0,0)');

    // Create scale color (max 10 grades
    const scaleCount = Math.max(2, Math.min(max, 10));
    const scale = ColorScale.custom(scaleCount, {
      min: min,
      max: max,
      opacity: mainColor.opacity,
      startColor: startColor.rgb,
      mainColor: mainColor.rgb,
      mainColorIndex: Math.trunc(scaleCount * 0.9),
      endColor: endColor.rgb
    });

    this.$legendItems.next(scale.legend.items);
    this.showLegendForm = false;
    return scale;
  }

  protected updateLegendStyle(scale: ColorScale) {
    const items = scale.legend.items;
    const longerItemLabel = items.length > 2 && items[items.length - 2].label || '9999'; // Use N-2, because last item is shorter
    let minWidth = Math.max(105, 36 /* start offset */ + longerItemLabel.length * 4.7 /* average width of a letter */ );
    this.legendStyle = {
      minWidth: `${minWidth || 150}px`,
      maxWidth: '250px'
    };

  }

  protected getFilterValue(strata?: CustomAggregationStrata): ExtractionFilter {

    const filter = super.getFilterValue();

    strata = strata || this.getStrataValue();
    if (!strata) return filter;

    const json = this.form.value;
    const sheetName = this.sheetName;

    // Time strata = year
    if (strata.timeColumnName === 'year' && json.year > 0) {
      filter.criteria.push({name: 'year', operator: '=', value: json.year, sheetName: sheetName} as ExtractionFilterCriterion);
    }

    // Time strata = quarter
    else if (strata.timeColumnName === 'quarter' && json.year > 0 && json.quarter > 0) {
      filter.criteria.push({name: 'year', operator: '=', value: json.year, sheetName: sheetName} as ExtractionFilterCriterion);
      filter.criteria.push({name: 'quarter', operator: '=', value: json.quarter, sheetName: sheetName} as ExtractionFilterCriterion);
    }

    // Time strata = month
    else if (strata.timeColumnName === 'month' && json.year > 0 && json.month > 0) {
      filter.criteria.push({name: 'year', operator: '=', value: json.year, sheetName: sheetName} as ExtractionFilterCriterion);
      filter.criteria.push({name: 'month', operator: '=', value: json.month, sheetName: sheetName} as ExtractionFilterCriterion);
    }

    return filter;
  }

  protected isEquals(t1: AggregationType, t2: AggregationType): boolean {
    return AggregationType.equals(t1, t2);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected getStrataValue(): CustomAggregationStrata {
    const json = this.form.get('strata').value;
    delete json.__typename;
    return json as AggregationStrata;
  }

  protected formatNumber(value: number|string, columnName?: string): string|undefined {
    if (isNil(value)) return undefined;
    columnName = columnName || this.aggColumnName;
    if (typeof value === 'string') {
      value = parseFloat(value);
    }
    const symbol = columnName && columnName.endsWith('_weight') ? 'kg' : '';
    return value.toLocaleString(this.formatNumberLocale, {
      useGrouping: true,
      maximumSignificantDigits: 2}) + ' ' + symbol;
  }
}

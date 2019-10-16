import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Injector,
  NgZone,
  OnInit,
  ViewChild
} from "@angular/core";
import {PlatformService} from "../../core/services/platform.service";
import {AggregationTypeFilter, ExtractionFilter} from "../services/extraction.service";
import {BehaviorSubject, Observable, Subject, Subscription} from "rxjs";
import {
  arraySize,
  isNil,
  isNilOrBlank, isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  suggestFromArray,
  suggestFromStringArray
} from "../../shared/functions";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {AggregationStrata, AggregationType, ExtractionColumn, ExtractionType} from "../services/extraction.model";
import {Location} from "@angular/common";
import {MatExpansionPanel} from "@angular/material";
import {ExtractionForm} from "./extraction-filter.form";
import {Color, ColorScale, fadeInAnimation, fadeInOutAnimation} from "../../shared/shared.module";
import {ColorScaleLegendItem} from "../../shared/graph/graph-colors";
import * as L from 'leaflet';
import {CRS, LayerGroup} from 'leaflet';
import {Feature} from "geojson";
import {map, switchMap, tap, throttleTime} from "rxjs/operators";
import {ModalController} from "@ionic/angular";
import {AggregationTypeSelectModal} from "./aggregation-type-select.modal";
import {AccountService} from "../../core/services/account.service";
import {FormFieldDefinition, FormFieldType} from "../../shared/form/field.model";


const SPACE_STRATA_COLUMNS: string[] = [
  /* FIXME: there is no Area geometry, in DB, so ignore this column for now
    'area',*/
  'rect', 'statistical_rectangle', 'square'];
const TIME_STRATA_COLUMNS: string[] = ['year', 'quarter', 'month'];

@Component({
  selector: 'app-extraction-map-page',
  templateUrl: './extraction-map.page.html',
  styleUrls: ['./extraction-map.page.scss'],
  animations: [fadeInAnimation, fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExtractionMapPage extends ExtractionForm<AggregationType> implements OnInit {


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
    zoom: 5,
    center: L.latLng(46.879966, -10)
  };
  layersControl = {
    baseLayers: {
      'Sextant (Ifremer)': this.sextantBaseLayer,
      'Open Street Map': this.osmBaseLayer
    },
    overlays: {
      'Graticule': this.sextantGraticuleLayer
    }
  };
  data = {
    total: 0,
    min: 0,
    max: 0
  };
  legendForm: FormGroup;
  showLegendForm = false;
  columnNames = {};
  showDetails = false;
  map: L.Map;
  typesFilter: AggregationTypeFilter;

  $title = new BehaviorSubject<string>(undefined);
  $layers = new BehaviorSubject<L.GeoJSON<L.Polygon>[]>(null);
  $legendItems = new BehaviorSubject<ColorScaleLegendItem[] | undefined>([]);
  $onOverFeature = new Subject<Feature>();
  $selectedFeature = new BehaviorSubject<Feature | undefined>(undefined);

  $techColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $spaceColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $timeColumns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  $columns = new BehaviorSubject<ExtractionColumn[]>(undefined);

  $columnValueDefinitions = new BehaviorSubject<FormFieldDefinition[]>(undefined);
  $columnValueDefinitionsByIndex: { [index: number]: BehaviorSubject<FormFieldDefinition> } = {};

  $details = new Subject<{ title: string; properties: { name: string; value: string }[]; }>();
  $stats = new Subject<{ title: string; properties: { name: string; value: string }[] }>();
  $years = new BehaviorSubject<number[]>(undefined);

  animation: Subscription;

  @ViewChild(MatExpansionPanel) filterExpansionPanel: MatExpansionPanel;

  get techStrata(): string {
    return this.form.get('strata').get('tech').value;
  }

  get year(): number {
    return this.form.get('year').value;
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


  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected platform: PlatformService,
    protected location: Location,
    protected accountService: AccountService,
    protected modalCtrl: ModalController,
    protected zone: NgZone,
    protected cd: ChangeDetectorRef
  ) {
    super(injector,
      formBuilder.group({
        type: [null, Validators.required],
        sheetName: [null],
        strata: formBuilder.group({
          space: [null, Validators.required],
          time: [null, Validators.required],
          tech: [null, Validators.required]
        }),
        year: [null, Validators.required],
        quarter: [null],
        month: [null],
        sheets: formBuilder.group({})
      }));

    this.routePath = 'map';
    this._enable = true; // enable the form

    // If supervisor, allow to see all aggregations types
    this.typesFilter = {
      statusIds: this.accountService.hasMinProfile("SUPERVISOR") ? [0, 1, 2] : [1],
      isSpatial: true
    };

    // TODO: get Settings preference ?
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

  }

  ngOnInit() {

    super.ngOnInit();

    this.platform.ready().then(() => {
      setTimeout(async () => {
        this.ready = true;
        if (!this.loading) return this.start();
      }, 500);
    });

    this.onRefresh
      .subscribe(() => {
        if (!this.ready || this.loading || isNil(this.type)) return; // avoid multiple load
        console.debug('[extraction-map] Refreshing...');
        return this.load(this.type);
      });

    this.registerSubscription(
      this.$onOverFeature
        .pipe(
          throttleTime(200)
        )
        .subscribe((feature) => this.openFeatureDetails(feature)));
  }

  onMapReady(map: L.Map) {
    this.map = map;
    this.zone.run(() => {
      this.start.bind(this);
    });
  }

  protected watchTypes(): Observable<AggregationType[]> {
    return this.service.loadAggregationTypes(this.typesFilter)
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

  async setType(type: AggregationType, opts?: { emitEvent?: boolean; skipLocationChange?: boolean; sheetName?: string }): Promise<void> {
    await super.setType(type, opts);

    // Update the title
    const typeName = this.getI18nTypeName(this.type);
    this.$title.next(typeName);

    // Update filter columns
    const columns = (await this.service.loadColumns(this.type, this.sheetName)) || [];

    // Translate name
    this.columnNames = columns.reduce((res, c) => {
      // Make sure to translate name
      c.name = c.name !== c.columnName ? c.name : this.getI18nColumnName(c.columnName);
      res[c.columnName] = c.name;
      return res;
    }, {});

    const techColumns = columns.filter(c =>
      (!c.type || c.type === 'integer' || c.type === 'double')
      && (c.columnName.endsWith('_count')
          || c.columnName.endsWith('_time')
          || c.columnName.endsWith('_weight')));
    this.$techColumns.next(techColumns);

    const spaceColumns = columns.filter(c => SPACE_STRATA_COLUMNS.includes(c.columnName)
      && isNotEmptyArray(c.values));
    this.$spaceColumns.next(spaceColumns);

    const timeColumns = columns.filter(c => TIME_STRATA_COLUMNS.includes(c.columnName));
    this.$timeColumns.next(timeColumns);

    const excludedFilterColumns = spaceColumns.concat(timeColumns);
    const filterColumns = columns.filter(c => !excludedFilterColumns.includes(c) &&
      // Exclude string column if only one value
      (c.type !== 'string' || arraySize(c.values) > 1));
    this.$columns.next(filterColumns);

    this.$columnValueDefinitions.next(filterColumns.map(c => this.getColumnDefinition(c)));

    const yearColumn = (columns || []).find(c => c.columnName === 'year');
    const years = (yearColumn && yearColumn.values || []).map(s => parseInt(s));
    this.$years.next(years);
  }

  protected async tryLoadByYearIterations(
    type?: AggregationType,
    startYear?: number,
    endYear?: number
  ) {

    startYear = isNotNil(startYear) ? startYear : new Date().getFullYear();
    endYear = isNotNil(endYear) && endYear < startYear ? endYear : startYear - 3;

    let year = startYear;
    let hasData = false;
    do {
      this.loading = true;

      // Set default filter
      this.form.patchValue({
        strata: {space: 'square', time: 'year'},
        year: year--
      });

      await this.load(type);

      hasData = this.hasData;
    }
    while (!hasData && year > endYear);

    return hasData;
  }

  protected async load(type?: AggregationType) {
    if (!this.ready) return;
    if (!type && this.invalid) {
      this.loading = false;
      return;
    }

    this.type = type || this.form.get('type').value;
    this.loading = true;
    this.$details.next(); // hide details
    this.error = null;

    const strata = this.form.get('strata').value;
    const filter = this.getFilterValue();
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
      const techStrata = this.techStrata;
      let maxValue = 0;

      while (hasMore) {

        // Get geo json using slice
        const geoJson = await this.service.loadAggregationGeoJson(this.type,
          strata,
          offset, size,
          null, null,
          filter);

        const hasData = isNotNil(geoJson) && geoJson.features && geoJson.features.length || false;

        if (hasData) {
          // Add data to layer
          layer.addData(geoJson);

          // Compute max value (need for legend)
          maxValue = geoJson.features
            .map(feature => feature.properties[techStrata] as number)
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

        // Create scale color (max 10 grades
        this.legendForm.get('max').setValue(Math.max(10, Math.round(maxValue + 0.5)), {emitEvent: false});
        const scale = this.createLegendScale();
        layer.setStyle(this.getFeatureStyleFn(scale, techStrata));

        const typeName = this.$title.getValue();

        // Remove old data layer
        Object.getOwnPropertyNames(this.layersControl.overlays)
          .forEach((layerName, index) => {
            if (index === 0) return; // Keep graticule layer
            const existingLayer = this.layersControl.overlays[layerName] as LayerGroup<any>;
            existingLayer.remove();
            delete this.layersControl.overlays[layerName];
          });

        // Add new layer to layers control
        this.layersControl.overlays[typeName] = layer;

        // Refresh layer
        this.$layers.next([layer]);

        console.debug(`[extraction-map] ${total} geometries loaded in ${Date.now() - now}ms (${Math.floor(offset / size)} slices)`);

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
    } finally {
      this.loading = false;
      this.enable();
    }

  }

  setYear(event: UIEvent, value) {
    if (event && event.defaultPrevented) return; // skip
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    if (this.year == value) return; // same: skip

    this.form.get('year').setValue(value);

    this.onRefresh.emit();
  }

  protected onEachFeature(feature: Feature, layer: L.Layer) {
    layer.on('mouseover', (_) => this.zone.run(() => this.$onOverFeature.next(feature)));
    layer.on('mouseout', (_) => this.zone.run(() => this.closeFeatureDetails(feature)));
  }

  protected openFeatureDetails(feature: Feature) {
    if (this.$selectedFeature.getValue() === feature) return; // skip if already selected
    const strata = this.form.get('strata').value as AggregationStrata;
    const properties = Object.getOwnPropertyNames(feature.properties)
      .filter(key => !strata.tech || key !== strata.tech)
      .map(key => {
        return {
          name: this.columnNames[key],
          value: feature.properties[key]
        };
      });
    const title = isNotNilOrBlank(strata.tech) ? `${this.columnNames[strata.tech]}: <b>${feature.properties[strata.tech]}</b>` : undefined;

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
        //this.openSelectTypeModal();
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
    if (this.animation) {
      this.animation.unsubscribe();
      this.animation = null;
    }
    else {
      const years = this.$years.getValue();
      console.info("[extraction-map] Starting animation...");
      this.animation = isNotEmptyArray(years) && Observable.timer(500, 500)
        .pipe(
          tap(index => {
            const year = years[index % arraySize(years)];
            console.info("[extraction-map] Rendering animation for year {" + year + "}...");
            this.setYear(null, year);
          })
        )
        .subscribe();

      this.animation.add(() => {
        console.info("[extraction-map] Animation stopped");
      });
    }
  }


  /* -- protected methods -- */

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

  protected createLegendScale(): ColorScale {
    const json = this.legendForm.value;
    const min = json.min || 0;
    const max = json.max;
    const startColor = Color.parseRgba(json.startColor);
    const mainColor = Color.parseRgba(json.endColor);
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


  protected getFilterValue(): ExtractionFilter {

    const filter = super.getFilterValue();

    const json = this.form.value;
    const strata = json.strata as AggregationStrata;
    const sheetName = this.sheetName;

    if (!strata) return filter;

    // Time strata = year
    if (strata.time === 'year' && json.year > 0) {
      filter.criteria.push({name: 'year', operator: '=', value: json.year, sheetName: sheetName});
    }

    // Time strata = quarter
    else if (strata.time === 'quarter' && json.year > 0 && json.quarter > 0) {
      filter.criteria.push({name: 'year', operator: '=', value: json.year, sheetName: sheetName});
      filter.criteria.push({name: 'quarter', operator: '=', value: json.quarter, sheetName: sheetName});
    }

    // Time strata = month
    else if (strata.time === 'month' && json.year > 0 && json.month > 0) {
      filter.criteria.push({name: 'year', operator: '=', value: json.year, sheetName: sheetName});
      filter.criteria.push({name: 'month', operator: '=', value: json.month, sheetName: sheetName});
    }

    return filter;
  }

  resetFilterCriteria() {

    // Close the filter panel
    if (this.filterExpansionPanel && this.filterExpansionPanel.expanded) {
      this.filterExpansionPanel.close();
    }

    super.resetFilterCriteria();
  }

  getCriterionValueDefinition(index: number): Observable<FormFieldDefinition> {
    return this.$columnValueDefinitionsByIndex[index] || this.updateCriterionValueDefinition(index);
  }

  updateCriterionValueDefinition(index: number, columnName?: string, resetValue?: boolean): Observable<FormFieldDefinition> {
    const criterionForm = this.sheetCriteriaForm.at(index) as FormGroup;
    columnName = columnName || (criterionForm && criterionForm.controls.name.value);
    const definition = columnName && this.$columnValueDefinitions.getValue().find(d => d.key === columnName) || null;

    // Reset the criterion value, is ask by caller
    if (resetValue) criterionForm.patchValue({value: null});

    let subject = this.$columnValueDefinitionsByIndex[index];
    if (!subject) {
      subject = new BehaviorSubject(definition);
      this.$columnValueDefinitionsByIndex[index] = subject;
    }
    else {
      subject.next(definition);
      //this.markForCheck();
    }
    return subject;
  }

  protected getColumnDefinition(column: ExtractionColumn): FormFieldDefinition {
    if (column.type === 'string' && isNotEmptyArray(column.values)) {
      return {
        key: column.columnName,
        label: column.name,
        type: 'entity',
        autocomplete: {
          items: column.values,
          attributes: [undefined],
          columnNames: ['EXTRACTION.FILTER.CRITERION_VALUE'],
          displayWith: (value) => value
        }
      };
    }
    else {
      return  {
        key: column.columnName,
        label: column.name,
        type: column.type as FormFieldType
      };
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }


}

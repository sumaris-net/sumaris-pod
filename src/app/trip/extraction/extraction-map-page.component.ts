import {ChangeDetectionStrategy, ChangeDetectorRef, Component, NgZone, OnInit, ViewChild} from "@angular/core";
import {PlatformService} from "../../core/services/platform.service";
import {AggregationTypeFilter, ExtractionFilter, ExtractionService} from "../services/extraction.service";
import {BehaviorSubject, Observable, Subject} from "rxjs";
import {isNil, isNotNil} from "../../shared/functions";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {AggregationStrata, AggregationType, ExtractionType} from "../services/extraction.model";
import {TranslateService} from "@ngx-translate/core";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from "@angular/common";
import {DateAdapter, MatExpansionPanel} from "@angular/material";
import {ExtractionForm} from "./extraction-filter.form";
import {Moment} from "moment";
import {Color, ColorScale, fadeInAnimation, fadeInOutAnimation} from "../../shared/shared.module";
import {ColorScaleLegendItem} from "../../shared/graph/graph-colors";
import * as L from 'leaflet';
import {Feature} from "geojson";
import {throttleTime} from "rxjs/operators";
import {ModalController} from "@ionic/angular";
import {ExtractionSelectTypeModal} from "./extraction-list-modal.component";
import {AccountService} from "../../core/services/account.service";
import {CRS} from "leaflet";

@Component({
  selector: 'app-extraction-map-page',
  templateUrl: './extraction-map-page.component.html',
  styleUrls: ['./extraction-map-page.component.scss'],
  animations: [fadeInAnimation, fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExtractionMapPage extends ExtractionForm<AggregationType> implements OnInit {


  // -- Map Layers --
  osmBaseLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {maxZoom: 18, attribution: '<a href=\'https://www.openstreetmap.org\'>OSM</a>'});
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
  detailsLabels = {};
  showDetails = false;
  map: L.Map;
  typesFilter: AggregationTypeFilter;

  $title = new Subject<string>();
  $layers = new BehaviorSubject<L.GeoJSON<L.Polygon>[]>(null);
  $legendItems = new BehaviorSubject<ColorScaleLegendItem[] | undefined>([]);
  $onOverFeature = new Subject<Feature>();
  $selectedFeature = new BehaviorSubject<Feature | undefined>(undefined);

  $details = new Subject<{ title: string; properties: { name: string; value: string }[]; }>();
  $stats = new Subject<{ title: string; properties: { name: string; value: string }[] }>();

  @ViewChild(MatExpansionPanel) filterExpansionPanel: MatExpansionPanel;

  get techStrata(): string {
    return this.form.get('strata').get('tech').value;
  }

  get hasData(): boolean {
    return this.ready && !this.loading && this.data && this.data.total > 0;
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
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected route: ActivatedRoute,
    protected router: Router,
    protected translate: TranslateService,
    protected service: ExtractionService,
    protected platform: PlatformService,
    protected location: Location,
    protected accountService: AccountService,
    protected modalCtrl: ModalController,
    protected zone: NgZone,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter,
      formBuilder,
      route,
      router,
      translate,
      service,
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

  protected loadTypes(): Observable<AggregationType[]> {
    return this.service.loadAggregationTypes(this.typesFilter);
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
        strata: {space: 'square', time: 'year', tech: 'station_count'},
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

          // Translate property names
          if (offset === 0) {
            this.detailsLabels = {};
            Object.getOwnPropertyNames(geoJson.features[0].properties)
              .forEach(columnName => {
                this.detailsLabels[columnName] = this.getI18nColumnName(columnName);
              });
          }

          offset += size;
          total += geoJson.features.length;
        }

        hasMore = hasData && geoJson.features.length >= size;
      }

      this.data.total = total;
      this.data.max = maxValue;

      if (total === 0) {
        console.debug(`[extraction-map] No data found, in ${Date.now() - now}ms`);
        this.filterExpansionPanel.open();
        return;
      }

      // Create scale color (max 10 grades
      this.legendForm.get('max').setValue(Math.max(10, Math.round(maxValue + 0.5)), {emitEvent: false});
      const scale = this.createLegendScale();
      layer.setStyle(this.getFeatureStyleFn(scale, techStrata));

      // Add to layers control
      const typeName = this.getI18nTypeName(this.type);
      // Update layer control layers (Remove old data layer)
      Object.getOwnPropertyNames(this.layersControl.overlays).forEach((layerName, index) => {
        if (index === 0) return; // Keep graticule layer
        delete this.layersControl.overlays[layerName];
      });
      this.layersControl.overlays[typeName] = layer;

      // Emit event
      this.$layers.next([layer]);
      this.$title.next(typeName);

      console.debug(`[extraction-map] ${total} geometries loaded in ${Date.now() - now}ms (${Math.floor(offset / size)} slices)`);

      // TODO
      /*map.fitBounds(this.lalayersyer.getBounds(), {
        padding: point(24, 24),
        maxZoom: 12,
        animate: true
      });*/


    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
    } finally {
      this.loading = false;
      this.enable();
    }

  }

  protected onEachFeature(feature: Feature, layer: L.Layer) {
    layer.on('mouseover', (_) => this.zone.run(() => this.$onOverFeature.next(feature)));
    layer.on('mouseout', (_) => this.zone.run(() => this.closeFeatureDetails(feature)));
  }

  protected openFeatureDetails(feature: Feature) {
    if (this.$selectedFeature.getValue() === feature) return; // skip if already selected
    const strata = this.form.get('strata').value as AggregationStrata;
    const properties = Object.getOwnPropertyNames(feature.properties)
      .filter(key => key !== strata.tech)
      .map(key => {
        return {
          name: this.detailsLabels[key],
          value: feature.properties[key]
        };
      });
    const title = this.detailsLabels[strata.tech] + ': <b>' + feature.properties[strata.tech] + '</b>';

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

  async openSelectTypeModal() {
    // If supervisor, allow to see all aggregations types
    const filter: AggregationTypeFilter = {
      statusIds: this.accountService.hasMinProfile("SUPERVISOR") ? [0, 1, 2] : [1],
      isSpatial: true
    };
    const modal = await this.modalCtrl.create({
      component: ExtractionSelectTypeModal,
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
      await this.setType(type);

      const hasData = await this.tryLoadByYearIterations(type);

      // If no data: loop
      if (!hasData) {
        this.openSelectTypeModal();
      }
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
    const endColor = Color.parseRgba(json.endColor);

    // Create scale color (max 10 grades
    const scaleCount = Math.max(2, Math.min(max, 10));
    const scale = ColorScale.custom(scaleCount, {
      min: min,
      max: max,
      opacity: endColor.opacity,
      startColor: startColor.rgb,
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

  public resetFilterCriteria() {

    // Close the filter panel
    if (this.filterExpansionPanel && this.filterExpansionPanel.expanded) {
      this.filterExpansionPanel.close();
    }

    super.resetFilterCriteria();
  }


  // public getCriteriaName(form): string {
  //   console.log("form", form);
  //   return "TOTO";
  // }

  protected markForCheck() {
    this.cd.markForCheck();
  }


}

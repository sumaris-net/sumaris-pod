import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, NgZone, OnInit} from "@angular/core";
import {BehaviorSubject, Subject} from "rxjs";
import {Operation} from "../../services/model/trip.model";
import * as L from "leaflet";
import {CRS, LayerGroup, MapOptions, PathOptions} from "leaflet";
import {PlatformService} from "../../../core/services/platform.service";
import {BBox, Feature, LineString, Polygon} from "geojson";
import {AlertController, ModalController} from "@ionic/angular";
import {TranslateService} from "@ngx-translate/core";
import {isNotEmptyArray, isNotNil, isNotNilOrBlank} from "../../../shared/functions";
import {filter, tap, throttleTime} from "rxjs/operators";
import {AppTabEditor} from "../../../core/form/tab-editor.class";
import {fadeInOutAnimation} from "../../../shared/material/material.animations";
import {ActivatedRoute, Router} from "@angular/router";
import {DateFormatPipe} from "../../../shared/pipes/date-format.pipe";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {EntityUtils} from "../../../core/services/model/entity.model";
import {ProgramService} from "../../../referential/services/program.service";
import {ProgramProperties} from "../../../referential/services/config/program.config";
import {LeafletControlLayersConfig} from "@asymmetrik/ngx-leaflet/src/leaflet/layers/control/leaflet-control-layers-config.model";

@Component({
  selector: 'app-operations-map',
  templateUrl: './operations.map.html',
  styleUrls: ['./operations.map.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationsMap extends AppTabEditor<Operation[]> implements OnInit {

  private _program: string;
  private _dateTimePattern: string;

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
  options = <MapOptions>{
    layers: [this.sextantBaseLayer],
    maxZoom: 10, // max zoom to sextant layer
    zoom: 5, // (can be override by a program property)
    center: L.latLng(46.879966, -10) // Atlantic (can be override by a program property)
  };
  layersControl = <LeafletControlLayersConfig>{
    baseLayers: {
      'Sextant (Ifremer)': this.sextantBaseLayer,
      'Open Street Map': this.osmBaseLayer
    },
    overlays: {
      'Graticule': this.sextantGraticuleLayer
    }
  };
  map: L.Map;
  $layers = new BehaviorSubject<L.GeoJSON<L.Polygon>[]>(null);
  $onOverFeature = new Subject<Feature>();
  $onOutFeature = new Subject<Feature>();
  $selectedFeature = new BehaviorSubject<Feature>(null);

  get isNewData(): boolean {
    return false;
  }

  get modalName(): string {
    return this.constructor.name;
  }

  @Input() operations: Operation[];

  @Input()
  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
      this._program = value;
      if (this.ready && !this.loading) this.loadDefaultsFromProgram();
    }
  }

  get program(): string {
    return this._program;
  }

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected platform: PlatformService,
    protected viewCtrl: ModalController,
    protected dateFormatPipe: DateFormatPipe,
    protected settings: LocalSettingsService,
    protected zone: NgZone,
    protected cd: ChangeDetectorRef,
    protected programService: ProgramService
  ) {
    super(route, router, alertCtrl, translate);
    this.loading = false;
    this._dateTimePattern = this.translate.instant('COMMON.DATE_TIME_PATTERN');

    setTimeout(async () => {
      this.ready = true;
      if (!this.loading) return this.start();
    }, 500);
  }

  ngOnInit() {
    this.registerSubscription(
      this.$onOverFeature
        .pipe(
          throttleTime(200),
          filter(feature => feature !== this.$selectedFeature.getValue()),
          tap(feature => this.$selectedFeature.next(feature))
        ).subscribe());

    this.registerSubscription(
      this.$onOutFeature
        .pipe(
          throttleTime(5000),
          filter(feature => feature === this.$selectedFeature.getValue()),
          tap(feature => this.$selectedFeature.next(undefined))
        ).subscribe());


  }

  onMapReady(leafletMap: L.Map) {
    this.map = leafletMap;
    this.zone.run(() => {
      this.start.bind(this);
    });
  }

  async cancel(event?: Event) {
    await this.viewCtrl.dismiss(null, 'cancel');
  }

  /* -- protected functions -- */

  protected async start() {
    if (!this.ready || this.loading) return; // skip

    // Load map defaults, from program (center, zoom)
    if (isNotNilOrBlank(this._program)) {
      await this.loadDefaultsFromProgram({emitEvent: false});
    }

    await this.load();
  }

  async save(event, options?: any): Promise<any> {
    throw new Error('Nothing to save');
  }

  async load(id?: number, opts?:  any) {
    if (!this.ready) return; // Skip

    this.loading = true;
    this.error = null;

    try {
      // Clean existing layers, if any
      this.cleanMapLayers();

      const tripLayer = L.geoJSON(null, {
        style: this.getTripLayerStyle()
      });
      const operationLayer = L.geoJSON(null, {
        onEachFeature: this.onEachFeature.bind(this),
        style: (feature) => this.getOperationLayerStyle(feature)
      });

      // Add operation to layer
      const allPositionsCoords: [number, number][] = [];
      (this.operations || [])
        .sort(EntityUtils.sortComparator('rankOrderOnPeriod', 'asc'))
        .forEach((ope, index) => {
          const operationCoords: [number, number][] = [ope.startPosition, ope.endPosition]
            .filter(pos => pos && isNotNil(pos.latitude) && isNotNil(pos.longitude))
            .map(pos => [pos.longitude, pos.latitude]);
          if (operationCoords.length > 0) {
            // Add to operation layer
            operationLayer.addData(<Feature>{
              type: "Feature",
              id: ope.id,
              geometry: <LineString>{
                type: "LineString",
                coordinates: operationCoords
              },
              properties: {
                first: index === 0,
                ...ope,
                // Replace date with a formatted date
                startDateTime: this.dateFormatPipe.format(ope.startDateTime, this._dateTimePattern),
                // Add index
                index
              }
            });

            // Add to all position array
            operationCoords.forEach(coords => allPositionsCoords.push(coords));
          }
      });

      // Add trip feature to layer
      tripLayer.addData(<Feature>{
        type: "Feature",
        id: 'trip',
        geometry: <LineString>{
          type: "LineString",
          coordinates: allPositionsCoords
        }
      });

      // Add new layer to layers control
      const tripLayerName = this.translate.instant('TRIP.OPERATION.MAP.TRIP_LAYER');
      this.layersControl.overlays[tripLayerName] = tripLayer;
      const operationLayerName = this.translate.instant('TRIP.OPERATION.MAP.OPERATIONS_LAYER');
      this.layersControl.overlays[operationLayerName] = operationLayer;

      // Center to start position
      const operationBounds = operationLayer.getBounds();
      if (operationBounds.isValid()) {
        setTimeout(() => this.map.fitBounds(operationBounds, {maxZoom: 10}));
      }

      // Refresh layer
      this.$layers.next([operationLayer, tripLayer]);
    } catch (err) {
      this.error = err && err.message || err;
    } finally {
      this.loading = false;
      this.markForCheck();
    }
  }

  async reload(): Promise<any> {
    return this.load();
  }

  protected onEachFeature(feature: Feature, layer: L.Layer) {
    layer.on('mouseover', (_) => this.zone.run(() => this.$onOverFeature.next(feature)));
    layer.on('mouseout', (_) => this.zone.run(() => this.$onOutFeature.next(feature)));
    layer.on('click', (_) => this.zone.run(() => this.onFeatureClick(feature)));
  }

  protected onFeatureClick(feature: Feature) {
    const operation = this.getOperationFromFeature(feature);
    this.viewCtrl.dismiss(operation);
  }

  protected getOperationFromFeature(feature: Feature): Operation|undefined {
    return feature && (this.operations||[]).find(ope => ope.id === feature.id) || undefined;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected getTripLayerStyle(): PathOptions {
    return {
      weight: 2,
      opacity: 0.6,
      color: 'green'
    };
  }

  protected getOperationLayerStyle(feature?: Feature): PathOptions {
    return {
      weight: 10,
      opacity: 0.8,
      color: 'blue'
    };
  }

  protected async loadDefaultsFromProgram(opts?: {emitEvent?: boolean; }) {
    if (!this._program) return; // Skip

    const program = await this.programService.loadByLabel(this._program);
    if (!program) return; //  Program not found

    // Map center
    const centerCoords = program.getPropertyAsNumbers(ProgramProperties.TRIP_MAP_CENTER);
    if (isNotEmptyArray(centerCoords) && centerCoords.length === 2) {
      try {
        this.options.center = L.latLng(centerCoords as [number, number]);
      }
      catch(err) {
        console.error(err);
      }
    }

    // Map zoom
    const zoom = program.getProperty(ProgramProperties.TRIP_MAP_ZOOM);
    if (isNotNil(zoom)) {
      this.options.zoom = +zoom;
    }

    // Emit event
    if (!opts || opts.emitEvent !== false) {
      this.markForCheck();
    }
  }

  protected cleanMapLayers() {

    // Remove all layers (except first = graticule)
    Object.getOwnPropertyNames(this.layersControl.overlays)
      .forEach((layerName, index) => {
        if (index === 0) return; // We keep the graticule layer

        const existingLayer = this.layersControl.overlays[layerName] as LayerGroup<any>;
        existingLayer.remove();
        delete this.layersControl.overlays[layerName];
      });
  }
}

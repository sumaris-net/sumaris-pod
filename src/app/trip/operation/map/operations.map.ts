import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, NgZone, OnInit} from "@angular/core";
import {BehaviorSubject, Subject} from "rxjs";
import {Operation} from "../../services/model/trip.model";
import * as L from "leaflet";
import {CRS, LayerGroup, PathOptions} from "leaflet";
import {PlatformService} from "../../../core/services/platform.service";
import {Feature, LineString} from "geojson";
import {AlertController, ModalController} from "@ionic/angular";
import {TranslateService} from "@ngx-translate/core";
import {isNil, isNotNil} from "../../../shared/functions";
import {tap, throttleTime} from "rxjs/operators";
import {AppTabForm} from "../../../core/form/tab-form.class";
import {fadeInOutAnimation} from "../../../shared/material/material.animations";
import {ActivatedRoute, Router} from "@angular/router";
import {DateFormatPipe} from "../../../shared/pipes/date-format.pipe";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {EntityUtils} from "../../../core/services/model/entity.model";

@Component({
  selector: 'app-operations-map',
  templateUrl: './operations.map.html',
  styleUrls: ['./operations.map.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationsMap extends AppTabForm<Operation[]> implements OnInit {

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
  map: L.Map;
  $layers = new BehaviorSubject<L.GeoJSON<L.Polygon>[]>(null);
  $onOverFeature = new Subject<Feature>();
  $selectedFeature = new BehaviorSubject<Feature>(null);

  get modalName(): string {
    return this.constructor.name;
  }

  @Input() operations: Operation[]

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
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, alertCtrl, translate);
    this.loading = false;
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
          tap(feature => this.openFeatureDetails(feature))
        ).subscribe());
  }

  onMapReady(leafletMap: L.Map) {
    this.map = leafletMap;
    this.zone.run(() => {
      this.start.bind(this);
    });
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  /* -- protected functions -- */

  protected async start() {
    if (!this.ready || this.loading) return; // skip

    await this.load();
  }

  protected async load() {
    if (!this.ready) return; // Skip

    this.loading = true;
    this.error = null;

    try {

      const datePattern = this.translate.instant('COMMON.DATE_TIME_PATTERN');

      const tripLayer = L.geoJSON(null, {
        style: this.getTripLayerStyle()
      });
      let tripCoordinates = [];
      const operationLayer = L.geoJSON(null, {
        onEachFeature: this.onEachFeature.bind(this),
        style: this.getOperationLayerStyle()
      });



      // Add operation to layer
      (this.operations || [])
        .sort(EntityUtils.sortComparator('rankOrderOnPeriod', 'asc'))
        .forEach((ope, index) => {
          const operationCoords = [ope.startPosition, ope.endPosition]
            .filter(pos => pos && isNotNil(pos.latitude) && isNotNil(pos.longitude))
            .map(pos => [pos.longitude, pos.latitude]);
        tripCoordinates = tripCoordinates.concat(operationCoords);

        const operationFeature = <Feature>{
          type: "Feature",
          id: ope.id,
          geometry: <LineString>{
            type: "LineString",
            coordinates: operationCoords
          },
          properties: {
            ...ope,
            // Replace date with a formatted date
            startDateTime: this.dateFormatPipe.format(ope.startDateTime, datePattern),
            // Add index
            index
          }
        }
        operationLayer.addData(operationFeature);
      });

      // Add trip feature to layer
      tripLayer.addData(<Feature>{
        type: "Feature",
        id: 'trip',
        geometry: <LineString>{
          type: "LineString",
          coordinates: tripCoordinates
        }
      });

      // Remove all layers (except first = graticule)
      Object.getOwnPropertyNames(this.layersControl.overlays)
        .forEach((layerName, index) => {
          if (index === 0) return; // Skip if graticule
          const existingLayer = this.layersControl.overlays[layerName] as LayerGroup<any>;
          existingLayer.remove();
          delete this.layersControl.overlays[layerName];
        });

      // Add new layer to layers control
      const tripLayerName = this.translate.instant('TRIP.OPERATION.MAP.TRIP_LAYER');
      this.layersControl.overlays[tripLayerName] = tripLayer;
      const operationLayerName = this.translate.instant('TRIP.OPERATION.MAP.OPERATIONS_LAYER');
      this.layersControl.overlays[operationLayerName] = operationLayer;

      // Refresh layer
      this.$layers.next([operationLayer]);
    } catch (err) {
      this.error = err && err.message || err;
    } finally {
      this.loading = false;
      this.markForCheck();
    }
  }

  protected onEachFeature(feature: Feature, layer: L.Layer) {
    layer.on('mouseover', (_) => this.zone.run(() => this.$onOverFeature.next(feature)));
    layer.on('mouseout', (_) => this.zone.run(() => this.closeFeatureDetails(feature)));
  }

  protected openFeatureDetails(feature: Feature) {
    if (this.$selectedFeature.getValue() === feature) return; // Skip

    // Emit events
    this.$selectedFeature.next(feature);
  }

  protected closeFeatureDetails(feature: Feature, force?: boolean) {
    if (this.$selectedFeature.getValue().id !== feature.id) return; // skip is not the selected feature

    // Close now, of forced (already wait 5s)
    if (force) {
      this.$selectedFeature.next(undefined); // Hide details
      return;
    }

    // Wait 3s before closing
    return setTimeout(() => this.closeFeatureDetails(feature, true), 3000);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected getTripLayerStyle(): PathOptions {
    return {
      weight: 2,
      opacity: 0.6,
      color: 'red'
    };
  }

  protected getOperationLayerStyle(): PathOptions {
    return {
      weight: 10,
      opacity: 0.8,
      color: 'red'
    };
  }
}

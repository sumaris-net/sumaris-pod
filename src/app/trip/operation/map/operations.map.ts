import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, NgZone, OnInit} from "@angular/core";
import {BehaviorSubject, Observable} from "rxjs";
import {Operation} from "../../services/model/trip.model";
import * as L from "leaflet";
import {CRS} from "leaflet";
import {PlatformService} from "../../../core/services/platform.service";
import {Feature, LineString} from "geojson";
import {ModalController} from "@ionic/angular";

@Component({
  selector: 'app-operations-map',
  templateUrl: './operations.map.html',
  styleUrls: ['./operations.map.scss'],
  //animations: [fadeInAnimation, fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationsMap  implements OnInit {

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

  loading = false;
  ready = false;
  error: string;
  options = {
    //layers: [this.sextantBaseLayer],
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

  get modalName(): string {
    return this.constructor.name;
  }

  @Input() operations: Observable<Operation[]>

  constructor(
    protected platform: PlatformService,
    protected viewCtrl: ModalController,
    protected zone: NgZone,
    protected cd: ChangeDetectorRef
  ) {

    this.platform.ready().then(() => {
      setTimeout(async () => {
        this.ready = true;
        if (!this.loading) return this.start();
      }, 500);
    });
  }

  ngOnInit() {
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

      //this.$layers.next([]);
// Add data to layer
      const line = <LineString>{
        type: "LineString",
        coordinates: [[2,51],[3,51]]
      };
      const layer = L.geoJSON(line, {
        onEachFeature: this.onEachFeature.bind(this)
      });



      //layer.addData(line);

      // Remove old data layer
      /*Object.getOwnPropertyNames(this.layersControl.overlays)
        .forEach((layerName, index) => {
          if (index === 0) return; // Keep graticule layer
          const existingLayer = this.layersControl.overlays[layerName] as LayerGroup<any>;
          existingLayer.remove();
          delete this.layersControl.overlays[layerName];
        });*/

      // Add new layer to layers control
      this.layersControl.overlays['OPE'] = layer;

      // Refresh layer
      this.$layers.next([layer]);
    }
    catch(err) {
      this.error = err && err.message || err;
    }
    finally {
      this.loading = false;
      this.markForCheck();
    }
  }

  protected onEachFeature(feature: Feature, layer: L.Layer) {
    layer.on('mouseover', (_) => this.zone.run(() => {
      //this.$onOverFeature.next(feature)
    }));
    layer.on('mouseout', (_) => this.zone.run(() => {
      //this.closeFeatureDetails(feature);
    }));
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

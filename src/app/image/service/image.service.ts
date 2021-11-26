import { Injectable } from '@angular/core';
import { ImagePicker } from '@ionic-native/image-picker/ngx';
import { MediaCapture } from '@ionic-native/media-capture/ngx';
import { PhotoViewer } from '@ionic-native/photo-viewer/ngx';
import { ActionSheetController } from '@ionic/angular';
import { PlatformService } from '@sumaris-net/ngx-components';
import { LocalSettings } from '../../../../ngx-sumaris-components/src/app/core/services/model/settings.model';
import { isNotNil } from '../../../../ngx-sumaris-components/src/app/shared/functions';
import { environment } from '@environments/environment';

const MEDIA_FOLDER_NAME = 'my_media';

@Injectable({providedIn: 'root'})
export class ImageService {

  private readonly _debug: boolean;
  private _started = false;
  private _startPromise: Promise<any>;

  constructor(
    private imagePicker: ImagePicker,
    private mediaCapture: MediaCapture,
    private file: File,
    private photoViewer: PhotoViewer,
    private actionSheetController: ActionSheetController,
    private platform: PlatformService
  ) {

    this._debug = !environment.production;
    if (this._debug) console.debug('[image] Creating service');
  }

  start(): Promise<void> {
    if (this._startPromise) return this._startPromise;
    if (this._started) return Promise.resolve();

    console.info('[image] Starting settings...');

    // Restoring local settings
    this._startPromise = this.platform.ready()
      .then(() => {
        // Do stuff
      })
      .then(data => {
        this._started = true;
        this._startPromise = undefined;
        return data;
      });
    return this._startPromise;
  }

  get started(): boolean {
    return this._started;
  }

  ready(): Promise<LocalSettings> {
    if (this._started) return Promise.resolve(this.data);
    return this.start();
  }
}

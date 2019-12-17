import {Injectable} from "@angular/core";
import {File} from '@ionic-native/file/ngx';
import {Platform} from "@ionic/angular";
import {FileTransfer, FileTransferObject} from "@ionic-native/file-transfer/ngx";
import {environment} from "../../../environments/environment";

import * as uuidv4 from "uuid/v4";
import {Base64ImageReader, Base64ImageResizeOptions} from "./base64-image-reader";
import {concatPromises} from "../observables";

@Injectable({
  providedIn: 'root',
  deps: [
    Platform, FileTransfer, File
  ]
})
export class FileService {

  private _debug = false;
  private _started = false;

  get canWriteInfileSystem(): boolean {
    return !!this.file.dataDirectory;
  }

  constructor(
    private platform: Platform,
    private transfer: FileTransfer,
    private file: File) {
    platform.ready().then(() => this.start());

    //this._debug = !environment.production;
  }

  downloadImages(sources: string[], opts?: Base64ImageResizeOptions): Promise<string[]> {
    if (!this._started) throw new Error("Platform must be started first!");

    const fileTransfer = this.file.dataDirectory && this.transfer.create() || undefined;

    const jobsFactories = (sources || []).map(source => () => this.downloadImage(source, {...opts, fileTransfer}));
    return concatPromises<string>(jobsFactories);
  }

  async downloadImage(source: string, opts?: Base64ImageResizeOptions & {
    fileTransfer?: FileTransferObject;
    }): Promise<string> {

    const debug = this._debug && (!opts || !opts.fileTransfer);
    const now = debug && Date.now();
    if (debug) console.debug(`[file] Getting image {${source}}...`);

    // Read the filename
    const lastSlashIndex = source.lastIndexOf('/');
    if (lastSlashIndex === -1) throw new Error("Invalid URL: " + source);
    const fileName = lastSlashIndex < (source.length - 1) && source.substring(lastSlashIndex + 1) || uuidv4();

    // Download into the file system
    if (this.canWriteInfileSystem) {
      const fileTransfer = (opts && opts.fileTransfer) || this.transfer.create();
      const targetFile = this.file.dataDirectory + fileName;

      await fileTransfer.download(source, targetFile)
      if (debug) console.debug(`[file] Image {${source}} downloaded in ${Date.now() - now}ms`);
      return targetFile;
    }

    // Download as data url
    else {
      const base64 = await new Base64ImageReader().readAndResizeFromUrl(source, opts);
      if (debug) console.debug(`[file] Image {${source}} converted to base64 in ${Date.now() - now}ms`);
      return base64;
    }
  }

  /* -- protected methods -- */

  protected start() {
    if (this.file.dataDirectory) {
      console.info("[file] Data directory: ", this.file.dataDirectory);
    }
    this._started = true;
  }

}

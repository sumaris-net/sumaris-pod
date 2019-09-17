import {Subject} from "rxjs";
import {Injectable} from "@angular/core";
import {Storage} from "@ionic/storage";
import {Platform} from "@ionic/angular";
import {environment} from "../../../environments/environment";


export const SEQUENCE_STORAGE_KEY = "sequence";

@Injectable({providedIn: 'root'})
export class LocalEntitiesService {

  private _started = false;
  private _sequences: { [key: string]: number } = {};

  public onStart = new Subject<void>();

  protected _debug = false;

  public constructor(
    private platform: Platform,
    private storage: Storage
  ) {

    this.platform.ready()
      .then(() => this.start());

    this._debug = !environment.production;
  }

  nextValue(entityName: string): number {
    return this._sequences[entityName] = (this._sequences[entityName] || 0) + 1;
  }

  /* -- protected methods -- */

  protected async start() {
    console.info("[local-entities] Starting...");

    // Restore sequences
    await this.restoreSequences();

    this._started = true;

    // Emit event
    this.onStart.next();
  }

  protected async stop() {

    this._started = false;

    await this.restoreSequences();
  }

  protected async restart() {
    if (this._started) await this.stop();
    await this.start();
  }

  protected async restoreSequences() {

    const sequences = await this.storage.get(SEQUENCE_STORAGE_KEY);
    if (this._debug) console.debug("[local-entities] Restored sequences: ", sequences);

    this._sequences = sequences as any;
  }

  protected async storeSequences() {
    if (this._debug) console.debug("[local-entities] Saving sequences in storage...");
    await this.storage.set(SEQUENCE_STORAGE_KEY, this._sequences);
  }
}

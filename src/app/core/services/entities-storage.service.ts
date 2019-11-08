import {merge, Subject, Subscription, timer} from "rxjs";
import {Injectable} from "@angular/core";
import {Storage} from "@ionic/storage";
import {Platform} from "@ionic/angular";
import {environment} from "../../../environments/environment";
import {throttleTime} from "rxjs/operators";


export const SEQUENCE_STORAGE_KEY = "sequence";

@Injectable({providedIn: 'root'})
export class EntityStorage {

  private _started = false;
  private _startPromise: Promise<void>;
  private _subscription = new Subscription();

  private _sequences: { [key: string]: number } = {};
  private _$save = new Subject();
  private _dirty = false;

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

  async nextValue(entityName: string): Promise<number> {

    await this.ready();

    const nextValue = (this._sequences[entityName] || 0) - 1;
    this._sequences[entityName] = nextValue;

    this._dirty = true;

    return nextValue;
  }

  async currentValue(entityName: string): Promise<number> {

    await this.ready();

    return (this._sequences[entityName] || 0);
  }

  /* -- protected methods -- */

  protected async ready() {
    if (this._started) return;
    return this.start();
  }

  protected async start() {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    console.info("[local-entities] Starting...");

    // Restore sequences
    this._startPromise = this.restoreSequences()
      .then(() => {

        this._subscription.add(
          merge(
            this._$save,
            timer(2000, 10000)
          )
          .pipe(throttleTime(10000))
          .subscribe(() => this.save())
        );

        this._started = true;
        this._startPromise = undefined;
        // Emit event
        this.onStart.next();
      });

    return this._startPromise;
  }

  protected async stop() {
    this._started = false;
    this._subscription.unsubscribe();
    this._subscription = new Subscription();

    if (this._dirty) {
      this.save();
    }
  }

  protected async restart() {
    if (this._started) await this.stop();
    await this.start();
  }

  protected async restoreSequences() {

    const sequences = await this.storage.get(SEQUENCE_STORAGE_KEY);
    if (this._debug) console.debug("[local-entities] Restored sequences: ", sequences);

    this._sequences = (sequences as any) || {};
  }

  protected async save() {
    if (this._dirty) {
      await this.storeSequences();
      this._dirty = false;
    }
  }

  protected async storeSequences(): Promise<void> {
    if (this._debug) console.debug("[local-entities] Saving sequences in storage...");
    await this.storage.set(SEQUENCE_STORAGE_KEY, this._sequences);
  }
}

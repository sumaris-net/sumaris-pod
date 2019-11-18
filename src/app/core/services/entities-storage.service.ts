import {BehaviorSubject, merge, Observable, of, Subject, Subscription, timer} from "rxjs";
import {Injectable} from "@angular/core";
import {Storage} from "@ionic/storage";
import {Platform} from "@ionic/angular";
import {environment} from "../../../environments/environment";
import {map, switchMap, tap, throttleTime} from "rxjs/operators";
import {Entity} from "./model";
import {isEmptyArray, isNil, isNilOrBlank, isNotNil} from "../../shared/functions";
import {DataStore} from "apollo-client/data/store";


export const ENTITIES_STORAGE_KEY = "entities";

export class EntityStore<T extends Entity<T>> {

  static fromEntities<T extends Entity<T>>(json: any[], opts?: {emitEvent?: boolean}): EntityStore<T> {
    const target = new EntityStore<T>();

    target._entities = (json || []).filter(item => isNotNil(item) && item.id < 0);
    target.sequence = target._entities.reduce((res, item) => Math.min(res, item.id), 0);
    target.indexById = target._entities.reduce((res, item, index) => {
      res[item.id] = index;
      return res;
    }, {});

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      target.emitEvent();
    }

    return target;
  }

  private _entities: T[];

  dirty = false;
  sequence: number;
  dataType?: () => T;
  indexById: { [key: number]: number };
  entitiesSubject = new BehaviorSubject<T[]>([]);

  get entities(): T[] {
    return this._entities;
  }

  constructor() {
    this._entities = [];
    this.sequence = 0;
    this.indexById = {};
  }

  nextValue(): number {
    this.sequence = this.sequence - 1;
    this.dirty = true;
    return this.sequence;
  }

  currentValue(): number {
    return this.sequence;
  }

  load(id: number): T {
    const index = this.indexById[id];
    if (isNil(index)) return undefined;
    return this._entities[index];
  }

  save(entity: T, opts? : {emitEvent?: boolean}): T {
    const index =  isNotNil(entity.id) ? this.indexById[entity.id] : undefined;
    const isNew = isNil(index);
    if (isNew) {
      if (isNil(entity.id)) {
        entity.id = this.nextValue();
      }
      else if (entity.id > this.sequence) {
        console.warn("Trying to save a entity with an invalid id (outside the current sequence). Will replace the input id, by sequence next value");
        entity.id = this.nextValue();
      }
      else {
        // OK: use a valid id (less than the sequence, and not exists in the index map)
      }
    }

    if (isNew) {
      this._entities.push(entity);
      this.indexById[entity.id] = this._entities.length - 1;
    }
    else {
      this._entities[index] = entity;
    }

    // Mark as dirty
    this.dirty = true;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }

    return entity;
  }

  delete(id: number, opts?: {emitEvent?: boolean}): T | undefined {
    const index = isNotNil(id) ? this.indexById[id] : undefined;
    if (isNil(index)) return undefined;

    const entity = this._entities[index];
    if (!entity) return undefined;

    this._entities[index] = undefined;
    this.indexById[entity.id] = undefined;

    // Mark as dirty
    this.dirty = true;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }

    return entity as T;
  }

  deleteMany(ids: number[], opts?: {emitEvent?: boolean}): T[] {

    // Delete by id, but NOT emit event on each deletion
    const deletedEntities = (ids || [])
      .map(id => this.delete(id, {emitEvent: false}))
      .filter(isNotNil);

    // No changes: exit
    if (!deletedEntities.length) return [];

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }

    return deletedEntities;
  }

  /* -- protected methods -- */

  /**
   * Update the entitiesSubject.
   *
   * Before wending items, a filter is apply, to remove null value.
   * This filter will create a array copy.
   */
  protected emitEvent() {
    this.entitiesSubject.next(this._entities
      // Exclude null value (can occur when entity has been deleted)
      .filter(isNotNil)
    );
  }
}

@Injectable({providedIn: 'root'})
export class EntityStorage {

  private _started = false;
  private _startPromise: Promise<void>;
  private _subscription = new Subscription();

  private _stores: { [key: string]: EntityStore<any> } = {};

  private _$save = new Subject();
  private _dirty = false;

  public onStart = new Subject<void>();

  protected _debug = false;

  get dirty(): boolean {
    return this._dirty || Object.entries(this._stores).find(([entityName, store]) => store.dirty) !== undefined;
  }

  public constructor(
    private platform: Platform,
    private storage: Storage
  ) {

    this.platform.ready()
      .then(() => this.start());

    this._debug = !environment.production;
  }

  watchAll<T extends Entity<T>>(entityName: string): Observable<T[]> {
    return of(this.ready())
      .pipe(
        switchMap(() => {
          const entityStore = this.getEntityStore<T>(entityName, {create: true});
          return entityStore.entitiesSubject.asObservable();
        })
      );
  }

  async loadAll<T extends Entity<T>>(entityName: string): Promise<T[]> {
    await this.ready();
    const entityStore = this.getEntityStore<T>(entityName, {create: false});
    if (!entityStore) return [];
    return entityStore.entities;
  }

  async load<T extends Entity<T>>(id: number, entityName: string): Promise<T> {
    await this.ready();
    const entityStore = this.getEntityStore<T>(entityName, {create: false});
    if (!entityStore) return undefined;
    return entityStore.load(id);
  }

  async nextValue(entityOrName: string | any): Promise<number> {
    await this.ready();
    this._dirty = true;
    return this.getEntityStore(this.detectEntityName(entityOrName)).nextValue();
  }

  async currentValue(entityOrName: string | any): Promise<number> {
    await this.ready();
    return this.getEntityStore(this.detectEntityName(entityOrName)).currentValue();
  }

  async save<T extends Entity<T>>(entity: T, entityName?: string): Promise<T> {
    if (!entity) return; // skip

    await this.ready();

    this._dirty = true;
    entityName = entityName || this.detectEntityName(entity);
    this.getEntityStore<T>(entityName)
      .save(entity);

    return entity;
  }

  async delete<T extends Entity<T>>(entity: T, entityName?: string): Promise<T> {
    if (!entity) return; // skip
    await this.ready();

    entityName = entityName || this.detectEntityName(entity);
    return this.deleteById(entity.id, entityName);
  }

  async deleteById<T extends Entity<T>>(id: number, entityName: string): Promise<T> {
    await this.ready();

    if (isNilOrBlank(entityName)) throw new Error('Missing entityName !');
    //if (id >= 0) throw new Error('Invalid id a local entity (not a negative number): ' + id);

    const entityStore = this.getEntityStore<T>(entityName, {create: false});
    if (!entityStore) return undefined;

    this._dirty = true;
    return entityStore.delete(id);
  }

  async deleteMany<T extends Entity<T>>(ids: number[], entityName): Promise<T[]> {
    await this.ready();

    if (isNilOrBlank(entityName)) throw new Error('Missing entityName !');

    const entityStore = this.getEntityStore<T>(entityName, {create: false});
    if (!entityStore) return undefined;

    const deletedEntities = entityStore.deleteMany(ids);
    this._dirty = this._dirty || deletedEntities.length > 0;

    return deletedEntities;
  }

  /* -- protected methods -- */

  protected getEntityStore<T extends Entity<T>>(entityName: string, opts?: {
    create?: boolean;
  }): EntityStore<T> {
    let res = this._stores[entityName];
    if (!res && (!opts || opts.create !== false)) {
      res = new EntityStore<T>();
      this._stores[entityName] = res;
    }
    return res;
  }

  protected detectEntityName(entityOrName: string | Entity<any>): string {
    if (!entityOrName) throw Error("Unable to detect entityName of object: " + entityOrName);
    if (typeof entityOrName === 'string') return entityOrName;
    if (entityOrName.__typename) {
      return entityOrName.__typename;
    }
    return entityOrName.constructor.name + 'VO';
  }

  protected async ready() {
    if (this._started) return;
    return this.start();
  }

  protected async start() {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    console.info("[local-entities] Starting...");

    // Restore sequences
    this._startPromise = this.restoreLocally()
      .then(() => {
        this._subscription.add(
          merge(
            this._$save,
            timer(2000, 10000)
          )
          .pipe(
            throttleTime(10000),
            tap(() => this.storeLocally())
          )
          .subscribe()
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

    if (this.dirty) {
      this.storeLocally();
    }
  }

  protected async restart() {
    if (this._started) await this.stop();
    await this.start();
  }

  protected async restoreLocally() {

    const entityNames = await this.storage.get(ENTITIES_STORAGE_KEY);
    if (!entityNames) return;

    if (this._debug) console.debug("[local-entities] Restoring entities from LocalStorage...");
    await Promise.all(
      entityNames.map((entityName) => {
        if (this._debug) console.debug("[local-entities] - Restoring " + entityName);
        return this.storage.get(ENTITIES_STORAGE_KEY + '#' + entityName)
          .then(entities => {
            if (entities instanceof Array) {
              this._stores[entityName] = EntityStore.fromEntities<any>(entities);
            }
          });
      })
    );
  }

  protected async storeLocally() {
    if (!this.dirty) return; // skip

    const entityNames = Object.keys(this._stores);
    this._dirty = false;

    await Promise.all(
      (entityNames.slice() /*copy, to be able to remove items in the original array*/ || [])
          .map(entityName => {
            if (this._debug) console.debug("[local-entities] Saving entities " + entityName + "...");
            const entityStore = this.getEntityStore(entityName, {create: false});
            // Save only dirty entity storage
            if (!entityStore || !entityStore.dirty) return;
            entityStore.dirty = false;

            // If no entity found
            if (isEmptyArray(entityStore.entities)) {

              // Remove from the entity names array
              entityNames.splice(entityNames.findIndex(e => e === entityName), 1);

              // Clean the local storage
              return this.storage.remove(ENTITIES_STORAGE_KEY + '#' + entityName);
            }

            // Save in the local storage
            return this.storage.set(ENTITIES_STORAGE_KEY + '#' + entityName, entityStore.entities);
          }));

    return this.storage.set(ENTITIES_STORAGE_KEY, entityNames);
  }

}

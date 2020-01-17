import {BehaviorSubject, concat, defer, merge, Observable, Subject, Subscription, timer} from "rxjs";
import {EventEmitter, Injectable} from "@angular/core";
import {Storage} from "@ionic/storage";
import {Platform} from "@ionic/angular";
import {environment} from "../../../environments/environment";
import {catchError, map, switchMap, tap, throttleTime} from "rxjs/operators";
import {Entity, EntityUtils} from "./model";
import {isEmptyArray, isNil, isNilOrBlank, isNotNil} from "../../shared/functions";
import {LoadResult} from "../../shared/services/data-service.class";


export const ENTITIES_STORAGE_KEY = "entities";

export class EntityStore<T extends Entity<T>> {

  static fromEntities<T extends Entity<T>>(json: any[], opts?: {
    name?: string;
    emitEvent?: boolean;
    onlyTemporaryId?: boolean;
  }): EntityStore<T> {
    const target = new EntityStore<T>(opts && opts.name || undefined);
    target.setEntities(json, opts);
    return target;
  }

  private _entities: T[];

  dirty = false;
  sequence: number;
  name: string;
  dataType?: () => T;
  indexById: { [key: number]: number };
  entitiesSubject = new BehaviorSubject<T[]>([]);

  get entities(): T[] {
    return this._entities;
  }

  constructor(name?: string) {
    this.name = name;
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
      else if (entity.id < 0 && entity.id > this.sequence) {
        console.warn("Trying to save a local entity with an id > sequence. Will replace id by sequence next value");
        entity.id = this.nextValue();
      }
      else {
        // OK: use a valid id :
        // - id >= 0 (should be a remote id)
        // - OR < sequence, and not exists in the index map
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

  saveAll(entities: T[], opts? : {emitEvent?: boolean}): T[] {
    if (isEmptyArray(entities)) return entities; // Nothing to save

    let result: T[];

    // First save
    if (isEmptyArray(this._entities)) {
      this.setEntities(entities);
    }
    else {
      result = entities.map((entity) => this.save(entity, {emitEvent: false}));
    }

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }

    return result || this.entities;
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

  protected setEntities(entities: T[], opts?: {emitEvent?: boolean, filterTemporary?: boolean;}) {
    this._entities = entities && (
      opts && opts.filterTemporary
        // Filter NOT nil and only local id
        ? entities.filter(item => isNotNil(item) && item.id < 0)
        // Filter NOT nil
        : entities.filter(isNotNil)) || [];

    // Update the sequence with min(id) of all temporary ids
    this.sequence = this._entities.reduce((res, item) => item.id < 0 ? Math.min(res, item.id) : res, 0);

    this.indexById = this._entities.reduce((res, item, index) => {
      res[item.id] = index;
      return res;
    }, {});

    this.dirty = false;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }
  }

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

  private _$save = new EventEmitter(true);
  private _dirty = false;
  private _saving = false;

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

  watchAll<T extends Entity<T>>(entityName: string,
                                opts?: {
                                  offset?: number;
                                  size?: number;
                                  sortBy?: string;
                                  sortDirection?: string;
                                  filter?: (T) => boolean;
                                }): Observable<LoadResult<T>> {
    // Make sure store is ready
    if (!this._started) {
      return defer(() => this.ready())
        .pipe(switchMap(() => this.watchAll<T>(entityName, opts))); // Loop
    }

    const entityStore = this.getEntityStore<T>(entityName, {create: true});
    return entityStore.entitiesSubject
      .asObservable()
      .pipe(
        map(data => this.reduceAndSort(data, opts))
      );
  }

  async loadAll<T extends Entity<T>>(entityName: string,
                                     opts?: {
                                       offset?: number;
                                       size?: number;
                                       sortBy?: string;
                                       sortDirection?: string;
                                       filter?: (T) => boolean;
                                    }): Promise<LoadResult<T>> {

    // Make sure store is ready
    if (!this._started) await this.ready();

    if (this._debug) console.debug(`[entity-store] Loading ${entityName}...`);

    const entityStore = this.getEntityStore<T>(entityName, {create: false});
    if (!entityStore) return {data: [], total: 0}; // No store for this entity name

    return this.reduceAndSort(entityStore.entities, opts);
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

  async nextValues(entityOrName: string | any, entityCount: number): Promise<number> {
    await this.ready();
    this._dirty = true;
    const entityName = this.detectEntityName(entityOrName);
    const store = this.getEntityStore(entityName);
    const firstValue = store.nextValue();
    for (let i = 0; i < entityCount - 1; i++) {
      store.nextValue();
    }
    if (this._debug) console.debug(`[local-entities] Reserving range [${firstValue},${store.currentValue()}] for ${entityName}'s sequence`);
    return firstValue;
  }

  async currentValue(entityOrName: string | any): Promise<number> {
    await this.ready();
    return this.getEntityStore(this.detectEntityName(entityOrName)).currentValue();
  }

  async save<T extends Entity<T>>(entity: T, opts?: {
    entityName?: string;
    emitEvent?: boolean;
  }): Promise<T> {
    if (!entity) return; // skip

    await this.ready();

    this._dirty = true;
    const entityName = opts && opts.entityName || this.detectEntityName(entity);
    this.getEntityStore<T>(entityName)
      .save(entity, opts);

    // Ask to save
    this._$save.emit();

    return entity;
  }

  async saveAll<T extends Entity<T>>(entities: T[], opts?: {
    entityName?: string;
    emitEvent?: boolean;
  }): Promise<T[]> {
    if (isEmptyArray(entities)) return; // skip

    await this.ready();

    this._dirty = true;
    const entityName = opts && opts.entityName || this.detectEntityName(entities[0]);
    return this.getEntityStore<T>(entityName)
      .saveAll(entities, opts);
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

    // Ask to save
    this._$save.emit();

    return entityStore.delete(id);
  }

  async deleteMany<T extends Entity<T>>(ids: number[], entityName): Promise<T[]> {
    await this.ready();

    if (isNilOrBlank(entityName)) throw new Error('Missing entityName !');

    const entityStore = this.getEntityStore<T>(entityName, {create: false});
    if (!entityStore) return undefined;

    const deletedEntities = entityStore.deleteMany(ids);
    this._dirty = this._dirty || deletedEntities.length > 0;

    // Ask to save
    if (this._dirty) this._$save.emit();

    return deletedEntities;
  }

  persist(): Promise<void> {
    if (this._dirty) {
      return this.storeLocally();
    }
    return Promise.resolve();
  }

  /* -- protected methods -- */

  /**
   * WIll apply a filter, then a sort, then a page slice
   * @param data
   * @param opts
   */
  protected reduceAndSort<T extends Entity<T>>(data: T[],
                                               opts?: {
                              offset?: number;
                              size?: number;
                              sortBy?: string;
                              sortDirection?: string;
                              filter?: (T) => boolean;
                            }): LoadResult<T> {

    if (!opts || !data.length) return {data, total: data.length};

    // Apply the filter, if any
    if (opts.filter) {
      data = data.filter(isNotNil).filter(opts.filter);
    }

    // Compute the total length
    const total = data.length;

    // If page size<=0 (e.g. only need total)
    if (opts.size && opts.size < 0 || opts.size === 0) return {data: [], total};

    // Sort by
    if (data.length && opts.sortBy) {
      EntityUtils.sort(data, opts.sortBy, opts.sortDirection);
    }

    // Slice in a page (using offset and size)
    if (opts.offset > 0) {

      // Offset after the end: no result
      if (opts.offset >= data.length || opts.size === 0) {
        data = [];
      }
      else {
        data = (opts.size && ((opts.offset + opts.size) < data.length)) ?
          // Slice using limit to size
          data.slice(opts.offset, (opts.offset + opts.size) - 1) :
          // Slice without limit
          data.slice(opts.offset);
      }
    }
    else if (opts.size > 0){
      data = data.slice(0, opts.size - 1);
    } else if (opts.size === 0){
      data = [];
    }

    return {data, total};
  }

  protected getEntityStore<T extends Entity<T>>(entityName: string, opts?: {
    create?: boolean;
  }): EntityStore<T> {
    let res = this._stores[entityName];
    if (!res && (!opts || opts.create !== false)) {
      res = new EntityStore<T>(entityName);
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

    const now = Date.now();
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

        console.info(`[local-entities] Starting [OK] in ${Date.now() - now}ms`);
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

    const now = this._debug && Date.now();
    if (this._debug) console.info("[local-entities] Restoring entities...");
    let entitiesCount = 0;
    await Promise.all(
      entityNames.map((entityName) => {
        return this.storage.get(ENTITIES_STORAGE_KEY + '#' + entityName)
          .then(entities => {
            // If there is something to restore
            if (entities instanceof Array && entities.length) {
              entitiesCount += entities.length;
              //if (this._debug) console.debug(`[local-entities] - Restoring ${entities.length} ${entityName}...`);

              // Create a entity store, with all given entities
              this._stores[entityName] = EntityStore.fromEntities<any>(entities, {
                name: entityName
              });
            }
          });
      })
    );
    if (this._debug) console.debug(`[local-entities] Restoring entities [OK] ${entitiesCount} entities found in ${Date.now() - now}ms`);
  }

  protected storeLocally(): Promise<any> {
    if (!this.dirty || this._saving) return Promise.resolve(); // skip

    this._saving = true;
    this._dirty = false;
    const entityNames = Object.keys(this._stores) || [];

    const now = Date.now();
    if (this._debug) console.debug("[local-entities] Saving to local storage...");

    let currentEntityName;
    return concat(
      ...(entityNames.slice()) // copy to enable changes in the original array (e.g. remove an item)
        .map(entityName => {
          return defer(() => {
            currentEntityName = entityName;
            const entityStore = this.getEntityStore(entityName, {create: false});

            if (!entityStore || !entityStore.dirty) return; // Skip is not dirty

            return this.persistEntityStore<any>(entityStore)
              .then(count => {
                // If no entity found, remove from the entity names array
                if (!count) {
                  entityNames.splice(entityNames.findIndex(e => e === entityName), 1);
                }
              });
          });
        }),
      defer(() =>  {
        currentEntityName = undefined;
        return isEmptyArray(entityNames) ?
          this.storage.remove(ENTITIES_STORAGE_KEY) :
          this.storage.set(ENTITIES_STORAGE_KEY, entityNames);
      }),
      defer(() =>  {
        if (this._debug) console.debug(`[local-entities] Entities saved in local storage, in ${Date.now() - now}ms...`);
        this._saving = false;
      })
    )
      .pipe(
        catchError(err => {
          this._saving = false;
          if (currentEntityName) {
            console.error(`[local-entities] Error while saving entities ${currentEntityName}`, err);
          }
          else {
            console.error("[local-entities] Error while saving entities: " + (err && err.message || err), err);
          }
          return err;
        })
      ).toPromise();
  }

  protected persistEntityStore<T extends Entity<T>>(entityStore: EntityStore<T>): Promise<number> {
    // Save only dirty entity storage
    entityStore.dirty = false;
    const entities = entityStore.entities.slice(); // Copy it!
    if (this._debug) console.debug(`[local-entities] Saving ${entities.length} ${entityStore.name}(s)...`);

    // If no entity found
    let promise;
    if (isEmptyArray(entities)) {

      // Clean the local storage
      promise = this.storage.remove(ENTITIES_STORAGE_KEY + '#' + entityStore.name);
    }
    else {
      // Save in the local storage
      promise = this.storage.set(ENTITIES_STORAGE_KEY + '#' + entityStore.name, entities);
    }

    return promise.then(() => entities.length);
  }

}

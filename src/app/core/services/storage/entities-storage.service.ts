import {concat, defer, merge, Observable, Subject, Subscription, timer} from "rxjs";
import {EventEmitter, Inject, Injectable, InjectionToken, Optional} from "@angular/core";
import {Storage} from "@ionic/storage";
import {Platform} from "@ionic/angular";
import {environment} from "../../../../environments/environment";
import {catchError, switchMap, throttleTime} from "rxjs/operators";
import {Entity} from "../model/entity.model";
import {isEmptyArray, isNilOrBlank} from "../../../shared/functions";
import {LoadResult} from "../../../shared/services/entity-service.class";
import {ENTITIES_STORAGE_KEY, EntityStorageLoadOptions, EntityStore, EntityStoreTypePolicy} from "./entity-store.class";


export declare type EntitiesStorageTypePolicies = {[__typename: string]: EntityStoreTypePolicy<any> };


export const APP_LOCAL_STORAGE_TYPE_POLICIES = new InjectionToken<EntitiesStorageTypePolicies>('localStorageTypePolicies');

@Injectable({providedIn: 'root'})
export class EntitiesStorage
  // TODO: implements EntityService<T, EntitiesStorageLoadOption>
  {

  public static TRASH_PREFIX = "Trash#";

  private readonly _debug: boolean;
  private readonly _typePolicies: EntitiesStorageTypePolicies;
  private _started = false;
  private _startPromise: Promise<void>;
  private _subscription = new Subscription();

  private _stores: { [key: string]: EntityStore<any> } = {};

  private _$save = new EventEmitter(true);
  private _dirty = false;
  private _saving = false;


  onStart = new Subject<void>();

  get dirty(): boolean {
    return this._dirty || Object.entries(this._stores).find(([_, store]) => store.dirty) !== undefined;
  }

  public constructor(
    private platform: Platform,
    private storage: Storage,
    @Optional() @Inject(APP_LOCAL_STORAGE_TYPE_POLICIES) typePolicies: EntitiesStorageTypePolicies
  ) {
    this._typePolicies = typePolicies || {};

    // For DEV only
    this._debug = !environment.production;
    if (this._debug) console.debug('[entity-storage] Creating entity storage');
  }

  watchAll<T extends Entity<T>>(entityName: string,
                                variables: {
                                  offset?: number;
                                  size?: number;
                                  sortBy?: string;
                                  sortDirection?: string;
                                  trash?: boolean;
                                  filter?: (T) => boolean;
                                },
                                opts?: EntityStorageLoadOptions): Observable<LoadResult<T>> {
    // Make sure store is ready
    if (!this._started) {
      return defer(() => this.ready())
        .pipe(switchMap(() => this.watchAll<T>(entityName, variables, opts))); // Loop
    }

    const storeName = variables && variables.trash ? (EntitiesStorage.TRASH_PREFIX + entityName) : entityName;
    return this.getEntityStore<T>(storeName, {create: true})
      .watchAll(variables, opts);
  }

  async loadAll<T extends Entity<T>>(entityName: string,
                                     variables?: {
                                       offset?: number;
                                       size?: number;
                                       sortBy?: string;
                                       sortDirection?: string;
                                       filter?: (T) => boolean;
                                     },
                                     opts?: EntityStorageLoadOptions): Promise<LoadResult<T>> {

    // Make sure store is ready
    if (!this._started) await this.ready();

    if (this._debug) console.debug(`[entity-storage] Loading ${entityName}...`);

    const entityStore = this.getEntityStore<T>(entityName, {create: false});
    if (!entityStore) return {data: [], total: 0}; // No store for this entity name

    return entityStore.loadAll(variables, opts);
  }

  async load<T extends Entity<T>>(id: number, entityName: string, opts?: EntityStorageLoadOptions): Promise<T> {
    await this.ready();
    const entityStore = this.getEntityStore<T>(entityName, {create: false});
    if (!entityStore) return undefined;
    return entityStore.load(id, opts);
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
    if (this._debug) console.debug(`[entity-storage] Reserving range [${firstValue},${store.currentValue()}] for ${entityName}'s sequence`);
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
    let storeName = opts && opts.entityName || this.detectEntityName(entity);
    this.getEntityStore<T>(storeName)
      .save(entity, opts);

    // Ask to save
    this._$save.emit();

    return entity;
  }

  async saveAll<T extends Entity<T>>(entities: T[], opts?: {
    entityName?: string;
    reset?: boolean;
    emitEvent?: boolean;
  }): Promise<T[]> {
    if (isEmptyArray(entities)) return entities; // skip

    await this.ready();

    this._dirty = true;
    const entityName = opts && opts.entityName || this.detectEntityName(entities[0]);
    return this.getEntityStore<T>(entityName).saveAll(entities, opts);
  }

  async delete<T extends Entity<T>>(entity: T, opts?: {
    entityName?: string;
  }): Promise<T> {
    if (!entity) return undefined; // skip

    await this.ready();

    return this.deleteById(entity.id, {
      ...opts,
      entityName: opts && opts.entityName || this.detectEntityName(entity)
    });
  }

  async deleteById<T extends Entity<T>>(id: number, opts: {
    entityName: string;
    emitEvent?: boolean;
  }): Promise<T> {
    await this.ready();

    if (!opts || isNilOrBlank(opts.entityName)) throw new Error("Missing argument 'opts' or 'entityName'");
    //if (id >= 0) throw new Error('Invalid id a local entity (not a negative number): ' + id);

    const entityStore = this.getEntityStore<T>(opts.entityName, {create: false});
    if (!entityStore) return undefined;

    const deletedEntity = entityStore.delete(id, opts);

    // If something deleted
    if (deletedEntity) {

      // Mark as dirty
      this._dirty = true;

      // Ask to save
      this._$save.emit();
    }

    return deletedEntity;
  }

  async deleteMany<T extends Entity<T>>(ids: number[], opts: {
    entityName: string;
    emitEvent?: boolean;
  }): Promise<T[]> {
    await this.ready();

    if (!opts || isNilOrBlank(opts.entityName)) throw new Error("Missing argument 'opts' or 'opts.entityName'");

    const entityStore = this.getEntityStore<T>(opts.entityName, {create: false});
    if (!entityStore) return undefined;

    // Do deletion
    const deletedEntities = entityStore.deleteMany(ids, opts);

    // If something deleted
    if (deletedEntities.length > 0) {

      // Mark as dirty
      this._dirty = true;

      // Mark as save need
      this._$save.emit();

    }

    return deletedEntities;
  }

  async deleteFromTrash<T extends Entity<T>>(entity: T, opts?: {
    entityName?: string;
  }): Promise<T> {
    if (!entity) return undefined; // skip

    await this.ready();

    return this.deleteFromTrashById(entity.id, {
      ...opts,
      entityName: opts && opts.entityName || this.detectEntityName(entity)
    });
  }

  async deleteFromTrashById<T extends Entity<T>>(id: number, opts: {
    entityName: string;
    emitEvent?: boolean;
  }): Promise<T> {
    await this.ready();

    if (!opts || isNilOrBlank(opts.entityName)) throw new Error("Missing argument 'opts' or 'entityName'");

    return this.deleteById(id, {
      ...opts,
      entityName: EntitiesStorage.TRASH_PREFIX + opts.entityName
    });
  }

  async moveToTrash<T extends Entity<T>>(entity: T, opts?: {
    entityName?: string;
    emitEvent?: boolean;
  }): Promise<T> {
    if (!entity) return undefined; // skip

    await this.ready();

    const entityName = opts && opts.entityName || this.detectEntityName(entity);

    // Delete entity by id, if exists
    const entityStore = this.getEntityStore<T>(entityName);
    if (entityStore) entityStore.delete(entity.id, opts);

    // Clean id (a new id will be set by the trash store)
    entity.id = undefined;

    // Add to trash
    const trashName = EntitiesStorage.TRASH_PREFIX + entityName;
    this.getEntityStore<T>(trashName).save(entity, opts);

    this._$save.emit();

    return entity;
  }

  async moveManyToTrash<T extends Entity<T>>(ids: number[], opts: {
    entityName: string;
    emitEvent?: boolean;
  }): Promise<T[]> {
    await this.ready();

    if (!opts || isNilOrBlank(opts.entityName)) throw new Error("Missing argument 'opts.entityName'");

    const entityStore = this.getEntityStore<T>(opts.entityName, {create: false});
    if (!entityStore) return undefined;

    // Do deletion
    const deletedEntities = entityStore.deleteMany(ids, opts);

    // If something deleted
    if (deletedEntities.length > 0) {

      // Clean ids (new ids will be set by the trash store)
      deletedEntities.forEach(e => e.id = undefined);

      const trashName = EntitiesStorage.TRASH_PREFIX + opts.entityName;
      this.getEntityStore<T>(trashName, {create: true})
        .saveAll(deletedEntities, opts);

      // Mark as dirty
      this._dirty = true;

      // Mark as save need
      this._$save.emit();
    }

    return deletedEntities;
  }

  async saveToTrash<T extends Entity<T>>(entity: T, opts?: {
    entityName?: string;
    emitEvent?: boolean;
  }): Promise<T> {
    if (!entity) return undefined; // skip

    await this.ready();

    const entityName = opts && opts.entityName || this.detectEntityName(entity);
    const trashName = EntitiesStorage.TRASH_PREFIX + entityName;
    this.getEntityStore<T>(trashName).save(entity, opts);

    this._$save.emit();

    return entity;
  }

  async clearTrash(entityName: string) {
    await this.ready();

    const trashName = EntitiesStorage.TRASH_PREFIX + entityName;
    const entityStore = this.getEntityStore(trashName, {create: false});
    if (!entityStore) return; // Skip

    entityStore.reset();
    this._dirty = true;

    this._$save.emit();
  }

  persist(): Promise<void> {
    if (this._dirty) {
      return this.storeLocally();
    }
    return Promise.resolve();
  }

  ready(): Promise<void> {
    if (this._started) return Promise.resolve();
    return this.start();
  }

  start(): Promise<void> {
    if (this._startPromise) return this._startPromise;
    if (this._started) return Promise.resolve();

    const now = Date.now();
    console.info(`[entity-storage] Starting entity storage...`);

    // Restore sequences
    this._startPromise = this.restoreLocally()
      .then(() => {
        // Start a save timer
        this._subscription.add(
          merge(
            this._$save,
            timer(2000, 10000)
          )
          .pipe(
            throttleTime(10000)
          )
          .subscribe(() => this.storeLocally())
        );

        this._started = true;
        this._startPromise = undefined;

        console.info(`[entity-storage] Starting [OK] in ${Date.now() - now}ms`);

        // Emit event
        this.onStart.next();
      });

    return this._startPromise;
  }

  async stop() {
    this._started = false;
    this._subscription.unsubscribe();
    this._subscription = new Subscription();

    if (this.dirty) {
      await this.storeLocally();
    }
  }

  async restart() {
    if (this._started) await this.stop();
    await this.start();
  }

  /* -- protected methods -- */

  protected getEntityStore<T extends Entity<T>>(name: string, opts?: {
    create?: boolean;
  }): EntityStore<T> {
    let store = this._stores[name];
    if (!store && (!opts || opts.create !== false)) {
      if (this._debug) console.debug(`[entity-storage] Creating store ${name}`);
      const typePolicy = this._typePolicies[name];
      store = new EntityStore<T>(name, this.storage, typePolicy);
      this._stores[name] = store;
    }
    return store;
  }

  protected detectEntityName(entityOrName: string | Entity<any>): string {
    if (!entityOrName) throw Error("Unable to detect entityName of object: " + entityOrName);
    if (typeof entityOrName === 'string') return entityOrName;
    if (entityOrName.__typename) {
      return entityOrName.__typename;
    }
    return entityOrName.constructor.name + 'VO';
  }

  protected async restoreLocally() {

    const entityNames = await this.storage.get(ENTITIES_STORAGE_KEY);
    if (!entityNames) return;

    const now = this._debug && Date.now();
    if (this._debug) console.info("[entity-storage] Restoring entities...");
    let entitiesCount = (await Promise.all<number>(
      entityNames
        .map(name => this.getEntityStore<any>(name))
        .map((store: EntityStore<any>) => store.restore()))
    ).reduce((res, count) => (res + count), 0);

    if (this._debug) console.debug(`[entity-storage] Restoring entities [OK] ${entitiesCount} entities found in ${Date.now() - now}ms`);
  }

  protected async storeLocally(): Promise<any> {
    if (!this.dirty) return; // skip

    // Saving progress: report this save later
    if (this._saving) {
      if (this._debug) console.debug("[entity-storage] Previous persist not finished. Waiting...");
      this._$save.emit();
      return;
    }

    this._saving = true;
    this._dirty = false;
    const entityNames = this._stores && Object.keys(this._stores) || [];

    const now = Date.now();
    if (this._debug) console.debug("[entity-storage] Persisting...");

    let currentEntityName;
    return concat(
      ...entityNames.map(entityName => {
          return defer(() => {
            currentEntityName = entityName;
            const entityStore = this.getEntityStore(entityName, {create: false});

            if (!entityStore) {
              console.warn(`[entity-storage] Persisting ${entityName}: store not found!`);
              return;
            }
            return entityStore.persist()
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
          if (this._debug) console.debug(`[entity-storage] Persisting [OK] ${entityNames.length} stores saved in ${Date.now() - now}ms...`);
          this._saving = false;
        })
      )
      .pipe(
        catchError(err => {
          this._saving = false;
          if (currentEntityName) {
            console.error(`[entity-storage] Error while persisting ${currentEntityName}`, err);
          }
          else {
            console.error(`[entity-storage] Error while persisting: ${err && err.message || err}`, err);
          }
          return err;
        })
      ).toPromise();
  }

}

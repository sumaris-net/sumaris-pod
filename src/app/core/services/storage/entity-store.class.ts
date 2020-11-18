import {BehaviorSubject, Observable} from "rxjs";
import {Storage} from "@ionic/storage";
import {map} from "rxjs/operators";
import {Entity, EntityUtils} from "../model/entity.model";
import {isEmptyArray, isNil, isNotNil} from "../../../shared/functions";
import {LoadResult} from "../../../shared/services/entity-service.class";
import {concatPromises} from "../../../shared/observables";

export const ENTITIES_STORAGE_KEY = "entities";

export interface EntityStoreOptions<T extends Entity<T>>  {
  storeById?: boolean; // false by efault
  onlyLocalEntities?: boolean;
  detailedAttributes?: (keyof T)[];
}

declare type EntityStatusMap = { [key: number]: {index: number; dirty?: boolean; } };

export class EntityStore<T extends Entity<T>> {

  private readonly _storageKey: string;
  private readonly _onChange = new BehaviorSubject(true);
  private readonly _mapToLightEntity: (T) => T;
  private _entities: T[];
  private _statusById: EntityStatusMap;
  private _sequence: number;
  private _dirty = false;
  private _loaded: boolean;

  readonly options: EntityStoreOptions<T>;

  get entities(): T[] {
    return this._entities;
  }

  get dirty(): boolean {
    return this._dirty;
  }

  load: (id: number) => Promise<T>;

  constructor(
    protected readonly name: string,
    protected readonly storage: Storage,
    opts?: EntityStoreOptions<T>) {
    this.options = opts || {};
    this.reset({emitEvent: false});
    this._dirty = false;
    this._loaded = false;
    this._storageKey = ENTITIES_STORAGE_KEY + '#' + name;
    this._mapToLightEntity = this.options.storeById && this.createLightEntityMapFn(this.options.detailedAttributes);
    this.load = this._mapToLightEntity ? this.loadFull : this.loadLight;
  }

  nextValue(): number {
    this._sequence = this._sequence - 1;
    this._dirty = true;
    return this._sequence;
  }

  currentValue(): number {
    return this._sequence;
  }

  watchAll(opts?: {
            offset?: number;
            size?: number;
            sortBy?: string;
            sortDirection?: string;
            filter?: (T) => boolean;
          }): Observable<LoadResult<T>> {

    return this._onChange
      .pipe(
        map((_) => this.reduceAndSort(this._entities, opts))
      );
  }

  /**
   * WIll apply a filter, then a sort, then a page slice
   * @param data
   * @param opts
   */
  async loadAll(opts?: {
    offset?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
    filter?: (T) => boolean;
  }): Promise<LoadResult<T>> {

    return this.reduceAndSort(this._entities, opts);
  }

  save(entity: T, opts? : {emitEvent?: boolean}): T {
    let status = isNotNil(entity.id) && this._statusById[+entity.id] || {index: undefined};
    const isNew = isNil(status.index);
    if (isNew) {
      if (isNil(entity.id)) {
        entity.id = this.nextValue();
      }
      else if (entity.id < 0 && entity.id > this._sequence) {
        console.warn("Trying to save a local entity with an id > sequence. Will replace id by sequence next value");
        entity.id = this.nextValue();
      }
      else {
        // OK: use a valid id :
        // - id >= 0 (should be a remote id)
        // - OR < sequence, and not exists in the index map
      }

      this._statusById[+entity.id] = status;
      status.index = this._entities.push(entity) - 1;
    }
    else {
      this._entities[status.index] = entity;
    }

    // Mark entity as dirty
    status.dirty = true;

    // Mark storage as dirty
    this._dirty = true;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }

    return entity;
  }

  saveAll(entities: T[], opts?: { emitEvent?: boolean; reset?: boolean; }): T[] {
    if (isEmptyArray(entities)) return entities; // Nothing to save

    let result: T[];

    console.info(`[entity-storage] Saving ${entities.length} ${this.name}(s)`);

    // First save
    if (isEmptyArray(this._entities) || (opts && opts.reset)) {
      this.setEntities(entities, {emitEvent: false});
    }
    else {
      result = entities.map((entity) => this.save(entity, {emitEvent: false}));
    }

    // Mark as dirty
    this._dirty = true;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }

    return result || this._entities.slice();
  }

  delete(id: number, opts?: { emitEvent?: boolean; }): T | undefined {
    const status = isNotNil(id) ? this._statusById[+id] : undefined;
    const index = status ? status.index : undefined;
    if (isNil(index)) return undefined;

    const entity = this._entities[index];
    if (!entity) return undefined;

    this._entities[index] = undefined;
    this._statusById[+entity.id] = undefined;

    // Mark as dirty
    this._dirty = true;

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

  reset(opts? : {emitEvent?: boolean}) {
    this._entities = [];
    this._sequence = 0;
    this._statusById = {};

    this._dirty = true;
    this._loaded = true;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }
  }

  async persist(): Promise<number> {
    // Skip is not dirty
    if (!this._dirty) {
      console.debug(`[entity-storage] Persisting ${this.name} not need. Skip`);
      return this._entities.length;
    }

    // Copy some data, BEFORE to call markAsPristine() to allow parallel changes
    const dirtyIndexes = Object.values(this._statusById)
      .filter(s => s && s.dirty)
      .map(s => s.index);
    const entities = this._entities.slice();

    // Mark all (entities and status) as pristine
    this.markAsPristine({emitEvent: false});

    // Map dirty entities to light entities (AFTER the previous copy)
    if (this._mapToLightEntity) {
      dirtyIndexes.forEach(index => this._entities[index] = this._mapToLightEntity(this._entities[index]));
    }

    // If no entity found
    if (isEmptyArray(entities)) {

      // Clean the local storage
      await this.storage.remove(this._storageKey);
    }
    else {

      // Save each entity into a unique key (advanced mode)
      if (this.options.storeById) {
        // Save ids
        await this.storage.set(this._storageKey + "#ids", entities.map(e => e.id));

        // Saved dirty entities
        await Promise.all(
          dirtyIndexes
            .map(index => {
              const entity = entities[index];
              console.info(`[entity-storage] Persisting ${this.name}#${entity.id}...`);
              return this.storage.set(this._storageKey + "#" + entity.id, entity);
            }));

        console.info(`[entity-storage] Persisting ${dirtyIndexes.length}/${entities.length} ${this.name}(s)...`);
      }

      // Save all entities in a single key (default)
      else {

        console.info(`[entity-storage] Persisting ${entities.length} ${this.name}(s)...`);
        await this.storage.set(this._storageKey, entities);
      }
    }

    return entities.length;
  }

  async restore(opts?: { emitEvent?: boolean; }): Promise<number> {
    let entities: T[];

    if (this.options.storeById) {
      const ids = await this.storage.get(this._storageKey+"#ids");
      if (isNotNil(ids)) {
        // Load entity by id (one by one)
        entities = await concatPromises<T>(
          ids.map(id =>() => this.storage.get(this._storageKey + "#" + id)
            .then(entity => this._mapToLightEntity ? this._mapToLightEntity(entity) : entity))
        );
      }
    }

    if (isNil(entities)) {
      const values = await this.storage.get(this._storageKey);
      // OK, there is something in storage...
      entities = values && values instanceof Array ? values : [];

      if (entities && entities.length >= 1000) {
        console.warn(`[entity-storage] - Restoring ${entities.length} ${this.name}...`);
      }

      // Map entities
      if (this._mapToLightEntity) {
        entities = entities.map(this._mapToLightEntity);
      }
    }

    this.setEntities(entities || [], {...opts, emitEvent: false });
    this._dirty = false;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }

    return this._entities.length;
  }

  markAsDirty(opts?: { emitEvent?: boolean; }) {
    this._dirty = true;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }
  }

  markAsPristine(opts?: { emitEvent?: boolean; }) {

    Object.values(this._statusById)
      .filter(isNotNil)
      .forEach(s => s.dirty = false);
    this._dirty = false;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }
  }

  /* -- protected methods -- */

  private async loadLight(id: number): Promise<T> {
    const status = this._statusById[+id];
    const index = status && status.index;
    if (isNil(index)) return undefined; // not exists
    return this._entities[index];
  }

  private async loadFull(id: number): Promise<T> {
    const status = this._statusById[+id];
    const index = status && status.index;
    if (isNil(index)) return undefined; // not exists

    // Reload from storage, if need (e.g. some attributes has been excluded in light elements)
    if (status.dirty === false) {
      console.debug(`[entity-storage] Full reloading ${this.name}#${id}...`);
      return await this.storage.get(this._storageKey + '#' + id);
    }

    return this._entities[index];
  }

  private setEntities(entities: T[], opts?: {emitEvent?: boolean; }) {
    entities = entities && (
      (this.options && this.options.onlyLocalEntities)
        // Filter NOT nil AND local id
        ? entities.filter(item => isNotNil(item) && item.id < 0)
        // Filter NOT nil
        : entities.filter(isNotNil)) || [];

    this._entities = entities;

    // Update the sequence with min(id) of all temporary ids
    this._sequence = this._entities.reduce((res, item) => item.id < 0 ? Math.min(res, item.id) : res, 0);

    this._statusById = this._entities.reduce((res, item, index) => {
      res[item.id] = {
        index,
        dirty: false
      };
      return res;
    }, {});

    this._loaded = true;

    // Emit update event
    if (!opts || opts.emitEvent !== false) {
      this.emitEvent();
    }
  }


  /**
   * WIll apply a filter, then a sort, then a page slice
   * @param data
   * @param opts
   */
  private reduceAndSort(data: T[],
                          opts?: {
                            offset?: number;
                            size?: number;
                            sortBy?: string;
                            sortDirection?: string;
                            filter?: (T) => boolean;
                          }): LoadResult<T> {
    if (!data || !data.length) {
      return {data: [], total: 0};
    }

    // Remove nil values
    data = data.filter(isNotNil);

    if (!opts) {
      // Return all (but copy and filter array)
      return {data, total: data.length};
    }

    // Apply the filter, if any
    if (opts.filter) {
      data = data.filter(opts.filter);
    }

    // Compute the total length
    const total = data.length;

    // If page size=0 (e.g. only need total)
    if (opts.size === 0) return {data: [], total};

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
        data = (opts.size > 0 && ((opts.offset + opts.size) < data.length)) ?
          // Slice using limit to size
          data.slice(opts.offset, (opts.offset + opts.size) - 1) :
          // Slice without limit
          data.slice(opts.offset);
      }
    }
    else if (opts.size > 0){
      data = data.slice(0, opts.size - 1);
    } else if (opts.size < 0){
      // Force to keep all data
    }

    return {data, total};
  }

  private createLightEntityMapFn(excludedAttributes: (keyof T)[]): (T) => T {
    if (isEmptyArray(excludedAttributes)) return undefined; // skip

    // Create a immutable mask object, use to clean some properties
    const excludeAttributesMask = Object.freeze(this.options.detailedAttributes.reduce((res, attr) => {
      res[attr] = undefined;
      return res;
    }, <T>{}));

    return (entity) => entity ? Object.assign(<T>{}, entity, excludeAttributesMask) : undefined;
  }

  /**
   * Update the entitiesSubject.
   *
   * Before wending items, a filter is apply, to remove null value.
   * This filter will create a array copy.
   */
  private emitEvent() {
    this._onChange.next(true);
  }
}

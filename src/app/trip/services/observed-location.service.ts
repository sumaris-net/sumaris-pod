import {Injectable, Injector} from "@angular/core";
import {
  EntitiesServiceWatchOptions,
  EntityServiceLoadOptions,
  FilterFn,
  IEntitiesService,
  IEntityService,
  LoadResult
} from "../../shared/services/entity-service.class";
import {AccountService} from "../../core/services/account.service";
import {Observable} from "rxjs";
import {Moment} from "moment";
import {gql} from "@apollo/client/core";
import {Fragments} from "./trip.queries";
import {ErrorCodes} from "./trip.errors";
import {filter, map} from "rxjs/operators";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {
  DataEntityAsObjectOptions,
  SAVE_AS_OBJECT_OPTIONS,
  SAVE_LOCALLY_AS_OBJECT_OPTIONS
} from "../../data/services/model/data-entity.model";
import {FormErrors} from "../../core/form/form.utils";
import {ObservedLocation} from "./model/observed-location.model";
import {
  Beans,
  fromDateISOString,
  isEmptyArray,
  isNil,
  isNotEmptyArray,
  isNotNil,
  KeysEnum,
  toDateISOString
} from "../../shared/functions";
import {SynchronizationStatus, SynchronizationStatusEnum} from "../../data/services/model/root-data-entity.model";
import {SortDirection} from "@angular/material/sort";
import {Trip} from "./model/trip.model";
import {TripFilter} from "./trip.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {NetworkService} from "../../core/services/network.service";
import {IDataEntityQualityService} from "../../data/services/data-quality-service.class";
import {Entity} from "../../core/services/model/entity.model";
import {LandingService} from "./landing.service";
import {IDataSynchroService, RootDataSynchroService} from "../../data/services/data-synchro-service.class";
import {chainPromises} from "../../shared/observables";
import {MINIFY_OPTIONS} from "../../core/services/model/referential.model";


export class ObservedLocationFilter {
  programLabel?: string;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationId?: number;
  recorderDepartmentId?: number;
  recorderPersonId?: number;
  synchronizationStatus?: SynchronizationStatus;

  static isEmpty(f: ObservedLocationFilter|any): boolean {
    return Beans.isEmpty<ObservedLocationFilter>({...f, synchronizationStatus: null}, ObservedLocationFilterKeys, {
      blankStringLikeEmpty: true
    });
  }

  static searchFilter<T extends ObservedLocation>(f: ObservedLocationFilter): (T) => boolean {
    if (!f) return undefined;

    const filterFns: FilterFn<T>[] = [];

    // Program
    if (f.programLabel) {
      filterFns.push(t => (t.program && t.program.label === f.programLabel));
    }

    // Location
    if (isNotNil(f.locationId)) {
      filterFns.push(t => (t.location && t.location.id === f.locationId));
    }

    // Start/end period
    const startDate = fromDateISOString(f.startDate);
    let endDate = fromDateISOString(f.endDate);
    if (startDate) {
      filterFns.push(t => t.endDateTime ? startDate.isSameOrBefore(t.endDateTime) : startDate.isSameOrBefore(t.startDateTime));
    }
    if (endDate) {
      endDate = endDate.add(1, 'day');
      filterFns.push(t => t.startDateTime && endDate.isAfter(t.startDateTime));
    }

    // Recorder department
    if (isNotNil(f.recorderDepartmentId)) {
      filterFns.push(t => (t.recorderDepartment && t.recorderDepartment.id === f.recorderDepartmentId));
    }

    // Recorder person
    if (isNotNil(f.recorderPersonId)) {
      filterFns.push(t => (t.recorderPerson && t.recorderPerson.id === f.recorderPersonId));
    }

    // Synchronization status
    if (f.synchronizationStatus) {
      filterFns.push(t => // Check trip synchro status, if any
        (t.synchronizationStatus && t.synchronizationStatus === f.synchronizationStatus)
        // Else, if SYNC is wanted: must be remote (not local) id
        || (f.synchronizationStatus === 'SYNC' && !t.synchronizationStatus && t.id >= 0)
        // Or else, if DIRTY or READY_TO_SYNC wanted: must be local id
        || (f.synchronizationStatus !== 'SYNC' && !t.synchronizationStatus && t.id < 0));
    }

    if (!filterFns.length) return undefined;

    return (entity) => !filterFns.find(fn => !fn(entity));
  }

  /**
   * Clean a filter, before sending to the pod (e.g remove 'synchronizationStatus')
   * @param f
   */
  static asPodObject(f: ObservedLocationFilter): any {
    if (!f) return f;
    return {
      ...f,
      // Serialize all dates
      startDate: f && toDateISOString(f.startDate),
      endDate: f && toDateISOString(f.endDate),
      // Remove fields that not exists in pod
      synchronizationStatus: undefined
    };
  }
}

export const ObservedLocationFilterKeys: KeysEnum<ObservedLocationFilter> = {
  programLabel: true,
  startDate: true,
  endDate: true,
  locationId: true,
  recorderDepartmentId: true,
  recorderPersonId: true,
  synchronizationStatus: true
};


export interface ObservedLocationSaveOptions {
  withLanding?: boolean;
  enableOptimisticResponse?: boolean; // True by default
}

export interface ObservedLocationServiceLoadOptions extends EntityServiceLoadOptions {
  withLanding?: boolean;
  toEntity?: boolean;
}

export const ObservedLocationFragments = {
  lightObservedLocation: gql`fragment LightObservedLocationFragment on ObservedLocationVO {
    id
    program {
      id
      label
    }
    startDateTime
    endDateTime
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    location {
      ...LocationFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    recorderPerson {
      ...LightPersonFragment
    }
    observers {
      ...LightPersonFragment
    }
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}
  `,
  observedLocation: gql`fragment ObservedLocationFragment on ObservedLocationVO {
    id
    program {
      id
      label
    }
    startDateTime
    endDateTime
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    location {
      ...LocationFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    recorderPerson {
      ...LightPersonFragment
    }
    observers {
      ...LightPersonFragment
    }
    measurementValues
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}
  `
};

// Search query
const LoadAllQuery: any = gql`
  query ObservedLocations($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $trash: Boolean, $filter: ObservedLocationFilterVOInput){
    observedLocations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, trash: $trash){
      ...LightObservedLocationFragment
    }
    observedLocationsCount(filter: $filter, trash: $trash)
  }
  ${ObservedLocationFragments.lightObservedLocation}
`;
// Load query
const LoadQuery: any = gql`
  query ObservedLocation($id: Int!) {
    observedLocation(id: $id) {
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;
// Save all query
const SaveAllQuery: any = gql`
  mutation SaveObservedLocations($observedLocations:[ObservedLocationVOInput], $saveOption: ObservedLocationSaveOptionsInput!){
    saveObservedLocations(observedLocations: $observedLocations, $saveOption){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;
const DeleteByIdsMutation: any = gql`
  mutation DeleteObservedLocations($ids:[Int]){
    deleteObservedLocations(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription UpdateObservedLocation($id: Int!, $interval: Int){
    updateObservedLocation(id: $id, interval: $interval) {
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;

@Injectable({providedIn: "root"})
export class ObservedLocationService
  extends RootDataSynchroService<ObservedLocation, ObservedLocationFilter, ObservedLocationServiceLoadOptions>
  implements
    IEntitiesService<ObservedLocation, ObservedLocationFilter>,
    IEntityService<ObservedLocation, ObservedLocationServiceLoadOptions>,
    IDataEntityQualityService<ObservedLocation>,
    IDataSynchroService<ObservedLocation, ObservedLocationServiceLoadOptions>
{

  protected loading = false;

  constructor(
    injector: Injector,
    protected graphql: GraphqlService,
    protected accountService: AccountService,
    protected network: NetworkService,
    protected entities: EntitiesStorage,
    protected landingService: LandingService
  ) {
    super(injector);

  }

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection,
           dataFilter?: ObservedLocationFilter,
           opts?: EntitiesServiceWatchOptions): Observable<LoadResult<ObservedLocation>> {

    // Load offline
    const offlineData = this.network.offline || (dataFilter && dataFilter.synchronizationStatus && dataFilter.synchronizationStatus !== 'SYNC') || false;
    if (offlineData) {
      return this.watchAllLocally(offset, size, sortBy, sortDirection, dataFilter, opts);
    }

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'startDateTime',
      sortDirection: sortDirection || 'asc',
      trash: opts && opts.trash || false,
      filter: ObservedLocationFilter.asPodObject(dataFilter)
    };

    let now = Date.now();
    console.debug("[observed-location-service] Watching observed locations... using options:", variables);

    return this.mutableWatchQuery<{ observedLocations: ObservedLocation[]; observedLocationsCount: number }>({
        queryName: 'LoadAll',
        query: LoadAllQuery,
        arrayFieldName: 'observedLocations',
        totalFieldName: 'observedLocationsCount',
        insertFilterFn: ObservedLocationFilter.searchFilter(dataFilter),
        variables: variables,
        error: {code: ErrorCodes.LOAD_OBSERVED_LOCATIONS_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ALL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || 'cache-and-network'
      })
      .pipe(
        filter(() => !this.loading),
        map(res => {
          const data = (res && res.observedLocations || []).map(ObservedLocation.fromObject);
          const total = res && isNotNil(res.observedLocationsCount) ? res.observedLocationsCount : undefined;
          if (now) {
            console.debug(`[observed-location-service] Loaded {${data.length || 0}} observed locations in ${Date.now() - now}ms`, data);
            now = undefined;
          } else {
            console.debug(`[observed-location-service] Refreshed {${data.length || 0}} observed locations`);
          }
          return {data, total};
        }));
  }

  watchAllLocally(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection,
           dataFilter?: ObservedLocationFilter,
           opts?: EntitiesServiceWatchOptions): Observable<LoadResult<ObservedLocation>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'startDateTime',
      sortDirection: sortDirection || 'asc',
      filter: TripFilter.searchFilter<Trip>(dataFilter)
    };

    console.debug("[observed-location-service] Watching local observed locations... using options:", variables);

    return this.entities.watchAll<ObservedLocation>(ObservedLocation.TYPENAME, variables)
      .pipe(
        map(res => {
          const data = (res && res.data || []).map(ObservedLocation.fromObject);
          const total = res && isNotNil(res.total) ? res.total : undefined;
          return {data, total};
        }));
  }

  async load(id: number, opts?: ObservedLocationServiceLoadOptions): Promise<ObservedLocation> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    const now = Date.now();
    if (this._debug) console.debug(`[observed-location-service] Loading observed location {${id}}...`);
    this.loading = true;

    try {
      const res = await this.graphql.query<{ observedLocation: ObservedLocation }>({
        query: LoadQuery,
        variables: {
          id: id
        },
        error: {code: ErrorCodes.LOAD_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || 'cache-first'
      });
      const data = res && res.observedLocation && ObservedLocation.fromObject(res.observedLocation);
      if (data && this._debug) console.debug(`[observed-location-service] Observed location #${id} loaded in ${Date.now() - now}ms`, data);

      return data;
    }
    finally {
      this.loading = false;
    }
  }

  public listenChanges(id: number): Observable<ObservedLocation> {
    if (!id && id !== 0) throw new Error("Missing argument 'id' ");

    if (this._debug) console.debug(`[observed-location-service] [WS] Listening changes for trip {${id}}...`);

    return this.graphql.subscribe<{ updateObservedLocation: ObservedLocation }, { id: number, interval: number }>({
      query: UpdateSubscription,
      variables: {
        id: id,
        interval: 10
      },
      error: {
        code: ErrorCodes.SUBSCRIBE_OBSERVED_LOCATION_ERROR,
        message: 'OBSERVED_LOCATION.ERROR.SUBSCRIBE_ERROR'
      }
    })
      .pipe(
        map(res => {
          const data = res && res.updateObservedLocation && ObservedLocation.fromObject(res.updateObservedLocation);
          if (data && this._debug) console.debug(`[observed-location-service] Observed location {${id}} updated on server !`, data);
          return data;
        })
      );
  }

  /**
   * Save many observed locations
   * @param entities
   * @param opts
   */
  async saveAll(entities: ObservedLocation[], opts?: ObservedLocationSaveOptions): Promise<ObservedLocation[]> {
    if (isEmptyArray(entities)) return entities;

    if (this._debug) console.debug(`[observed-location-service] Saving ${entities.length} observed locations...`);
    const jobsFactories = (entities || []).map(entity => () => this.save(entity, {...opts}));
    return chainPromises<ObservedLocation>(jobsFactories);
  }

  async save(entity: ObservedLocation, opts?: ObservedLocationSaveOptions): Promise<ObservedLocation> {
    const isNew = isNil(entity.id);

    // If is a local entity: force a local save
    const isLocal = isNew ? (entity.synchronizationStatus && entity.synchronizationStatus !== 'SYNC') : entity.id < 0;
    if (isLocal) {
      return this.saveLocally(entity, opts);
    }

    const now = Date.now();
    if (this._debug) console.debug("[observed-location-service] Saving an observed location...");

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Reset the control date
    entity.controlDate = undefined;

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (isNew) delete json.id; // Make to remove temporary id, before sending to graphQL
    if (this._debug) console.debug("[observed-location-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ saveObservedLocations: any }>({
      mutation: SaveAllQuery,
      variables: {
        observedLocations: [json]
      },
      error: {code: ErrorCodes.SAVE_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.SAVE_ERROR"},
      update: (proxy, {data}) => {
        const savedEntity = data && data.saveObservedLocations && data.saveObservedLocations[0];
        if (savedEntity !== entity) {
          if (this._debug) console.debug(`[observed-location-service] Observed location saved in ${Date.now() - now}ms`, entity);
          this.copyIdAndUpdateDate(savedEntity, entity);
        }

        // Add to cache
        if (isNew) {
          this.insertIntoMutableCachedQuery(proxy, {
            query: LoadAllQuery,
            data: savedEntity
          });
        }
      }
    });


    return entity;
  }


  async saveLocally(entity: ObservedLocation, opts?: ObservedLocationSaveOptions): Promise<ObservedLocation> {
    if (entity.id >= 0) throw new Error('Must be a local entity');
    opts = {
      withLanding: false,
      ...opts
    };

    this.fillDefaultProperties(entity);

    // Reset quality properties
    this.resetQualityProperties(entity);

    // Make sure to fill id, with local ids
    await this.fillOfflineDefaultProperties(entity);

    // Reset synchro status
    entity.synchronizationStatus = 'DIRTY';

    // Extract landings (saved just after)
    const landings = entity.landings;
    delete entity.landings;

    const jsonLocal = this.asObject(entity, SAVE_LOCALLY_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug('[observed-location-service] [offline] Saving observed location locally...', jsonLocal);

    // Save observed location locally
    await this.entities.save(jsonLocal, {entityName: ObservedLocation.TYPENAME});

    // Save landings
    if (opts.withLanding && isNotEmptyArray(landings)) {

      // Link to physical gear id, using the rankOrder
      landings.forEach(o => {
        o.id = null; // Clean ID, to force new ids
        o.observedLocationId = entity.id;
        o.updateDate = undefined;
      });

      entity.landings = await this.landingService.saveAll(landings, {observedLocationId: entity.id});
    }

    return entity;
  }

  async delete(data: ObservedLocation): Promise<any> {
    await this.deleteAll([data]);
  }

  async deleteAll(entities: ObservedLocation[], options?: any): Promise<any> {
    const ids = entities && entities
      .map(t => t.id)
      .filter(isNotNil);

    const now = Date.now();
    if (this._debug) console.debug("[observed-location-service] Deleting observed locations... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeleteByIdsMutation,
      variables: {
        ids: ids
      },
      update: (proxy) => {
        // Update the cache
        this.removeFromMutableCachedQueryByIds(proxy, {
          query: LoadAllQuery,
          ids
        });

        if (this._debug) console.debug(`[observed-location-service] Observed locations deleted in ${Date.now() - now}ms`);

      }
    });
  }

  /* -- TODO implement this methods -- */
  async control(data: ObservedLocation): Promise<FormErrors> { return undefined; }
  async terminate(data: ObservedLocation): Promise<ObservedLocation> { return data; }
  async validate(data: ObservedLocation): Promise<ObservedLocation> { return data; }
  async unvalidate(data: ObservedLocation): Promise<ObservedLocation> { return data; }
  async qualify(data: ObservedLocation, qualityFlagId: number): Promise<ObservedLocation> { return data; }


  /* -- -- */
  async synchronizeById(id: number): Promise<ObservedLocation> {
    const entity = await this.load(id);
    if (!entity || entity.id >= 0) return; // skip

    return await this.synchronize(entity);
  }

  async synchronize(entity: ObservedLocation, opts?: ObservedLocationSaveOptions): Promise<ObservedLocation> {
    opts = {
      withLanding: true,
      enableOptimisticResponse: false, // Optimistic response not need
      ...opts
    };

    const localId = entity && entity.id;
    if (isNil(localId) || localId >= 0) throw new Error("Entity must be a local entity");
    if (this.network.offline) throw new Error("Could not synchronize if network if offline");

    // Clone (to keep original entity unchanged)
    entity = entity instanceof Entity ? entity.clone() : entity;
    entity.synchronizationStatus = 'SYNC';
    entity.id = undefined;

    // Fill landings
    const res = await this.landingService.loadAllByObservedLocation( {observedLocationId: localId},
      {fullLoad: true, rankOrderOnPeriod: false});
    entity.landings = res && res.data || [];

    try {

      entity = await this.save(entity, opts);

      // Check return entity has a valid id
      if (isNil(entity.id) || entity.id < 0) {
        throw {code: ErrorCodes.SYNCHRONIZE_OBSERVED_LOCATION_ERROR};
      }
    } catch (err) {
      throw {
        ...err,
        code: ErrorCodes.SYNCHRONIZE_OBSERVED_LOCATION_ERROR,
        message: "OBSERVED_LOCATION.ERROR.SYNCHRONIZE_ERROR",
        context: entity.asObject(SAVE_LOCALLY_AS_OBJECT_OPTIONS)
      };
    }

    try {
      if (this._debug) console.debug(`[observed-location-service] Deleting observedLocation {${entity.id}} from local storage`);

      // Delete landings
      await this.landingService.deleteLocally({observedLocationId: localId});

      // Delete observedLocation
      await this.entities.deleteById(localId, {entityName: ObservedLocation.TYPENAME});
    }
    catch (err) {
      console.error(`[observed-location-service] Failed to locally delete observedLocation {${entity.id}} and its landings`, err);
      // Continue
    }

    return entity;
  }

  /* -- protected methods -- */

  protected asObject(entity: ObservedLocation, opts?: DataEntityAsObjectOptions): any {
    opts = { ...MINIFY_OPTIONS, ...opts };

    const copy = super.asObject(entity, opts);

    return copy;
  }


  protected async fillOfflineDefaultProperties(entity: ObservedLocation) {
    const isNew = isNil(entity.id);

    // If new, generate a local id
    if (isNew) {
      entity.id =  await this.entities.nextValue(entity);
    }

    // Fill default synchronization status
    entity.synchronizationStatus = entity.synchronizationStatus || SynchronizationStatusEnum.DIRTY;
  }
}

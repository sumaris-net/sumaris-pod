import {Inject, Injectable, Injector} from "@angular/core";
import {
  EntitiesServiceWatchOptions,
  EntityServiceLoadOptions, FilterFn,
  IEntitiesService,
  IEntityService,
  LoadResult
} from "../../shared/services/entity-service.class";
import {BehaviorSubject, EMPTY, Observable} from "rxjs";
import {Landing} from "./model/landing.model";
import {gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {DataFragments, Fragments} from "./trip.queries";
import {ErrorCodes} from "./trip.errors";
import {filter, map} from "rxjs/operators";
import {
  isEmptyArray,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
} from "../../shared/functions";
import {BaseRootDataService} from "./root-data-service.class";
import {Sample} from "./model/sample.model";
import {EntityUtils} from "../../core/services/model/entity.model";
import {
  DataEntityAsObjectOptions,
  SAVE_AS_OBJECT_OPTIONS,
  SAVE_LOCALLY_AS_OBJECT_OPTIONS,
  SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS
} from "../../data/services/model/data-entity.model";
import {VesselSnapshotFragments} from "../../referential/services/vessel-snapshot.service";
import {FormErrors} from "../../core/form/form.utils";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import * as momentImported from "moment";
const moment = momentImported;
import {Moment} from "moment";
import {DataRootEntityUtils, SynchronizationStatus} from "../../data/services/model/root-data-entity.model";
import {MINIFY_OPTIONS} from "../../core/services/model/referential.model";
import {SortDirection} from "@angular/material/sort";
import {chainPromises, firstNotNilPromise} from "../../shared/observables";
import {JobUtils} from "../../shared/services/job.utils";
import {environment} from "../../../environments/environment";
import {fromDateISOString, toDateISOString} from "../../shared/dates";
import {Person} from "../../core/services/model/person.model";
import {ProgramRefService} from "../../referential/services/program-ref.service";
import {
  BaseEntityGraphqlMutations, BaseEntityGraphqlQueries,
  BaseEntityGraphqlSubscriptions, EntitySaveOptions
} from "../../referential/services/base-entity-service.class";
import {ReferentialFragments} from "../../referential/services/referential.fragments";


export class LandingFilter {

  programLabel?: string;
  vesselId?: number;
  locationId?: number;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  recorderDepartmentId?: number;
  recorderPersonId?: number;
  synchronizationStatus?: SynchronizationStatus;
  groupByVessel?: boolean;
  excludeVesselIds?: number[];

  // Linked entities
  observedLocationId?: number;
  tripId?: number;

  static isEmpty(landingFilter: LandingFilter|any): boolean {
    return !landingFilter || (
      isNil(landingFilter.observedLocationId) && isNil(landingFilter.tripId)
      && isNilOrBlank(landingFilter.programLabel) && isNilOrBlank(landingFilter.vesselId) && isNilOrBlank(landingFilter.locationId)
      && !landingFilter.startDate && !landingFilter.endDate
      && isNil(landingFilter.recorderDepartmentId)
      && isNil(landingFilter.recorderPersonId)
      && isEmptyArray(landingFilter.excludeVesselIds)
    );
  }

  static searchFilter<T extends Landing>(f: LandingFilter): (T) => boolean {

    if (LandingFilter.isEmpty(f)) return undefined;
    const filterFns: FilterFn<T>[] = [];

    // observedLocationId
    if (isNotNil(f.observedLocationId)) {
      filterFns.push((entity) => entity.observedLocationId === f.observedLocationId);
    }

    // tripId
    if (isNotNil(f.tripId)) {
      filterFns.push((entity) => entity.tripId === f.tripId);
    }

    // Vessel
    if (isNotNil(f.vesselId)) {
      filterFns.push((entity) => entity.vesselSnapshot && entity.vesselSnapshot.id === f.vesselId);
    }

    // Vessel exclude
    if (isNotEmptyArray(f.excludeVesselIds)) {
      filterFns.push((entity) => entity.vesselSnapshot && !f.excludeVesselIds.includes(entity.vesselSnapshot.id));
    }

      // Location
    if (isNotNil(f.locationId)) {
      filterFns.push((entity) => entity.location && entity.location.id === f.locationId);
    }

    // Start/end period
    const startDate = fromDateISOString(f.startDate);
    let endDate = fromDateISOString(f.endDate);
    if (startDate) {
      filterFns.push(t => t.dateTime && startDate.isSameOrBefore(t.dateTime));
    }
    if (endDate) {
      endDate = endDate.add(1, 'day').startOf('day');
      filterFns.push(t => t.dateTime && endDate.isAfter(t.dateTime));
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
      if (f.synchronizationStatus === 'SYNC') {
        filterFns.push(t => t.synchronizationStatus === 'SYNC' || (!t.synchronizationStatus && t.id >= 0));
      }
      else {
        filterFns.push(t => t.synchronizationStatus && t.synchronizationStatus !== 'SYNC' || t.id < 0);
      }
    }

    if (!filterFns.length) return undefined;

    return (entity) => !filterFns.find(fn => !fn(entity));
  }

  static toPodObject(dataFilter: LandingFilter) {
    return {
      ...dataFilter,

      // Serialize all dates
      startDate: dataFilter && toDateISOString(dataFilter.startDate),
      endDate: dataFilter && toDateISOString(dataFilter.endDate),

      // Remove fields that not exists in pod
      synchronizationStatus: undefined,
      groupByVessel: undefined
    };
  }
}

export declare interface LandingSaveOptions extends EntitySaveOptions {
  observedLocationId?: number;
  tripId?: number;

  enableOptimisticResponse?: boolean;
}

export declare interface LandingServiceWatchOptions
  extends EntitiesServiceWatchOptions {

  computeRankOrder?: boolean;
  fullLoad?: boolean;
  toEntity?: boolean;
  withTotal?: boolean;
}

export const LandingFragments = {
  lightLanding: gql`fragment LightLandingFragment on LandingVO {
    id
    program {
      id
      label
    }
    dateTime
    location {
      ...LocationFragment
    }
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    rankOrder
    observedLocationId
    tripId
    rankOrderOnVessel
    vesselSnapshot {
      ...LightVesselSnapshotFragment
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
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${ReferentialFragments.referential}
  `,
  landing: gql`fragment LandingFragment on LandingVO {
    id
    program {
      id
      label
    }
    dateTime
    location {
      ...LocationFragment
    }
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    rankOrder
    observedLocationId
    tripId
    rankOrderOnVessel
    vesselSnapshot {
      ...VesselSnapshotFragment
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
    samples {
      ...SampleFragment
    }
  }`
};

const LandingQueries = {
  load: gql`query Landing($id: Int!){
    data: landing(id: $id){
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${VesselSnapshotFragments.vesselSnapshot}
  ${DataFragments.sample}`,

  loadAll: gql`query LightLandings($filter: LandingFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: landings(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightLandingFragment
    }
  }
  ${LandingFragments.lightLanding}`,

  loadAllWithTotal: gql`query LightLandingsWithTotal($filter: LandingFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: landings(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightLandingFragment
    }
    total: landingsCount(filter: $filter)
  }
  ${LandingFragments.lightLanding}`,

  loadAllFullWithTotal: gql`query Landings($filter: LandingFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: landings(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LandingFragment
    }
    total: landingsCount(filter: $filter)
  }
  ${LandingFragments.landing}
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${VesselSnapshotFragments.vesselSnapshot}
  ${DataFragments.sample}`
};

const LandingMutations: BaseEntityGraphqlMutations = {
  save: gql`mutation SaveLanding($data:LandingVOInput!){
    data: saveLanding(landing: $data){
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${VesselSnapshotFragments.vesselSnapshot}
  ${DataFragments.sample}`,

  saveAll: gql`mutation SaveLandings($data:[LandingVOInput!]!){
    data: saveLandings(landings: $data){
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${VesselSnapshotFragments.vesselSnapshot}
  ${DataFragments.sample}`,

  deleteAll: gql`mutation DeleteLandings($ids:[Int!]!){
    deleteLandings(ids: $ids)
  }`
};

const LandingSubscriptions: BaseEntityGraphqlSubscriptions = {
  listenChanges: gql`subscription UpdateLanding($id: Int!, $interval: Int){
    data: updateLanding(id: $id, interval: $interval) {
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${VesselSnapshotFragments.vesselSnapshot}
  ${DataFragments.sample}`
};


const sortByDateFn = (n1: Landing, n2: Landing) => {
  return n1.dateTime.isSame(n2.dateTime)
    ? (n1.id === n2.id ? 0 : n1.id > n2.id ? 1 : -1)
    : (n1.dateTime.isAfter(n2.dateTime) ? 1 : -1);
};

@Injectable({providedIn: 'root'})
export class LandingService extends BaseRootDataService<Landing, LandingFilter>
  implements
    IEntitiesService<Landing, LandingFilter, LandingServiceWatchOptions>,
    IEntityService<Landing> {

  protected loading = false;

  constructor(
    injector: Injector,
    protected network: NetworkService,
    protected entities: EntitiesStorage,
    protected programRefService: ProgramRefService
  ) {
    super(injector,
      Landing,
      {
        queries: LandingQueries,
        mutations: LandingMutations,
        subscriptions: LandingSubscriptions,
        filterAsObjectFn: LandingFilter.toPodObject,
        filterFnFactory: LandingFilter.searchFilter
      }
    );
  }

  async loadAllByObservedLocation(filter?: LandingFilter & { observedLocationId: number; }, opts?: LandingServiceWatchOptions): Promise<LoadResult<Landing>> {
    return firstNotNilPromise(this.watchAllByObservedLocation(filter, opts));
  }

  watchAllByObservedLocation(filter?: LandingFilter & { observedLocationId: number; }, opts?: LandingServiceWatchOptions): Observable<LoadResult<Landing>> {
    return this.watchAll(0, -1, null, null, filter, opts);
  }

  watchAll(offset: number, size: number,
           sortBy?: string, sortDirection?: SortDirection,
           dataFilter?: LandingFilter,
           opts?: LandingServiceWatchOptions): Observable<LoadResult<Landing>> {

    // Load offline
    const offline = this.network.offline
      || (dataFilter && (
        (dataFilter.synchronizationStatus && dataFilter.synchronizationStatus !== 'SYNC')
        || dataFilter.observedLocationId < 0 || dataFilter.tripId < 0)) || false;
    if (offline) {
      return this.watchAllLocally(offset, size, sortBy, sortDirection, dataFilter, opts);
    }

    const groupByVessel = dataFilter && dataFilter.groupByVessel === true;
    if (groupByVessel) {
      // sortBy = 'dateTime';
      // sortDirection = 'desc';
      size = 1000;
    }

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: (sortBy !== 'id' && sortBy) || 'dateTime',
      sortDirection: sortDirection || 'asc',
      filter: this.filterAsObjectFn(dataFilter)
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug("[landing-service] Watching landings... using variables:", variables);

    const fullLoad = (opts && opts.fullLoad === true);
    const withTotal = (!opts || opts.withTotal !== false);
    const query = fullLoad ? LandingQueries.loadAllFullWithTotal :
      (withTotal ? this.queries.loadAllWithTotal : this.queries.loadAll);

    return this.mutableWatchQuery<{ data: any[]; total: number; }>({
        queryName: 'LoadAll',
        query,
        arrayFieldName: "data",
        totalFieldName: withTotal ? "total" : undefined,
        insertFilterFn: this.filterFnFactory(dataFilter),
        variables,
        error: {code: ErrorCodes.LOAD_LANDINGS_ERROR, message: "LANDING.ERROR.LOAD_ALL_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || undefined
      })
        .pipe(
          // Skip update during load()
          filter(() => !this.loading),
          map(({data, total}) => {
            let entities = (!opts || opts.toEntity !== false)
              ? (data || []).map(Landing.fromObject)
              : (data || []) as Landing[];
            if (this._debug) {
              if (now) {
                console.debug(`[landing-service] Loaded {${entities.length || 0}} landings in ${Date.now() - now}ms`, entities);
                now = undefined;
              }
            }

            // Group by vessel (keep last landing)
            if (isNotEmptyArray(entities) && groupByVessel) {
              const landingByVesselMap = new Map<number, Landing>();
              entities.forEach(landing => {
                const existingLanding = landingByVesselMap.get(landing.vesselSnapshot.id);
                if (!existingLanding || fromDateISOString(existingLanding.dateTime).isBefore(landing.dateTime)) {
                  landingByVesselMap.set(landing.vesselSnapshot.id, landing);
                }
              });
              entities = Object.values(landingByVesselMap);
              total = entities.length;
            }

            // Compute rankOrder, by tripId or observedLocationId
            if (!opts || opts.computeRankOrder !== false) {
              this.computeRankOrderAndSort(entities, offset, total, sortBy, sortDirection, dataFilter);
            }

            return {data: entities, total};
          }));
  }


  async loadAll(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: SortDirection,
                dataFilter?: LandingFilter,
                opts?: LandingServiceWatchOptions): Promise<LoadResult<Landing>> {
    return firstNotNilPromise(this.watchAll(offset, size, sortBy, sortDirection, dataFilter, opts));
  }

  async load(id: number, options?: EntityServiceLoadOptions): Promise<Landing> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    const now = Date.now();
    if (this._debug) console.debug(`[landing-service] Loading landing {${id}}...`);
    this.loading = true;

    try {
      let data: any;

      // If local entity
      if (id < 0) {
        data = await this.entities.load<Landing>(id, Landing.TYPENAME);
      }

      else {
        // Load remotely
        const res = await this.graphql.query<{ data: any }>({
          query: this.queries.load,
          variables: {
            id: id
          },
          error: {code: ErrorCodes.LOAD_LANDING_ERROR, message: "LANDING.ERROR.LOAD_ERROR"},
          fetchPolicy: options && options.fetchPolicy || undefined
        });
        data = res && res.data;
      }

      // Transform to entity
      const entity = data && Landing.fromObject(data);
      if (entity && this._debug) console.debug(`[landing-service] landing #${id} loaded in ${Date.now() - now}ms`, entity);
      return entity;
    }
    finally {
      this.loading = false;
    }
  }

  async saveAll(entities: Landing[], opts?: LandingSaveOptions): Promise<Landing[]> {
    if (!entities) return entities;

    const localEntities = entities.filter(entity => entity
      && (entity.id < 0 || (entity.synchronizationStatus && entity.synchronizationStatus !== 'SYNC'))
    );
    if (isNotEmptyArray(localEntities)) {
      return this.saveAllLocally(localEntities, opts);
    }

    const json = entities
      .map(entity => {
        // Fill default properties (as recorder department and person)
        this.fillDefaultProperties(entity, opts);
        // Reset quality properties
        this.resetQualityProperties(entity);
        return this.asObject(entity);
      });

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Saving landings...", json);

    await this.graphql.mutate<{data: any[]}>({
      mutation: this.mutations.saveAll,
      variables: {
        data: json
      },
      error: {code: ErrorCodes.SAVE_LANDINGS_ERROR, message: "LANDING.ERROR.SAVE_ALL_ERROR"},
      update: (proxy, {data}) => {

        if (this._debug) console.debug(`[landing-service] Landings saved remotely in ${Date.now() - now}ms`);

        // For each result, copy ID+updateDate to source entity
        // Then filter to keep only new landings (need to cache update)
        const newSavedLandings = (data.data && entities || [])
          .map(entity => {
            const savedEntity = data.data.find(obj => entity.equals(obj));
            const isNew = isNil(entity.id);
            this.copyIdAndUpdateDate(savedEntity, entity);
            return isNew ? savedEntity : null;
          }).filter(isNotNil);

        // Add to cache
        if (isNotEmptyArray(newSavedLandings)) {
          this.insertIntoMutableCachedQuery(proxy, {
            queryName: 'LoadAll',
            data: newSavedLandings
          });
        }
      }
    });

    return entities;
  }

  async saveAllLocally(entities: Landing[], opts?: LandingSaveOptions): Promise<Landing[]> {
    if (!entities) return entities;

    if (this._debug) console.debug(`[landing-service] Saving ${entities.length} landings locally...`);
    const jobsFactories = (entities || []).map(entity => () => this.saveLocally(entity, {...opts}));
    return chainPromises<Landing>(jobsFactories);
  }

  async save(entity: Landing, opts?: LandingSaveOptions): Promise<Landing> {
    const isNew = isNil(entity.id);

    // If parent is a local entity: force to save locally
    // If is a local entity: force a local save
    const offline = (isNew ? (entity.synchronizationStatus && entity.synchronizationStatus !== 'SYNC') : entity.id < 0)
      || entity.observedLocationId < 0;
    if (offline) {
      return await this.saveLocally(entity, opts);
    }

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Saving a landing...", entity);

    // Prepare to save
    this.fillDefaultProperties(entity, opts);

    // Reset quality properties
    this.resetQualityProperties(entity);

    // When offline, provide an optimistic response
    const offlineResponse = (!opts || opts.enableOptimisticResponse !== false) ?
      async (context) => {
        // Make sure to fill id, with local ids
        await this.fillOfflineDefaultProperties(entity);

        // For the query to be tracked (see tracked query link) with a unique serialization key
        context.tracked = (!entity.synchronizationStatus || entity.synchronizationStatus === 'SYNC');
        if (isNotNil(entity.id)) context.serializationKey = `${Landing.TYPENAME}:${entity.id}`;

        return { data: [this.asObject(entity, SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS)] };
      } : undefined;

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[landing-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ data: any }>({
      mutation: this.mutations.save,
      variables: {
        data: json
      },
      offlineResponse,
      error: {code: ErrorCodes.SAVE_OBSERVED_LOCATION_ERROR, message: "ERROR.SAVE_ERROR"},
      update: async (proxy, {data}) => {
        const savedEntity = data && data.data ;

        // Local entity: save it
        if (savedEntity.id < 0) {
          if (this._debug) console.debug('[landing-service] [offline] Saving landing locally...', savedEntity);

          // Save response locally
          await this.entities.save<Landing>(savedEntity);
        }

        // Update the entity and update GraphQL cache
        else {

          // Remove existing entity from the local storage
          if (entity.id < 0 && (savedEntity.id > 0 || savedEntity.updateDate)) {
            if (this._debug) console.debug(`[landing-service] Deleting landing {${entity.id}} from local storage`);
            await this.entities.delete(entity);
          }

          this.copyIdAndUpdateDate(savedEntity, entity);

          if (this._debug) console.debug(`[landing-service] Landing saved remotely in ${Date.now() - now}ms`, entity);

          // Add to cache
          if (isNew) {
            // Cache load by parent
            this.insertIntoMutableCachedQuery(proxy, {
              queryName: 'LoadAll',
              data: savedEntity
            });
          }
        }
      }
    });

    return entity;
  }

  /**
   * Delete landing locally (from the entity storage)
   * @param filter (required observedLocationId)
   */
  async deleteLocally(filter: LandingFilter & { observedLocationId: number; }): Promise<Landing[]> {
    if (!filter || isNil(filter.observedLocationId)) throw new Error("Missing arguments 'filter.observedLocationId'");

    try {
      // Find landing to delete
      const res = await this.entities.loadAll<Landing>(Landing.TYPENAME, {
        filter: LandingFilter.searchFilter<Landing>(filter)
      });
      const ids = (res && res.data || []).map(o => o.id);
      if (isEmptyArray(ids)) return undefined; // Skip

      // Apply deletion
      return await this.entities.deleteMany(ids, {entityName: Landing.TYPENAME});
    }
    catch (err) {
      console.error(`[landing-service] Failed to delete landings ${JSON.stringify(filter)}`, err);
      throw err;
    }
  }


  /**
   * Load many local landings
   */
  watchAllLocally(offset: number,
                  size: number,
                  sortBy?: string,
                  sortDirection?: SortDirection,
                  dataFilter?: LandingFilter,
                  opts?: LandingServiceWatchOptions): Observable<LoadResult<Landing>> {

    if (LandingFilter.isEmpty(dataFilter)) {
      console.warn("[landing-service] Trying to watch landings without 'filter': skipping.");
      return EMPTY;
    }
    if (isNotNil(dataFilter.observedLocationId) && dataFilter.observedLocationId >= 0) throw new Error("Invalid 'filter.observedLocationId': must be a local ID (id<0)!");

    const variables = {
      offset: offset || 0,
      size: size || 20,
      sortBy: (sortBy !== 'id' && sortBy) || 'dateTime',
      sortDirection: sortDirection || 'asc',
      trash: opts && opts.trash || false,
      filter: LandingFilter.searchFilter<Landing>(dataFilter)
    };

    const entityName = (!dataFilter.synchronizationStatus || dataFilter.synchronizationStatus !== 'SYNC')
      ? Landing.TYPENAME // Local entities
      : EntitiesStorage.REMOTE_PREFIX + Landing.TYPENAME; // Remote entities

    if (this._debug) console.debug(`[landing-service] Loading ${entityName} locally... using options:`, variables);
    return this.entities.watchAll<Landing>(entityName, variables, {fullLoad: opts && opts.fullLoad})
      .pipe(map(({data, total}) => {
        const entities = (!opts || opts.toEntity !== false)
          ? (data || []).map(Landing.fromObject)
          : (data || []) as Landing[];
        total = total || entities.length;

        // Compute rankOrder, by tripId or observedLocationId
        if (!opts || opts.computeRankOrder !== false) {
          this.computeRankOrderAndSort(entities, offset, total, sortBy, sortDirection, dataFilter);
        }

        return {
          data: entities,
          total
        };
      }));
  }

  async deleteAll(entities: Landing[], options?: any): Promise<any> {

    // Get local entity ids, then delete id
    const localIds = entities && entities
      .map(t => t.id)
      .filter(id => id < 0);
    if (isNotEmptyArray(localIds)) {
      if (this._debug) console.debug("[landing-service] Deleting landings locally... ids:", localIds);
      await this.entities.deleteMany<Landing>(localIds, {entityName: Landing.TYPENAME});
    }

    const ids = entities && entities
      .map(t => t.id)
      .filter(id => id >= 0);
    if (isEmptyArray(ids)) return; // stop, if nothing else to do

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Deleting landings... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: this.mutations.deleteAll,
      variables: {
        ids
      },
      update: (proxy) => {

        // Remove from cache
        this.removeFromMutableCachedQueryByIds(proxy, {queryName: 'LoadAll', ids});

        if (this._debug) console.debug(`[landing-service] Landings deleted in ${Date.now() - now}ms`);
      }
    });
  }


  listenChanges(id: number): Observable<Landing> {
    if (!id && id !== 0) throw new Error("Missing argument 'id'");

    if (this._debug) console.debug(`[landing-service] [WS] Listening changes for trip {${id}}...`);

    return this.graphql.subscribe<{ data: any }, { id: number, interval: number }>({
      query: this.subscriptions.listenChanges,
      variables: {
        id: id,
        interval: 10
      },
      error: {
        code: ErrorCodes.SUBSCRIBE_LANDING_ERROR,
        message: 'LANDING.ERROR.SUBSCRIBE_ERROR'
      }
    })
      .pipe(
        map(res => {
          const data = res && Landing.fromObject(res.data);
          if (data && this._debug) console.debug(`[landing-service] Landing {${id}} updated on server !`, data);
          return data;
        })
      );
  }

  async synchronizeById(id: number): Promise<Landing> {
    const entity = await this.load(id);

    if (!entity || entity.id >= 0) return; // skip

    return await this.synchronize(entity);
  }

  async synchronize(data: Landing): Promise<Landing> {
    console.warn('Not implemented', new Error());
    return data;
  }

  async control(data: Landing): Promise<FormErrors> {
    console.warn('Not implemented', new Error());
    return undefined;
  }

  async executeImport(progression: BehaviorSubject<number>,
                opts?: {
                  maxProgression?: number;
                  filter?: LandingFilter
                }) {
    const now = this._debug && Date.now();
    const maxProgression = opts && opts.maxProgression || 100;

    const filter: LandingFilter = {
      startDate: moment().startOf('day').add(-15, 'day'),
      ...opts?.filter
    };

    console.info('[landing-service] Importing remote landings...', filter);

    const {data} = await JobUtils.fetchAllPages<any>((offset, size) =>
        this.loadAll(offset, size, 'id', null, filter, {
          fetchPolicy: "no-cache", // Skip cache
          fullLoad: false,
          toEntity: false
        }),
      progression,
      {
        maxProgression: maxProgression * 0.9,
        fetchSize: 5,
        logPrefix: '[landing-service]'
      });


    // Save locally
    await this.entities.saveAll(data || [], {
      entityName: EntitiesStorage.REMOTE_PREFIX + Landing.TYPENAME,
      reset: true
    });

    if (this._debug) console.debug(`[landing-service] Importing remote landings [OK] in ${Date.now() - now}ms`, data);

  }

  copyIdAndUpdateDate(source: Landing | undefined, target: Landing) {
    if (!source) return;

    super.copyIdAndUpdateDate(source, target);

    // Update samples (recursively)
    if (target.samples && source.samples) {
      this.copyIdAndUpdateDateOnSamples(source.samples, target.samples);
    }
  }

  /* -- protected methods -- */

  /**
   * Save into the local storage
   * @param data
   */
  protected async saveLocally(entity: Landing, opts?: LandingSaveOptions): Promise<Landing> {
    if (entity.observedLocationId >= 0) throw new Error('Must be a local entity');

    // Fill default properties (as recorder department and person)
    this.fillDefaultProperties(entity, opts);

    // Make sure to fill id, with local ids
    await this.fillOfflineDefaultProperties(entity);

    const jsonLocal = this.asObject(entity, SAVE_LOCALLY_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug('[landing-service] [offline] Saving landing locally...', jsonLocal);

    // Save response locally
    await this.entities.save(jsonLocal);

    return entity;
  }

  protected asObject(entity: Landing, opts?: DataEntityAsObjectOptions): any {
    opts = {...MINIFY_OPTIONS, ...opts};
    const copy: any = entity.asObject(opts);

    if (opts.minify && !opts.keepEntityName && !opts.keepTypename) {
      // Clean vessel features object, before saving
      copy.vesselSnapshot = {id: entity.vesselSnapshot && entity.vesselSnapshot.id};

      // Comment because need to keep recorder person
      copy.recorderPerson = entity.recorderPerson && <Person>{
        id: entity.recorderPerson.id,
        firstName: entity.recorderPerson.firstName,
        lastName: entity.recorderPerson.lastName
      };

      // Keep id only, on recorder department
      copy.recorderDepartment = entity.recorderDepartment && {id: entity.recorderDepartment && entity.recorderDepartment.id} || undefined;
    }

    return copy;
  }

  protected fillDefaultProperties(entity: Landing, opts?: Partial<LandingSaveOptions>) {
    super.fillDefaultProperties(entity);

    // Fill parent id, if not already set
    if (!entity.tripId && !entity.observedLocationId && opts) {
      entity.observedLocationId = opts.observedLocationId;
      entity.tripId = opts.tripId;
    }

    // Make sure to set all samples attributes
    (entity.samples || []).forEach(s => {
      // Always fill label
      if (isNilOrBlank(s.label)) {
        s.label = `#${s.rankOrder}`;
      }
    });

    // Measurement: compute rankOrder
    // fillRankOrder(entity.measurements); // todo ? use measurements instead of measurementValues
  }

  protected async fillOfflineDefaultProperties(entity: Landing) {
    const isNew = isNil(entity.id);

    // If new, generate a local id
    if (isNew) {
      entity.id = await this.entities.nextValue(entity);
    }

    // Fill default synchronization status
    entity.synchronizationStatus = entity.synchronizationStatus || 'DIRTY';

    // Fill all sample ids
    const samples = entity.samples && EntityUtils.listOfTreeToArray(entity.samples) || [];
    await EntityUtils.fillLocalIds(samples, (_, count) => this.entities.nextValues(Sample.TYPENAME, count));
  }


  /**
   * Copy Id and update, in sample tree (recursively)
   * @param sources
   * @param targets
   */
  protected copyIdAndUpdateDateOnSamples(sources: (Sample | any)[], targets: Sample[]) {
    // Update samples
    if (sources && targets) {
      targets.forEach(target => {
        const source = sources.find(json => target.equals(json));
        EntityUtils.copyIdAndUpdateDate(source, target);
        DataRootEntityUtils.copyControlAndValidationDate(source, target);

        // Apply to children
        if (target.children && target.children.length) {
          this.copyIdAndUpdateDateOnSamples(sources, target.children); // recursive call
        }
      });
    }
  }

  protected computeRankOrderAndSort(data: Landing[],
                                    offset: number,
                                    total: number,
                                    sortBy: string,
                                    sortDirection: string,
                                    filter?: LandingFilter) {

    // Compute rankOrder, by tripId or observedLocationId
    if (filter && (isNotNil(filter.tripId) || isNotNil(filter.observedLocationId))) {
      const asc = (!sortDirection || sortDirection === 'asc');
      const after = asc ? 1 : -1;
      let rankOrder = asc ? 1 + offset : total - offset; // TODO Question BLA: A quoi ca sert cette ligne ?
      // apply a sorted copy (do NOT change original order), then compute rankOrder
      data.slice().sort(sortByDateFn)
        .forEach(o => o.rankOrder = asc ? rankOrder++ : rankOrder--);

      // sort by rankOrderOnPeriod (aka id)
      if (!sortBy || sortBy === 'rankOrder') {
        data.sort((a, b) => {
          const valueA = a.rankOrder;
          const valueB = b.rankOrder;
          return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
        });
      }
    }
  }
}

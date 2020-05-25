import {Injectable, Injector} from "@angular/core";
import {
  EditorDataService,
  EditorDataServiceLoadOptions,
  LoadResult,
  TableDataService
} from "../../shared/services/data-service.class";
import {Observable} from "rxjs";
import {environment} from "../../../environments/environment";
import {Landing} from "./model/landing.model";
import gql from "graphql-tag";
import {DataFragments, Fragments} from "./trip.queries";
import {ErrorCodes} from "./trip.errors";
import {filter, map, throttleTime} from "rxjs/operators";
import {
  fromDateISOString,
  isEmptyArray,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  toDateISOString
} from "../../shared/functions";
import {RootDataService} from "./root-data-service.class";
import {TripFilter} from "./trip.service";
import {Sample} from "./model/sample.model";
import {EntityUtils, MINIFY_OPTIONS} from "../../core/services/model";
import {
  DataEntityAsObjectOptions,
  DataRootEntityUtils, fillRankOrder,
  SAVE_AS_OBJECT_OPTIONS, SAVE_LOCALLY_AS_OBJECT_OPTIONS, SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS,
  SynchronizationStatus
} from "./model/base.model";
import {WatchQueryFetchPolicy} from "apollo-client";
import {VesselSnapshotFragments} from "../../referential/services/vessel-snapshot.service";
import {FormErrors} from "../../core/form/form.utils";
import {NetworkService} from "../../core/services/network.service";
import {EntityStorage} from "../../core/services/entities-storage.service";
import {AcquisitionLevelType} from "../../referential/services/model";
import {Trip} from "./model/trip.model";
import {dataIdFromObject} from "../../core/graphql/graphql.utils";
import {concatPromises} from "../../shared/observables";
import {Moment} from "moment";


export class LandingFilter {

  programLabel?: string;
  vesselId?: number;
  locationId?: number;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  recorderDepartmentId?: number;
  recorderPersonId?: number;
  synchronizationStatus?: SynchronizationStatus;

  observedLocationId?: number;
  tripId?: number;

  static isEmpty(landingFilter: LandingFilter|any): boolean {
    return !landingFilter || (
      isNil(landingFilter.observedLocationId) && isNil(landingFilter.tripId)
      && isNilOrBlank(landingFilter.programLabel) && isNilOrBlank(landingFilter.vesselId) && isNilOrBlank(landingFilter.locationId)
      && !landingFilter.startDate && !landingFilter.endDate
      && isNil(landingFilter.recorderDepartmentId)
      && isNil(landingFilter.recorderPersonId)
    );
  }

  static searchFilter<T extends Landing>(f: LandingFilter): (T) => boolean {
    if (this.isEmpty(f)) return undefined;
    return (t: T) => {

      // observedLocationId
      if (isNotNil(f.observedLocationId) && f.observedLocationId !== t.observedLocationId) return false;

      // tripId
      if (isNotNil(f.tripId) && f.tripId !== t.tripId) return false;

      // Vessel
      if (isNotNil(f.vesselId) && t.vesselSnapshot && t.vesselSnapshot.id !== f.vesselId) return false;

      // Location
      if (isNotNil(f.locationId) && t.location && t.location.id !== f.locationId) return false;

      // Start/end period
      const startDate = fromDateISOString(f.startDate);
      const endDate = fromDateISOString(f.endDate);
      if ((startDate && t.dateTime && startDate.isAfter(t.dateTime))
        || (endDate && t.dateTime && endDate.add(1, 'day').isSameOrBefore(t.dateTime))) {
        return false;
      }

      // Recorder department
      if (isNotNil(f.recorderDepartmentId) && t.recorderDepartment && t.recorderDepartment.id !== f.recorderDepartmentId) {
        return false;
      }

      // Recorder person
      if (isNotNil(f.recorderPersonId) && (!t.recorderPerson || t.recorderPerson.id !== f.recorderPersonId)) {
        return false;
      }

      // Synchronization status
      if (f.synchronizationStatus && (
        // Check landing synchro status, if any
        (t.synchronizationStatus && t.synchronizationStatus !== f.synchronizationStatus)
        // Else, if SYNC is wanted: exclude if local id
        || (f.synchronizationStatus === 'SYNC' && !t.synchronizationStatus && t.id < 0)
        // Or else, if DIRTY or READY_TO_SYNC wanted: exclude if id is NOT local
        || (f.synchronizationStatus !== 'SYNC' && !t.synchronizationStatus && t.id >= 0))
      ) {
        return false;
      }

      return true;
    };
  }
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
  }
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${VesselSnapshotFragments.lightVesselSnapshot}
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
    observedLocationId
    tripId
    vesselSnapshot {
      ...VesselSnapshotFragment
    }
    rankOrderOnVessel
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
  }
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${VesselSnapshotFragments.vesselSnapshot}
  ${DataFragments.sample}
  `
};

// Search query
const LoadAllQuery: any = gql`
  query Landings($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: LandingFilterVOInput){
    landings(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LandingFragment
    }
    landingsCount(filter: $filter)
  }
  ${LandingFragments.landing}
`;

const LoadQuery: any = gql`
  query Landing($id: Int!){
    landing(id: $id){
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
`;
// Save all query
const SaveAllQuery: any = gql`
  mutation SaveLandings($landings:[LandingVOInput]){
    saveLandings(landings: $landings){
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
`;

const DeleteByIdsMutation: any = gql`
  mutation DeleteLandings($ids:[Int!]){
    deleteLandings(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription UpdateLanding($id: Int!, $interval: Int){
    updateLanding(id: $id, interval: $interval) {
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
`;


const sortByDateFn = (n1: Landing, n2: Landing) => {
  return n1.dateTime.isSame(n2.dateTime)
    ? (n1.id === n2.id ? 0 : n1.id > n2.id ? 1 : -1)
    : (n1.dateTime.isAfter(n2.dateTime) ? 1 : -1);
};

@Injectable({providedIn: 'root'})
export class LandingService extends RootDataService<Landing, LandingFilter>
  implements TableDataService<Landing, LandingFilter>, EditorDataService<Landing, LandingFilter> {

  protected $importationProgress: Observable<number>;
  protected loading = false;

  constructor(
    injector: Injector,
    protected network: NetworkService,
    protected entities: EntityStorage
  ) {
    super(injector);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  watchAll(offset: number, size: number,
           sortBy?: string, sortDirection?: string,
           dataFilter?: LandingFilter,
           options?: {
             fetchPolicy?: WatchQueryFetchPolicy
           }): Observable<LoadResult<Landing>> {

    if (sortBy && sortBy === 'id')
      sortBy = undefined;

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'dateTime',
      sortDirection: sortDirection || 'asc',
      filter: {
        ...dataFilter,
        // Serialize all dates
        startDate: dataFilter && toDateISOString(dataFilter.startDate),
        endDate: dataFilter && toDateISOString(dataFilter.endDate),
        // Remove fields that not exists in pod
        synchronizationStatus: undefined
      }
    };

    let now = this._debug && Date.now();
    let $loadResult: Observable<{ landings: Landing[]; landingsCount?: number; }>;
    if (this._debug) console.debug("[landing-service] Watching landings... using options:", variables);

    const offline = this.network.offline || (dataFilter && dataFilter.synchronizationStatus && dataFilter.synchronizationStatus !== 'SYNC') || false;

    if (offline) {
      $loadResult = this.entities.watchAll<Landing>('LandingVO', {
        ...variables,
        filter: LandingFilter.searchFilter<Landing>(dataFilter)
      })
        .pipe(
          map(res => {
            return {landings: res && res.data, landingsCount: res && res.total};
          }));
    }
    else {

      // Save filter if load for a tables
      // This is need to update cache, when save new entity (see save())
      if (dataFilter && (isNotNil(dataFilter.observedLocationId) || isNotNil(dataFilter.tripId))) {
        this._lastVariables.loadByParent = variables;
      } else {
        this._lastVariables.loadAll = variables;
      }

      $loadResult = this.graphql.watchQuery<{ landings: Landing[]; landingsCount: number; }>({
        query: LoadAllQuery,
        variables,
        error: {code: ErrorCodes.LOAD_LANDINGS_ERROR, message: "LANDING.ERROR.LOAD_ALL_ERROR"},
        fetchPolicy: options && options.fetchPolicy || (this.network.offline ? 'cache-only' : 'cache-and-network')
      })
        .pipe(
          filter(() => !this.loading)
        );
    }

    return $loadResult.pipe(
        // throttleTime(200), // avoid multiple call
        map(res => {
          const data = (res && res.landings || []).map(Landing.fromObject);
          const total = res && isNotNil(res.landingsCount) ? res.landingsCount : undefined;
          if (this._debug) {
            if (now) {
              console.debug(`[landing-service] Loaded {${data.length || 0}} landings in ${Date.now() - now}ms`, data);
              now = undefined;
            }
          }

          // Compute rankOrder, by tripId or observedLocationId
          if (dataFilter && (isNotNil(dataFilter.tripId) || isNotNil(dataFilter.observedLocationId))) {
            const asc = variables.sortDirection === 'asc';
            let rankOrder = asc ? 1 + offset : total - offset;
            // apply a sorted copy (do NOT change original order), then compute rankOrder
            data.slice().sort(sortByDateFn)
              .forEach(o => o.rankOrder = asc ? rankOrder++ : rankOrder--);

            // sort by rankOrderOnPeriod (aka id)
            if (!sortBy || sortBy === 'rankOrder') {
              const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
              data.sort((a, b) => {
                const valueA = a.rankOrder;
                const valueB = b.rankOrder;
                return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
              });
            }
          }

          return {
            data: data,
            total: total
          };
        }));
  }

  async load(id: number, options?: EditorDataServiceLoadOptions): Promise<Landing> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    const now = Date.now();
    if (this._debug) console.debug(`[landing-service] Loading landing {${id}}...`);
    this.loading = true;

    try {
      let json: any;

      // If local entity
      if (id < 0) {
        json = await this.entities.load<Landing>(id, Landing.TYPENAME);
      }

      else {
        // Load remotely
        const res = await this.graphql.query<{ landing: Landing }>({
          query: LoadQuery,
          variables: {
            id: id
          },
          error: {code: ErrorCodes.LOAD_LANDING_ERROR, message: "LANDING.ERROR.LOAD_ERROR"},
          fetchPolicy: options && options.fetchPolicy || undefined
        });
        json = res && res.landing;
      }

      // Transform to entity
      const data = Landing.fromObject(json);
      if (data && this._debug) console.debug(`[landing-service] landing #${id} loaded in ${Date.now() - now}ms`, data);
      return data;
    }
    finally {
      this.loading = false;
    }

  }

  async saveAll(entities: Landing[], options?: any): Promise<Landing[]> {
    if (!entities) return entities;

    const json = entities.map(t => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(t, options);
      return this.asObject(t);
    });

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Saving landings...", json);

    const res = await this.graphql.mutate<{ saveLandings: Landing[] }>({
      mutation: SaveAllQuery,
      variables: {
        landings: json
      },
      error: {code: ErrorCodes.SAVE_LANDINGS_ERROR, message: "LANDING.ERROR.SAVE_ALL_ERROR"},
      update: (proxy, {data}) => {

        if (this._debug) console.debug(`[landing-service] Landings saved remotely in ${Date.now() - now}ms`, entities);

        (res && res.saveLandings && entities || [])
          .forEach(entity => {
            const savedEntity = res.saveLandings.find(obj => entity.equals(obj));
            this.copyIdAndUpdateDate(savedEntity, entity);
          });

      }
    });

    return entities;
  }

  async save(entity: Landing): Promise<Landing> {

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Saving a landing...", entity);

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Reset the control date
    entity.controlDate = undefined;
    entity.validationDate = undefined;
    entity.qualificationDate = undefined;
    entity.qualityFlagId = undefined;

    // If new, create a temporary if (for offline mode)
    const isNew = isNil(entity.id);

    // If is a local entity: force a local save
    const offline = isNew ? (entity.synchronizationStatus && entity.synchronizationStatus !== 'SYNC') : entity.id < 0;
    if (offline) {
      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      // Reset synchro status
      entity.synchronizationStatus = 'DIRTY';

      const object = this.asObject(entity, SAVE_LOCALLY_AS_OBJECT_OPTIONS);
      if (this._debug) console.debug('[landing-service] [offline] Saving landing locally...', object);

      // Save response locally
      await this.entities.save(object);

      return entity;
    }

    // When offline, provide an optimistic response
    const offlineResponse = async (context) => {
      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      // For the query to be tracked (see tracked query link) with a unique serialization key
      context.tracked = (!entity.synchronizationStatus || entity.synchronizationStatus === 'SYNC');
      if (isNotNil(entity.id)) context.serializationKey = dataIdFromObject(entity);

      return { saveLandings: [this.asObject(entity, SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS)] };
    };

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[landing-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ saveLandings: any }>({
      mutation: SaveAllQuery,
      variables: {
        landings: [json]
      },
      offlineResponse,
      error: {code: ErrorCodes.SAVE_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.SAVE_ERROR"},
      update: async (proxy, {data}) => {
        const savedEntity = data && data.saveLandings && data.saveLandings[0];

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
          if (isNew && this._lastVariables.loadAll) {
            this.graphql.addToQueryCache(proxy, {
              query: LoadAllQuery,
              variables: this._lastVariables.loadAll
            }, 'landings', savedEntity);
          }
          else if (this._lastVariables.load) {
            this.graphql.updateToQueryCache(proxy, {
              query: LoadQuery,
              variables: this._lastVariables.load
            }, 'landing', savedEntity);
          }
        }
      }
    });

    return entity;
  }

  async delete(data: Landing): Promise<any> {
    if (!data) return;
    await this.deleteAll([data]);
  }

  async deleteAll(entities: Landing[], options?: any): Promise<any> {

    // Get local entity ids, then delete id
    const localIds = entities && entities
      .map(t => t.id)
      .filter(id => id < 0);
    if (isNotEmptyArray(localIds)) {
      if (this._debug) console.debug("[landing-service] Deleting landings locally... ids:", localIds);
      await this.entities.deleteMany<Landing>(localIds, 'LandingVO');
    }

    const remoteIds = entities && entities
      .map(t => t.id)
      .filter(id => id >= 0);
    if (isEmptyArray(remoteIds)) return; // stop, if nothing else to do

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Deleting landings... ids:", remoteIds);

    await this.graphql.mutate<any>({
      mutation: DeleteByIdsMutation,
      variables: {
        ids: remoteIds
      },
      update: (proxy) => {

        // Remove from cache
        if (this._lastVariables.loadAll) {
          this.graphql.removeToQueryCacheByIds(proxy, {
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'landings', remoteIds);
        }

        if (this._debug) console.debug(`[landing-service] Landings deleted in ${Date.now() - now}ms`);
      }
    });
  }


  listenChanges(id: number): Observable<Landing> {
    if (!id && id !== 0) throw new Error("Missing argument 'id'");

    if (this._debug) console.debug(`[landing-service] [WS] Listening changes for trip {${id}}...`);

    return this.graphql.subscribe<{ updateLanding: Landing }, { id: number, interval: number }>({
      query: UpdateSubscription,
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
          const data = res && res.updateLanding && Landing.fromObject(res.updateLanding);
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

  /* -- TODO implement this methods -- */
  async synchronize(data: Landing): Promise<Landing> {
    return data;
  }

  async control(data: Landing): Promise<FormErrors> {
    return undefined;
  }

  async terminate(data: Landing): Promise<Landing> {
    return data;
  }

  async validate(data: Landing): Promise<Landing> {
    return data;
  }

  async unvalidate(data: Landing): Promise<Landing> {
    return data;
  }

  async qualify(data: Landing, qualityFlagId: number): Promise<Landing> {
    return data;
  }

  executeImport(opts?: {
    maxProgression?: number;
  }): Observable<number> {
    return undefined;
  }

  async downloadToLocal(id: number, opts?: {
    withOperations?: boolean
  }): Promise<Trip> {
    return undefined;
  }

  /* -- private -- */

  protected asObject(entity: Landing, options?: DataEntityAsObjectOptions): any {
    options = {...MINIFY_OPTIONS, ...options};
    const copy: any = entity.asObject(options);

    if (options.minify && !options.keepEntityName && !options.keepTypename) {
      // Clean vessel features object, before saving
      copy.vesselSnapshot = {id: entity.vesselSnapshot && entity.vesselSnapshot.id};

      // Keep id only, on person and department
      copy.recorderPerson = {id: entity.recorderPerson && entity.recorderPerson.id};
      copy.recorderDepartment = entity.recorderDepartment && {id: entity.recorderDepartment && entity.recorderDepartment.id} || undefined;
    }

    return copy;
  }

  protected fillDefaultProperties(entity: Landing, options?: any) {
    super.fillDefaultProperties(entity);

    // Fill parent id, if not already set
    if (!entity.tripId && !entity.observedLocationId && options) {
      entity.observedLocationId = options.observedLocationId;
      entity.tripId = options.tripId;
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
  }

  protected copyIdAndUpdateDate(source: Landing | undefined, target: Landing) {
    super.copyIdAndUpdateDate(source, target);

    // Update samples (recursively)
    if (target.samples && source.samples) {
      this.copyIdAndUpdateDateOnSamples(source.samples, target.samples);
    }
  }

  /**
   * Copy Id and update, in sample tree (recursively)
   * @param sources
   * @param targets
   */
  copyIdAndUpdateDateOnSamples(sources: (Sample | any)[], targets: Sample[]) {
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

  // applyFilter(filter: LandingFilter, source: Landing): boolean {
  //   return !filter ||
  //     // Filter by parent
  //     (isNotNil(filter.observedLocationId) && filter.observedLocationId === source.observedLocationId) ||
  //     (isNotNil(filter.tripId) && filter.tripId === source.tripId) ||
  //     // Filter by search criteria
  //     (
  //       // Program
  //       (!filter.programLabel || filter.programLabel === source.program.label) &&
  //       // Location
  //       (isNil(filter.locationId) || (source.location && filter.locationId === source.location.id)) &&
  //       // start date
  //       (!filter.startDate || (source.dateTime && fromDateISOString(source.dateTime).isSameOrAfter(filter.startDate))) &&
  //       // end date
  //       (!filter.endDate || (source.dateTime && fromDateISOString(source.dateTime).isSameOrBefore(filter.endDate)))
  //     );
  // }
}

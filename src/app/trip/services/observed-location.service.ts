import {Injectable, Injector} from "@angular/core";
import {EntitiesServiceWatchOptions, EntityServiceLoadOptions, FilterFn, IEntitiesService, IEntityService, LoadResult} from "../../shared/services/entity-service.class";
import {AccountService} from "../../core/services/account.service";
import {Observable} from "rxjs";
import * as momentImported from "moment";
import {Moment} from "moment";
import {gql} from "@apollo/client/core";
import {DataFragments, Fragments} from "./trip.queries";
import {ErrorCodes} from "./trip.errors";
import {filter, map} from "rxjs/operators";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {SAVE_AS_OBJECT_OPTIONS, SAVE_LOCALLY_AS_OBJECT_OPTIONS} from "../../data/services/model/data-entity.model";
import {AppFormUtils, FormErrors} from "../../core/form/form.utils";
import {ObservedLocation} from "./model/observed-location.model";
import {Beans, isEmptyArray, isNil, isNotEmptyArray, isNotNil, KeysEnum, toNumber} from "../../shared/functions";
import {DataRootEntityUtils, SynchronizationStatus} from "../../data/services/model/root-data-entity.model";
import {SortDirection} from "@angular/material/sort";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {NetworkService} from "../../core/services/network.service";
import {IDataEntityQualityService} from "../../data/services/data-quality-service.class";
import {Entity} from "../../core/services/model/entity.model";
import {LandingFilter, LandingFragments, LandingService} from "./landing.service";
import {IDataSynchroService, RootDataSynchroService} from "../../data/services/root-data-synchro-service.class";
import {chainPromises} from "../../shared/observables";
import {Landing} from "./model/landing.model";
import {ObservedLocationValidatorService} from "./validator/observed-location.validator";
import {environment} from "../../../environments/environment";
import {JobUtils} from "../../shared/services/job.utils";
import {fromDateISOString, toDateISOString} from "../../shared/dates";
import {VesselSnapshotFragments} from "../../referential/services/vessel-snapshot.service";
import {OBSERVED_LOCATION_FEATURE_NAME} from "./config/trip.config";
import DurationConstructor = moment.unitOfTime.DurationConstructor;
import {ProgramProperties} from "../../referential/services/config/program.config";

const moment = momentImported;


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

export interface ObservedLocationLoadOptions extends EntityServiceLoadOptions {
  withLanding?: boolean;
  toEntity?: boolean;
}

export class ObservedLocationOfflineFilter  {
  programLabel?: string;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationId?: number;
  periodDuration?: number;
  periodDurationUnit?: DurationConstructor;
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
  }`
};

// Load query
const ObservedLocationQueries = {
  load: gql`query ObservedLocation($id: Int!) {
    data: observedLocation(id: $id) {
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}`,

  loadAll: gql`query ObservedLocations($filter: ObservedLocationFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $trash: Boolean){
    data: observedLocations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, trash: $trash){
      ...LightObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.lightObservedLocation}`,

  loadAllWithTotal: gql`query ObservedLocations($filter: ObservedLocationFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $trash: Boolean){
    data: observedLocations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, trash: $trash){
      ...LightObservedLocationFragment
    }
    total: observedLocationsCount(filter: $filter, trash: $trash)
  }
  ${ObservedLocationFragments.lightObservedLocation}`
};

const ObservedLocationMutations = {
  save: gql`mutation SaveObservedLocation($data: ObservedLocationVOInput!, $options: ObservedLocationSaveOptionsInput!){
    data: saveObservedLocation(observedLocation: $data, options: $options){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}`,

  saveWithLandings: gql`mutation SaveObservedLocationWithLandings($data: ObservedLocationVOInput!, $options: ObservedLocationSaveOptionsInput!){
    data: saveObservedLocation(observedLocation: $data, options: $options){
      ...ObservedLocationFragment
      landings {
        ...LandingFragment
      }
    }
  }
  ${ObservedLocationFragments.observedLocation}
  ${LandingFragments.landing}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}
  ${VesselSnapshotFragments.vesselSnapshot}
  ${DataFragments.sample}`,

  deleteAll: gql`mutation DeleteObservedLocations($ids:[Int]!){
    deleteObservedLocations(ids: $ids)
  }`,

  terminate: gql`mutation TerminateObservedLocation($data: ObservedLocationVOInput!){
    data: controlObservedLocation(observedLocation: $data){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}`,

  validate:  gql`mutation ValidateObservedLocation($data: ObservedLocationVOInput!){
    data: validateObservedLocation(observedLocation: $data){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}`,

  unvalidate: gql`mutation UnvalidateObservedLocation($data: ObservedLocationVOInput!){
    data: unvalidateObservedLocation(observedLocation: $data){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}`,

  qualify: gql`mutation QualifyObservedLocation($data: ObservedLocationVOInput!){
    data: qualifyObservedLocation(observedLocation: $data){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}`
};


const ObservedLocationSubscriptions = {
  listenChanges: gql`subscription UpdateObservedLocation($id: Int!, $interval: Int){
    updateObservedLocation(id: $id, interval: $interval) {
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}`
};

@Injectable({providedIn: "root"})
export class ObservedLocationService
  extends RootDataSynchroService<ObservedLocation, ObservedLocationFilter, ObservedLocationLoadOptions>
  implements
    IEntitiesService<ObservedLocation, ObservedLocationFilter>,
    IEntityService<ObservedLocation, ObservedLocationLoadOptions>,
    IDataEntityQualityService<ObservedLocation>,
    IDataSynchroService<ObservedLocation, ObservedLocationLoadOptions>
{

  protected loading = false;

  constructor(
    injector: Injector,
    protected graphql: GraphqlService,
    protected accountService: AccountService,
    protected network: NetworkService,
    protected entities: EntitiesStorage,
    protected validatorService: ObservedLocationValidatorService,
    protected landingService: LandingService
  ) {
    super(injector, ObservedLocation, {
      queries: ObservedLocationQueries,
      mutations: ObservedLocationMutations,
      subscriptions: ObservedLocationSubscriptions,
      filterFnFactory: ObservedLocationFilter.asPodObject,
      filterAsObjectFn: ObservedLocationFilter.searchFilter
    });

    this._featureName = OBSERVED_LOCATION_FEATURE_NAME;

    // FOR DEV ONLY
    this._debug = !environment.production;
    if (this._debug) console.debug('[observed-location-service] Creating service');
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

    return this.mutableWatchQuery<{ data: ObservedLocation[]; total: number }>({
        queryName: 'LoadAll',
        query: this.queries.loadAllWithTotal,
        arrayFieldName: 'data',
        totalFieldName: 'total',
        insertFilterFn: ObservedLocationFilter.searchFilter(dataFilter),
        variables: variables,
        error: {code: ErrorCodes.LOAD_OBSERVED_LOCATIONS_ERROR, message: "ERROR.LOAD_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || 'cache-and-network'
      })
      .pipe(
        filter(() => !this.loading),
        map(({data, total}) => {
          const entities = (data || []).map(ObservedLocation.fromObject);
          if (now) {
            console.debug(`[observed-location-service] Loaded {${entities.length || 0}} observed locations in ${Date.now() - now}ms`, entities);
            now = undefined;
          } else {
            console.debug(`[observed-location-service] Refreshed {${entities.length || 0}} observed locations`);
          }
          return {data: entities, total};
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
      filter: ObservedLocationFilter.searchFilter<ObservedLocation>(dataFilter)
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

  async load(id: number, opts?: ObservedLocationLoadOptions): Promise<ObservedLocation> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    const now = Date.now();
    if (this._debug) console.debug(`[observed-location-service] Loading observed location {${id}}...`);
    this.loading = true;

    try {
      let data: any;

      // If local entity
      if (id < 0) {
        data = await this.entities.load<ObservedLocation>(id, ObservedLocation.TYPENAME);
        if (!data) throw {code: ErrorCodes.LOAD_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ERROR"};

        if (opts && opts.withLanding) {
          data.landings = await this.entities.loadAll<Landing>(Landing.TYPENAME, {
            filter: LandingFilter.searchFilter<Landing>({observedLocationId: id})
          });
        }
      }

      else {
        const res = await this.graphql.query<{ data: ObservedLocation }>({
          query: this.queries.load,
          variables: { id },
          error: {code: ErrorCodes.LOAD_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ERROR"},
          fetchPolicy: opts && opts.fetchPolicy || undefined
        });
        data = res && res.data;
      }
      const entities = (!opts || opts.toEntity !== false)
        ? ObservedLocation.fromObject(data)
        : (data as ObservedLocation);

      if (entities && this._debug) console.debug(`[observed-location-service] Observed location #${id} loaded in ${Date.now() - now}ms`, entities);

      return entities;
    }
    finally {
      this.loading = false;
    }
  }

  public listenChanges(id: number, opts?: { interval?: number }): Observable<ObservedLocation> {
    if (!id && id !== 0) throw new Error("Missing argument 'id' ");

    if (this._debug) console.debug(`[observed-location-service] [WS] Listening changes for observedLocation {${id}}...`);

    return this.graphql.subscribe<{ data: ObservedLocation }, { id: number, interval: number }>({
      query: this.subscriptions.listenChanges,
      variables: { id, interval: toNumber(opts && opts.interval, 10) },
      error: {
        code: ErrorCodes.SUBSCRIBE_OBSERVED_LOCATION_ERROR,
        message: 'ERROR.SUBSCRIBE_ERROR'
      }
    })
      .pipe(
        map(({data}) => {
          const entity = data && ObservedLocation.fromObject(data);
          if (entity && this._debug) console.debug(`[observed-location-service] Observed location {${id}} updated on server !`, entity);
          return entity;
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

    opts = {
      withLanding: false,
      ...opts
    };

    const now = Date.now();
    if (this._debug) console.debug("[observed-location-service] Saving an observed location...");

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Reset quality properties
    this.resetQualityProperties(entity);

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (isNew) delete json.id; // Make to remove temporary id, before sending to graphQL
    if (this._debug) console.debug("[observed-location-service] Using minify object, to send:", json);

    const variables = {
      data: json,
      options: {
        withLanding: opts.withLanding
      }
    };

    const mutation = opts.withLanding ? ObservedLocationMutations.saveWithLandings : this.mutations.save;
    await this.graphql.mutate<{ data: ObservedLocation }>({
      mutation,
      variables,
      error: {code: ErrorCodes.SAVE_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.SAVE_ERROR"},
      update: (proxy, {data}) => {
        const savedEntity = data && data.data;
        if (savedEntity !== entity) {
          if (this._debug) console.debug(`[observed-location-service] Observed location saved in ${Date.now() - now}ms`, entity);
          this.copyIdAndUpdateDate(savedEntity, entity);
        }

        // Add to cache
        if (isNew) {
          this.insertIntoMutableCachedQuery(proxy, {
            queryName: 'LoadAll',
            data: savedEntity
          });
        }
      }
    });

    // Update date of children entities, if need (see IMAGINE-276)
    if (!isNew) {
      await this.updateChildrenDate(entity);
    }

    return entity;
  }

  async saveLocally(entity: ObservedLocation, opts?: ObservedLocationSaveOptions): Promise<ObservedLocation> {
    if (isNotNil(entity.id) && entity.id >= 0) throw new Error('Must be a local entity');
    opts = {
      withLanding: false,
      ...opts
    };

    const isNew = isNil(entity.id);

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

      const program = await this.programRefService.loadByLabel(entity.program.label);
      const landingHasDateTime = program.getPropertyAsBoolean(ProgramProperties.LANDING_DATE_TIME_ENABLE);

      landings.forEach(l => {
        l.id = null; // Clean ID, to force new ids
        l.observedLocationId = entity.id; // Link to parent entity
        l.updateDate = undefined;

        // Copy date to landing and samples (IMAGINE-276)
        if (!landingHasDateTime) {
         l.dateTime = entity.startDateTime;
          (l.samples || []).forEach(s => {
            s.sampleDate = l.dateTime;
          });
        }
      });

      // Save landings
      entity.landings = await this.landingService.saveAll(landings, {observedLocationId: entity.id});
    }

    // Update date of children entities, if need (see IMAGINE-276)
    else if (!opts.withLanding && !isNew) {
      await this.updateChildrenDate(entity);
    }

    return entity;
  }

  /**
   * Delete many observations
   * @param entities
   * @param opts
   */
  async deleteAll(entities: ObservedLocation[], opts?: {
    trash?: boolean; // True by default
  }): Promise<any> {

    // Delete local entities
    const localEntities = entities && entities.filter(DataRootEntityUtils.isLocal);
    if (isNotEmptyArray(localEntities)) {
      return this.deleteAllLocally(localEntities, opts);
    }

    const ids = entities && entities.map(t => t.id)
      .filter(id => id >= 0);
    if (isEmptyArray(ids)) return; // stop if empty

    const now = Date.now();
    if (this._debug) console.debug(`[observed-location-service] Deleting {${ids.join(',')}}`, ids);

    await this.graphql.mutate<any>({
      mutation: this.mutations.deleteAll,
      variables: { ids },
      update: (proxy) => {
        // Update the cache
        this.removeFromMutableCachedQueryByIds(proxy, {
          queryName: 'LoadAll',
          ids
        });

        if (this._debug) console.debug(`[observed-location-service] Observed locations deleted in ${Date.now() - now}ms`);

      }
    });
  }

  /**
   * Delete many local entities
   * @param entities
   * @param opts
   */
  async deleteAllLocally(entities: ObservedLocation[], opts?: {
    trash?: boolean; // True by default
  }): Promise<any> {

    // Get local entity ids, then delete id
    const localEntities = entities && entities
      .filter(DataRootEntityUtils.isLocal);

    if (isEmptyArray(localEntities)) return; // Skip if empty

    const trash = !opts || opts !== false;
    const trashUpdateDate = trash && moment();

    if (this._debug) console.debug(`[observedLocation-service] Deleting locally... {trash: ${trash}`);

    await chainPromises(localEntities.map(entity => async () => {

      // Load children
      const res = await this.landingService.loadAllByObservedLocation({observedLocationId: entity.id},
        {fullLoad: true, computeRankOrder: false});
      const landings = res && res.data;

      await this.entities.delete(entity, {entityName: ObservedLocation.TYPENAME});

      if (isNotNil(landings)) {
        await this.landingService.deleteAll(landings, {trash: false});
      }

      if (trash) {
        // Fill observedLocation's operation, before moving it to trash
        entity.landings = landings;
        entity.updateDate = trashUpdateDate;

        const json = entity.asObject({...SAVE_LOCALLY_AS_OBJECT_OPTIONS, keepLocalId: false});

        // Add to trash
        await this.entities.saveToTrash(json, {entityName: ObservedLocation.TYPENAME});
      }

    }));
  }

  async control(entity: ObservedLocation): Promise<FormErrors> {

    const now = this._debug && Date.now();
    if (this._debug) console.debug(`[observed-location-service] Control {${entity.id}}...`, entity);

    const programLabel = entity.program && entity.program.label || null;
    if (!programLabel) throw new Error("Missing entity's program. Unable to control the entity");
    const program = await this.programRefService.loadByLabel(programLabel);

    const form = this.validatorService.getFormGroup(entity, {
      program,
      withMeasurements: true // Need by full validation
    });

    if (!form.valid) {
      // Wait end of validation (e.g. async validators)
      await AppFormUtils.waitWhilePending(form);

      // Get form errors
      if (form.invalid) {
        const errors = AppFormUtils.getFormErrors(form, 'observedLocation');

        if (this._debug) console.debug(`[observed-location-service] Control {${entity.id}} [INVALID] in ${Date.now() - now}ms`, errors);

        return errors;
      }
    }

    if (this._debug) console.debug(`[observed-location-service] Control ${entity.id}} [OK] in ${Date.now() - now}ms`);

    return undefined;
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
        message: "ERROR.SYNCHRONIZE_ERROR",
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

  /**
   * List of importation jobs.
   * @protected
   * @param opts
   */
  protected getImportJobs(opts: {
    maxProgression: undefined;
  }): Observable<number>[] {

    const feature = this.settings.getOfflineFeature(this.featureName);
    if (feature && feature.filter) {
      const landingFilter = {
        ...feature.filter,
        // Remove unused attribute
        periodDuration: undefined,
        periodDurationUnit: undefined
      };
      return [
        ...super.getImportJobs(opts),
        // Landing (historical data)
        JobUtils.defer((p, o) => this.landingService.executeImport(p, {
          ...o,
          filter: landingFilter
        }), opts)
      ];
    }
    else {
      return super.getImportJobs(opts);
    }
  }

  protected async updateChildrenDate(entity: ObservedLocation) {
    if (!entity || !entity.program || !entity.program.label || !entity.startDateTime) return; // Skip

    const program = await this.programRefService.loadByLabel(entity.program.label);
    const landingHasDateTime = program.getPropertyAsBoolean(ProgramProperties.LANDING_DATE_TIME_ENABLE);
    if (landingHasDateTime) return; // Not need to update children dates

    const now = Date.now();
    console.info("[observed-location-service] Applying date to children entities (Landing, Sample)...");

    try {
      let res: LoadResult<Landing>;
      let offset = 0;
      const size = 10; // Use paging, to avoid loading ALL landings once
      do {
        res = await this.landingService.loadAll(offset, size, null, null, {observedLocationId: entity.id}, {fullLoad: true});

        const updatedLandings = (res.data || []).map(l => {
          if (!l.dateTime || !l.dateTime.isSame(entity.startDateTime)) {
            l.dateTime = entity.startDateTime;
            (l.samples || []).forEach(sample => {
              sample.sampleDate = l.dateTime;
            });
            return l;
          }
          return undefined;
        }).filter(isNotNil);

        // Save landings, if need
        if (isNotEmptyArray(updatedLandings)) {
          await this.landingService.saveAll(updatedLandings, {observedLocationId: entity.id, enableOptimisticResponse: false});
        }

        offset += size;
      } while (offset < res.total);

      console.info(`[observed-location-service] Applying date to children entities (Landing, Sample) [OK] in ${Date.now() - now}ms`);
    }
    catch (err) {
      throw {
        ...err,
        code: ErrorCodes.UPDATE_OBSERVED_LOCATION_CHILDREN_DATE_ERROR,
        message: "OBSERVED_LOCATION.ERROR.UPDATE_CHILDREN_DATE_ERROR"
      };
    }
  }

}

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
import * as moment from "moment";
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
import {AppFormUtils, FormErrors} from "../../core/form/form.utils";
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
import {
  DataRootEntityUtils,
  SynchronizationStatus,
  SynchronizationStatusEnum
} from "../../data/services/model/root-data-entity.model";
import {SortDirection} from "@angular/material/sort";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {NetworkService} from "../../core/services/network.service";
import {IDataEntityQualityService} from "../../data/services/data-quality-service.class";
import {Entity} from "../../core/services/model/entity.model";
import {LandingFilter, LandingService} from "./landing.service";
import {IDataSynchroService, RootDataSynchroService} from "../../data/services/data-synchro-service.class";
import {chainPromises} from "../../shared/observables";
import {MINIFY_OPTIONS} from "../../core/services/model/referential.model";
import {Landing} from "./model/landing.model";
import {ObservedLocationValidatorService} from "./validator/observed-location.validator";
import {environment} from "../../../environments/environment";
import {TripFragments} from "./trip.service";
import {RootEntityMutations} from "./root-data-service.class";


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
// Save query
const SaveQuery: any = gql`
  mutation SaveObservedLocation($observedLocation: ObservedLocationVOInput, $options: ObservedLocationSaveOptionsInput!){
    saveObservedLocation(observedLocation: $observedLocation, options: $options){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;

const ControlMutation: any = gql`
  mutation TerminateObservedLocation($entity: ObservedLocationVOInput!){
    entity: controlObservedLocation(observedLocation: $entity){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;
const ValidateMutation: any = gql`
  mutation ValidateObservedLocation($entity: ObservedLocationVOInput!){
    entity: validateObservedLocation(observedLocation: $entity){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;
const QualifyMutation: any = gql`
  mutation QualifyObservedLocation($entity: ObservedLocationVOInput!){
    entity: qualifyObservedLocation(observedLocation: $entity){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;
const UnvalidateMutation: any = gql`
  mutation UnvalidateObservedLocation($entity: ObservedLocationVOInput!){
    entity: unvalidateObservedLocation(observedLocation: $entity){
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
    super(injector, {
      terminate: ControlMutation,
      validate: ValidateMutation,
      unvalidate: UnvalidateMutation,
      qualify: QualifyMutation
    });

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
      let json: any;

      // If local entity
      if (id < 0) {
        json = await this.entities.load<ObservedLocation>(id, ObservedLocation.TYPENAME);
        if (!json) throw {code: ErrorCodes.LOAD_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ERROR"};

        if (opts && opts.withLanding) {
          json.landings = await this.entities.loadAll<Landing>('OperationVO', {
            filter: LandingFilter.searchFilter<Landing>({observedLocationId: id})
          });
        }
      }

      else {
        const res = await this.graphql.query<{ observedLocation: ObservedLocation }>({
          query: LoadQuery,
          variables: {
            id: id
          },
          error: {code: ErrorCodes.LOAD_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ERROR"},
          fetchPolicy: opts && opts.fetchPolicy || undefined
        });
        json = res && res.observedLocation;
      }
      const data: ObservedLocation = (!opts || opts.toEntity !== false) ? ObservedLocation.fromObject(json) : (json as ObservedLocation);
      if (data && this._debug) console.debug(`[observed-location-service] Observed location #${id} loaded in ${Date.now() - now}ms`, data);

      return data;
    }
    finally {
      this.loading = false;
    }
  }

  public listenChanges(id: number): Observable<ObservedLocation> {
    if (!id && id !== 0) throw new Error("Missing argument 'id' ");

    if (this._debug) console.debug(`[observed-location-service] [WS] Listening changes for observedLocation {${id}}...`);

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
      observedLocation: json,
      options: {
        withLanding: opts.withLanding
      }
    };

    const mutation = (opts.withLanding) ? SaveQuery : SaveQuery;
    await this.graphql.mutate<{ saveObservedLocation: ObservedLocation }>({
      mutation,
      variables,
      error: {code: ErrorCodes.SAVE_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.SAVE_ERROR"},
      update: (proxy, {data}) => {
        const savedEntity = data && data.saveObservedLocation;
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
    if (!data) return; // skip
    await this.deleteAll([data]);
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
    const program = await this.programService.loadByLabel(programLabel);

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

}

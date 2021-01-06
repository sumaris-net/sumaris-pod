import {Inject, Injectable, Injector, Optional} from "@angular/core";
import {gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {catchError, filter, map, switchMap, tap} from "rxjs/operators";
import * as momentImported from "moment";
import {Moment} from "moment";
import {ErrorCodes} from "./trip.errors";
import {AccountService} from "../../core/services/account.service";
import {DataFragments, Fragments, OperationGroupFragment, PhysicalGearFragments, SaleFragments} from "./trip.queries";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {RootDataService} from "./root-data-service.class";
import {
  COPY_LOCALLY_AS_OBJECT_OPTIONS,
  DataEntityAsObjectOptions,
  SAVE_AS_OBJECT_OPTIONS,
  SAVE_LOCALLY_AS_OBJECT_OPTIONS,
  SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS
} from "../../data/services/model/data-entity.model";
import {NetworkService} from "../../core/services/network.service";
import {concat, defer, Observable, of, timer} from "rxjs";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
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
import {DataQualityService} from "../../data/services/base.service";
import {OperationFilter, OperationService} from "./operation.service";
import {VesselSnapshotFragments, VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {PersonService} from "../../admin/services/person.service";
import {ProgramService} from "../../referential/services/program.service";
import {chainPromises} from "../../shared/observables";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {TripValidatorService} from "./validator/trip.validator";
import {AppFormUtils, FormErrors} from "../../core/form/form.utils";
import {Operation, PhysicalGear, Trip} from "./model/trip.model";
import {
  DataRootEntityUtils,
  SynchronizationStatus,
  SynchronizationStatusEnum
} from "../../data/services/model/root-data-entity.model";
import {fillRankOrder, IWithRecorderDepartmentEntity} from "../../data/services/model/model.utils";
import {MINIFY_OPTIONS} from "../../core/services/model/referential.model";
import {SortDirection} from "@angular/material/sort";
import {
  EntitiesService,
  EntityService,
  EntityServiceLoadOptions,
  FilterFn,
  LoadResult
} from "../../shared/services/entity-service.class";
import {UserEventService} from "../../social/services/user-event.service";
import {ShowToastOptions, Toasts} from "../../shared/toasts";
import {OverlayEventDetail} from "@ionic/core";
import {TranslateService} from "@ngx-translate/core";
import {ToastController} from "@ionic/angular";
import {TrashRemoteService} from "../../core/services/trash-remote.service";
import {TRIP_FEATURE_NAME} from "./config/trip.config";
import {Entity, EntityUtils} from "../../core/services/model/entity.model";
import {Department} from "../../core/services/model/department.model";
import {EnvironmentService} from "../../../environments/environment.class";

const moment = momentImported;

export const TripFragments = {
  lightTrip: gql`fragment LightTripFragment on TripVO {
    id
    program {
      id
      label
    }
    departureDateTime
    returnDateTime
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    qualityFlagId
    comments
    departureLocation {
      ...LocationFragment
    }
    returnLocation {
      ...LocationFragment
    }
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
  trip: gql`fragment TripFragment on TripVO {
    id
    program {
      id
      label
    }
    departureDateTime
    returnDateTime
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    qualityFlagId
    comments
    departureLocation {
      ...LocationFragment
    }
    returnLocation {
      ...LocationFragment
    }
    vesselSnapshot {
      ...LightVesselSnapshotFragment
    }
    sale {
      ...LightSaleFragment
    }
    gears {
      ...PhysicalGearFragment
    }
    measurements {
      ...MeasurementFragment
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
    metiers {
      ...MetierFragment
    }
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.measurement}
  ${Fragments.referential}
  ${Fragments.location}
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${PhysicalGearFragments.physicalGear}
  ${Fragments.metier},
  ${SaleFragments.lightSale}
  `,
  landedTrip: gql`fragment LandedTripFragment on TripVO {
    id
    program {
      id
      label
    }
    departureDateTime
    returnDateTime
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    qualityFlagId
    comments
    landing {
      id
      rankOrderOnVessel
    }
    observedLocationId
    departureLocation {
      ...LocationFragment
    }
    returnLocation {
      ...LocationFragment
    }
    vesselSnapshot {
      ...LightVesselSnapshotFragment
    }
    gears {
      ...PhysicalGearFragment
    }
    measurements {
      ...MeasurementFragment
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
    metiers {
      ...MetierFragment
    }
    operationGroups {
      ...OperationGroupFragment
    }
    sale {
      ...SaleFragment
    }
    fishingArea {
      ...FishingAreaFragment
    }
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.measurement}
  ${Fragments.referential}
  ${Fragments.location}
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${Fragments.metier}
  ${OperationGroupFragment.operationGroup}
  ${SaleFragments.sale}
  ${DataFragments.fishingArea}
  `
};


export class TripFilter {
  programLabel?: string;
  vesselId?: number;
  locationId?: number;
  startDate?: Date | Moment | string;
  endDate?: Date | Moment | string;
  recorderDepartmentId?: number;
  recorderPersonId?: number;
  synchronizationStatus?: SynchronizationStatus;

  static isEmpty(f: TripFilter|any): boolean {
    return Beans.isEmpty<TripFilter>({...f, synchronizationStatus: null}, TripFilterKeys, {
      blankStringLikeEmpty: true
    });
  }

  static searchFilter<T extends Trip>(f: TripFilter): (T) => boolean {
    if (!f) return undefined;

    const filterFns: FilterFn<T>[] = [];

    // Program
    if (f.programLabel) {
      filterFns.push(t => (t.program && t.program.label === f.programLabel));
    }

    // Vessel
    if (f.vesselId) {
      filterFns.push(t => (t.vesselSnapshot && t.vesselSnapshot.id === f.vesselId));
    }

    // Location
    if (isNotNil(f.locationId)) {
      filterFns.push(t => ((t.departureLocation && t.departureLocation.id === f.locationId) || (t.returnLocation && t.returnLocation.id === f.locationId)));
    }

    // Start/end period
    const startDate = fromDateISOString(f.startDate);
    let endDate = fromDateISOString(f.endDate);
    if (startDate) {
      filterFns.push(t => t.returnDateTime ? startDate.isSameOrBefore(t.returnDateTime) : startDate.isSameOrBefore(t.departureDateTime));
    }
    if (endDate) {
      endDate = endDate.add(1, 'day');
      filterFns.push(t => t.departureDateTime && endDate.isAfter(t.departureDateTime));
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
  static asPodObject(f: TripFilter): any {
    if (!f) return f;
    return {
      ...f,
      // Convert dates to string
      startDate: toDateISOString(f.startDate),
      endDate: toDateISOString(f.endDate),
      // Remove fields that not exists in pod
      synchronizationStatus: undefined
    };
  }
}

export const TripFilterKeys: KeysEnum<TripFilter> = {
  programLabel: true,
  vesselId: true,
  startDate: true,
  endDate: true,
  locationId: true,
  recorderDepartmentId: true,
  recorderPersonId: true,
  synchronizationStatus: true
};

export interface TripServiceLoadOptions extends EntityServiceLoadOptions {
  isLandedTrip?: boolean;
  withOperation?: boolean;
  withOperationGroup?: boolean;
  toEntity?: boolean;
}

export interface TripServiceSaveOptions {
  withLanding?: boolean;
  withOperation?: boolean;
  withOperationGroup?: boolean;
  enableOptimisticResponse?: boolean; // True by default
}

export interface TripServiceCopyOptions extends TripServiceSaveOptions {
  keepRemoteId?: boolean;
  deletedFromTrash?: boolean;
  displaySuccessToast?: boolean;
}

const LoadAllQuery: any = gql`
  query Trips($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $trash: Boolean, $filter: TripFilterVOInput){
    trips(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, trash: $trash){
      ...LightTripFragment
    }
    tripsCount(filter: $filter, trash: $trash)
  }
  ${TripFragments.lightTrip}
`;
// Load a trip
const LoadTripQuery: any = gql`
  query Trip($id: Int!) {
    trip(id: $id) {
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
// Load a landed trip
const LoadLandedTripQuery: any = gql`
  query Trip($id: Int!) {
    trip(id: $id) {
      ...LandedTripFragment
    }
  }
  ${TripFragments.landedTrip}
`;
// Save a trip
const SaveTripQuery: any = gql`
  mutation saveTrip($trip:TripVOInput, $tripSaveOption: TripSaveOptionsInput!){
    saveTrip(trip: $trip, saveOptions: $tripSaveOption){
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
// Save a landed trip
const SaveLandedTripQuery: any = gql`
  mutation saveTrip($trip:TripVOInput, $tripSaveOption: TripSaveOptionsInput!){
    saveTrip(trip: $trip, saveOptions: $tripSaveOption){
      ...LandedTripFragment
    }
  }
  ${TripFragments.landedTrip}
`;
const ControlMutation: any = gql`
  mutation ControlTrip($trip:TripVOInput){
    controlTrip(trip: $trip){
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
const ValidateMutation: any = gql`
  mutation ValidateTrip($trip:TripVOInput){
    validateTrip(trip: $trip){
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
const QualifyMutation: any = gql`
  mutation QualifyTrip($trip:TripVOInput){
    qualifyTrip(trip: $trip){
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
const UnvalidateMutation: any = gql`
  mutation UnvalidateTrip($trip:TripVOInput){
    unvalidateTrip(trip: $trip){
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
const DeleteByIdsMutation: any = gql`
  mutation DeleteTrips($ids:[Int]){
    deleteTrips(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription UpdateTrip($id: Int!, $interval: Int){
    updateTrip(id: $id, interval: $interval) {
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;


@Injectable({providedIn: 'root'})
export class TripService extends RootDataService<Trip, TripFilter>
  implements
    EntitiesService<Trip, TripFilter>,
    EntityService<Trip, TripServiceLoadOptions>,
    DataQualityService<Trip> {

  protected $importationProgress: Observable<number>;
  protected loading = false;

  constructor(
    injector: Injector,
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected referentialRefService: ReferentialRefService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected personService: PersonService,
    protected programService: ProgramService,
    protected entities: EntitiesStorage,
    protected operationService: OperationService,
    protected settings: LocalSettingsService,
    protected validatorService: TripValidatorService,
    protected userEventService: UserEventService,
    protected trashRemoteService: TrashRemoteService,
    @Inject(EnvironmentService) protected environment,
    @Optional() private translate: TranslateService,
    @Optional() private toastController: ToastController
  ) {
    super(injector);

    // Register user event actions
    userEventService.registerAction({
      __typename: Trip.TYPENAME,
      matIcon: "content_copy",
      name: 'copyToLocal',
      title: 'SOCIAL.USER_EVENT.BTN_COPY_TO_LOCAL',
      color: 'primary',
      executeAction: (event, context) => this.copyLocally(Trip.fromObject(context), {displaySuccessToast: true})
    });

    // FOR DEV ONLY
    this._debug = !environment.production;
    if (this._debug)console.debug('[trip-service] Creating service');
  }

  /**
   * Load many trips
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param dataFilter
   * @param options
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           dataFilter?: TripFilter,
           options?: {
             fetchPolicy?: WatchQueryFetchPolicy;
             trash?: boolean;
           }): Observable<LoadResult<Trip>> {
    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'departureDateTime',
      sortDirection: sortDirection || 'asc',
      trash: options && options.trash || false,
      filter: TripFilter.asPodObject(dataFilter)
    };

    let now = this._debug && Date.now();
    let $loadResult: Observable<{ trips: Trip[]; tripsCount?: number; }>;
    if (this._debug) console.debug("[trip-service] Watching trips... using options:", variables);

    // Offline
    const offline = this.network.offline || (dataFilter && dataFilter.synchronizationStatus && dataFilter.synchronizationStatus !== 'SYNC') || false;
    if (offline) {
      $loadResult = this.entities.watchAll<Trip>(Trip.TYPENAME, {
          ...variables,
          filter: TripFilter.searchFilter<Trip>(dataFilter)
        })
        .pipe(
          map(res => {
            return {trips: res && res.data, tripsCount: res && res.total};
          }));
    }
    else {
      $loadResult = this.mutableWatchQuery<{ trips: Trip[]; tripsCount: number; }>({
        queryName: 'LoadAll',
        query: LoadAllQuery,
        arrayFieldName: 'trips',
        totalFieldName: 'tripsCount',
        insertFilterFn: TripFilter.searchFilter(dataFilter),
        variables,
        error: { code: ErrorCodes.LOAD_TRIPS_ERROR, message: "TRIP.ERROR.LOAD_TRIPS_ERROR" },
        fetchPolicy: options && options.fetchPolicy || 'cache-and-network'
      })
        .pipe(
          filter(() => !this.loading)
        );
    }

    return $loadResult.pipe(
        map(res => {
          const data = (res && res.trips || []).map(Trip.fromObject);
          const total = res && isNotNil(res.tripsCount) ? res.tripsCount : undefined;
          if (now) {
            console.debug(`[trip-service] Loaded {${data.length || 0}} trips in ${Date.now() - now}ms`, data);
            now = undefined;
          }else {
            console.debug(`[trip-service] Refreshed {${data.length || 0}} trips`);
          }
          return {data, total};
        })
      );
  }

  async load(id: number, opts?: TripServiceLoadOptions): Promise<Trip | null> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    // use landedTrip option if itself or withOperationGroups is present in service options
    const isLandedTrip = opts && (opts.isLandedTrip || opts.withOperationGroup);

    const now = this._debug && Date.now();
    if (this._debug) console.debug(`[trip-service] Loading trip #${id}...`);
    this.loading = true;

    try {
      let json: any;

      // If local entity
      if (id < 0) {
        json = await this.entities.load<Trip>(id, Trip.TYPENAME);
        if (!json) throw {code: ErrorCodes.LOAD_TRIP_ERROR, message: "TRIP.ERROR.LOAD_TRIP_ERROR"};

        if (opts && opts.withOperation) {
          json.operations = await this.entities.loadAll<Operation>('OperationVO', {
            filter: OperationFilter.searchFilter<Operation>({tripId: id})
          });
        }
      }

      else {
        const query = isLandedTrip === true ? LoadLandedTripQuery : LoadTripQuery;

        // Load remotely
        const res = await this.graphql.query<{ trip: Trip }>({
          query,
          variables: {
            id: id
          },
          error: { code: ErrorCodes.LOAD_TRIP_ERROR, message: "TRIP.ERROR.LOAD_TRIP_ERROR" },
          fetchPolicy: opts && opts.fetchPolicy || undefined
        });
        json = res && res.trip;
      }

      // Transform to entity
      const data: Trip = (!opts || opts.toEntity !== false) ? Trip.fromObject(json) : (json as Trip);

      if (data && this._debug) console.debug(`[trip-service] Trip #${id} loaded in ${Date.now() - now}ms`, data);
      return data;
    }
    finally {
      this.loading = false;
    }
  }

  async hasOfflineData(): Promise<boolean> {
    const res = await this.entities.loadAll('TripVO', {
      offset: 0,
      size: 0
    });
    return res && res.total > 0;
  }

  listenChanges(id: number): Observable<Trip> {
    if (isNil(id)) throw new Error("Missing argument 'id' ");

    if (this._debug) console.debug(`[trip-service] [WS] Listening changes for trip {${id}}...`);

    return this.graphql.subscribe<{ updateTrip: Trip }, { id: number, interval: number }>({
      query: UpdateSubscription,
      variables: {
        id: id,
        interval: 10
      },
      error: {
        code: ErrorCodes.SUBSCRIBE_TRIP_ERROR,
        message: 'TRIP.ERROR.SUBSCRIBE_TRIP_ERROR'
      }
    })
      .pipe(
        map(res => {
          const data = res && res.updateTrip && Trip.fromObject(res.updateTrip);
          if (data && this._debug) console.debug(`[trip-service] Trip {${id}} updated on server !`, data);
          return data;
        })
      );
  }

  /**
   * Save many trips
   * @param entities
   * @param opts
   */
  async saveAll(entities: Trip[], opts?: TripServiceSaveOptions): Promise<Trip[]> {
    if (isEmptyArray(entities)) return entities;

    if (this._debug) console.debug(`[trip-service] Saving ${entities.length} trips...`);
    const jobsFactories = (entities || []).map(entity => () => this.save(entity, {...opts}));
    return chainPromises<Trip>(jobsFactories);
  }

  /**
   * Save a trip
   * @param entity
   * @param opts
   */
  async save(entity: Trip, opts?: TripServiceSaveOptions): Promise<Trip> {
    const isNew = isNil(entity.id);

    // If is a local entity: force a local save
    const isLocal = isNew ? (entity.synchronizationStatus && entity.synchronizationStatus !== 'SYNC') : entity.id < 0;
    if (isLocal) {
      return this.saveLocally(entity, opts);
    }

    opts = {
      withLanding: false,
      withOperation: false,
      withOperationGroup: false,
      ...opts
    };

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Saving trip...", entity);

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Reset the control date
    entity.controlDate = undefined;
    entity.validationDate = undefined;
    entity.qualificationDate = undefined;
    entity.qualityFlagId = undefined;

    // Provide an optimistic response, if connection lost
    const offlineResponse = (!opts || opts.enableOptimisticResponse !== false) ?
      async (context) => {
        // Make sure to fill id, with local ids
        await this.fillOfflineDefaultProperties(entity);

        // For the query to be tracked (see tracked query link) with a unique serialization key
        context.tracked = (!entity.synchronizationStatus || entity.synchronizationStatus === 'SYNC');
        if (isNotNil(entity.id)) context.serializationKey = `${Trip.TYPENAME}:${entity.id}`;

        return {
          saveTrip: !opts.withLanding && [this.asObject(entity, SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS)],
          saveLandedTrip: opts.withLanding && [this.asObject(entity, SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS)]
        };
      } : undefined;

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[trip-service] Using minify object, to send:", json);

    const variables = {
      trip: json,
      tripSaveOption: {
        withLanding: opts.withLanding,
        withOperation: opts.withOperation,
        withOperationGroup: opts.withOperationGroup
      }
    };
    const mutation = (opts.withLanding) ? SaveLandedTripQuery : SaveTripQuery;
    await this.graphql.mutate<{ saveTrip: any, saveLandedTrip: any }>({
       mutation,
       variables,
       offlineResponse,
       error: { code: ErrorCodes.SAVE_TRIP_ERROR, message: "TRIP.ERROR.SAVE_TRIP_ERROR" },
       update: async (cache, {data}) => {
         const savedEntity = data && (data.saveTrip || data.saveLandedTrip);

         // Local entity: save it
         if (savedEntity.id < 0) {
           if (this._debug) console.debug('[trip-service] [offline] Saving trip locally...', savedEntity);

           // Save response locally
           await this.entities.save<Trip>(savedEntity);
         }

         // Update the entity and update GraphQL cache
         else {

           // Remove existing entity from the local storage
           if (entity.id < 0 && (savedEntity.id > 0 || savedEntity.updateDate)) {
             if (this._debug) console.debug(`[trip-service] Deleting trip {${entity.id}} from local storage`);
             await this.entities.delete(entity);

             try {
               // Remove linked operations
               if (opts && opts.withOperation) {
                 await this.operationService.deleteLocally({tripId: entity.id});
               }
             }
             catch (err) {
               console.error(`[trip-service] Failed to locally delete operations of trip {${entity.id}}`, err);
             }
           }

           // Copy id and update Date
           this.copyIdAndUpdateDate(savedEntity, entity, opts);

           if (this._debug) console.debug(`[trip-service] Trip saved remotely in ${Date.now() - now}ms`, entity);

           // Add to cache
           if (isNew) {
             this.insertIntoMutableCachedQuery(cache, {
               query: LoadAllQuery,
               data: savedEntity
             });
           }
         }

       }
     });

    return entity;
  }

  async saveLocally(entity: Trip, opts?: TripServiceSaveOptions): Promise<Trip> {
    if (entity.id >= 0) throw new Error('Must be a local entity');
    opts = {
      withLanding: false,
      withOperation: false,
      withOperationGroup: false,
      ...opts
    };

    this.fillDefaultProperties(entity);

    // Reset the control date
    entity.controlDate = undefined;
    entity.validationDate = undefined;
    entity.qualificationDate = undefined;
    entity.qualityFlagId = undefined;

    // Make sure to fill id, with local ids
    await this.fillOfflineDefaultProperties(entity);

    // Reset synchro status
    entity.synchronizationStatus = 'DIRTY';

    // Extract operations (saved just after)
    const operations = entity.operations;
    delete entity.operations;

    const jsonLocal = this.asObject(entity, {...SAVE_LOCALLY_AS_OBJECT_OPTIONS, batchAsTree: false});
    if (this._debug) console.debug('[trip-service] [offline] Saving trip locally...', jsonLocal);

    // Save trip locally
    await this.entities.save(jsonLocal, {entityName: Trip.TYPENAME});

    // Save operations
    if (opts.withOperation && isNotEmptyArray(operations)) {

      // Link to physical gear id, using the rankOrder
      operations.forEach(o => {
        o.id = null; // Clean ID, to force new ids
        o.physicalGear = o.physicalGear && (entity.gears || []).find(g => g.rankOrder === o.physicalGear.rankOrder);
        o.tripId = entity.id;
        o.trip = undefined;
        o.updateDate = undefined;
      });

      entity.operations = await this.operationService.saveAll(operations, {tripId: entity.id});
    }

    return entity;
  }

  async synchronizeById(id: number): Promise<Trip> {
    const entity = await this.load(id);

    if (!entity || entity.id >= 0) return; // skip

    // todo attention pour l'instant synchronize ne gère que les marées standards
    return await this.synchronize(entity);
  }

  async synchronize(entity: Trip, opts?: TripServiceSaveOptions): Promise<Trip> {
    opts = {
      withOperation: true, // Change default to true
      withLanding: false, // todo manage landedTrip
      withOperationGroup: false,
      enableOptimisticResponse: false, // Optimistic response not need
      ...opts
    };

    const localId = entity && entity.id;
    if (isNil(localId) || localId >= 0) {
      throw new Error("Entity must be a local entity");
    }
    if (this.network.offline) {
      throw new Error("Could not synchronize if network if offline");
    }

    // Clone (to keep original entity unchanged)
    entity = entity instanceof Entity ? entity.clone() : entity;
    entity.synchronizationStatus = 'SYNC';
    entity.id = undefined;

    // Fill operations
    const res = await this.operationService.loadAllByTrip( {tripId: localId},
      {fullLoad: true, rankOrderOnPeriod: false});
    entity.operations = res && res.data || [];

    try {

      entity = await this.save(entity, opts);

      // Check return entity has a valid id
      if (isNil(entity.id) || entity.id < 0) {
        throw {code: ErrorCodes.SYNCHRONIZE_TRIP_ERROR};
      }
    } catch (err) {
      throw {
        ...err,
        code: ErrorCodes.SYNCHRONIZE_TRIP_ERROR,
        message: "TRIP.ERROR.SYNCHRONIZE_TRIP_ERROR",
        context: entity.asObject(SAVE_LOCALLY_AS_OBJECT_OPTIONS)
      };
    }

    try {
      if (this._debug) console.debug(`[trip-service] Deleting trip {${entity.id}} from local storage`);

      // Delete trip's operations
      await this.operationService.deleteLocally({tripId: localId});

      // Delete trip
      await this.entities.deleteById(localId, {entityName: Trip.TYPENAME});
    }
    catch (err) {
      console.error(`[trip-service] Failed to locally delete trip {${entity.id}} and its operations`, err);
      // Continue
    }

    // TODO: add to a synchro history (using class SynchronizationHistory) and store it in local settings ?

    return entity;
  }

  /**
   * Control the validity of an trip
   * @param entity
   * @param opts
   */
  async control(entity: Trip, opts?: any): Promise<FormErrors> {


    const now = this._debug && Date.now();
    if (this._debug) console.debug(`[trip-service] Control trip {${entity.id}}...`, entity);

    const programLabel = entity.program && entity.program.label || null;
    if (!programLabel) throw new Error("Missing trip's program. Unable to control the trip");
    const program = await this.programService.loadByLabel(programLabel);

    const form = this.validatorService.getFormGroup(entity, {
      isOnFieldMode: false, // Always disable 'on field mode'
      program,
      withMeasurements: true // Need by full validation
    });

    if (!form.valid) {
      // Wait end of validation (e.g. async validators)
      await AppFormUtils.waitWhilePending(form);

      // Get form errors
      if (form.invalid) {
        const errors = AppFormUtils.getFormErrors(form, 'trip');

        if (this._debug) console.debug(`[trip-service] Control trip {${entity.id}} [INVALID] in ${Date.now() - now}ms`, errors);

        return errors;
      }
    }

    if (this._debug) console.debug(`[trip-service] Control trip {${entity.id}} [OK] in ${Date.now() - now}ms`);

    return undefined;
  }

  /**
   * Terminate the trip
   * @param entity
   */
  async terminate(entity: Trip): Promise<Trip> {
    if (isNil(entity.id)) {
      throw new Error("Entity must be saved before terminate !");
    }

    // If local entity: save locally
    const offline = entity.id < 0;
    if (offline) {

      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      // Update sync status
      entity.synchronizationStatus = 'READY_TO_SYNC';

      const jsonLocal = this.asObject(entity, SAVE_LOCALLY_AS_OBJECT_OPTIONS);
      if (this._debug) console.debug('[trip-service] [offline] Terminate trip locally...', jsonLocal);

      // Save response locally
      await this.entities.save(jsonLocal);

      return entity;
    }

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);

    const now = this._debug && Date.now();
    if (this._debug) console.debug("[trip-service] Terminate trip...", json);

    await this.graphql.mutate<{ controlTrip: any }>({
      mutation: ControlMutation,
      variables: {
        trip: json
      },
      error: { code: ErrorCodes.TERMINATE_TRIP_ERROR, message: "TRIP.ERROR.TERMINATE_TRIP_ERROR" },
      update: async (proxy, {data}) => {
        const savedEntity = data && data.controlTrip;
        if (savedEntity) {
          this.copyIdAndUpdateDate(savedEntity, entity);
          entity.controlDate = savedEntity.controlDate || entity.controlDate;
          entity.validationDate = savedEntity.validationDate || entity.validationDate;
        }

        if (this._debug) console.debug(`[trip-service] Trip controlled in ${Date.now() - now}ms`, entity);
      }
    });


    return entity;
  }

  /**
   * Validate the trip
   * @param entity
   */
  async validate(entity: Trip): Promise<Trip> {

    if (isNil(entity.id) || entity.id < 0) {
      throw new Error("Entity must be saved once before validate !");
    }
    if (isNil(entity.controlDate)) {
      throw new Error("Entity must be controlled before validate !");
    }
    if (isNotNil(entity.validationDate)) {
      throw new Error("Entity is already validated !");
    }

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Validate trip...", json);

    const res = await this.graphql.mutate<{ validateTrip: any }>({
      mutation: ValidateMutation,
      variables: {
        trip: json
      },
      error: { code: ErrorCodes.VALIDATE_TRIP_ERROR, message: "TRIP.ERROR.VALIDATE_TRIP_ERROR" }
    });

    const savedEntity = res && res.validateTrip;
    if (savedEntity) {
      this.copyIdAndUpdateDate(savedEntity, entity);
      entity.controlDate = savedEntity.controlDate || entity.controlDate;
      entity.validationDate = savedEntity.validationDate || entity.validationDate;
    }

    if (this._debug) console.debug(`[trip-service] Trip validated in ${Date.now() - now}ms`, entity);

    return entity;
  }

  /**
   * Unvalidate the trip
   * @param entity
   */
  async unvalidate(entity: Trip): Promise<Trip> {

    if (isNil(entity.validationDate)) {
      throw new Error("Entity is not validated yet !");
    }

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Unvalidate trip...", json);

    await this.graphql.mutate<{ unvalidateTrip: any }>({
      mutation: UnvalidateMutation,
      variables: {
        trip: json
      },
      context: {
        // TODO serializationKey:
        tracked: true
      },
      error: { code: ErrorCodes.UNVALIDATE_TRIP_ERROR, message: "TRIP.ERROR.UNVALIDATE_TRIP_ERROR" },
      update: (proxy, {data}) => {
        const savedEntity = data && data.unvalidateTrip;
        if (savedEntity) {
          if (savedEntity !== entity) {
            this.copyIdAndUpdateDate(savedEntity, entity);
          }

          entity.controlDate = savedEntity.controlDate || entity.controlDate;
          entity.validationDate = savedEntity.validationDate; // should be null

          if (this._debug) console.debug(`[trip-service] Trip unvalidated in ${Date.now() - now}ms`, entity);
        }

      }
    });

    return entity;
  }

  async qualify(entity: Trip, qualityFlagId: number): Promise<Trip> {

    if (isNil(entity.validationDate)) {
      throw new Error("Entity is not validated yet !");
    }

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);

    json.qualityFlagId = qualityFlagId;

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Qualifying trip...", json);

    const res = await this.graphql.mutate<{ qualifyTrip: any }>({
      mutation: QualifyMutation,
      variables: {
        trip: json
      },
      error: { code: ErrorCodes.QUALIFY_TRIP_ERROR, message: "TRIP.ERROR.QUALIFY_TRIP_ERROR" }
    });

    const savedEntity = res && res.qualifyTrip;
    if (savedEntity) {
      this.copyIdAndUpdateDate(savedEntity, entity);
      entity.controlDate = savedEntity.controlDate;
      entity.validationDate = savedEntity.validationDate;
      entity.qualificationDate = savedEntity.qualificationDate; // can be null
      entity.qualityFlagId = savedEntity.qualityFlagId; // can be 0
    }

    if (this._debug) console.debug(`[trip-service] Trip qualified in ${Date.now() - now}ms`, entity);

    return entity;
  }

  async delete(data: Trip): Promise<any> {
    if (!data) return; // skip
    await this.deleteAll([data]);
  }

  /**
   * Save many trips
   * @param entities
   * @param opts
   */
  async deleteAll(entities: Trip[], opts?: {
    trash?: boolean; // True by default
  }): Promise<any> {


    // Get local entity ids, then delete id
    const localEntities = entities && entities
      .filter(DataRootEntityUtils.isLocal);

    if (isNotEmptyArray(localEntities)) {

      const trash = !opts || opts !== false;
      const trashUpdateDate = trash && moment();

      if (this._debug) console.debug(`[trip-service] Deleting trips locally... {trash: ${trash}`);

      await chainPromises(localEntities.map(entity => async () => {


        // Load trip's operations
        const res = await this.operationService.loadAllByTrip({tripId: entity.id},
          {fullLoad: true, computeRankOrder: false});
        const operations = res && res.data;

        await this.entities.delete(entity, {entityName: Trip.TYPENAME});

        if (isNotNil(operations)) {
          await this.operationService.deleteAll(operations, {trash: false});
        }

        if (trash) {
          // Fill trip's operation, before moving it to trash
          entity.operations = operations;
          entity.updateDate = trashUpdateDate;

          const json = entity.asObject({...SAVE_LOCALLY_AS_OBJECT_OPTIONS, keepLocalId: false});

          // Add to trash
          await this.entities.saveToTrash(json, {entityName: Trip.TYPENAME});
        }

      }));
    }

    const ids = entities && entities
      .map(t => t.id)
      .filter(id => id >= 0);
    if (isEmptyArray(ids)) return; // stop, if nothing else to do

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Deleting trips... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeleteByIdsMutation,
      variables: {
        ids
      },
      update: (proxy) => {
        // Update the cache
        this.removeFromMutableCachedQueryByIds(proxy, {
          query: LoadAllQuery,
          ids
        });

        if (this._debug) console.debug(`[trip-service] Trips deleted remotely in ${Date.now() - now}ms`);
      }
    });
  }

  /**
   * Copy entities (local or remote) to the local storage
   * @param entities
   * @param opts
   */
  copyAllLocally(entities: Trip[], opts?: TripServiceCopyOptions): Promise<Trip[]> {
    return chainPromises(entities.map(source => () => this.copyLocally(source, opts)));
  }


  async copyLocallyById(id: number, opts?: TripServiceLoadOptions & {}): Promise<Trip> {

    // Load existing data
    const data = await this.load(id, {...opts, fetchPolicy: "network-only"});

    // Add operations
    if (!opts || opts.withOperation !== false) {
      const res = await this.operationService.loadAllByTrip({tripId: id}, {
        fetchPolicy: "network-only",
        fullLoad: true
      });
      data.operations = res.data;
    }

    await this.copyLocally(data, opts);

    return data;
  }

  /**
   * Copy an entity (local or remote) to the local storage
   * @param source
   * @param opts
   */
  async copyLocally(source: Trip, opts?: TripServiceCopyOptions): Promise<Trip> {
    console.debug("[trip-service] Copy trip locally...", source);

    opts = {
      keepRemoteId: false,
      deletedFromTrash: false,
      withOperation: true, // Change default value to 'true'
      ...opts
    };
    const isLocal = DataRootEntityUtils.isLocal(source);

    // Create a new entity (without id and updateDate)
    const json = this.asObject(source, { ...COPY_LOCALLY_AS_OBJECT_OPTIONS, keepRemoteId: opts.keepRemoteId });
    json.synchronizationStatus = SynchronizationStatusEnum.DIRTY; // To make sure it will be saved locally

    // Save
    const target = await this.saveLocally(Trip.fromObject(json), opts);

    // Remove from the local trash
    if (opts.deletedFromTrash) {
      if (isLocal) {
        await this.entities.deleteFromTrash(source, {entityName: Trip.TYPENAME});
      }
      else {
        await this.trashRemoteService.delete('Trip', source.id);
      }
    }

    if (opts.displaySuccessToast) {
      await this.showToast({message: 'SOCIAL.USER_EVENT.INFO.COPIED_LOCALLY', type: 'info'});
    }

    return target;
  }

  executeImport(opts?: {
    maxProgression?: number;
  }): Observable<number>{
    if (this.$importationProgress) return this.$importationProgress; // Skip to may call

    const maxProgression = opts && opts.maxProgression || 100;

    const jobOpts = {maxProgression: undefined};
    const jobDefers: Observable<number>[] = [
      // Clear caches
      defer(() => timer()
        .pipe(
          switchMap(() => this.network.clearCache()),
          map(() => jobOpts.maxProgression as number)
        )
      ),
      // Start to import data
      defer(() => this.referentialRefService.executeImport(jobOpts)),
      defer(() =>  this.personService.executeImport(jobOpts)),
      defer(() => this.vesselSnapshotService.executeImport(jobOpts)),
      defer(() => this.programService.executeImport(jobOpts)),
      // Save data to local storage, then set progression to the max
      defer(() => timer()
        .pipe(
          switchMap(() => this.entities.persist()),
          map(() => jobOpts.maxProgression as number)
        ))
    ];
    const jobCount = jobDefers.length;
    jobOpts.maxProgression = Math.trunc(maxProgression / jobCount);

    const now = Date.now();
    console.info(`[trip-service] Starting ${jobDefers.length} importation jobs...`);

    // Execute all jobs, one by one
    let jobIndex = 0;
    this.$importationProgress = concat(
      ...jobDefers.map((jobDefer: Observable<number>, index) => {
        return jobDefer
          .pipe(
            //switchMap(() => jobDefer),
            map(jobProgression => {
              jobIndex = index;
              if (this._debug && jobProgression > jobOpts.maxProgression) {
                console.warn(`[trip-service] WARN job #${jobIndex} return a jobProgression > maxProgression (${jobProgression} > ${jobOpts.maxProgression})!`);
              }
              // Compute total progression
              return index * jobOpts.maxProgression + Math.min(jobProgression || 0, jobOpts.maxProgression);
            })
          );
      }),

      // Finish (force to reach max value)
      of(maxProgression)
        .pipe(
          tap(() => {
            this.$importationProgress = null;
            console.info(`[trip-service] Importation finished in ${Date.now() - now}ms`);
            this.settings.registerOfflineFeature(TRIP_FEATURE_NAME);
          })
        ))

      .pipe(
        catchError((err) => {
          this.$importationProgress = null;
          console.error(`[trip-service] Error during importation (job #${jobIndex + 1}): ${err && err.message || err}`, err);
          throw err;
        }),
        // Compute total progression (= job offset + job progression)
        // (and make ti always <= maxProgression)
        map((progression) =>  Math.min(progression, maxProgression))
      );

    return this.$importationProgress;
  }


  /* -- protected methods -- */


  protected asObject(entity: Trip, opts?: DataEntityAsObjectOptions & { batchAsTree?: boolean }): any {
    opts = { ...MINIFY_OPTIONS, ...opts };
    const copy: any = entity.asObject(opts);

    // Fill return date using departure date
    copy.returnDateTime = copy.returnDateTime || copy.departureDateTime;

    // Fill return location using departure location
    if (!copy.returnLocation || !copy.returnLocation.id) {
      copy.returnLocation = { ...copy.departureLocation };
    }

    // Full json optimisation
    if (opts.minify && !opts.keepEntityName && !opts.keepTypename) {
      // Clean vessel features object, before saving
      copy.vesselSnapshot = {id: entity.vesselSnapshot && entity.vesselSnapshot.id};
    }

    return copy;
  }

  protected fillDefaultProperties(entity: Trip) {

    super.fillDefaultProperties(entity);

    if (entity.operationGroups) {
      this.fillRecorderDepartment(entity.operationGroups, entity.recorderDepartment);
      entity.operationGroups.forEach(operationGroup => {
        this.fillRecorderDepartment(operationGroup.products, entity.recorderDepartment);
        this.fillRecorderDepartment(operationGroup.packets, entity.recorderDepartment);
      });
    }
    // todo maybe others tables ?

    // Physical gears: compute rankOrder
    fillRankOrder(entity.gears);

    // Measurement: compute rankOrder
    fillRankOrder(entity.measurements);
  }

  protected fillRecorderDepartment(entities: IWithRecorderDepartmentEntity<any> | IWithRecorderDepartmentEntity<any>[], department?: Department) {

    if (isNil(entities)) return;
    if (!Array.isArray(entities)) {
      entities = [entities];
    }
    department = department || this.accountService.department;

    entities.forEach(entity => {
      if (!entity.recorderDepartment || !entity.recorderDepartment.id) {
        // Recorder department
        if (department) {
          entity.recorderDepartment = department;
        }
      }
    });
  }

  protected async fillOfflineDefaultProperties(entity: Trip) {
    const isNew = isNil(entity.id);

    // If new, generate a local id
    if (isNew) {
      entity.id =  await this.entities.nextValue(entity);
    }

    // Fill default synchronization status
    entity.synchronizationStatus = entity.synchronizationStatus || SynchronizationStatusEnum.DIRTY;

    // Fill gear id
    const gears = entity.gears || [];
    await EntityUtils.fillLocalIds(gears, (_, count) => this.entities.nextValues(PhysicalGear.TYPENAME, count));
  }

  protected copyIdAndUpdateDate(source: Trip | undefined, target: Trip, opts?: TripServiceSaveOptions) {
    if (!source) return;

    // Update (id and updateDate), and control validation
    super.copyIdAndUpdateDate(source, target);

    // Update parent link
    target.observedLocationId = source.observedLocationId;
    if (opts && opts.withLanding && source.landing && target.landing) {
      EntityUtils.copyIdAndUpdateDate(source.landing, target.landing);
    }

    // Update sale
    if (source.sale && target.sale) {
      EntityUtils.copyIdAndUpdateDate(source.sale, target.sale);
      DataRootEntityUtils.copyControlAndValidationDate(source.sale, target.sale);

      // For a landedTrip with operationGroups, copy directly sale's product, a reload must be done after service call
      if (opts && opts.withLanding && source.sale.products) {
        target.sale.products = source.sale.products;
      }
    }

    // Update gears
    if (source.gears && target.gears) {
      target.gears.forEach(targetGear => {
        const sourceGear = source.gears.find(json => targetGear.equals(json));
        EntityUtils.copyIdAndUpdateDate(sourceGear, targetGear);
        DataRootEntityUtils.copyControlAndValidationDate(sourceGear, targetGear);

        // Update measurements
        if (sourceGear && sourceGear.measurements && targetGear.measurements) {
          targetGear.measurements.forEach(targetMeasurement => {
            const sourceMeasurement = sourceGear.measurements.find(m => targetMeasurement.equals(m));
            EntityUtils.copyIdAndUpdateDate(sourceMeasurement, targetMeasurement);
          });
        }
      });

      // Update gears in operation groups
      if (target.operationGroups) {
        target.operationGroups.forEach(operationGroup => {
          operationGroup.physicalGear = source.gears.find(json => operationGroup.physicalGear.equals(json));
        });
      }
    }

    // Update measurements
    if (target.measurements && source.measurements) {
      target.measurements.forEach(entity => {
        const savedMeasurement = source.measurements.find(m => entity.equals(m));
        EntityUtils.copyIdAndUpdateDate(savedMeasurement, entity);
      });
    }

    // Update operation groups
    if (source.operationGroups && target.operationGroups && opts && opts.withOperationGroup) {
      target.operationGroups.forEach(targetOperationGroup => {
        const sourceOperationGroup = source.operationGroups.find(json => targetOperationGroup.equals(json));
        EntityUtils.copyIdAndUpdateDate(sourceOperationGroup, targetOperationGroup);

        // Operation group's measurements
        if (sourceOperationGroup && sourceOperationGroup.measurements && targetOperationGroup.measurements) {
          targetOperationGroup.measurements.forEach(targetMeasurement => {
            const sourceMeasurement = sourceOperationGroup.measurements.find(m => targetMeasurement.equals(m));
            EntityUtils.copyIdAndUpdateDate(sourceMeasurement, targetMeasurement);
          });
        }

        // Operation group's products
        if (sourceOperationGroup && sourceOperationGroup.products && targetOperationGroup.products) {
          targetOperationGroup.products.forEach(targetProduct => {
            const sourceProduct = sourceOperationGroup.products.find(json => targetProduct.equals(json));
            EntityUtils.copyIdAndUpdateDate(sourceProduct, targetProduct);
          });
        }

        // Operation group's packets
        if (sourceOperationGroup && sourceOperationGroup.packets && targetOperationGroup.packets) {
          targetOperationGroup.packets.forEach(targetPacket => {
            const sourcePacket = sourceOperationGroup.packets.find(json => targetPacket.equals(json));
            EntityUtils.copyIdAndUpdateDate(sourcePacket, targetPacket);

            // Packet's compositions
            if (sourcePacket && sourcePacket.composition && targetPacket.composition) {
              targetPacket.composition.forEach(targetComposition => {
                const sourceComposition = sourcePacket.composition.find(json => targetComposition.equals(json));
                EntityUtils.copyIdAndUpdateDate(sourceComposition, targetComposition);
              });
            }
          });
        }
      });
    }
  }

  protected showToast<T = any>(opts: ShowToastOptions): Promise<OverlayEventDetail<T>> {
    return Toasts.show(this.toastController, this.translate, opts);
  }
}

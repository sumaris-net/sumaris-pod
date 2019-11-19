import {Injectable, Injector} from "@angular/core";
import gql from "graphql-tag";
import {EntityUtils, fillRankOrder, isNil, Trip} from "./trip.model";
import {
  EditorDataService,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  LoadResult,
  TableDataService
} from "../../shared/shared.module";
import {environment} from "../../core/core.module";
import {map} from "rxjs/operators";
import {Moment} from "moment";
import {ErrorCodes} from "./trip.errors";
import {AccountService} from "../../core/services/account.service";
import {DataFragments, Fragments} from "./trip.queries";
import {FetchPolicy, WatchQueryFetchPolicy} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";
import {dataIdFromObject} from "../../core/graphql/graphql.utils";
import {RootDataService} from "./root-data-service.class";
import {
  DataEntityAsObjectOptions,
  DataRootEntityUtils,
  MINIFY_OPTIONS,
  OPTIMISTIC_AS_OBJECT_OPTIONS,
  SAVE_AS_OBJECT_OPTIONS
} from "./model/base.model";
import {NetworkService} from "../../core/services/network.service";
import {Observable} from "rxjs";
import {EntityStorage} from "../../core/services/entities-storage.service";
import {isEmptyArray} from "../../shared/functions";
import {DataQualityService} from "./trip.services";
import * as moment from "moment";

const physicalGearFragment = gql`fragment PhysicalGearFragment on PhysicalGearVO {
    id
    rankOrder
    updateDate
    creationDate
    comments
    gear {
      ...ReferentialFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    measurementValues
  }
`;

export const TripFragments = {
  physicalGear: physicalGearFragment,
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
  }
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${DataFragments.vesselSnapshot}
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
      ...VesselSnapshotFragment
    }
    sale {
      id
      startDateTime
      creationDate
      updateDate
      comments
      saleType {
        ...ReferentialFragment
      }
      saleLocation {
        ...LocationFragment
      }
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
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.measurement}
  ${Fragments.referential}
  ${Fragments.location}
  ${DataFragments.vesselSnapshot}
  ${physicalGearFragment}
  `
};

export class TripFilter {

  static isEmpty(filter: TripFilter|any): boolean {
    return !filter || (isNilOrBlank(filter.programLabel) && isNilOrBlank(filter.vesselId) && isNilOrBlank(filter.locationId)
      && !filter.startDate && !filter.endDate
      && isNil(filter.recorderDepartmentId));
  }

  programLabel?: string;
  vesselId?: number;
  locationId?: number;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  recorderDepartmentId?: number;

}
const LoadAllQuery: any = gql`
  query Trips($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: TripFilterVOInput){
    trips(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightTripFragment
    }
    tripsCount(filter: $filter)
  }
  ${TripFragments.lightTrip}
`;
// Load a trip
const LoadQuery: any = gql`
  query Trip($id: Int) {
    trip(id: $id) {
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
const SaveAllQuery: any = gql`
  mutation saveTrips($trips:[TripVOInput]){
    saveTrips(trips: $trips){
      ...TripFragment
    }
  }
  ${TripFragments.trip}
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
  subscription UpdateTrip($id: Int, $interval: Int){
    updateTrip(id: $id, interval: $interval) {
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;

@Injectable({providedIn: 'root'})
export class TripService extends RootDataService<Trip, TripFilter> implements TableDataService<Trip, TripFilter>,
  EditorDataService<Trip, TripFilter>,
  DataQualityService<Trip> {

  protected loading = false;

  constructor(
    injector: Injector,
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected entities: EntityStorage
  ) {
    super(injector);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  /**
   * Load many trips
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: string,
           filter?: TripFilter,
           options?: {
             fetchPolicy?: WatchQueryFetchPolicy
           }): Observable<LoadResult<Trip>> {
    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'departureDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    // Offline
    if (this.network.offline) {
      return this.entities.watchAll<Trip>('TripVO')
        .pipe(
          map(trips => {
          return {
            data: trips.map(Trip.fromObject),
            total: trips.length
          };
        }));
    }

    this._lastVariables.loadAll = variables;

    let now = Date.now();
    if (this._debug) console.debug("[trip-service] Watching trips... using options:", variables);
    return this.graphql.watchQuery<{ trips: Trip[]; tripsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_TRIPS_ERROR, message: "TRIP.ERROR.LOAD_TRIPS_ERROR" },
      fetchPolicy: options && options.fetchPolicy || (this.network.offline ? 'cache-only' : 'cache-and-network')
    })
      .pipe(
        map(res => {
          const data = (res && res.trips || []).map(Trip.fromObject);
          const total = res && isNotNil(res.tripsCount) ? res.tripsCount : undefined;
          if (/*this._debug &&*/ now) {
            console.debug(`[trip-service] Loaded {${data.length || 0}} trips in ${Date.now() - now}ms`, data);
            now = undefined;
          }
          else {
            console.debug(`[trip-service] Refreshed {${data.length || 0}} trips`);
          }
          return {
            data: data,
            total: total
          };
        })
      );
  }

  async load(id: number, options?: {fetchPolicy: FetchPolicy}): Promise<Trip | null> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    const now = Date.now();
    if (this._debug) console.debug(`[trip-service] Loading trip #${id}...`);

    // If local entity
    if (id < 0) {
      const json = await this.entities.load<Trip>(id, 'TripVO');
      return json && Trip.fromObject(json);
    }

    const res = await this.graphql.query<{ trip: Trip }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: { code: ErrorCodes.LOAD_TRIP_ERROR, message: "TRIP.ERROR.LOAD_TRIP_ERROR" },
      fetchPolicy: options && options.fetchPolicy || undefined
    });
    const data = res && res.trip && Trip.fromObject(res.trip);
    if (data && this._debug) console.debug(`[trip-service] Trip #${id} loaded in ${Date.now() - now}ms`, data);

    return data;
  }

  public listenChanges(id: number): Observable<Trip> {
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
   * @param options
   */
  async saveAll(entities: Trip[], options?: any): Promise<Trip[]> {
    if (!entities) return entities;

    const json = entities.map(t => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(t);
      return this.asObject(t);
    });

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Saving trips...", json);

    await this.graphql.mutate<{ saveTrips: Trip[] }>({
      mutation: SaveAllQuery,
      variables: {
        trips: json
      },
      error: { code: ErrorCodes.SAVE_TRIPS_ERROR, message: "TRIP.ERROR.SAVE_TRIPS_ERROR" },
      update: (proxy, {data}) => {

        if (this._debug) console.debug(`[trip-service] Trips saved remotely in ${Date.now() - now}ms`, entities);

        (data && data.saveTrips && entities || [])
          .forEach(entity => {
            const savedEntity = data.saveTrips.find(t => entity.equals(t));
            if (savedEntity && savedEntity !== entity) {
              this.copyIdAndUpdateDate(savedEntity, entity);
            }
          });
      }
    });

    return entities;
  }

  /**
   * Save a trip
   * @param data
   */
  async save(entity: Trip): Promise<Trip> {

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Saving a trip...");

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Reset the control date
    entity.controlDate = undefined;

    // If new, create a temporary if (for offline mode)
    const isNew = isNil(entity.id);

    // When offline, provide an optimistic response
    const offlineResponse = async (context) => {
      if (isNew) {
        entity.id = await this.entities.nextValue(entity);
        if (this._debug) console.debug(`[trip-service] [offline] Using local entity id #${entity.id}`);
      }

      // For the query to be tracked (see tracked query link) with a unique serialization key
      context.tracked = true;
      if (isNotNil(entity.id)) context.serializationKey = dataIdFromObject(entity);

      return { saveTrips: [this.asObject(entity, OPTIMISTIC_AS_OBJECT_OPTIONS)] };
    };

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (isNew) delete json.id; // Make to remove temporary id, before sending to graphQL
    if (this._debug) console.debug("[trip-service] Using minify object, to send:", json);

   return new Promise<Trip>((resolve, reject) => {
     this.graphql.mutate<{ saveTrips: any }>({
       mutation: SaveAllQuery,
       variables: {
         trips: [json]
       },
       offlineResponse,
       error: { reject,  code: ErrorCodes.SAVE_TRIP_ERROR, message: "TRIP.ERROR.SAVE_TRIP_ERROR" },
       update: async (proxy, {data}) => {
         const savedEntity = data && data.saveTrips && data.saveTrips[0];

         // Local entity: save it
         if (savedEntity.id < 0) {
           if (this._debug) console.debug('[trip-service] [offline] Saving trip locally...', savedEntity);

           // Save response locally
           await this.entities.save(savedEntity);
         }

         // Update the entity and update GraphQL cache
         else {

           // Remove existing entity from the local storage
           if (entity.id < 0 && savedEntity.updateDate) {
             await this.entities.delete(entity);
           }

           // Copy id and update Date
           this.copyIdAndUpdateDate(savedEntity, entity);

           if (this._debug) console.debug(`[trip-service] Trip saved remotely in ${Date.now() - now}ms`, entity);

           // Add to cache
           if (isNew && this._lastVariables.loadAll) {
             this.graphql.addToQueryCache(proxy,{
               query: LoadAllQuery,
               variables: this._lastVariables.loadAll
             }, 'trips', savedEntity);
           }
           else if (this._lastVariables.load) {
             this.graphql.updateToQueryCache(proxy,{
               query: LoadQuery,
               variables: this._lastVariables.load
             }, 'trip', savedEntity);
           }
         }

         resolve(entity);
       }

     });
   });


  }

  async synchronize(entity: Trip): Promise<Trip> {
    if (isNil(entity.id) || entity.id >= 0) {
      throw new Error("Entity must be a local entity");
    }
    if (this.network.offline) {
      throw new Error("Could not synchronize if network if offline");
    }

    entity = await this.save(entity);

    if (entity.id < 0) {
      throw {code: ErrorCodes.SYNCHRONIZE_TRIP_ERROR, message: "TRIP.ERROR.SYNCHRONIZE_TRIP_ERROR"};
    }

    return this.control(entity);
  }

  /**
   * Control the trip
   * @param entity
   */
  async control(entity: Trip): Promise<Trip> {

    // TODO vérifier que le formulaire est dirty et/ou s'il est valide, car le control provoque une sauvegarde

    if (isNil(entity.id)) {
      throw new Error("Entity must be saved before control !");
    }

    if (entity.id < 0) {
      if (this.network.offline) {
        throw new Error("Could not control when offline");
      }
      entity = await this.save(entity);
    }

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);

    const now = new Date();
    if (this._debug) console.debug("[trip-service] Control trip...", json);

    const res = await this.graphql.mutate<{ controlTrip: any }>({
      mutation: ControlMutation,
      variables: {
        trip: json
      },
      error: { code: ErrorCodes.CONTROL_TRIP_ERROR, message: "TRIP.ERROR.CONTROL_TRIP_ERROR" }
    });

    const savedEntity = res && res.controlTrip;
    if (savedEntity) {
      this.copyIdAndUpdateDate(savedEntity, entity);
      entity.controlDate = savedEntity.controlDate || entity.controlDate;
      entity.validationDate = savedEntity.validationDate || entity.validationDate;
    }

    if (this._debug) console.debug("[trip-service] Trip controlled in " + (new Date().getTime() - now.getTime()) + "ms", entity);

    return entity;
  }

  /**
   * Validate the trip
   * @param entity
   */
  async validate(entity: Trip): Promise<Trip> {

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

    // If local entity
    if (data.id < 0) {
      await this.entities.delete<Trip>(data);
    }
    else {
      await this.deleteAll([data]);
    }
  }

  /**
   * Save many trips
   * @param entities
   */
  async deleteAll(entities: Trip[]): Promise<any> {

    // Get local entity ids, then delete id
    const localIds = entities && entities
      .map(t => t.id)
      .filter(id => id < 0);
    if (isNotEmptyArray(localIds)) {
      if (this._debug) console.debug("[trip-service] Deleting trips locally... ids:", localIds);
      await this.entities.deleteMany<Trip>(localIds, 'TripVO');
    }

    const remoteIds = entities && entities
      .map(t => t.id)
      .filter(isNotNil);
    if (isEmptyArray(remoteIds)) return; // stop, if nothing else to do

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Deleting trips... ids:", remoteIds);

    await this.graphql.mutate<any>({
      mutation: DeleteByIdsMutation,
      variables: {
        ids: remoteIds
      },
      update: (proxy) => {
        // Update the cache
        if (this._lastVariables.loadAll) {
          this.graphql.removeToQueryCacheByIds(proxy, {
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'trips', remoteIds);
        }

        if (this._debug) console.debug(`[trip-service] Trips deleted remotely in ${Date.now() - now}ms`);
      }
    });
  }

  canUserWrite(trip: Trip): boolean {
    if (!trip) return false;

    // If the user is the recorder: can write
    if (trip.recorderPerson && this.accountService.account.equals(trip.recorderPerson)) {
      return true;
    }

    // TODO: check rights on program (need model changes)

    return this.accountService.canUserWriteDataForDepartment(trip.recorderDepartment);
  }

  /* -- protected methods -- */

  protected asObject(entity: Trip, options?: DataEntityAsObjectOptions): any {
    options = { ...MINIFY_OPTIONS, ...options };
    const copy: any = entity.asObject(options);

    // Fill return date using departure date
    copy.returnDateTime = copy.returnDateTime || copy.departureDateTime;

    // Fill return location using departure location
    if (!copy.returnLocation || !copy.returnLocation.id) {
      copy.returnLocation = { ...copy.departureLocation };
    }

    if (options && options.minify) {
      // Clean vessel features object, before saving
      copy.vesselSnapshot = {id: entity.vesselSnapshot && entity.vesselSnapshot.id};

      // Keep id only, on person and department
      copy.recorderPerson = {id: entity.recorderPerson && entity.recorderPerson.id};
      copy.recorderDepartment = entity.recorderDepartment && {id: entity.recorderDepartment && entity.recorderDepartment.id} || undefined;
    }

    return copy;
  }

  protected fillDefaultProperties(entity: Trip) {

    // If new trip
    if (!entity.id || entity.id < 0) {

      const person = this.accountService.person;

      // Recorder department
      if (person && person.department && !entity.recorderDepartment) {
        entity.recorderDepartment = person.department;
      }

      // Recorder person
      if (person && person.id && !entity.recorderPerson) {
        entity.recorderPerson = person;
      }
    }

    // Physical gears: compute rankOrder
    fillRankOrder(entity.gears);

    // Measurement: compute rankOrder
    fillRankOrder(entity.measurements);
  }

  copyIdAndUpdateDate(source: Trip | undefined, target: Trip) {
    if (!source) return;

    // Update (id and updateDate), and control validation
    super.copyIdAndUpdateDate(source, target);

    // Update sale
    if (target.sale && source.sale) {
      EntityUtils.copyIdAndUpdateDate(source.sale, target.sale);
      DataRootEntityUtils.copyControlAndValidationDate(source.sale, target.sale);
    }

    // Update gears
    if (target.gears && source.gears) {
      target.gears.forEach(targetGear => {
        const sourceGear = source.gears.find(json => targetGear.equals(json));
        EntityUtils.copyIdAndUpdateDate(sourceGear, targetGear);
        DataRootEntityUtils.copyControlAndValidationDate(sourceGear, targetGear);

        // Update measurements
        if (sourceGear && targetGear.measurements && sourceGear.measurements) {
          targetGear.measurements.forEach(targetMeasurement => {
            const savedMeasurement = sourceGear.measurements.find(m => targetMeasurement.equals(m));
            EntityUtils.copyIdAndUpdateDate(savedMeasurement, targetMeasurement);
          });
        }
      });
    }

    // Update measurements
    if (target.measurements && source.measurements) {
      target.measurements.forEach(entity => {
        const savedMeasurement = source.measurements.find(m => entity.equals(m));
        EntityUtils.copyIdAndUpdateDate(savedMeasurement, entity);
      });
    }
  }

}

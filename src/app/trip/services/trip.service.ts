import {Injectable, Injector} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs-compat";
import {EntityUtils, fillRankOrder, isNil, Person, Trip} from "./trip.model";
import {EditorDataService, isNotNil, LoadResult, TableDataService} from "../../shared/shared.module";
import {environment, NetworkService} from "../../core/core.module";
import {map} from "rxjs/operators";
import {Moment} from "moment";
import {ErrorCodes} from "./trip.errors";
import {AccountService} from "../../core/services/account.service";
import {DataFragments, Fragments} from "./trip.queries";
import {FetchPolicy, WatchQueryFetchPolicy} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";
import {dataIdFromObject} from "../../core/graphql/graphql.utils";
import {RootDataService} from "./root-data-service.class";
import {DataRootEntityUtils} from "./model/base.model";

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
      ...RecorderDepartmentFragment
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
    recorderDepartment {
      ...RecorderDepartmentFragment
    }
    recorderPerson {
      ...RecorderPersonFragment
    }
    vesselFeatures {
     ...VesselFeaturesFragment
    }
    observers {
      ...RecorderPersonFragment
    }
  }
  ${Fragments.location}
  ${Fragments.recorderDepartment}
  ${Fragments.recorderPerson}
  ${DataFragments.vesselFeatures}
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
    recorderDepartment {
      ...RecorderDepartmentFragment
    }
    recorderPerson {
      ...RecorderPersonFragment
    }
    vesselFeatures {
      ...VesselFeaturesFragment
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
    observers {
      ...RecorderPersonFragment
    }
  }
  ${Fragments.recorderDepartment}
  ${Fragments.recorderPerson}
  ${Fragments.measurement}
  ${Fragments.referential}
  ${Fragments.location}
  ${DataFragments.vesselFeatures}
  ${physicalGearFragment}
  `
};

export class TripFilter {
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  programLabel?: string;
  vesselId?: number
  recorderDepartmentId?: number;
  locationId?: number;
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
export class TripService extends RootDataService<Trip, TripFilter> implements TableDataService<Trip, TripFilter>, EditorDataService<Trip, TripFilter> {

  protected loading = false;

  constructor(
    injector: Injector,
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected accountService: AccountService
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

    this._lastVariables.loadAll = variables;

    let now = Date.now();
    if (this._debug) console.debug("[trip-service] Watching trips... using options:", variables);
    return this.graphql.watchQuery<{ trips: Trip[]; tripsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_TRIPS_ERROR, message: "TRIP.ERROR.LOAD_TRIPS_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'cache-and-network'
    })
      .pipe(
        //throttleTime(200),
        map(res => {
          const data = (res && res.trips || []).map(Trip.fromObject);
          const total = res && res.tripsCount || 0;
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

    const res = await this.graphql.query<{ trip: Trip }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: { code: ErrorCodes.LOAD_TRIP_ERROR, message: "TRIP.ERROR.LOAD_TRIP_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'cache-first'
    });
    const data = res && res.trip && Trip.fromObject(res.trip);
    if (data && this._debug) console.debug(`[trip-service] Trip #${id} loaded in ${Date.now() - now}ms`, data);

    return data;
  }

  public listenChanges(id: number): Observable<Trip> {
    if (isNil(id)) throw new Error("Missing argument 'id' ");

    if (this._debug) console.debug(`[trip-service] [WS] Listening changes for trip {${id}}...`);

    return this.subscribe<{ updateTrip: Trip }, { id: number, interval: number }>({
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

    const res = await this.graphql.mutate<{ saveTrips: Trip[] }>({
      mutation: SaveAllQuery,
      variables: {
        trips: json
      },
      error: { code: ErrorCodes.SAVE_TRIPS_ERROR, message: "TRIP.ERROR.SAVE_TRIPS_ERROR" }
    });
    (res && res.saveTrips && entities || [])
      .forEach(entity => {
        const savedTrip = res.saveTrips.find(res => entity.equals(res));
        this.copyIdAndUpdateDate(savedTrip, entity);
      });

    if (this._debug) console.debug(`[trip-service] Trips saved and updated in ${Date.now() - now}ms`, entities);


    return entities;
  }

  /**
   * Save a trip
   * @param data
   */
  async save(entity: Trip): Promise<Trip> {

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);
    const isNew = isNil(entity.id);

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Saving trip...", json);

    if (isNew) {
      console.log("TODO: generate new temp ID for TRIP")
      entity.id = await this.graphql.getTemporaryId('Trip');
    }
    else {
      json.controlDate = undefined; // reset control date
    }

    await this.graphql.mutate<{ saveTrips: any }>({
      mutation: SaveAllQuery,
      variables: {
        trips: [json]
      },
      context: {
        serializationKey: dataIdFromObject(entity),
        tracked: true,
        optimisticResponse: {saveTrips: [entity]}
      },
      error: { code: ErrorCodes.SAVE_TRIP_ERROR, message: "TRIP.ERROR.SAVE_TRIP_ERROR" },
      update: (proxy, {data}) => {
        const savedEntity = data && data.saveTrips && data.saveTrips[0];
        if (savedEntity && savedEntity !== entity) {
          this.copyIdAndUpdateDate(savedEntity, entity);
          if (this._debug) console.debug(`[trip-service] Trip saved in ${Date.now() - now}ms`, entity);
        }

        // Add to cache
        if (isNew && this._lastVariables.loadAll) {
          this.addToQueryCache({
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'trips', savedEntity);
        }
        else if(this._lastVariables.load) {
          this.updateToQueryCache({
            query: LoadQuery,
            variables: this._lastVariables.load
          }, 'trip', savedEntity);
        }
      }
    });


    return entity;
  }

  /**
   * Control the trip
   * @param entity
   */
  async controlTrip(entity: Trip) {

    // TODO vérifier que le formulaire est dirty et/ou s'il est valide, car le control provoque une sauvegarde

    if (isNil(entity.id)) {
      throw new Error("Entity must be saved before control !");
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
  async validateTrip(entity: Trip) {

    if (isNil(entity.controlDate)) {
      throw "Entity must be controlled before validate !"
    }
    if (isNotNil(entity.validationDate)) {
      throw "Entity is already validated !";
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
  async unvalidateTrip(entity: Trip) {

    if (isNil(entity.validationDate)) {
      throw "Entity is not validated yet !";
    }

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Unvalidate trip...", json);

    const res = await this.graphql.mutate<{ unvalidateTrip: any }>({
      mutation: UnvalidateMutation,
      variables: {
        trip: json
      },
      error: { code: ErrorCodes.UNVALIDATE_TRIP_ERROR, message: "TRIP.ERROR.UNVALIDATE_TRIP_ERROR" }
    });

    let savedEntity = res && res.unvalidateTrip;
    if (savedEntity) {
      this.copyIdAndUpdateDate(savedEntity, entity);
      entity.controlDate = savedEntity.controlDate || entity.controlDate;
      entity.validationDate = savedEntity.validationDate; // should be null
    }

    if (this._debug) console.debug(`[trip-service] Trip unvalidated in ${Date.now() - now}ms`, entity);

    return entity;
  }

  async delete(data: Trip): Promise<any> {
    await this.deleteAll([data]);
  }

  /**
   * Save many trips
   * @param entities
   */
  async deleteAll(entities: Trip[]): Promise<any> {
    const ids = entities && entities
      .map(t => t.id)
      .filter(isNotNil);

    const now = Date.now();
    if (this._debug) console.debug("[trip-service] Deleting trips... ids:", ids);

    const res = await this.graphql.mutate<any>({
      mutation: DeleteByIdsMutation,
      variables: {
        ids: ids
      }
    });

    // Update the cache
    if (this._lastVariables.loadAll) {
      this.removeToQueryCacheByIds({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'trips', ids);
    }

    if (this._debug) console.debug(`[trip-service] Trips deleted in ${Date.now() - now}ms`);

    return res;
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

  protected asObject(entity: Trip): any {
    const copy: any = entity.asObject(true/*minify*/);

    // Fill return date using departure date
    copy.returnDateTime = copy.returnDateTime || copy.departureDateTime;

    // Fill return location using departure location
    if (!copy.returnLocation || !copy.returnLocation.id) {
      copy.returnLocation = { id: copy.departureLocation && copy.departureLocation.id };
    }

    // Clean vessel features object, before saving
    copy.vesselFeatures = { vesselId: entity.vesselFeatures && entity.vesselFeatures.vesselId };

    // Keep id only, on person and department
    copy.recorderPerson = { id: entity.recorderPerson && entity.recorderPerson.id };
    copy.recorderDepartment = entity.recorderDepartment && { id: entity.recorderDepartment && entity.recorderDepartment.id } || undefined;

    return copy;
  }

  protected fillDefaultProperties(entity: Trip) {

    // If new trip
    if (!entity.id || entity.id < 0) {

      const person: Person = this.accountService.account;

      // Recorder department
      if (person && person.department && !entity.recorderDepartment) {
        entity.recorderDepartment = person.department;
        //entity.recorderDepartment = Department.fromObject({id: person.department.id, __typename: 'DepartmentVO'});
      }

      // Recorder person
      if (person && person.id && !entity.recorderPerson) {
        entity.recorderPerson = person;
        //entity.recorderPerson = Person.fromObject({id: person.id, __typename: 'PersonVO'});
      }
    }

    // Physical gears: compute rankOrder
    fillRankOrder(entity.gears);

    // Measurement: compute rankOrder
    fillRankOrder(entity.measurements);
  }

  copyIdAndUpdateDate(source: Trip | undefined, target: Trip) {
    if (!source) return;
    super.copyIdAndUpdateDate(source, target);

    // Update (id and updateDate)
    EntityUtils.copyIdAndUpdateDate(source, target);

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

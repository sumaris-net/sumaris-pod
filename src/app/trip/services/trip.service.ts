import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Apollo } from "apollo-angular";
import { Observable, Subscription } from "rxjs-compat";
import { Trip, Person, fillRankOrder } from "./trip.model";
import { DataService, BaseDataService } from "../../core/services/data-service.class";
import { map } from "rxjs/operators";
import { Moment } from "moment";

import { ErrorCodes } from "./trip.errors";
import { AccountService } from "../../core/services/account.service";
import { Fragments } from "./trip.queries";
import { ServerErrorCodes } from "../../core/services/errors";

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
    comments
    departureLocation {
      ...LocationFragment
    }
    returnLocation {
      ...LocationFragment
    }
    recorderDepartment {
      ...DepartmentFragment
    }
    recorderPerson {
      ...PersonFragment
    }
    vesselFeatures {
      vesselId,
      name,
      exteriorMarking
    }
  }
  ${Fragments.location}
  ${Fragments.department}
  ${Fragments.person}
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
    comments
    departureLocation {
      ...LocationFragment
    }
    returnLocation {
      ...LocationFragment
    }
    recorderDepartment {
      ...DepartmentFragment
    }
    recorderPerson {
      ...PersonFragment
    }
    vesselFeatures {
      vesselId
      name
      exteriorMarking
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
      id
      rankOrder
      updateDate
      creationDate
      comments
      gear {
        ...ReferentialFragment
      }
      recorderDepartment {
        ...DepartmentFragment
      }
      measurements {
        ...MeasurementFragment
      }
    }
    measurements {
      ...MeasurementFragment
    }
  }
  ${Fragments.department}
  ${Fragments.person}
  ${Fragments.measurement}
  ${Fragments.referential}
  ${Fragments.location}
  `
};

export declare class TripFilter {
  programLabel?: string;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationId?: number;
}
const LoadAllQuery: any = gql`
  query Trips($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: TripFilterVOInput){
    trips(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightTripFragment
    }
  }
  ${TripFragments.lightTrip}
`;
const LoadQuery: any = gql`
  query Trip($id: Int) {
    trip(id: $id) {
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
const SaveTrips: any = gql`
  mutation saveTrips($trips:[TripVOInput]){
    saveTrips(trips: $trips){
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;
const DeleteTrips: any = gql`
  mutation deleteTrips($ids:[Int]){
    deleteTrips(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription updateTrip($tripId: Int, $interval: Int){
    updateTrip(tripId: $tripId, interval: $interval) {
      ...TripFragment
    }
  }
  ${TripFragments.trip}
`;

@Injectable()
export class TripService extends BaseDataService implements DataService<Trip, TripFilter>{

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);

    // FOR DEV ONLY
    this._debug = true;
  }

  /**
   * Load many trips
   * @param offset 
   * @param size 
   * @param sortBy 
   * @param sortDirection 
   * @param filter 
   */
  loadAll(offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: TripFilter): Observable<Trip[]> {
    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'departureDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    this._lastVariables.loadAll = variables;

    const now = new Date();
    if (this._debug) console.debug("[trip-service] Loading trips... using options:", variables);
    return this.watchQuery<{ trips: Trip[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_TRIPS_ERROR, message: "TRIP.ERROR.LOAD_TRIPS_ERROR" },
      fetchPolicy: 'cache-and-network'
    })
      .pipe(
        map((data) => {
          const res = (data && data.trips || []).map(Trip.fromObject);
          if (this._debug) console.debug("[trip-service] Loaded {" + (res.length || 0) + "} trips in " + (new Date().getTime() - now.getTime()) + "ms", res);
          return res;
        }));
  }

  load(id: number): Observable<Trip | null> {
    if (!id) throw new Error("id should not be null");

    const now = new Date();
    if (this._debug) console.debug("[trip-service] Loading trip {" + id + "}...");

    return this.watchQuery<{ trip: Trip }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: { code: ErrorCodes.LOAD_TRIP_ERROR, message: "TRIP.ERROR.LOAD_TRIP_ERROR" }
    })
      .pipe(
        map(data => {
          if (data && data.trip) {
            const res = Trip.fromObject(data.trip);
            if (this._debug) console.debug("[trip-service] Trip {" + id + "} loaded in " + (new Date().getTime() - now.getTime()) + "ms", res);
            return res;
          }
          return null;
        })
      );
  }

  public listenChanges(id: number): Observable<Trip> {
    if (!id && id !== 0) throw "Missing argument 'id' ";

    console.debug(`[trip-service] [WS] Listening changes for trip {${id}}...`);

    return this.subscribe<{ updateTrip: Trip }, { tripId: number, interval: number }>({
      query: UpdateSubscription,
      variables: {
        tripId: id,
        interval: 10
      },
      error: {
        code: ErrorCodes.SUBSCRIBE_TRIP_ERROR,
        message: 'ERROR.TRIP.SUBSCRIBE_TRIP_ERROR'
      }
    })
      .pipe(
        map(data => {
          if (data && data.updateTrip) {
            const res = Trip.fromObject(data.updateTrip);
            if (this._debug) console.debug(`[trip-service] Trip {${id}} updated on server !`, res);
            return res;
          }
          return null; // deleted ?
        })
      );
  }

  /**
   * Save many trips
   * @param data 
   */
  async saveAll(entities: Trip[], options?: any): Promise<Trip[]> {
    if (!entities) return entities;

    const json = entities.map(t => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(t)
      return this.asObject(t);
    });

    const now = new Date();
    if (this._debug) console.debug("[trip-service] Saving trips...", json);

    const res = await this.mutate<{ saveTrips: Trip[] }>({
      mutation: SaveTrips,
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

    if (this._debug) console.debug("[trip-service] Trips saved and updated in " + (new Date().getTime() - now.getTime()) + "ms", entities);


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
    const isNew = !entity.id;

    const now = new Date();
    if (this._debug) console.debug("[trip-service] Saving trip...", json);

    const res = await this.mutate<{ saveTrips: any }>({
      mutation: SaveTrips,
      variables: {
        trips: [json]
      },
      error: { code: ErrorCodes.SAVE_TRIP_ERROR, message: "TRIP.ERROR.SAVE_TRIP_ERROR" }
    })

    var savedTrip = res && res.saveTrips && res.saveTrips[0];
    if (savedTrip) {
      this.copyIdAndUpdateDate(savedTrip, entity);

      // Update the cache
      if (isNew && this._lastVariables.loadAll) {
        const list = this.addToQueryCache({
          query: LoadAllQuery,
          variables: this._lastVariables.loadAll
        }, 'trips', savedTrip);
      }
    }

    if (this._debug) console.debug("[trip-service] Trip saved and updated in " + (new Date().getTime() - now.getTime()) + "ms", entity);

    return entity;
  }

  /**
   * Save many trips
   * @param entities 
   */
  async deleteAll(entities: Trip[]): Promise<any> {


    let ids = entities && entities
      .map(t => t.id)
      .filter(id => (id > 0));

    const now = new Date();
    if (this._debug) console.debug("[trip-service] Deleting trips... ids:", ids);

    const res = await this.mutate<any>({
      mutation: DeleteTrips,
      variables: {
        ids: ids
      }
    });

    // Update the cache
    if (this._lastVariables.loadAll) {
      const list = this.removeToQueryCacheByIds({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'trips', ids);
    }

    if (this._debug) console.debug("[trip-service] Trips deleted in " + (new Date().getTime() - now.getTime()) + "ms");

    return res;
  }

  /* -- protected methods -- */

  protected asObject(entity: Trip): any {
    const copy: any = entity.asObject();

    // Fill return date using departure date
    copy.returnDateTime = copy.returnDateTime || copy.departureDateTime;

    // Fill return location using departure lcoation
    if (!copy.returnLocation || !copy.returnLocation.id) {
      copy.returnLocation = { id: copy.departureLocation && copy.departureLocation.id };
    }

    // Clean vesselfeatures object, before saving
    copy.vesselFeatures = { vesselId: entity.vesselFeatures && entity.vesselFeatures.vesselId }
    copy.recorderPerson = { id: entity.recorderPerson && entity.recorderPerson.id }

    return copy;
  }

  protected fillDefaultProperties(entity: Trip) {

    // If new trip
    if (!entity.id || entity.id < 0) {

      const person: Person = this.accountService.account;

      // Recorder department
      if (person && person.department) {
        entity.recorderDepartment.id = person.department.id;
      }

      // Recorder person
      if (person && person.id) {
        entity.recorderPerson.id = person.id;
      }
    }

    // Physical gears: compute rankOrder
    fillRankOrder(entity.gears);

    // Measurement: compute rankOrder
    fillRankOrder(entity.measurements);
  }

  copyIdAndUpdateDate(source: Trip | undefined, target: Trip) {
    if (!source) return;

    // Update (id and updateDate)
    target.id = source.id || target.id;
    target.updateDate = source.updateDate || target.updateDate;
    target.creationDate = source.creationDate || target.creationDate;
    target.dirty = false;

    // Update sale
    if (target.sale && source.sale) {
      target.sale.id = source.sale.id || target.sale.id;
      target.sale.updateDate = source.sale.updateDate || target.sale.updateDate;
      target.sale.creationDate = source.sale.creationDate || target.sale.creationDate;
      target.sale.dirty = false;
    }

    // Update gears
    if (target.gears && source.gears) {
      target.gears.forEach(entity => {
        const savedGear = source.gears.find(json => entity.equals(json));
        entity.id = savedGear && savedGear.id || entity.id;
        entity.updateDate = savedGear && savedGear.updateDate || entity.updateDate;
        entity.creationDate = savedGear && savedGear.creationDate || entity.creationDate;
        entity.dirty = false;

        // Update measurements
        if (savedGear && entity.measurements && savedGear.measurements) {
          entity.measurements.forEach(entity => {
            const savedMeasurement = savedGear.measurements.find(m => entity.equals(m));
            entity.id = savedMeasurement && savedMeasurement.id || entity.id;
            entity.updateDate = savedMeasurement && savedMeasurement.updateDate || entity.updateDate;
            entity.dirty = false;
          });
        }
      });
    }

    // Update measurements
    if (target.measurements && source.measurements) {
      target.measurements.forEach(entity => {
        const savedMeasurement = source.measurements.find(m => entity.equals(m));
        entity.id = savedMeasurement && savedMeasurement.id || entity.id;
        entity.updateDate = savedMeasurement && savedMeasurement.updateDate || entity.updateDate;
        entity.dirty = false;
      });
    }
  }
}

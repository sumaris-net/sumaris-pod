import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Apollo} from "apollo-angular";
import {Observable, Subject} from "rxjs";
import {Trip, Person} from "./model";
import {DataService, BaseDataService} from "./data-service";
import {map} from "rxjs/operators";
import { Moment } from "moment";
import { DocumentNode } from "graphql";
import { ErrorCodes } from "./errors";
import { AccountService } from "./account-service";

export declare class TripFilter {
  startDate: Date|Moment;
  endDate: Date|Moment;
}
export declare class TripsVariables extends TripFilter {
  offset: number;
  size: number;
  sortBy?: string;
  sortDirection?: string;
};
const LoadAllQuery: DocumentNode = gql`
  query Trips($startDate: Date, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    trips(filter: {startDate: $startDate}, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      id
      departureDateTime
      returnDateTime
      creationDate
      updateDate
      comments
      departureLocation {
        id
        label
        name
      }
      returnLocation {
        id
        label
        name
      }
      recorderDepartment {
        id
        label
        name
      }
      recorderPerson {
        id
        firstName
        lastName
        department {
          id
          label
          name
        }
      }
      vesselFeatures {
        vesselId,
        name,
        exteriorMarking
      }
    }
  }
`;
const LoadQuery: DocumentNode = gql`
  query Trip($id: Int) {
    trip(id: $id) {
      id
      departureDateTime
      returnDateTime
      creationDate
      updateDate
      comments
      departureLocation {
        id
        label
        name
      }
      returnLocation {
        id
        label
        name
      }
      recorderDepartment {
        id
        label
        name
      }
      recorderPerson {
        id
        firstName
        lastName
        department {
          id
          label
          name
        }
      }
      vesselFeatures {
        vesselId
        name
        exteriorMarking
      }
    }
  }
`;
const SaveTrips: DocumentNode = gql`
  mutation saveTrips($trips:[TripVOInput]){
    saveTrips(trips: $trips){
      id
      updateDate
    }
  }
`;
const DeleteTrips: DocumentNode = gql`
  mutation deleteTrips($ids:[Int]){
    deleteTrips(ids: $ids)
  }
`;

@Injectable()
export class TripService extends BaseDataService implements DataService<Trip, TripFilter>{

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);
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
    const variables: TripsVariables = {
      startDate: filter && filter.startDate || null,
      endDate: filter && filter.endDate || null,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'departureDateTime',
      sortDirection: sortDirection || 'asc'
    };
    console.debug("[trip-service] Loading trips... using options:", variables);
    return this.watchQuery<{trips: Trip[]}, TripsVariables>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_TRIPS_ERROR, message: "TRIP.ERROR.LOAD_TRIPS_ERROR"}
    })
    .pipe(
      map((data) => {
        console.debug("[trip-service] Loaded {"+ (data && data.trips && data.trips.length || 0) +"} trips");
        return (data && data.trips || []).map(t => {
          const res = new Trip();
          res.fromObject(t);
          return res;
        });
      }));
  }

  load(id: number): Promise<Trip|null> {
    console.debug("[trip-service] Loading trip {" + id+ "}...");

    return this.query<{trip: Trip}>({
      query: LoadQuery,
      variables: {
        id: id
      }
    })
    .then(data => {
      if (data && data.trip) {
        const res = new Trip();
        res.fromObject(data.trip);
        console.debug("[trip-service] Loaded trip {" + id+ "}", res);
        return res;
      }
      return null;
    });
  }

  /**
   * Save many trips
   * @param data 
   */
  saveAll(trips: Trip[]): Promise<Trip[]> {
    if (!trips) return Promise.resolve(trips);

    // Fill default properties (as recorder department and person)
    trips.forEach(t => this.fillDefaultProperties(t));

    let json = trips.map(t => this.asObject(t));
    console.debug("[trip-service] Saving trips: ", json);

    return this.mutate<{saveTrips: any}>({
        mutation: SaveTrips,
        variables: {
          trips: json
        },
        error: {code: ErrorCodes.SAVE_TRIPS_ERROR, message: "TRIP.ERROR.SAVE_TRIPS_ERROR"}
      })
      .then(data => (data && data.saveTrips && trips || Trip[0]).map(t => {
        const res = data.saveTrips.find(res => res.id == t.id);
        t.updateDate = res && res.updateDate || t.updateDate;
        return t;
      }) );
  }

  /**
   * Save a trip
   * @param data 
   */
  save(trip: Trip): Promise<Trip> {

    // Prepare to save
    this.fillDefaultProperties(trip);

    // Transform into json
    const json = this.asObject(trip);

    console.debug("[trip-service] Saving trip: ", json);

    return this.mutate<{saveTrips: any}>({
        mutation: SaveTrips,
        variables: {
          trips: [json]
        },
        error: {code: ErrorCodes.SAVE_TRIP_ERROR, message: "TRIP.ERROR.SAVE_TRIP_ERROR"}
      })
      .then(data => {
        var res = data && data.saveTrips && data.saveTrips[0];
        trip.updateDate = res && res.updateDate || trip.updateDate;
        return trip;
      });
  }

  /**
   * Save many trips
   * @param data 
   */
  deleteAll(trips: Trip[]): Promise<any> {

    let ids = trips && trips
      .map(t => t.id)
      .filter(id => (id > 0));

    console.debug("[trip-service] Deleting trips... ids:", ids);

    return this.mutate<any>({
        mutation: DeleteTrips,
        variables: {
          ids: ids
        }
      });
  }

  /* -- protected methods -- */

  protected asObject(trip: Trip): any {
    const copy:any = trip.asObject();

    // Fill return date using departure date
    copy.returnDateTime = copy.returnDateTime || copy.departureDateTime;

    // Fill return location using departure lcoation
    if (!copy.returnLocation || !copy.returnLocation.id) {
      copy.returnLocation  = {id: copy.departureLocation && copy.departureLocation.id};
    }

    // Clean vesselfeatures object, before saving
    copy.vesselFeatures = {vesselId: trip.vesselFeatures && trip.vesselFeatures.vesselId}
    copy.recorderPerson = {id: trip.recorderPerson && trip.recorderPerson.id}

    return copy;
  }

  protected fillDefaultProperties(trip: Trip): void {

    // If new trip
    if (!trip.id || trip.id < 0) {

      const person: Person = this.accountService.account;

      // Recorder department
      if (person && !trip.recorderDepartment.id && person.department) {
        trip.recorderDepartment.id = person.department.id;
      }

      // Recorder person
      if (person && !trip.recorderPerson.id) {
        trip.recorderPerson.id = person.id;
      }
      
    }
  }
}

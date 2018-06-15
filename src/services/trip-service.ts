import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Apollo } from "apollo-angular";
import { Observable, Subject } from "rxjs";
import { Trip, Person } from "./model";
import { DataService, BaseDataService } from "./data-service";
import { map } from "rxjs/operators";
import { Moment } from "moment";
import { DocumentNode } from "graphql";
import { ErrorCodes } from "./errors";
import { AccountService } from "./account-service";

export declare class TripFilter {
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationId?: number
}
const LoadAllQuery: DocumentNode = gql`
  query Trips($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: TripFilterVOInput){
    trips(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
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
        avatar
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
      sale {
        id
        startDateTime
        creationDate
        updateDate
        comments
        saleType {
          id
          label
          name
        }
        saleLocation {
          id
          label
          name          
        }
      }
    }
  }
`;
const SaveTrips: DocumentNode = gql`
  mutation saveTrips($trips:[TripVOInput]){
    saveTrips(trips: $trips){
      id
      updateDate
      sale {
        id
        updateDate
      }
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
    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'departureDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    console.debug("[trip-service] Loading trips... using options:", variables);
    return this.watchQuery<{ trips: Trip[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_TRIPS_ERROR, message: "TRIP.ERROR.LOAD_TRIPS_ERROR" }
    })
      .pipe(
        map((data) => {
          console.debug("[trip-service] Loaded {" + (data && data.trips && data.trips.length || 0) + "} trips");
          return (data && data.trips || []).map(t => {
            const res = new Trip();
            res.fromObject(t);
            return res;
          });
        }));
  }

  load(id: number): Promise<Trip | null> {
    console.debug("[trip-service] Loading trip {" + id + "}...");

    return this.query<{ trip: Trip }>({
      query: LoadQuery,
      variables: {
        id: id
      }
    })
      .then(data => {
        if (data && data.trip) {
          const res = new Trip();
          res.fromObject(data.trip);
          console.debug("[trip-service] Loaded trip {" + id + "}", res);
          return res;
        }
        return null;
      });
  }

  /**
   * Save many trips
   * @param data 
   */
  async saveAll(entities: Trip[]): Promise<Trip[]> {
    if (!entities) return entities;

    // Fill default properties (as recorder department and person)
    entities.forEach(t => this.fillDefaultProperties(t));

    const json = entities.map(t => this.asObject(t));
    console.debug("[trip-service] Saving trips: ", json);

    const res = await this.mutate<{ saveTrips: Trip[] }>({
      mutation: SaveTrips,
      variables: {
        trips: json
      },
      error: { code: ErrorCodes.SAVE_TRIPS_ERROR, message: "TRIP.ERROR.SAVE_TRIPS_ERROR" }
    });
    return (res && res.saveTrips && entities || [])
      .map(t => {
        const data = res.saveTrips.find(res => res.id == t.id);
        t.updateDate = data && data.updateDate || t.updateDate;
        if (t.sale) {
          t.sale.id = data.sale && data.sale.id;
          t.sale.updateDate = data.sale && data.sale.updateDate;
        }
        return t;
      });
  }

  /**
   * Save a trip
   * @param data 
   */
  save(entity: Trip): Promise<Trip> {

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);

    console.debug("[trip-service] Saving trip: ", json);

    return this.mutate<{ saveTrips: any }>({
      mutation: SaveTrips,
      variables: {
        trips: [json]
      },
      error: { code: ErrorCodes.SAVE_TRIP_ERROR, message: "TRIP.ERROR.SAVE_TRIP_ERROR" }
    })
      .then(data => {
        var res = data && data.saveTrips && data.saveTrips[0];
        entity.id = res && res.id || entity.id;
        entity.updateDate = res && res.updateDate || entity.updateDate;
        if (entity.sale) {
          entity.sale.id = res.sale && res.sale.id;
          entity.sale.updateDate = res.sale && res.sale.updateDate;
        }
        return entity;
      });
  }

  /**
   * Save many trips
   * @param entities 
   */
  deleteAll(entities: Trip[]): Promise<any> {

    let ids = entities && entities
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
  }
}

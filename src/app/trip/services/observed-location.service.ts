import {Injectable} from "@angular/core";
import {BaseDataService} from "../../core/services/base.data-service.class";
import {LoadResult, TableDataService} from "../../shared/services/data-service.class";
import {Apollo} from "apollo-angular";
import {AccountService} from "../../core/services/account.service";
import {Observable} from "rxjs";
import {Moment} from "moment";
import {environment} from "../../../environments/environment";
import {ObservedLocation} from "./observed-location.model";
import gql from "graphql-tag";
import {Fragments} from "./trip.queries";
import {isNil} from "./trip.model";
import {ErrorCodes} from "./trip.errors";
import {map, throttleTime} from "rxjs/operators";
import {FetchPolicy} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";


export declare class ObservedLocationFilter {
  programLabel?: string;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationId?: number;
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
      ...RecorderDepartmentFragment
    }
    recorderPerson {
      ...RecorderPersonFragment
    }
    observers {
      ...RecorderPersonFragment
    }
  }
  ${Fragments.recorderDepartment}
  ${Fragments.recorderPerson}
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
      ...RecorderDepartmentFragment
    }
    recorderPerson {
      ...RecorderPersonFragment
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
  ${Fragments.location}
  `
};

// Search query
const LoadAllQuery: any = gql`
  query ObservedLocations($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ObservedLocationFilterVOInput){
    observedLocations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightObservedLocationFragment
    }
    observedLocationCount(filter: $filter)
  }
  ${ObservedLocationFragments.lightObservedLocation}
`;
// Load query
const LoadQuery: any = gql`
  query ObservedLocation($id: Int) {
    observedLocation(id: $id) {
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;
@Injectable()
export class ObservedLocationService extends BaseDataService implements TableDataService<ObservedLocation, ObservedLocationFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: string, filter?: ObservedLocationFilter, options?: any): Observable<LoadResult<ObservedLocation>> {

    // Mock
    if (environment.mock) return Observable.of(this.getMockData());

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'startDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    this._lastVariables.loadAll = variables;

    let now;
    if (this._debug) {
      now = Date.now();
      console.debug("[observed-location-service] Watching observed locations... using options:", variables);
    }
    return this.watchQuery<{ observedLocations: ObservedLocation[]; observedLocationsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_OBSERVED_LOCATIONS_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ALL_ERROR" }
    })
    .pipe(
      throttleTime(200),
      map(res => {
        const data = (res && res.observedLocations || []).map(ObservedLocation.fromObject);
        const total = res && res.observedLocationsCount || 0;
        if (this._debug) {
          if (now) {
            console.debug(`[observed-location-service] Loaded {${data.length || 0}} observed locations in ${Date.now() - now}ms`, data);
            now = undefined;
          }
          else {
            console.debug(`[observed-location-service] Refreshed {${data.length || 0}} observed locations`);
          }
        }
        return {
          data: data,
          total: total
        };
      }));
  }

  async load(id: number, options?: {fetchPolicy: FetchPolicy}): Promise<ObservedLocation> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    const now = Date.now();
    if (this._debug) console.debug(`[observed-location-service] Loading observed location {${id}}...`);

    // Mock
    if (environment.mock) {
      return this.getMockData().data.find(sc => sc.id === id);
    }

    const res = await this.query<{ observedLocation: ObservedLocation }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: { code: ErrorCodes.LOAD_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'cache-first'
    });
    const data = res && res.observedLocation && ObservedLocation.fromObject(res.observedLocation);
    if (data && this._debug) console.debug(`[observed-location-service] Observed location #${id} loaded in ${Date.now() - now}ms`, data);

    return data;
  }

  async save(data: ObservedLocation): Promise<ObservedLocation> {
    console.warn("TODO: save observed location");
    // TODO
    return data;
  }

  async saveAll(data: ObservedLocation[], options?: any): Promise<ObservedLocation[]> {
    // TODO:
    return [];
  }

  async deleteAll(data: ObservedLocation[], options?: any): Promise<any> {
    // TODO:
    return {};
  }

  canUserWrite(date: ObservedLocation): boolean {
    if (!date) return false;

    // If the user is the recorder: can write
    if (date.recorderPerson && this.accountService.account.equals(date.recorderPerson)) {
      return true;
    }

    // TODO: check rights on program (need model changes)

    return this.accountService.canUserWriteDataForDepartment(date.recorderDepartment);
  }

  /* -- private -- */

  getMockData(): LoadResult<ObservedLocation> {
    const recorderPerson = {id: 1, firstName:'Jacques', lastName: 'Dupond'};
    const recorderPerson2 = {id: 2, firstName:'Alfred', lastName: 'Dupont'};
    const observers = [recorderPerson];
    const location = {id: 30, label:'DZ', name:'Douarnenez'};
    const location2 = {id: 31, label:'GV', name:'Guilvinec'};

    const data = [
        ObservedLocation.fromObject({
          id:1,
          program:  {id: 11, label:'ADAP-CONTROLE', name:'Contrôle en criée'},
          startDateTime: '2019-01-01T03:50:00Z',
          location: location,
          recorderPerson: recorderPerson,
          observers: [recorderPerson],
          sales: [
            {
              id: 100,
              startDateTime: '2019-01-01T03:50:00Z',
              location: location,
              vesselFeatures: {id: 1, vesselId:1, name:'Vessel 1', exteriorMarking:'FRA000851751'}
            }
          ],
          measurementValues: {
            130: {id: '220', label: 'AV V', name: 'Avant-vente'}
          }
        }),
        ObservedLocation.fromObject({
          id:2,
          program:  {id: 11, label:'ADAP-CONTROLE', name:'Contrôle en criée'},
          startDateTime: '2019-01-02T03:50:00Z',
          location: location,
          recorderPerson: recorderPerson2,
          observers: [recorderPerson2]
        }),
        ObservedLocation.fromObject({
          id:3,
          program:  {id: 11, label:'ADAP-CONTROLE', name:'Contrôle en criée'},
          startDateTime: '2019-01-03T03:50:00Z',
          location:location2,
          recorderPerson: recorderPerson,
          observers: [recorderPerson]
        }),
        ObservedLocation.fromObject({
          id:4,
          program:  {id: 11, label:'ADAP-CONTROLE', name:'Contrôle en criée'},
          startDateTime: '2019-01-04T03:50:00Z',
          location: location2,
          recorderPerson: recorderPerson2,
          observers: [recorderPerson2, recorderPerson]
        }),
      ];

    return {
      data: data,
      total: data.length
    };
  }
}

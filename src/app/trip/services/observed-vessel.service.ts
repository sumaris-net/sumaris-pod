import {Injectable} from "@angular/core";
import {BaseDataService} from "../../core/services/base.data-service.class";
import {LoadResult, TableDataService} from "../../shared/services/data-service.class";
import {Apollo} from "apollo-angular";
import {AccountService} from "../../core/services/account.service";
import {Observable} from "rxjs";
import {environment} from "../../../environments/environment";
import {ObservedVessel} from "./observed-location.model";
import gql from "graphql-tag";
import {DataFragments, Fragments} from "./trip.queries";
import {ErrorCodes} from "./trip.errors";
import {map, throttleTime} from "rxjs/operators";
import {FetchPolicy} from "apollo-client";
import {ObservedLocationFilter} from "./observed-location.service";


export declare class ObservedVesselFilter extends ObservedLocationFilter {
  observedLocationId?: number;
}

export const ObservedVesselFragments = {
  lightObservedVessel: gql`fragment LightObservedVesselFragment on ObservedVesselVO {
    id
    dateTime
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    landingCount
    vesselFeatures {
      ...VesselFeaturesFragment
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
  ${DataFragments.vesselFeatures}
  `
};

// Search query
const LoadAllQuery: any = gql`
  query ObservedVessels($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ObservedLocationFilterVOInput){
    observedVessels(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightObservedVesselFragment
    }
    observedVesselCount(filter: $filter)
  }
  ${ObservedVesselFragments.lightObservedVessel}
`;

@Injectable()
export class ObservedVesselService extends BaseDataService implements TableDataService<ObservedVessel, ObservedVesselFilter> {

  private mock = true;

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: string, filter?: ObservedVesselFilter, options?: any): Observable<LoadResult<ObservedVessel>> {

    // Mock
    if (this.mock) return Observable.of(this.getMockData());

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
      console.debug("[observed-location-service] Watching observed vessels... using options:", variables);
    }
    return this.watchQuery<{ observedVessels: ObservedVessel[]; observedVesselCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_OBSERVED_VESSELS_ERROR, message: "OBSERVED_VESSEL.ERROR.LOAD_ALL_ERROR"}
    })
      .pipe(
        throttleTime(200),
        map(res => {
          const data = (res && res.observedVessels || []).map(ObservedVessel.fromObject);
          const total = res && res.observedVesselCount || 0;
          if (this._debug) {
            if (now) {
              console.debug(`[observed-vessel-service] Loaded {${data.length || 0}} observed vessels in ${Date.now() - now}ms`, data);
              now = undefined;
            } else {
              console.debug(`[observed-vessel-service] Refreshed {${data.length || 0}} observed vessels`);
            }
          }
          return {
            data: data,
            total: total
          };
        }));
  }

  async load(id: number, options?: { fetchPolicy: FetchPolicy }): Promise<ObservedVessel> {
    console.warn("Not implemented");
    return null;
  }

  async save(data: ObservedVessel): Promise<ObservedVessel> {
    console.warn("Not implemented");
    return data;
  }

  async saveAll(data: ObservedVessel[], options?: any): Promise<ObservedVessel[]> {
    console.warn("Not implemented");
    return [];
  }

  async deleteAll(data: ObservedVessel[], options?: any): Promise<any> {
    console.warn("Not implemented");
    return {};
  }

  /* -- private -- */

  getMockData(): LoadResult<ObservedVessel> {
    const recorderPerson = {id: 1, firstName: 'Jacques', lastName: 'Dupond'};
    const recorderPerson2 = {id: 2, firstName: 'Alfred', lastName: 'Dupont'};
    const vessel = {id: 1, vesselId: 1, name: 'Vessel 1', exteriorMarking: 'GV0001001'};
    const vessel2 = {id: 2, vesselId: 2, name: 'Vessel 2', exteriorMarking: 'GV0001022'};

    const data = [
      ObservedVessel.fromObject({
        id: 1,
        rankOrder: 1,
        dateTime: '2019-01-01T03:50:00Z',
        vesselFeatures: vessel,
        recorderPerson: recorderPerson,
        observers: [recorderPerson],
        landingCount: 1,
        measurementValues: {
          130: {id: '220', label: 'AV V', name: 'Avant-vente'}
        }
      }),
      ObservedVessel.fromObject({
        id: 2,
        rankOrder: 2,
        dateTime: '2019-01-02T03:50:00Z',
        vesselFeatures: vessel2,
        recorderPerson: recorderPerson2,
        observers: [recorderPerson2],
        landingCount: 0
      })
    ];

    console.log("Using MOCK data:", data);

    return {
      data: data,
      total: data.length
    };
  }
}

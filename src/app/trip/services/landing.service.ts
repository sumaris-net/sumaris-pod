import {Injectable} from "@angular/core";
import {BaseDataService} from "../../core/services/base.data-service.class";
import {LoadResult, TableDataService} from "../../shared/services/data-service.class";
import {AccountService} from "../../core/services/account.service";
import {Observable} from "rxjs";
import {environment} from "../../../environments/environment";
import {Landing} from "./model/landing.model";
import gql from "graphql-tag";
import {DataFragments, Fragments} from "./trip.queries";
import {ErrorCodes} from "./trip.errors";
import {map, throttleTime} from "rxjs/operators";
import {FetchPolicy} from "apollo-client";
import {ObservedLocationFilter} from "./observed-location.service";
import {GraphqlService} from "../../core/services/graphql.service";


export declare class LandingFilter extends ObservedLocationFilter {
  observedLocationId?: number;
  tripId?: number;
}

export const LandingFragments = {
  lightLanding: gql`fragment LightLandingFragment on LandingVO {
    id
    landingDateTime
    landingLocation {
      ...LocationFragment
    }
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
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
      ...PersonFragment
    }
  }
  ${Fragments.location}
  ${Fragments.recorderDepartment}
  ${Fragments.recorderPerson}
  ${Fragments.person}
  ${DataFragments.vesselFeatures}
  `,
  landing: gql`fragment LandingFragment on LandingVO {
    id
    landingDateTime
    landingLocation {
      ...LocationFragment
    }
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
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
      ...PersonFragment
    }    
    measurementValues
  }
  ${Fragments.location}
  ${Fragments.recorderDepartment}
  ${Fragments.recorderPerson}
  ${Fragments.person}
  ${DataFragments.vesselFeatures}
  `
};

// Search query
const LoadAllQuery: any = gql`
  query Landings($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: LandingFilterVOInput){
    landings(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LandingFragment
    }
    landingCount(filter: $filter)
  }
  ${LandingFragments.landing}
`;

@Injectable()
export class LandingService extends BaseDataService implements TableDataService<Landing, LandingFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: string, filter?: LandingFilter, options?: any): Observable<LoadResult<Landing>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'landingDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    this._lastVariables.loadAll = variables;

    let now;
    if (this._debug) {
      now = Date.now();
      console.debug("[landing-service] Watching landings... using options:", variables);
    }
    return this.graphql.watchQuery<{ landings: Landing[]; landingCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_OBSERVED_VESSELS_ERROR, message: "OBSERVED_VESSEL.ERROR.LOAD_ALL_ERROR"}
    })
      .pipe(
        throttleTime(200),
        map(res => {
          const data = (res && res.landings || []).map(Landing.fromObject);
          const total = res && res.landingCount || 0;
          if (this._debug) {
            if (now) {
              console.debug(`[landing-service] Loaded {${data.length || 0}} landings in ${Date.now() - now}ms`, data);
              now = undefined;
            } else {
              console.debug(`[landing-service] Refreshed {${data.length || 0}} landings`);
            }
          }
          return {
            data: data,
            total: total
          };
        }));
  }

  async load(id: number, options?: { fetchPolicy: FetchPolicy }): Promise<Landing> {
    console.warn("Not implemented");
    return null;
  }

  async save(data: Landing): Promise<Landing> {
    console.warn("Not implemented");
    return data;
  }

  async saveAll(data: Landing[], options?: any): Promise<Landing[]> {
    console.warn("Not implemented");
    return [];
  }

  async deleteAll(data: Landing[], options?: any): Promise<any> {
    console.warn("Not implemented");
    return {};
  }

  /* -- private -- */

}

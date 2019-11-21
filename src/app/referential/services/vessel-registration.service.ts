import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {VesselRegistration} from "./model";
import {LoadResult, TableDataService} from "../../shared/shared.module";
import {BaseDataService} from "../../core/core.module";
import {map} from "rxjs/operators";
import {ErrorCodes} from "./errors";
import {GraphqlService} from "../../core/services/graphql.service";
import {ReferentialFragments} from "./referential.queries";
import {VesselFilter} from "./vessel-service";

export const RegistrationFragments = {
  registration: gql`fragment RegistrationFragment on VesselRegistrationVO {
    id
    startDate
    endDate
    registrationCode
    intRegistrationCode
    registrationLocation {
      ...LocationFragment
    }
  }`,
};

export const LoadRegistrationsQuery: any = gql`
    query VesselRegistrationHistory($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $vesselId: Int){
        vesselRegistrationHistory(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, vesselId: $vesselId){
            ...RegistrationFragment
        }
    }
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
`;

@Injectable({providedIn: 'root'})
export class VesselRegistrationService
  extends BaseDataService
  implements TableDataService<VesselRegistration, VesselFilter> {

  constructor(
    protected graphql: GraphqlService
  ) {
    super(graphql);
  }

  lastVariables() {
    return this._lastVariables;
  }

  /**
   * Load vessel registrations histroy
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
           filter?: VesselFilter): Observable<LoadResult<VesselRegistration>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'startDate',
      sortDirection: sortDirection || 'asc',
      vesselId: filter.vesselId
    };

    this._lastVariables.loadAll = variables;

    const now = Date.now();
    if (this._debug) console.debug("[vessel-registration-history-service] Getting vessel registration history using options:", variables);

    return this.graphql.watchQuery<{ vesselRegistrationHistory: any[] }>({
      query: LoadRegistrationsQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    })
      .pipe(
        map(({vesselRegistrationHistory}) => {
            const data = (vesselRegistrationHistory || []).map(VesselRegistration.fromObject);
            if (this._debug) console.debug(`[vessel-registration-history-service] Vessel registration history loaded in ${Date.now() - now}ms`, data);
            return {
              data: data
            };
          }
        )
      );
  }

  deleteAll(data: VesselRegistration[], options?: any): Promise<any> {
    throw new Error("Method not implemented.");
  }

  saveAll(data: VesselRegistration[], options?: any): Promise<VesselRegistration[]> {
    throw new Error("Method not implemented.");
  }

  /* -- protected methods -- */

}

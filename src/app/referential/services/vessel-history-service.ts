import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {EntityUtils, isNil, isNotNil, Person, StatusIds, VesselFeatures} from "./model";
import {EditorDataService, isNilOrBlank, LoadResult, TableDataService} from "../../shared/shared.module";
import {BaseDataService} from "../../core/core.module";
import {map} from "rxjs/operators";
import {Moment} from "moment";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {SuggestionDataService} from "../../shared/services/data-service.class";
import {GraphqlService} from "../../core/services/graphql.service";
import {ReferentialFragments} from "./referential.queries";
import {FetchPolicy} from "apollo-client";
import {isEmptyArray} from "../../shared/functions";
import {VesselFilter, VesselFragments} from "./vessel-service";

// export const RegistrationFragments = {
//   registration: gql`fragment RegistrationFragment on VesselRegistrationVO {
//     id
//     startDate
//     endDate
//     registrationCode
//     vesselId
//     registrationLocation {
//       ...LocationFragment
//     }
//   }`,
// };

const LoadAllQuery: any = gql`
    query VesselFeaturesHistory($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $vesselId: Int){
        vesselFeaturesHistory(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, vesselId: $vesselId){
            ...LightVesselFragment
        }
    }
    ${VesselFragments.lightVessel}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
`;
// const LoadRegistrationHistoryQuery: any = gql`
//     query VesselRegistrationHistory($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $vesselId: Int){
//         vesselRegistrationHistory(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, vesselId: $vesselId){
//             ...RegistrationFragment
//         }
//     }
//     ${RegistrationFragments.registration}
//     ${ReferentialFragments.location}
// `;

@Injectable({providedIn: 'root'})
export class VesselHistoryService
  extends BaseDataService
  implements TableDataService<VesselFeatures, VesselFilter> {

  constructor(
    protected graphql: GraphqlService,
    private accountService: AccountService
  ) {
    super(graphql);
  }

  /**
   * Load many vessels
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
           filter?: VesselFilter): Observable<LoadResult<VesselFeatures>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'startDate',
      sortDirection: sortDirection || 'asc',
      vesselId: filter.vesselId
    };
    const now = Date.now();
    if (this._debug) console.debug("[vessel-history-service] Getting vessel features history using options:", variables);

    return this.graphql.watchQuery<{ vesselFeaturesHistory: any[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    })
      .pipe(
        map(({vesselFeaturesHistory}) => {
            const data = (vesselFeaturesHistory || []).map(VesselFeatures.fromObject);
            if (this._debug) console.debug(`[vessel-history-service] Vessel features history loaded in ${Date.now() - now}ms`, data);
            return {
              data: data
            };
          }
        )
      );
  }

  deleteAll(data: VesselFeatures[], options?: any): Promise<any> {
    throw new Error("Method not implemented.");
  }

  saveAll(data: VesselFeatures[], options?: any): Promise<VesselFeatures[]> {
    throw new Error("Method not implemented.");
  }

  /* -- protected methods -- */

}

import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {VesselFeatures, VesselRegistration} from "./model/vessel.model";
import {LoadResult, EntitiesService} from "../../shared/shared.module";
import {BaseEntityService} from "../../core/core.module";
import {map} from "rxjs/operators";
import {ErrorCodes} from "./errors";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {ReferentialFragments} from "./referential.fragments";
import {VesselFilter} from "./vessel-service";
import {SortDirection} from "@angular/material/sort";
import {VesselFeaturesHistoryComponent} from "../vessel/page/vessel-features-history.component";

export const VesselFeaturesFragments = {
    vesselFeatures: gql`fragment VesselFeaturesFragment on VesselFeaturesVO {
        id
        startDate
        endDate
        name
        exteriorMarking
        administrativePower
        lengthOverAll
        grossTonnageGt
        grossTonnageGrt
        creationDate
        updateDate
        comments
        basePortLocation {
            ...LocationFragment
        }
        recorderDepartment {
            ...LightDepartmentFragment
        }
        recorderPerson {
            ...LightPersonFragment
        }
    }`,
};

export const LoadFeaturesQuery: any = gql`
    query VesselFeaturesHistory($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $vesselId: Int){
        vesselFeaturesHistory(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, vesselId: $vesselId){
            ...VesselFeaturesFragment
        }
    }
    ${VesselFeaturesFragments.vesselFeatures}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
`;

@Injectable({providedIn: 'root'})
export class VesselFeaturesService
  extends BaseEntityService
  implements EntitiesService<VesselFeatures, VesselFilter> {

  constructor(
    protected graphql: GraphqlService
  ) {
    super(graphql);
  }


  /**
   * Load vessel features history
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           filter?: VesselFilter): Observable<LoadResult<VesselFeatures>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'startDate',
      sortDirection: sortDirection || 'asc',
      vesselId: filter.vesselId
    };

    let now = Date.now();
    if (this._debug) console.debug("[vessel-features-history-service] Getting vessel features history using options:", variables);

    return this.mutableWatchQuery<{ vesselFeaturesHistory: any[] }>({
      queryName: 'LoadFeatures',
      query: LoadFeaturesQuery,
      arrayFieldName: 'vesselFeaturesHistory',
      insertFilterFn: (vr: VesselFeatures) => vr.vesselId === filter.vesselId,
      variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    })
      .pipe(
        map(({vesselFeaturesHistory}) => {
            const data = (vesselFeaturesHistory || []).map(VesselFeatures.fromObject);
            if (this._debug && now) {
              console.debug(`[vessel-features-history-service] Vessel features history loaded in ${Date.now() - now}ms`, data);
              now = null;
            }
            return { data };
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

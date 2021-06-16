import {Injectable} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {VesselFeatures} from "./model/vessel.model";
import {GraphqlService}  from "@sumaris-net/ngx-components";
import {ReferentialFragments} from "../../referential/services/referential.fragments";
import {IEntitiesService} from "@sumaris-net/ngx-components";
import {BaseEntityService} from "../../referential/services/base-entity-service.class";
import {VesselFeaturesFilter} from "./filter/vessel.filter";
import {PlatformService}  from "@sumaris-net/ngx-components";
import {isNotNil} from "@sumaris-net/ngx-components";

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

export const VesselFeatureQueries = {
  loadAll: gql`query VesselFeaturesHistory($filter: VesselFeaturesFilterVOInput!, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
        data: vesselFeaturesHistory(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
            ...VesselFeaturesFragment
        }
    }
    ${VesselFeaturesFragments.vesselFeatures}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}`
};

@Injectable({providedIn: 'root'})
export class VesselFeaturesService
  extends BaseEntityService<VesselFeatures, VesselFeaturesFilter>
  implements IEntitiesService<VesselFeatures, VesselFeaturesFilter> {

  constructor(
    graphql: GraphqlService,
    platform: PlatformService
  ) {
    super(graphql, platform,
      VesselFeatures, VesselFeaturesFilter, {
        queries: VesselFeatureQueries,
        defaultSortBy: 'startDate'
      });
  }

  async count(filter: Partial<VesselFeaturesFilter> & {vesselId: number}, opts?: {
    fetchPolicy?: FetchPolicy
  }): Promise<number> {
    const {data, total} = await this.loadAll(0, 100, null, null, filter, opts);
    return isNotNil(total) ? total : (data || []).length;
  }

  /* -- protected methods -- */

}

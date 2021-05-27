import {Injectable} from "@angular/core";
import {gql} from "@apollo/client/core";
import {VesselFeatures, VesselRegistration} from "./model/vessel.model";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {ReferentialFragments} from "../../referential/services/referential.fragments";
import {IEntitiesService, LoadResult} from "../../shared/services/entity-service.class";
import {BaseEntityService} from "../../referential/services/base-entity-service.class";
import {VesselFeaturesFilter, VesselRegistrationFilter} from "./filter/vessel.filter";
import {PlatformService} from "../../core/services/platform.service";
import {SortDirection} from "@angular/material/sort";
import {Observable} from "rxjs";

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
}

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

  /* -- protected methods -- */

}

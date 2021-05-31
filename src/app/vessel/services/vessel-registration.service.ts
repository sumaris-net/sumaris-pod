import {Injectable} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {VesselRegistration} from "./model/vessel.model";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {ReferentialFragments} from "../../referential/services/referential.fragments";
import {BaseEntityService} from "../../referential/services/base-entity-service.class";
import {PlatformService} from "../../core/services/platform.service";
import {VesselRegistrationFilter} from "./filter/vessel.filter";
import {isNotNil} from "../../shared/functions";

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

export const VesselRegistrationsQueries = {
  loadAll: gql`query VesselRegistrationHistory($filter: VesselRegistrationFilterVOInput!, , $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: vesselRegistrationHistory(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...RegistrationFragment
    }
  }
  ${RegistrationFragments.registration}
  ${ReferentialFragments.location}`
};

@Injectable({providedIn: 'root'})
export class VesselRegistrationService
  extends BaseEntityService<VesselRegistration, VesselRegistrationFilter> {

  constructor(
    graphql: GraphqlService,
    platform: PlatformService
  ) {
    super(graphql, platform, VesselRegistration, VesselRegistrationFilter, {
      queries: VesselRegistrationsQueries,
      defaultSortBy: 'startDate'
    });
  }

  async count(filter: Partial<VesselRegistrationFilter> & {vesselId: number}, opts?: {
    fetchPolicy?: FetchPolicy
  }): Promise<number> {
    const {data, total} = await this.loadAll(0, 100, null, null, filter, opts);
    return isNotNil(total) ? total : (data || []).length;
  }

  /* -- protected methods -- */

}

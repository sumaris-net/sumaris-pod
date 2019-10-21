import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {EntityUtils, Person, VesselFeatures} from "./model";
import {LoadResult, TableDataService} from "../../shared/shared.module";
import {BaseDataService} from "../../core/core.module";
import {map} from "rxjs/operators";
import {Moment} from "moment";

import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {SuggestionDataService} from "../../shared/services/data-service.class";
import {GraphqlService} from "../../core/services/graphql.service";
import {ReferentialFragments} from "./referential.queries";
import {FetchPolicy} from "apollo-client";

export declare class VesselFilter {
  date?: Date | Moment;
  vesselId?: number;
  searchText?: string;
}

export const VesselFragments = {
  lightVessel: gql`fragment LightVesselFragment on VesselFeaturesVO {
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
    vesselId
    vesselTypeId
    basePortLocation {
      ...ReferentialFragment
    }
    recorderDepartment {
     ...LightDepartmentFragment
    }
  }`,
  vessel: gql`fragment VesselFragment on VesselFeaturesVO {
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
    vesselId
    vesselTypeId
    basePortLocation {
      ...ReferentialFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    recorderPerson {
      ...LightPersonFragment
    }
  }`,
};

const LoadAllQuery: any = gql`
  query Vessels($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
    vessels(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...LightVesselFragment
    }
  }
  ${VesselFragments.lightVessel}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightDepartment}
`;
const LoadQuery: any = gql`
  query Vessel($vesselId: Int, $vesselFeaturesId: Int) {
    vessels(filter: {vesselId: $vesselId, vesselFeaturesId: $vesselFeaturesId}) {
      ...VesselFragment
    }
  }
  ${VesselFragments.vessel}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.lightPerson}
`;

const SaveVessels: any = gql`
  mutation saveVessels($vessels:[VesselFeaturesVOInput]){
    saveVessels(vessels: $vessels){
      ...VesselFragment
    }
  }
  ${VesselFragments.vessel}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.lightPerson}
`;

const DeleteVessels: any = gql`
  mutation deleteVessels($ids:[Int]){
    deleteVessels(ids: $ids)
  }
`;

@Injectable({providedIn: 'root'})
export class VesselService extends BaseDataService implements SuggestionDataService<VesselFeatures>, TableDataService<VesselFeatures, VesselFilter> {

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
      sortBy: sortBy || 'exteriorMarking',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    const now = Date.now();
    if (this._debug) console.debug("[vessel-service] Getting vessels using options:", variables);

    return this.graphql.watchQuery<{ vessels: any[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    })
      .pipe(
        map(({vessels}) => {
            const data = (vessels || []).map(VesselFeatures.fromObject);
            if (this._debug) console.debug(`[vessel-service] Vessels loaded in ${Date.now() - now}ms`, data);
            return {
              data: data
            };
          }
        )
      );
  }

  /**
   * Load many vessels
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  async loadAll(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: string,
                filter?: VesselFilter): Promise<LoadResult<VesselFeatures>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'exteriorMarking',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    console.debug("[vessel-service] Getting vessels using options:", variables);
    const res = await this.graphql.query<{ vessels: any[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    });

    const data = (res && res.vessels || []).map(VesselFeatures.fromObject);
    return {
      data: data
    };
  }

  async suggest(value: any, options?: {
    date: Date | Moment;
  }): Promise<VesselFeatures[]> {
    if (EntityUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        date: options && options.date || undefined,
        searchText: value as string
      }
    );
    return res.data;
  }

  // loadAll(offset: number,
  //         size: number,
  //         sortBy?: string,
  //         sortDirection?: string,
  //         filter?: VesselFilter): Promise<LoadResult<VesselFeatures>> {
  //   console.warn("TODO: implement loadAll");
  //   // TODO
  //   return this.watchAll(offset, size, sortBy, sortDirection, filter)
  //     .pipe(first()).toPromise();
  // }

  async load(id: number, opts? : {
    fetchPolicy?: FetchPolicy
  }): Promise<VesselFeatures | null> {
    console.debug("[vessel-service] Loading vessel " + id);

    const data = await this.graphql.query<{ vessels: any }>({
      query: LoadQuery,
      variables: {
        vesselId: id,
        vesselFeaturesId: null
      },
      fetchPolicy: opts && opts.fetchPolicy || undefined
    });

    if (data && data.vessels && data.vessels.length) {
      const res = new VesselFeatures();
      res.fromObject(data.vessels[0]);
      return res;
    }
    return null;
  }

  async loadByVesselFeaturesId(id: number): Promise<VesselFeatures | null> {
    console.debug("[vessel-service] Loading vessel by features " + id);

    const data = await this.graphql.query<{ vessels: any }>({
      query: LoadQuery,
      variables: {
        vesselId: null,
        vesselFeaturesId: id
      }
    });

    if (data && data.vessels && data.vessels.length) {
      const res = new VesselFeatures();
      res.fromObject(data.vessels[0]);
      return res;
    }
    return null;
  }

  /**
   * Save many vessels
   * @param data
   */
  async saveAll(vessels: VesselFeatures[]): Promise<VesselFeatures[]> {

    if (!vessels) return vessels;

    // Fill default properties (as recorder department and person)
    vessels.forEach(t => this.fillDefaultProperties(t));

    const json = vessels.map(t => this.asObject(t));
    console.debug("[vessel-service] Saving vessels: ", json);

    const res = await this.graphql.mutate<{ saveVessels: any }>({
      mutation: SaveVessels,
      variables: {
        vessels: json
      },
      error: {code: ErrorCodes.SAVE_VESSELS_ERROR, message: "VESSEL.ERROR.SAVE_VESSELS_ERROR"}
    });
    return (res && res.saveVessels && vessels || [])
      .map(t => {
        const data = res.saveVessels.find(res => res.id == t.id);
        t.updateDate = data && data.updateDate || t.updateDate;
        t.vesselId = data && data.vesselId || t.vesselId;
        return t;
      });
  }

  /**
   * Save a trip
   * @param data
   */
  async save(vessel: VesselFeatures): Promise<VesselFeatures> {

    // Prepare to save
    this.fillDefaultProperties(vessel);

    // Transform into json
    const json = this.asObject(vessel);

    console.debug("[vessel-service] Saving vessel: ", json);

    const res = await this.graphql.mutate<{ saveVessels: any }>({
      mutation: SaveVessels,
      variables: {
        vessels: [json]
      },
      error: {code: ErrorCodes.SAVE_VESSEL_ERROR, message: "VESSEL.ERROR.SAVE_VESSEL_ERROR"}
    });
    const data = res && res.saveVessels && res.saveVessels[0];
    vessel.id = data && data.id || vessel.id;
    vessel.updateDate = data && data.updateDate || vessel.updateDate;
    vessel.vesselId = data && data.vesselId || vessel.vesselId;
    return vessel;
  }

  deleteAll(vessels: VesselFeatures[]): Promise<any> {
    let ids = vessels && vessels
      .map(t => t.id)
      .filter(id => (id > 0));

    console.debug("[vessel-service] Deleting vessels... ids:", ids);

    return this.graphql.mutate<any>({
      mutation: DeleteVessels,
      variables: {
        ids: ids
      }
    });
  }

  /* -- protected methods -- */

  protected asObject(vessel: VesselFeatures): any {
    const copy: any = vessel.asObject(true/*minify*/);

    // If no vessel: set the default vessel type
    copy.vesselTypeId = !copy.vesselId ? 1/*TODO ?*/ : undefined;

    return copy;
  }

  protected fillDefaultProperties(vessel: VesselFeatures): void {

    // If new
    if (!vessel.id || vessel.id < 0) {

      const person: Person = this.accountService.account;

      // Recorder department
      if (person && !vessel.recorderDepartment.id && person.department) {
        vessel.recorderDepartment.id = person.department.id;
      }

      // Recorder person
      if (person && !vessel.recorderPerson.id) {
        vessel.recorderPerson.id = person.id;
      }

    }
  }
}

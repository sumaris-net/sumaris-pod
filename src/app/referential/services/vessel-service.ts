import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {Department, EntityUtils, isNotNil, Person, VesselFeatures} from "./model";
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
import {VesselFeaturesService} from "./vessel-features.service";
import {EntityAsObjectOptions, MINIFY_OPTIONS} from "../../core/services/model";

export class VesselFilter {
  date?: Date | Moment;
  vesselId?: number;
  searchText?: string;
  statusId?: number;
  statusIds?: number[];

  static isEmpty(filter: VesselFilter | any): boolean {
    return !filter || (
      !filter.date && isNilOrBlank(filter.vesselId) && isNilOrBlank(filter.searchText) && isNilOrBlank(filter.statusId) && isEmptyArray(filter.statusIds)
    );
  }
}

export const VesselFragments = {
  lightVessel: gql`fragment LightVesselFragment on VesselFeaturesVO {
    id
    startDate
    endDate
    name
    exteriorMarking
    registrationCode
    administrativePower
    lengthOverAll
    grossTonnageGt
    grossTonnageGrt
    creationDate
    updateDate
    comments
    vesselId
    vesselType {
      ...ReferentialFragment
    }
    vesselStatusId
    basePortLocation {
      ...LocationFragment
    }
    registrationLocation {
      ...LocationFragment
    }
    recorderDepartment {
     ...LightDepartmentFragment
    }
    entityName
  }`,
  vessel: gql`fragment VesselFragment on VesselFeaturesVO {
    id
    startDate
    endDate
    name
    exteriorMarking
    registrationId
    registrationCode
    registrationStartDate
    registrationEndDate
    administrativePower
    lengthOverAll
    grossTonnageGt
    grossTonnageGrt
    creationDate
    updateDate
    comments
    vesselId
    vesselType {
        ...ReferentialFragment
    }
    vesselStatusId
    basePortLocation {
      ...LocationFragment
    }
    registrationLocation {
      ...LocationFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    recorderPerson {
      ...LightPersonFragment
    }
    entityName
  }`,
};

const LoadAllQuery: any = gql`
  query Vessels($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
    vessels(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...LightVesselFragment
    }
  }
  ${VesselFragments.lightVessel}
  ${ReferentialFragments.location}
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.referential}
`;
const LoadQuery: any = gql`
  query Vessel($vesselId: Int, $vesselFeaturesId: Int) {
    vessels(filter: {vesselId: $vesselId, vesselFeaturesId: $vesselFeaturesId}) {
      ...VesselFragment
    }
  }
  ${VesselFragments.vessel}
  ${ReferentialFragments.location}
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.lightPerson}
  ${ReferentialFragments.referential}
`;

const SaveVessels: any = gql`
  mutation saveVessels($vessels:[VesselFeaturesVOInput]){
    saveVessels(vessels: $vessels){
      ...VesselFragment
    }
  }
  ${VesselFragments.vessel}
  ${ReferentialFragments.location}
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.lightPerson}
  ${ReferentialFragments.referential}
`;

const DeleteVessels: any = gql`
  mutation deleteVessels($ids:[Int]){
    deleteVessels(ids: $ids)
  }
`;

@Injectable({providedIn: 'root'})
export class VesselService
  extends BaseDataService
  implements SuggestionDataService<VesselFeatures>, TableDataService<VesselFeatures, VesselFilter>, EditorDataService<VesselFeatures, VesselFilter> {

  constructor(
    protected graphql: GraphqlService,
    private accountService: AccountService,
    private vesselFeatureService: VesselFeaturesService
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
      filter: filter && {
        date: filter.date,
        vesselId: filter.vesselId,
        searchText: filter.searchText,
        statusIds: isNotNil(filter.statusId) ?  [filter.statusId] : filter.statusIds
      }
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
      filter: {
        date: filter.date,
        vesselId: filter.vesselId,
        searchText: filter.searchText,
        statusIds: isNotNil(filter.statusId) ?  [filter.statusId] : filter.statusIds
      }
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

  async load(id: number, opts?: {
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

    return this.vesselFeatureService.save(vessel);
  }

  delete(data: VesselFeatures, options?: any): Promise<any> {
    return this.deleteAll([data]);
  }

  deleteAll(vessels: VesselFeatures[]): Promise<any> {
    const ids = vessels && vessels
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

  listenChanges(id: number, options?: any): Observable<VesselFeatures> {
    throw new Error("Method not implemented.");
  }

  /* -- protected methods -- */

  protected asObject(vessel: VesselFeatures, options?: EntityAsObjectOptions): any {
    const copy: any = vessel.asObject({ ...MINIFY_OPTIONS, options } as EntityAsObjectOptions);

    // If no vessel: set the default vessel type
    // if (!copy.vesselTypeId) {
    //   copy.vesselTypeId = (!copy.vesselTypeId && !copy.vesselId) ? 1 /*TODO ?*/ : undefined;
    // }

    return copy;
  }

  protected fillDefaultProperties(vessel: VesselFeatures): void {

    // If new
    // if (!vessel.id || vessel.id < 0) {

      const person: Person = this.accountService.account;

      // Recorder department
      if (person && person.department && (!vessel.recorderDepartment || vessel.recorderDepartment.id !== person.department.id)) {
        if (!vessel.recorderDepartment) {
          vessel.recorderDepartment = new Department();
        }
        vessel.recorderDepartment.id = person.department.id;
      }

      // Recorder person
      if (person && (!vessel.recorderPerson || vessel.recorderPerson.id !== person.id)) {
        if (!vessel.recorderPerson) {
          vessel.recorderPerson = new Person();
        }
        vessel.recorderPerson.id = person.id;
      }

    // }
  }
}

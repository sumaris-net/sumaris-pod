import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {Department, EntityUtils, isNil, isNotNil, Person, Vessel} from "./model";
import {EditorDataService, isNilOrBlank, LoadResult, TableDataService} from "../../shared/shared.module";
import {BaseDataService} from "../../core/core.module";
import {map} from "rxjs/operators";
import {Moment} from "moment";

import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {GraphqlService} from "../../core/services/graphql.service";
import {ReferentialFragments} from "./referential.queries";
import {FetchPolicy} from "apollo-client";
import {isEmptyArray} from "../../shared/functions";
import {EntityAsObjectOptions, MINIFY_OPTIONS} from "../../core/services/model";
import {VesselFeaturesFragments} from "./vessel-features.service";
import {RegistrationFragments} from "./vessel-registration.service";

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
    vessel: gql`fragment VesselFragment on VesselVO {
        id
        comments
        statusId
        #        qualityFlagId
        #        program
        creationDate
        controlDate
        validationDate
        qualificationDate
        qualificationComments
        updateDate
        comments
        vesselType {
            ...ReferentialFragment
        }
        features {
            ...VesselFeaturesFragment
        }
        registration{
            ...RegistrationFragment
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
            ...VesselFragment
        }
        vesselsCount(filter: $filter)
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}
`;
const LoadQuery: any = gql`
    query Vessel($vesselId: Int) {
        vessels(filter: {vesselId: $vesselId}) {
            ...VesselFragment
        }
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}
`;

const SaveVessels: any = gql`
    mutation saveVessels($vessels:[VesselVOInput]){
        saveVessels(vessels: $vessels){
            ...VesselFragment
        }
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
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
  implements TableDataService<Vessel, VesselFilter>, EditorDataService<Vessel, VesselFilter> {

  constructor(
    protected graphql: GraphqlService,
    private accountService: AccountService,
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
           filter?: VesselFilter): Observable<LoadResult<Vessel>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'features.exteriorMarking',
      sortDirection: sortDirection || 'asc',
      filter: filter && {
        date: filter.date,
        vesselId: filter.vesselId,
        searchText: filter.searchText,
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : filter.statusIds
      }
    };
    const now = Date.now();
    if (this._debug) console.debug("[vessel-service] Getting vessels using options:", variables);

    return this.graphql.watchQuery<{ vessels: any[]; vesselsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    })
      .pipe(
        map(({vessels, vesselsCount}) => {
            const data = (vessels || []).map(Vessel.fromObject);
            const total = vesselsCount || undefined;
            if (this._debug) console.debug(`[vessel-service] Vessels loaded in ${Date.now() - now}ms`, data);
            return {
              data: data,
              total: total
            };
          }
        )
      );
  }

  async load(id: number, opts?: {
    fetchPolicy?: FetchPolicy
  }): Promise<Vessel | null> {
    console.debug("[vessel-service] Loading vessel " + id);

    const data = await this.graphql.query<{ vessels: any }>({
      query: LoadQuery,
      variables: {
        vesselId: id
      },
      fetchPolicy: opts && opts.fetchPolicy || undefined
    });

    if (data && data.vessels && data.vessels.length) {
      const res = new Vessel();
      res.fromObject(data.vessels[0]);
      return res;
    }
    return null;
  }

  /**
   * Save many vessels
   * @param vessels
   */
  async saveAll(vessels: Vessel[]): Promise<Vessel[]> {

    if (!vessels) return vessels;

    const json = vessels.map(vessel => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(vessel);
      return this.asObject(vessel);
    });

    const now = Date.now();
    console.debug("[vessel-service] Saving vessels...", json);

    await this.graphql.mutate<{ saveVessels: Vessel[] }>({
      mutation: SaveVessels,
      variables: {
        vessels: json
      },
      error: {code: ErrorCodes.SAVE_VESSELS_ERROR, message: "VESSEL.ERROR.SAVE_VESSELS_ERROR"},
      update: (proxy, {data}) => {

        if (this._debug) console.debug(`[vessel-service] Vessels saved remotely in ${Date.now() - now}ms`, vessels);

        (data && data.saveVessels && vessels || [])
          .forEach(vessel => {
            const savedVessel = data.saveVessels.find(v => vessel.equals(v));
            if (savedVessel && savedVessel !== vessel) {
              this.copyIdAndUpdateDate(savedVessel, vessel);
            }
          });
      }
    });

    return vessels;
  }

  /**
   * Save a trip
   * @param vessel
   */
  async save(vessel: Vessel): Promise<Vessel> {

    // Prepare to save
    this.fillDefaultProperties(vessel);
    const isNew = isNil(vessel.id);

    // Transform into json
    const json = vessel.asObject();

    const now = Date.now();
    console.debug("[vessel-service] Saving vessel: ", json);

    await this.graphql.mutate<{ saveVessels: any }>({
      mutation: SaveVessels,
      variables: {
        vessels: [json]
      },
      error: {code: ErrorCodes.SAVE_VESSEL_ERROR, message: "VESSEL.ERROR.SAVE_VESSEL_ERROR"},
      update: (proxy, {data}) => {
        const savedVessel = data && data.saveVessels && data.saveVessels[0];

        if (savedVessel) {

          // Copy id and update Date
          if (savedVessel !== vessel) {
            this.copyIdAndUpdateDate(savedVessel, vessel);
            if (this._debug) console.debug(`[vessel-service] Vessel Feature saved in ${Date.now() - now}ms`, savedVessel);
          }

          // Add to cache
          if (isNew && this._lastVariables.loadAll) {
            this.graphql.addToQueryCache(proxy, {
              query: LoadAllQuery,
              variables: this._lastVariables.loadAll
            }, 'vessels', savedVessel);
          }

        }
      }
    });

    return vessel;
  }

  delete(data: Vessel, options?: any): Promise<any> {
    return this.deleteAll([data]);
  }

  deleteAll(vessels: Vessel[]): Promise<any> {
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

  listenChanges(id: number, options?: any): Observable<Vessel> {
    throw new Error("Method not implemented.");
  }

  /* -- protected methods -- */

  protected asObject(vessel: Vessel, options?: EntityAsObjectOptions): any {
    return vessel.asObject({...MINIFY_OPTIONS, options} as EntityAsObjectOptions);
  }

  protected fillDefaultProperties(vessel: Vessel): void {

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

  copyIdAndUpdateDate(source: Vessel | undefined, target: Vessel) {

    EntityUtils.copyIdAndUpdateDate(source, target);

  }
}

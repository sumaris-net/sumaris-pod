import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {EntityUtils, isNil, isNotNil, Person, StatusIds, VesselSnapshot} from "./model";
import {LoadResult} from "../../shared/shared.module";
import {BaseDataService} from "../../core/core.module";
import {Moment} from "moment";

import {ErrorCodes} from "./errors";
import {fetchAllPagesWithProgress, SuggestionDataService} from "../../shared/services/data-service.class";
import {GraphqlService} from "../../core/services/graphql.service";
import {ReferentialFragments} from "./referential.queries";
import {FetchPolicy} from "apollo-client";
import {VesselFilter, VesselFragments} from "./vessel-service";
import {BehaviorSubject, defer, Observable} from "rxjs";
import {NetworkService} from "../../core/services/network.service";
import {EntityStorage} from "../../core/services/entities-storage.service";

export const VesselSnapshotFragments = {
  lightVesselSnapshot: gql`fragment LightVesselSnapshotFragment on VesselSnapshotVO {
    id
    name
    exteriorMarking
    registrationCode
    basePortLocation {
      ...LocationFragment
    }
  }`,
  vesselSnapshot: gql`fragment VesselSnapshotFragment on VesselSnapshotVO {
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
  }`,
};

const LoadAllQuery: any = gql`
  query VesselSnapshots($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
    vesselSnapshots(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...LightVesselSnapshotFragment
    }
  }
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${ReferentialFragments.location}
`;
const LoadAllWithCountQuery: any = gql`
  query VesselSnapshots($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
    vesselSnapshots(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...LightVesselSnapshotFragment
    }
    vesselsCount(filter: $filter)
  }
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${ReferentialFragments.location}
`;
const LoadQuery: any = gql`
  query VesselSnapshot($vesselId: Int, $vesselFeaturesId: Int) {
    vesselSnapshots(filter: {vesselId: $vesselId, vesselFeaturesId: $vesselFeaturesId}) {
      ...VesselFragment
    }
  }
  ${VesselFragments.vessel}
  ${ReferentialFragments.location}
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.lightPerson}
  ${ReferentialFragments.referential}
`;

@Injectable({providedIn: 'root'})
export class VesselSnapshotService
  extends BaseDataService
  implements SuggestionDataService<VesselSnapshot> {

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected entities: EntityStorage,
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
   * @param opts
   */
  async loadAll(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: string,
                filter?: VesselFilter,
                opts?: {
                  [key: string]: any;
                  fetchPolicy?: FetchPolicy;
                  debug?: boolean;
                  withTotal?: boolean;
                  toEntity?: boolean;
                }): Promise<LoadResult<VesselSnapshot>> {

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

    const debug = this._debug && (!opts || opts.debug !== false);
    const now = debug && Date.now();
    if (debug) console.debug("[vessel-snapshot-service] Getting vessel snapshots using options:", variables);

    // Offline: use local store
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      const res = await this.entities.loadAll('VesselSnapshotVO',
        {
          ...variables,
          filter: VesselFilter.searchFilter(filter)
        }
      );
      const data = (!opts || opts.toEntity !== false) ?
        (res && res.data || []).map(VesselSnapshot.fromObject) :
        (res && res.data || []) as VesselSnapshot[];
      if (debug) console.debug(`[referential-ref-service] Vessels loaded (from offline storage) in ${Date.now() - now}ms`);
      return {
        data: data,
        total: res.total
      };
    }

    // Online: use GraphQL
    else {
      const query = (opts && opts.withTotal) ? LoadAllWithCountQuery : LoadAllQuery;
      const res = await this.graphql.query<{ vesselSnapshots: any[], vesselsCount?: number; }>({
        query,
        variables,
        error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || undefined /*use default*/
      });

      const data = (!opts || opts.toEntity !== false) ?
        (res && res.vesselSnapshots || []).map(VesselSnapshot.fromObject) :
        (res && res.vesselSnapshots || []) as VesselSnapshot[];
      return {
        data: data,
        total: res && res.vesselsCount
      };
    }
  }

  async suggest(value: any, options?: {
    date: Date | Moment;
    statusIds?: number[];
  }): Promise<VesselSnapshot[]> {
    if (EntityUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        statusIds: options && options.statusIds || undefined,
        date: options && options.date || undefined,
        searchText: value as string
      }
    );
    return res.data;
  }

  async load(id: number, opts?: {
    fetchPolicy?: FetchPolicy
  }): Promise<VesselSnapshot | null> {
    console.debug("[vessel-snapshot-service] Loading vessel snapshot " + id);

    const data = await this.graphql.query<{ vesselSnapshots: any }>({
      query: LoadQuery,
      variables: {
        vesselId: id,
        vesselFeaturesId: null
      },
      fetchPolicy: opts && opts.fetchPolicy || undefined
    });

    if (data && data.vesselSnapshots && data.vesselSnapshots.length) {
      const res = new VesselSnapshot();
      res.fromObject(data.vesselSnapshots[0]);
      return res;
    }
    return null;
  }

  executeImport(opts?: {
    maxProgression?: number;
  }): Observable<number>{

    const maxProgression = opts && opts.maxProgression || 100;
    const progression = new BehaviorSubject<number>(0);
    const filter: VesselFilter = {
      statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
    };

    const now = Date.now();
    console.info("[vessel-snapshot-service] Importing vessels (snapshot)...");

    fetchAllPagesWithProgress((offset, size) =>
        this.loadAll(offset, size, 'id', null, filter, {
          debug: false,
          fetchPolicy: "network-only",
          withTotal: (offset === 0), // Compute total only once
          toEntity: false
        }),
      progression,
      maxProgression * 0.9
    )
      .then(res =>  this.entities.saveAll(res.data, {entityName: 'VesselSnapshotVO'}))
      .then(data =>  {
        console.info(`[vessel-snapshot-service] Successfully import ${data && data.length || 0} vessels in ${Date.now() - now}ms`);
        progression.next(maxProgression);
        progression.complete();
      })
      .catch((err: any) => {
        console.error("[vessel-snapshot-service] Error during importation: " + (err && err.message || err), err);
        progression.error(err);
      });
    return progression;
  }

  /* -- protected methods -- */

}

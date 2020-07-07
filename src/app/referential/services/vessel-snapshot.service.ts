import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {isNotNil, LoadResult} from "../../shared/shared.module";
import {BaseEntityService, StatusIds} from "../../core/core.module";

import {ErrorCodes} from "./errors";
import {fetchAllPagesWithProgress, SuggestService} from "../../shared/services/entity-service.class";
import {GraphqlService} from "../../core/services/graphql.service";
import {ReferentialFragments} from "./referential.queries";
import {FetchPolicy} from "apollo-client";
import {VesselFilter} from "./vessel-service";
import {BehaviorSubject, Observable} from "rxjs";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/entities-storage.service";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {VesselSnapshot} from "./model/vessel-snapshot.model";
import {SortDirection} from "@angular/material/sort";

export const VesselSnapshotFragments = {
  lightVesselSnapshot: gql`fragment LightVesselSnapshotFragment on VesselSnapshotVO {
    id
    name
    exteriorMarking
    registrationCode
    basePortLocation {
      ...LocationFragment
    }
    vesselStatusId
  }`,
  vesselSnapshot: gql`fragment VesselSnapshotFragment on VesselSnapshotVO {
    id
    name
    exteriorMarking
    registrationCode
    basePortLocation {
      ...LocationFragment
    }
    vesselType {
      ...ReferentialFragment
    }
    vesselStatusId
  }`
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
const LoadAllWithTotalQuery: any = gql`
  query VesselSnapshotsWithTotal($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
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
      ...LightVesselSnapshotFragment
    }
  }
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${ReferentialFragments.location}
`;

@Injectable({providedIn: 'root'})
export class VesselSnapshotService
  extends BaseEntityService<VesselSnapshot, VesselFilter>
  implements SuggestService<VesselSnapshot, VesselFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected entities: EntitiesStorage,
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
                sortDirection?: SortDirection,
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
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : filter.statusIds
      }
    };

    const debug = this._debug && (!opts || opts.debug !== false);
    const now = debug && Date.now();
    if (debug) console.debug("[vessel-snapshot-service] Loading vessel snapshots using options:", variables);

    let res: LoadResult<VesselSnapshot>;

    // Offline: use local store
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      res = await this.entities.loadAll('VesselSnapshotVO',
        {
          ...variables,
          filter: VesselFilter.searchFilter(filter)
        }
      );
    }

    // Online: use GraphQL
    else {
      const query = (!opts || opts.withTotal !== false) ? LoadAllWithTotalQuery : LoadAllQuery;
      res = await this.graphql.query<{ vesselSnapshots: any[], vesselsCount?: number; }>({
        query,
        variables,
        error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || undefined /*use default*/
      })
        .then(res => {
          return {
            data: res && res.vesselSnapshots,
            total: res && res.vesselsCount
          };
        });
    }

    res.data = (!opts || opts.toEntity !== false) ?
      (res && res.data || []).map(VesselSnapshot.fromObject) :
      (res && res.data || []) as VesselSnapshot[];
    if (debug) console.debug(`[referential-ref-service] Vessels loaded (from offline storage) in ${Date.now() - now}ms`);
    return res;
  }

  async suggest(value: any, filter?: VesselFilter): Promise<VesselSnapshot[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        ...filter,
        searchText: value
      },
      { withTotal: false /* total not need */ }
    );
    return res.data;
  }

  async load(id: number, opts?: {
    fetchPolicy?: FetchPolicy,
    toEntity?: boolean;
  }): Promise<VesselSnapshot | null> {

    console.debug("[vessel-snapshot-service] Loading vessel snapshot " + id);
    let json: any;

    // Offline mode
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      json = await this.entities.load(id, 'VesselSnapshotVO');
    }

    // Online mode
    else {
      const res = await this.graphql.query<{ vesselSnapshots: any[]; }>({
        query: LoadQuery,
        variables: {
          vesselId: id,
          vesselFeaturesId: null
        },
        fetchPolicy: opts && opts.fetchPolicy || undefined
      });
      json = res && res.vesselSnapshots && res.vesselSnapshots.length && res.vesselSnapshots[0];
    }

    return json && ((!opts || opts.toEntity !== false) ? VesselSnapshot.fromObject(json) : json as VesselSnapshot)  || null;
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
        console.info(`[vessel-snapshot-service] Successfully import ${data && data.length || 0} vessels in ${Date.now() - now}ms`);
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

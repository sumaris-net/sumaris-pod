import {Injectable} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {ErrorCodes} from "./errors";
import {FilterFn, LoadResult, SuggestService} from "../../shared/services/entity-service.class";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {ReferentialFragments} from "./referential.fragments";
import {BehaviorSubject} from "rxjs";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {VesselSnapshot} from "./model/vessel-snapshot.model";
import {SortDirection} from "@angular/material/sort";
import {JobUtils} from "../../shared/services/job.utils";
import {BaseGraphqlService} from "../../core/services/base-graphql-service.class";
import {StatusIds} from "../../core/services/model/model.enum";
import {Beans, isNotEmptyArray, isNotNil, KeysEnum} from "../../shared/functions";
import {environment} from "../../../environments/environment";
import {Moment} from "moment";
import {SynchronizationStatus} from "../../data/services/model/root-data-entity.model";
import {EntityUtils} from "../../core/services/model/entity.model";
import {toDateISOString} from "../../shared/dates";


export class VesselSnapshotFilter {
  programLabel?: string;
  date?: Date | Moment;
  vesselId?: number;
  searchText?: string;
  statusId?: number;
  statusIds?: number[];
  synchronizationStatus?: SynchronizationStatus;

  static isEmpty(f: VesselSnapshotFilter|any): boolean {
    return Beans.isEmpty<VesselSnapshotFilter>({...f, synchronizationStatus: null}, VesselSnapshotFilterKeys, {
      blankStringLikeEmpty: true
    });
  }

  static searchFilter<T extends VesselSnapshot>(f: VesselSnapshotFilter): (T) => boolean {
    if (!f) return undefined;

    const filterFns: FilterFn<T>[] = [];

    // Program
    if (f.programLabel) {
      filterFns.push(t => (t.program && t.program.label === f.programLabel));
    }

    // Vessel id
    if (isNotNil(f.vesselId)) {
      filterFns.push(t => t.id === f.vesselId);
    }

    // Status
    const statusIds = (isNotEmptyArray(f.statusIds) && f.statusIds) || (isNotNil(f.statusId) && [f.statusId]);
    if (statusIds) {
      filterFns.push(t => statusIds.includes(t.vesselStatusId));
    }

    // Synchronization status
    if (f.synchronizationStatus) {
      if (f.synchronizationStatus === 'SYNC') {
        filterFns.push(EntityUtils.isRemote);
      }
      else {
        filterFns.push(EntityUtils.isLocal);
      }
    }

    const searchTextFilter = EntityUtils.searchTextFilter(['name', 'exteriorMarking', 'registrationCode'], f.searchText);
    if (searchTextFilter) filterFns.push(searchTextFilter);

    if (!filterFns.length) return undefined;

    return (entity) => !filterFns.find(fn => !fn(entity));
  }

  /**
   * Clean a filter, before sending to the pod (e.g remove 'synchronizationStatus')
   * @param f
   */
  static asPodObject(f: VesselSnapshotFilter): any {
    if (!f) return f;
    return {
      programLabel: f.programLabel,
      vesselId: f.vesselId,
      searchText: f.searchText,
      statusIds: isNotNil(f.statusId) ? [f.statusId] : f.statusIds,
      // Serialize date
      date: f && toDateISOString(f.date),
      // Remove sync status, as it not exists in pod
      //synchronizationStatus: undefined
    };
  }
}

export const VesselSnapshotFilterKeys: KeysEnum<VesselSnapshotFilter> = {
  programLabel: true,
  date: true,
  vesselId: true,
  searchText: true,
  statusId: true,
  statusIds: true,
  synchronizationStatus: true
};

export const VesselSnapshotFragments = {
  lightVesselSnapshot: gql`fragment LightVesselSnapshotFragment on VesselSnapshotVO {
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
    data: vesselSnapshots(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...LightVesselSnapshotFragment
    }
  }
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.location}
`;
const LoadAllWithTotalQuery: any = gql`
  query VesselSnapshotsWithTotal($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
    data: vesselSnapshots(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      ...LightVesselSnapshotFragment
    }
    total: vesselsCount(filter: $filter)
  }
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${ReferentialFragments.location}
  ${ReferentialFragments.referential}
`;
const LoadQuery: any = gql`
  query VesselSnapshot($vesselId: Int, $vesselFeaturesId: Int) {
    data: vesselSnapshots(filter: {vesselId: $vesselId, vesselFeaturesId: $vesselFeaturesId}) {
      ...LightVesselSnapshotFragment
    }
  }
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.location}
`;

@Injectable({providedIn: 'root'})
export class VesselSnapshotService
  extends BaseGraphqlService<VesselSnapshot, VesselSnapshotFilter>
  implements SuggestService<VesselSnapshot, VesselSnapshotFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected entities: EntitiesStorage,
  ) {
    super(graphql, environment);
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
                filter?: VesselSnapshotFilter,
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
      filter: VesselSnapshotFilter.asPodObject(filter)
    };

    const debug = this._debug && (!opts || opts.debug !== false);
    const now = debug && Date.now();
    if (debug) console.debug("[vessel-snapshot-service] Loading vessel snapshots using options:", variables);

    let res: LoadResult<VesselSnapshot>;

    // Offline: use local store
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      res = await this.entities.loadAll(VesselSnapshot.TYPENAME,
        {
          ...variables,
          filter: VesselSnapshotFilter.searchFilter(filter)
        }
      );
    }

    // Online: use GraphQL
    else {
      const query = (!opts || opts.withTotal !== false) ? LoadAllWithTotalQuery : LoadAllQuery;
      res = await this.graphql.query<LoadResult<any>>({
        query,
        variables,
        error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || undefined /*use default*/
      });
    }

    const entities = (!opts || opts.toEntity !== false) ?
      (res && res.data || []).map(VesselSnapshot.fromObject) :
      (res && res.data || []) as VesselSnapshot[];
    const total = res && res.total || entities.length;
    if (debug) console.debug(`[vessel-snapshot-service] Vessels loaded in ${Date.now() - now}ms`);
    return {data: entities, total};
  }

  async suggest(value: any, filter?: VesselSnapshotFilter): Promise<VesselSnapshot[]> {
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

    console.debug(`[vessel-snapshot-service] Loading vessel snapshot #${id}`);

    // Offline mode
    const offline = (id < 0) || (this.network.offline && (!opts || opts.fetchPolicy !== 'network-only'));
    if (offline) {
      const data = await this.entities.load<VesselSnapshot>(id, VesselSnapshot.TYPENAME);
      if (!data) throw {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"};
      return ((!opts || opts.toEntity !== false) ? VesselSnapshot.fromObject(data) : data as VesselSnapshot) || null;
    }

    const { data } = await this.graphql.query<{ data: any[]; }>({
      query: LoadQuery,
      variables: {
        vesselId: id,
        vesselFeaturesId: null
      },
      fetchPolicy: opts && opts.fetchPolicy || undefined
    });
    const res = data && data[0];
    return res && ((!opts || opts.toEntity !== false) ? VesselSnapshot.fromObject(res) : res as VesselSnapshot)  || null;
  }

  async executeImport(progression: BehaviorSubject<number>,
    opts?: {
      maxProgression?: number;
    }): Promise<void> {

    const maxProgression = opts && opts.maxProgression || 100;
    const filter: VesselSnapshotFilter = {
      statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
    };

    console.info("[vessel-snapshot-service] Importing vessels (snapshot)...");

    const res = await JobUtils.fetchAllPages((offset, size) =>
        this.loadAll(offset, size, 'id', null, filter, {
          debug: false,
          fetchPolicy: "network-only",
          withTotal: (offset === 0), // Compute total only once
          toEntity: false
        }),
      progression,
      {
        maxProgression: maxProgression * 0.9,
        logPrefix: '[vessel-snapshot-service]'
      }
    );

    // Save locally
    await this.entities.saveAll(res.data, {entityName: VesselSnapshot.TYPENAME});
  }

  /* -- protected methods -- */

}

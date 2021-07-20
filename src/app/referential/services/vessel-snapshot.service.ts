import {Injectable} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {ErrorCodes} from "./errors";
import {LoadResult, SuggestService} from "@sumaris-net/ngx-components";
import {GraphqlService}  from "@sumaris-net/ngx-components";
import {ReferentialFragments} from "./referential.fragments";
import {BehaviorSubject} from "rxjs";
import {NetworkService}  from "@sumaris-net/ngx-components";
import {EntitiesStorage}  from "@sumaris-net/ngx-components";
import {ReferentialUtils}  from "@sumaris-net/ngx-components";
import {VesselSnapshot} from "./model/vessel-snapshot.model";
import {SortDirection} from "@angular/material/sort";
import {JobUtils} from "@sumaris-net/ngx-components";
import {BaseGraphqlService}  from "@sumaris-net/ngx-components";
import {StatusIds}  from "@sumaris-net/ngx-components";
import {environment} from "../../../environments/environment";
import {EntityUtils}  from "@sumaris-net/ngx-components";
import {VesselSnapshotFilter} from "./filter/vessel.filter";


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
                filter?: Partial<VesselSnapshotFilter>,
                opts?: {
                  [key: string]: any;
                  fetchPolicy?: FetchPolicy;
                  debug?: boolean;
                  withTotal?: boolean;
                  toEntity?: boolean;
                }): Promise<LoadResult<VesselSnapshot>> {

    filter = this.asFilter(filter);

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'exteriorMarking',
      sortDirection: sortDirection || 'asc'
    };

    const debug = this._debug && (!opts || opts.debug !== false);
    const now = debug && Date.now();
    if (debug) console.debug("[vessel-snapshot-service] Loading vessel snapshots using options:", variables);

    const withTotal = (!opts || opts.withTotal !== false);
    let res: LoadResult<VesselSnapshot>;

    // Offline: use local store
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      res = await this.entities.loadAll(VesselSnapshot.TYPENAME,
        {
          ...variables,
          filter: filter && filter.asFilterFn()
        }
      );
    }

    // Online: use GraphQL
    else {
      const query = withTotal ? LoadAllWithTotalQuery : LoadAllQuery;
      res = await this.graphql.query<LoadResult<any>>({
        query,
        variables: {
          ...variables,
          filter: filter && filter.asPodObject()
        },
        error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || undefined /*use default*/
      });
    }

    const entities = (!opts || opts.toEntity !== false) ?
      (res?.data || []).map(VesselSnapshot.fromObject) :
      (res?.data || []) as VesselSnapshot[];

    const total = res?.total || entities.length;
    res = {
      data: entities,
      total: res?.total || entities.length
    }

    // Add fetch more capability, if total was fetched
    if (withTotal) {
      const nextOffset = offset + entities.length;
      if (nextOffset < res.total) {
        res.fetchMore = () => this.loadAll(nextOffset, size, sortBy, sortDirection, filter, opts);
      }
    }

    if (debug) console.debug(`[vessel-snapshot-service] Vessels loaded in ${Date.now() - now}ms`);
    return res;
  }

  async suggest(value: any, filter?: VesselSnapshotFilter): Promise<VesselSnapshot[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        ...filter,
        searchText: value
      }
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
    const filter: Partial<VesselSnapshotFilter> = {
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

  asFilter(source: Partial<VesselSnapshotFilter>) {
    return VesselSnapshotFilter.fromObject(source);
  }

  /* -- protected methods -- */

}

import {Injectable, Injector} from "@angular/core";
import {
  BaseEntityService,
  environment,
  isNotNil,
  LoadResult,
  EntitiesService,
  toDateISOString,
  isNil
} from "../../core/core.module";
import {AggregatedLanding} from "./model/aggregated-landing.model";
import {Moment} from "moment";
import {ErrorCodes} from "./trip.errors";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {Beans} from "../../shared/functions";
import gql from "graphql-tag";
import {VesselSnapshotFragments} from "../../referential/services/vessel-snapshot.service";
import {ReferentialFragments} from "../../referential/services/referential.fragments";
import {Observable} from "rxjs";
import {filter, map, tap} from "rxjs/operators";
import {SynchronizationStatus} from "../../data/services/model/root-data-entity.model";
import {SortDirection} from "@angular/material/sort";
import {Landing} from "./model/landing.model";
import {LandingFragments} from "./landing.service";
import {DataEntityAsObjectOptions} from "../../data/services/model/data-entity.model";
import {MINIFY_OPTIONS} from "../../core/services/model/referential.model";

export class AggregatedLandingFilter {
  programLabel?: string;
  startDate?: Moment;
  endDate?: Moment;
  locationId?: number;
  observedLocationId?: number;
  synchronizationStatus?: SynchronizationStatus;

  static equals(f1: AggregatedLandingFilter | any, f2: AggregatedLandingFilter | any): boolean {
    return (isNil(f1) && isNil(f2)) ||
      (
        isNotNil(f1) && isNotNil(f2) &&
        f1.programLabel === f2.programLabel &&
        f1.observedLocationId === f2.observedLocationId &&
        f1.locationId === f2.locationId &&
        f1.synchronizationStatus === f2.synchronizationStatus &&
        ((!f1.startDate && !f2.startDate) || (f1.startDate.isSame(f2.startDate))) &&
        ((!f1.endDate && !f2.endDate) || (f1.endDate.isSame(f2.endDate)))
      )
  }

  static isEmpty(f: AggregatedLandingFilter | any): boolean {
    return Beans.isEmpty({...f, synchronizationStatus: null}, undefined, {
      blankStringLikeEmpty: true
    });
  }

  static searchFilter<T extends AggregatedLanding>(filter: AggregatedLandingFilter): (T) => boolean {
    if (AggregatedLandingFilter.isEmpty(filter)) return undefined;
    return (data: T) => {

      // observedLocationId
      if (isNotNil(filter.observedLocationId) && filter.observedLocationId !== data.observedLocationId) return false;

    };
  }

}

const VesselActivityFragment = gql`fragment VesselActivityFragment on VesselActivityVO {
  __typename
  date
  rankOrder
  comments
  measurementValues
  metiers {
    ...ReferentialFragment
  }
  observedLocationId
  landingId
  tripId
}
${ReferentialFragments.referential}`;

const AggregatedLandingFragment = gql`fragment AggregatedLandingFragment on AggregatedLandingVO {
  __typename
  id
  vesselSnapshot {
    ...LightVesselSnapshotFragment
  }
  vesselActivities {
    ...VesselActivityFragment
  }
}
${VesselSnapshotFragments.lightVesselSnapshot}
${ReferentialFragments.location}
${VesselActivityFragment}`;

// Search query
const LoadAllQuery: any = gql`
  query AggregatedLandings($filter: AggregatedLandingFilterVOInput){
    aggregatedLandings(filter: $filter){
      ...AggregatedLandingFragment
    }
  }
  ${AggregatedLandingFragment}
`;
// Save all query
const SaveAllQuery: any = gql`
  mutation SaveAggregatedLandings($aggregatedLandings:[AggregatedLandingVOInput], $filter: AggregatedLandingFilterVOInput){
    saveAggregatedLandings(aggregatedLandings: $aggregatedLandings, filter: $filter){
      ...AggregatedLandingFragment
    }
  }
  ${AggregatedLandingFragment}
`;

@Injectable({providedIn: 'root'})
export class AggregatedLandingService
  extends BaseEntityService<AggregatedLanding, AggregatedLandingFilter>
  implements EntitiesService<AggregatedLanding, AggregatedLandingFilter> {

  protected loading = false;
  private _lastFilter;

  constructor(
    injector: Injector,
    protected network: NetworkService,
    protected entities: EntitiesStorage
  ) {
    super(injector.get(GraphqlService));

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           dataFilter?: AggregatedLandingFilter,
           options?: any): Observable<LoadResult<AggregatedLanding>> {

    this._lastFilter = {
      ...dataFilter,
      // Serialize all dates
      startDate: dataFilter && toDateISOString(dataFilter.startDate),
      endDate: dataFilter && toDateISOString(dataFilter.endDate),
      // Remove fields that not exists in pod
      synchronizationStatus: undefined
    };

    const variables: any = {
      filter: this._lastFilter
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug("[aggregated-landing-service] Loading aggregated landings... using options:", variables);

    let $loadResult: Observable<{ aggregatedLandings: AggregatedLanding[] }>;
    const offline = this.network.offline || (dataFilter && dataFilter.synchronizationStatus && dataFilter.synchronizationStatus !== 'SYNC') || false;

    if (offline) {
      $loadResult = this.entities.watchAll<AggregatedLanding>('AggregatedLandingVO', {
        ...variables,
        filter: AggregatedLandingFilter.searchFilter<AggregatedLanding>(dataFilter)
      })
        .pipe(
          tap(() => {
              if (this._debug) console.debug(`[aggregated-landing-service] Aggregated landings loaded (from offline storage) in ${Date.now() - now}ms`);
            }
          ),
          map(res => {
            return {aggregatedLandings: res && res.data};
          }));

    } else {

      $loadResult = this.mutableWatchQuery<{ aggregatedLandings: AggregatedLanding[] }>({
        queryName: 'LoadAll',
        query: LoadAllQuery,
        arrayFieldName: 'aggregatedLandings',
        insertFilterFn: AggregatedLandingFilter.searchFilter(dataFilter),
        variables,
        error: {code: ErrorCodes.LOAD_AGGREGATED_LANDINGS_ERROR, message: "AGGREGATED_LANDING.ERROR.LOAD_ALL_ERROR"},
        fetchPolicy: options && options.fetchPolicy || (this.network.offline ? 'cache-only' : 'cache-and-network')
      })
        .pipe(
          filter(() => !this.loading)
        );
    }

    return $loadResult.pipe(
      filter(isNotNil),
      map(res => {
        const data = (res && res.aggregatedLandings || []).map(AggregatedLanding.fromObject);
        if (now) {
          console.debug(`[aggregated-landing-service] Loaded {${data.length || 0}} landings in ${Date.now() - now}ms`, data);
          now = undefined;
        }
        return {
          data: data,
          total: undefined
        };
      })
    );
  }

  async saveAll(entities: AggregatedLanding[], options?: any): Promise<AggregatedLanding[]> {
    if (!entities) return entities;

    const json = entities.map(t => {
      return this.asObject(t);
    });

    const now = Date.now();
    if (this._debug) console.debug("[aggregated-landing-service] Saving aggregated landings...", json);

    await this.graphql.mutate<{ saveAggregatedLandings: AggregatedLanding[] }>({
      mutation: SaveAllQuery,
      variables: {
        aggregatedLandings: json,
        filter: this._lastFilter
      },
      error: {code: ErrorCodes.SAVE_AGGREGATED_LANDINGS_ERROR, message: "AGGREGATED_LANDING.ERROR.SAVE_ALL_ERROR"},
      update: (proxy, {data}) => {

        if (this._debug) console.debug(`[aggregated-landing-service] Aggregated landings saved remotely in ${Date.now() - now}ms`, entities);

        entities = (data && data.saveAggregatedLandings || []);

      }
    });

    return entities;
  }

  deleteAll(data: AggregatedLanding[], options?: any): Promise<any> {
    throw new Error('AggregatedLandingService.deleteAll() not implemented yet');
  }

  protected asObject(entity: AggregatedLanding, options?: DataEntityAsObjectOptions) {
    options = {...MINIFY_OPTIONS, ...options};
    const copy: any = entity.asObject(options);

    if (options.minify && !options.keepEntityName && !options.keepTypename) {
      // Clean vessel features object, before saving
      copy.vesselSnapshot = {id: entity.vesselSnapshot && entity.vesselSnapshot.id};

      // Keep id only, on activity.metier
      (copy.vesselActivities || []).forEach(activity => activity.metiers = (activity.metiers || []).map(metier => ({id: metier.id})));
    }

    return copy;
  }
}

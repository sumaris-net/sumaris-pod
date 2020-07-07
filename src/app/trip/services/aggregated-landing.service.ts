import {Injectable, Injector} from "@angular/core";
import {BaseEntityService, environment, isNotNil, LoadResult, EntitiesService, toDateISOString} from "../../core/core.module";
import {AggregatedLanding} from "./model/aggregated-landing.model";
import {Moment} from "moment";
import {ErrorCodes} from "./trip.errors";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/entities-storage.service";
import {GraphqlService} from "../../core/services/graphql.service";
import {Beans} from "../../shared/functions";
import gql from "graphql-tag";
import {VesselSnapshotFragments} from "../../referential/services/vessel-snapshot.service";
import {ReferentialFragments} from "../../referential/services/referential.queries";
import {Observable} from "rxjs";
import {filter, map, tap} from "rxjs/operators";
import {SynchronizationStatus} from "../../data/services/model/root-data-entity.model";
import {SortDirection} from "@angular/material/sort";

export class AggregatedLandingFilter {
  programLabel?: string;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationId?: number;
  observedLocationId?: number;
  synchronizationStatus?: SynchronizationStatus;

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
    ...MetierFragment
  }
  tripId
}
${ReferentialFragments.metier}`;

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


@Injectable({providedIn: 'root'})
export class AggregatedLandingService
  extends BaseEntityService<AggregatedLanding, AggregatedLandingFilter>
  implements EntitiesService<AggregatedLanding, AggregatedLandingFilter> {

  protected loading = false;

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

    const variables: any = {
      filter: {
        ...dataFilter,
        // Serialize all dates
        startDate: dataFilter && toDateISOString(dataFilter.startDate),
        endDate: dataFilter && toDateISOString(dataFilter.endDate),
        // Remove fields that not exists in pod
        synchronizationStatus: undefined
      }
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
        variables,
        error: {code: ErrorCodes.LOAD_AGGREGATED_LANDINGS_ERROR, message: "AGGREGATED_LANDING.ERROR.LOAD_ALL_ERROR"},
        fetchPolicy: options && options.fetchPolicy || 'cache-and-network'
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

  saveAll(data: AggregatedLanding[], options?: any): Promise<AggregatedLanding[]> {
    throw new Error('AggregatedLandingService.saveAll() not implemented yet');
  }

  deleteAll(data: AggregatedLanding[], options?: any): Promise<any> {
    throw new Error('AggregatedLandingService.deleteAll() not implemented yet');
  }


}

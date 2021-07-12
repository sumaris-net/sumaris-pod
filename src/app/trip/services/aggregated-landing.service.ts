import {Injectable, Injector} from '@angular/core';
import {AggregatedLanding} from './model/aggregated-landing.model';
import {ErrorCodes} from './trip.errors';
import {BaseGraphqlService, EntitiesStorage, GraphqlService, IEntitiesService, isNotNil, LoadResult, NetworkService, toDateISOString} from '@sumaris-net/ngx-components';
import {gql} from '@apollo/client/core';
import {VesselSnapshotFragments} from '../../referential/services/vessel-snapshot.service';
import {ReferentialFragments} from '../../referential/services/referential.fragments';
import {Observable} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {SortDirection} from '@angular/material/sort';
import {DataEntityAsObjectOptions} from '../../data/services/model/data-entity.model';
import {environment} from '../../../environments/environment';
import {MINIFY_OPTIONS} from '@app/core/services/model/referential.model';
import {AggregatedLandingFilter} from '@app/trip/services/filter/aggregated-landing.filter';

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
${ReferentialFragments.referential}
${VesselActivityFragment}`;

// Search query
const LoadAllQuery: any = gql`
  query AggregatedLandings($filter: AggregatedLandingFilterVOInput){
    data: aggregatedLandings(filter: $filter){
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
  extends BaseGraphqlService<AggregatedLanding, AggregatedLandingFilter>
  implements IEntitiesService<AggregatedLanding, AggregatedLandingFilter> {

  protected loading = false;
  private _lastFilter;

  constructor(
    injector: Injector,
    protected network: NetworkService,
    protected entities: EntitiesStorage
  ) {
    super(injector.get(GraphqlService), environment);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           dataFilter?: Partial<AggregatedLandingFilter>,
           options?: any): Observable<LoadResult<AggregatedLanding>> {

    // Update previous filter
    dataFilter = this.asFilter(dataFilter);
    this._lastFilter = dataFilter.clone();

    // TODO: manage offset/size/sort ?
    const variables: any = {};

    let now = this._debug && Date.now();
    if (this._debug) console.debug("[aggregated-landing-service] Loading aggregated landings... using options:", variables);

    let res: Observable<LoadResult<AggregatedLanding>>;

    const offline = this.network.offline || (dataFilter && dataFilter.synchronizationStatus && dataFilter.synchronizationStatus !== 'SYNC') || false;
    if (offline) {
      res = this.entities.watchAll<AggregatedLanding>('AggregatedLandingVO', {
        ...variables,
        filter: dataFilter && dataFilter.asFilterFn()
      });

    } else {

      res = this.mutableWatchQuery<LoadResult<AggregatedLanding>>({
        queryName: 'LoadAll',
        query: LoadAllQuery,
        arrayFieldName: 'data',
        insertFilterFn: dataFilter && dataFilter.asFilterFn(),
        variables: {
          ...variables,
          filter: dataFilter && dataFilter.asPodObject()
        },
        error: {code: ErrorCodes.LOAD_AGGREGATED_LANDINGS_ERROR, message: "AGGREGATED_LANDING.ERROR.LOAD_ALL_ERROR"},
        fetchPolicy: options && options.fetchPolicy || (this.network.offline ? 'cache-only' : 'cache-and-network')
      })
      .pipe(
        filter(() => !this.loading)
      );
    }

    return res.pipe(
      filter(isNotNil),
      map(res => {
        const data = (res && res.data || []).map(AggregatedLanding.fromObject);
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

    const json = entities.map(t => this.asObject(t));

    const now = Date.now();
    if (this._debug) console.debug("[aggregated-landing-service] Saving aggregated landings...", json);

    await this.graphql.mutate<{ saveAggregatedLandings: AggregatedLanding[] }>({
      mutation: SaveAllQuery,
      variables: {
        aggregatedLandings: json,
        filter: this._lastFilter && this._lastFilter.asPodObject()
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

  asFilter(filter: Partial<AggregatedLandingFilter>): AggregatedLandingFilter {
    return AggregatedLandingFilter.fromObject(filter);
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

import {Injectable, Injector} from '@angular/core';
import {AggregatedLanding} from './model/aggregated-landing.model';
import {Moment} from 'moment';
import {ErrorCodes} from './trip.errors';
import {
  BaseGraphqlService,
  EntitiesStorage,
  EntityAsObjectOptions,
  EntityClass,
  EntityFilter,
  FilterFn,
  fromDateISOString,
  GraphqlService,
  IEntitiesService,
  isNotNil,
  LoadResult,
  NetworkService,
  toDateISOString
} from '@sumaris-net/ngx-components';
import {gql} from '@apollo/client/core';
import {VesselSnapshotFragments} from '../../referential/services/vessel-snapshot.service';
import {ReferentialFragments} from '../../referential/services/referential.fragments';
import {Observable} from 'rxjs';
import {filter, map} from 'rxjs/operators';
import {SynchronizationStatus} from '../../data/services/model/root-data-entity.model';
import {SortDirection} from '@angular/material/sort';
import {DataEntityAsObjectOptions} from '../../data/services/model/data-entity.model';
import {environment} from '../../../environments/environment';
import {MINIFY_OPTIONS} from '@app/core/services/model/referential.model';

@EntityClass()
export class AggregatedLandingFilter extends EntityFilter<AggregatedLandingFilter, AggregatedLanding> {

  static fromObject: (source: any, opts?: any) => AggregatedLandingFilter;

  // FIXME: remove
  static searchFilter(source: any): FilterFn<AggregatedLanding> {
    return source && AggregatedLandingFilter.fromObject(source).asFilterFn();
  }

  programLabel?: string;
  startDate?: Moment;
  endDate?: Moment;
  locationId?: number;
  observedLocationId?: number;
  synchronizationStatus?: SynchronizationStatus;


  equals(f2: AggregatedLandingFilter): boolean {
    return isNotNil(f2)
      && this.programLabel === f2.programLabel
      && this.observedLocationId === f2.observedLocationId
      && this.locationId === f2.locationId
      && this.synchronizationStatus === f2.synchronizationStatus
      && ((!this.startDate && !f2.startDate) || (this.startDate.isSame(f2.startDate)))
      && ((!this.endDate && !f2.endDate) || (this.endDate.isSame(f2.endDate)));
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.programLabel = source.programLabel;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.locationId = source.locationId;
    this.observedLocationId = source.observedLocationId;
    this.synchronizationStatus = source.synchronizationStatus;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    return {
      programLabel: this.programLabel,
      locationId: this.locationId,
      startDate: toDateISOString(this.startDate),
      endDate: toDateISOString(this.endDate),
      observedLocationId: this.observedLocationId
    };
  }

  protected buildFilter(): FilterFn<AggregatedLanding>[] {
    const filterFns = super.buildFilter();

    // FIXME: this properties cannot b filtered locally, because not exists !
    /*// Program
    if (isNotNilOrBlank(this.programLabel)) {
      const programLabel = this.programLabel;
      filterFns.push(t => (t.program && t.program.label === this.programLabel));
    }

    // Location
    if (isNotNil(this.locationId)) {
      filterFns.push((entity) => entity.location && entity.location.id === this.locationId);
    }

    // Start/end period
    if (this.startDate) {
      const startDate = this.startDate.clone();
      filterFns.push(t => t.dateTime && startDate.isSameOrBefore(t.dateTime));
    }
    if (this.endDate) {
      const endDate = this.endDate.clone().add(1, 'day').startOf('day');
      filterFns.push(t => t.dateTime && endDate.isAfter(t.dateTime));
    }*/

    // observedLocationId
    if (isNotNil(this.observedLocationId)) {
      filterFns.push((entity) => entity.observedLocationId === this.observedLocationId);
    }

    return filterFns;
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

    let res: Observable<LoadResult<AggregatedLanding>>;

    const offline = this.network.offline || (dataFilter && dataFilter.synchronizationStatus && dataFilter.synchronizationStatus !== 'SYNC') || false;
    if (offline) {
      res = this.entities.watchAll<AggregatedLanding>('AggregatedLandingVO', {
        ...variables,
        filter: dataFilter.asFilterFn()
      });

    } else {


      res = this.mutableWatchQuery<{ data: AggregatedLanding[] }>({
        queryName: 'LoadAll',
        query: LoadAllQuery,
        arrayFieldName: 'data',
        insertFilterFn: dataFilter && dataFilter.asFilterFn(),
        variables,
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

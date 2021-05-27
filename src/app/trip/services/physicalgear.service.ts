import {Injectable, InjectionToken} from "@angular/core";
import {BaseGraphqlService} from "../../core/services/base-graphql-service.class";
import {IEntitiesService, LoadResult} from "../../shared/services/entity-service.class";
import {PhysicalGear, Trip} from "./model/trip.model";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {NetworkService} from "../../core/services/network.service";
import {AccountService} from "../../core/services/account.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {environment} from "../../../environments/environment";
import {EMPTY, Observable} from "rxjs";
import {arrayDistinct, getPropertyByPath, isNil} from "../../shared/functions";
import {filter, map, throttleTime} from "rxjs/operators";

import {ErrorCodes} from "./trip.errors";
import {gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {PhysicalGearFragments} from "./trip.queries";
import {ReferentialFragments} from "../../referential/services/referential.fragments";
import {SortDirection} from "@angular/material/sort";
import {fromDateISOString} from "../../shared/dates";
import {TripFilter} from "./filter/trip.filter";
import {PhysicalGearFilter} from "./filter/physical-gear.filter";
import {EntityUtils} from "../../core/services/model/entity.model";
import {CryptoService} from "../../core/services/crypto.service";


const LoadAllQuery: any = gql`
  query PhysicalGears($filter: PhysicalGearFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    data: physicalGears(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...PhysicalGearFragment
      trip {
        departureDateTime
        returnDateTime
      }
    }
  }
  ${PhysicalGearFragments.physicalGear}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.lightDepartment}
`;


const sortByTripDateFn = (n1: PhysicalGear, n2: PhysicalGear) => {
  const d1 = n1.trip && (n1.trip.returnDateTime || n1.trip.departureDateTime);
  const d2 = n2.trip && (n2.trip.returnDateTime || n2.trip.departureDateTime);
  return d1.isSame(d2) ? 0 : (d1.isAfter(d2) ? 1 : -1);
};

export const PHYSICAL_GEAR_DATA_SERVICE = new InjectionToken<IEntitiesService<PhysicalGear, PhysicalGearFilter>>('PhysicalGearDataService');


@Injectable({providedIn: 'root'})
export class PhysicalGearService extends BaseGraphqlService
  implements IEntitiesService<PhysicalGear, PhysicalGearFilter> {

  loading = false;

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected entities: EntitiesStorage,
    protected cryptoService: CryptoService
  ) {
    super(graphql, environment);

    // -- For DEV only
    this._debug = !environment.production;
  }

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: SortDirection,
    dataFilter?: Partial<PhysicalGearFilter>,
    opts?: {
      distinctByRankOrder?: boolean;
      fetchPolicy?: WatchQueryFetchPolicy;
    }
  ): Observable<LoadResult<PhysicalGear>> {

    // If offline, load locally
    const offlineData = this.network.offline || (dataFilter && dataFilter.tripId < 0) || false;
    if (offlineData) {
      return this.watchAllLocally(offset, size, sortBy, sortDirection, dataFilter, opts);
    }

    if (!dataFilter || isNil(dataFilter.vesselId)) {
      console.warn("[physical-gear-service] Trying to load gears without 'filter.vesselId'. Skipping.");
      return EMPTY;
    }

    dataFilter = this.asFilter(dataFilter);

    const variables: any = {
      offset: offset || 0,
      size: size >= 0 ? size : 1000,
      sortBy: (sortBy !== 'id' && sortBy) || 'rankOrder',
      sortDirection: sortDirection || 'desc',
      filter: dataFilter && dataFilter.asPodObject()
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug("[physical-gear-service] Loading physical gears... using options:", variables);

    return this.graphql.watchQuery<LoadResult<any>>({
        query: LoadAllQuery,
        variables,
        error: {code: ErrorCodes.LOAD_PHYSICAL_GEARS_ERROR, message: "TRIP.PHYSICAL_GEAR.ERROR.LOAD_PHYSICAL_GEARS_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || undefined
      })
      .pipe(
        throttleTime(200), // avoid multiple call
        filter(() => !this.loading),
        map((res) => {
          let data = (res && res.data || []).map(PhysicalGear.fromObject);
          if (now) {
            console.debug(`[physical-gear-service] Loaded ${data.length} physical gears in ${Date.now() - now}ms`);
            now = undefined;
          }

          // Sort by trip date
          if (dataFilter && dataFilter.vesselId && isNil(dataFilter.tripId)) {
            data.sort(sortByTripDateFn);
          }

          if (opts && opts.distinctByRankOrder === true) {
            data = arrayDistinct(data, ['gear.id', 'rankOrder', 'measurementValues']);
          }

          return {
            data,
            total: data.length
          };
        })
      );
  }

  async deleteAll(data: PhysicalGear[], options?: any): Promise<any> {
    console.error('PhysicalGearService.deleteAll() not implemented yet');
  }

  async saveAll(data: PhysicalGear[], options?: any): Promise<PhysicalGear[]> {
    console.error('PhysicalGearService.saveAll() not implemented yet !');
    return data;
  }

  watchAllLocally(
      offset: number,
      size: number,
      sortBy?: string,
      sortDirection?: SortDirection,
      filter?: Partial<PhysicalGearFilter>,
      opts?: {
        distinctByRankOrder?: boolean;
      }
  ): Observable<LoadResult<PhysicalGear>> {
    if (!filter || isNil(filter.vesselId)) {
      console.warn("[physical-gear-service] Trying to load gears without 'filter.vesselId'. Skipping.");
      return EMPTY;
    }

    const tripFilter = TripFilter.fromObject({
      vesselId: filter && filter.vesselId,
      startDate: filter && filter.startDate,
      endDate: filter && filter.endDate
    });

    const variables: any = {
      offset: offset || 0,
      size: size >= 0 ? size : 1000,
      sortBy: (sortBy !== 'id' && sortBy) || 'rankOrder',
      sortDirection: sortDirection || 'desc',
      filter: tripFilter.asFilterFn()
    };

    if (this._debug) console.debug("[physical-gear-service] Loading physical gears locally... using options:", variables);

    // First, search on trips
    return this.entities.watchAll<Trip>(Trip.TYPENAME, variables)
      .pipe(
        // Get trips array
        map(res =>  res && res.data || []),
        // Extract physical gears
        // TODO: group by unique gear (from a hash (GEAR.LABEL + measurements))
        map(trips => {
          let data: PhysicalGear[] = trips.reduce((res, trip) => {
            // Exclude if no gears
            const tripDate = fromDateISOString(trip.returnDateTime || trip.departureDateTime);
            if (!trip.gears || !tripDate
              // Or if endDate <= trip date
              || (filter.startDate && tripDate.isBefore(filter.startDate))
              || (filter.endDate && tripDate.isSameOrAfter(filter.endDate))
              // Or excluded trip id (e.g to ignore current trip gears)
              || (filter.excludeTripId && trip.id === filter.excludeTripId)) {
              return res;
            }

            return res.concat(trip.gears
              .map(gear => {
              return {
                ...gear,
                trip: {
                  departureDateTime: trip.departureDateTime,
                  returnDateTime: trip.returnDateTime
                }
              };
            }));
          }, []);

          // Sort by trip date
          if (filter && filter.vesselId && isNil(filter.tripId)) {
            data.sort(sortByTripDateFn);
          }

          if (opts && opts.distinctByRankOrder === true) {
            data = arrayDistinct(data, ['gear.id', 'rankOrder', 'measurementValues']);
          }

          return {
            data,
            total: data.length
          };
        })
      );
  }

  asFilter(filter: Partial<PhysicalGearFilter>): PhysicalGearFilter {
    return PhysicalGearFilter.fromObject(filter);
  }

}

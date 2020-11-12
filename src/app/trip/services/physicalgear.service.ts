import {Injectable, InjectionToken} from "@angular/core";
import {BaseEntityService} from "../../core/services/base.data-service.class";
import {LoadResult, EntitiesService} from "../../shared/services/entity-service.class";
import {PhysicalGear, Trip} from "./model/trip.model";
import {GraphqlService} from "../../core/services/graphql.service";
import {NetworkService} from "../../core/services/network.service";
import {AccountService} from "../../core/services/account.service";
import {EntitiesStorage} from "../../core/services/entities-storage.service";
import {environment} from "../../../environments/environment";
import {Moment} from "moment";
import {EMPTY, Observable} from "rxjs";
import {fromDateISOString, isNil} from "../../shared/functions";
import {filter, map, throttleTime} from "rxjs/operators";
import {TripFilter} from "./trip.service";
import {ErrorCodes} from "./trip.errors";
import gql from "graphql-tag";
import {PhysicalGearFragments} from "./trip.queries";
import {ReferentialFragments} from "../../referential/services/referential.fragments";
import {SortDirection} from "@angular/material/sort";


export class PhysicalGearFilter {
  tripId?: number;
  vesselId?: number;
  programLabel?: string;
  startDate?: Moment;
  endDate?: Moment;
  excludeTripId?: number;
}


const LoadAllQuery: any = gql`
  query PhysicalGears($filter: PhysicalGearFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    physicalGears(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
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

export const PHYSICAL_GEAR_DATA_SERVICE = new InjectionToken<EntitiesService<PhysicalGear, PhysicalGearFilter>>('PhysicalGearDataService');


@Injectable({providedIn: 'root'})
export class PhysicalGearService extends BaseEntityService
  implements EntitiesService<PhysicalGear, PhysicalGearFilter> {

  loading = false;

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected entities: EntitiesStorage
  ) {
    super(graphql);

    // -- For DEV only
    this._debug = !environment.production;
  }

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: SortDirection,
    dataFilter?: PhysicalGearFilter,
    options?: any
  ): Observable<LoadResult<PhysicalGear>> {

    // If offline, load locally
    const offlineData = this.network.offline || (dataFilter && dataFilter.tripId < 0) || false;
    if (offlineData) {
      return this.watchAllLocally(offset, size, sortBy, sortDirection, dataFilter, options);
    }

    if (!dataFilter || isNil(dataFilter.vesselId)) {
      console.warn("[physical-gear-service] Trying to load gears without 'filter.vesselId'. Skipping.");
      return EMPTY;
    }

    const remoteFilter = {...dataFilter};
    delete remoteFilter.excludeTripId;
    const variables: any = {
      offset: offset || 0,
      size: size >= 0 ? size : 1000,
      sortBy: (sortBy !== 'id' && sortBy) || 'rankOrder',
      sortDirection: sortDirection || 'desc',
      filter: remoteFilter
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug("[physical-gear-service] Loading physical gears... using options:", variables);

    return this.graphql.watchQuery<{physicalGears: any[]}>({
        query: LoadAllQuery,
        variables: {
          ...variables,
          filter: remoteFilter
        },
        error: {code: ErrorCodes.LOAD_PHYSICAL_GEARS_ERROR, message: "TRIP.PHYSICAL_GEAR.ERROR.LOAD_PHYSICAL_GEARS_ERROR"},
        fetchPolicy: options && options.fetchPolicy || undefined
      })
      .pipe(
        throttleTime(200), // avoid multiple call
        filter(() => !this.loading),
        map((res) => {
          const data = (res && res.physicalGears || []).map(PhysicalGear.fromObject);
          if (now) {
            console.debug(`[physical-gear-service] Loaded ${data.length} physical gears in ${Date.now() - now}ms`);
            now = undefined;
          }

          // Sort by trip date
          if (dataFilter && dataFilter.vesselId && isNil(dataFilter.tripId)) {
            data.sort(sortByTripDateFn);
          }

          return {
            data: data,
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
      dataFilter?: PhysicalGearFilter,
      opts?: any
  ): Observable<LoadResult<PhysicalGear>> {
      if (!dataFilter || isNil(dataFilter.vesselId)) {
      console.warn("[physical-gear-service] Trying to load gears without 'filter.vesselId'. Skipping.");
      return EMPTY;
    }

    const variables: any = {
      offset: offset || 0,
      size: size >= 0 ? size : 1000,
      sortBy: (sortBy !== 'id' && sortBy) || 'rankOrder',
      sortDirection: sortDirection || 'desc',
      filter: TripFilter.searchFilter<Trip>({
        vesselId: dataFilter.vesselId,
        startDate: dataFilter.startDate,
        endDate: dataFilter.endDate
      })
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
          const data: PhysicalGear[] = trips.reduce((res, trip) => {
            // Exclude if no gears
            const tripDate = fromDateISOString(trip.returnDateTime || trip.departureDateTime);
            if (!trip.gears || !tripDate
              // Or if endDate <= trip date
              || (dataFilter.startDate && tripDate.isBefore(dataFilter.startDate))
              || (dataFilter.endDate && tripDate.isSameOrAfter(dataFilter.endDate))
              // Or excluded trip id (e.g to ignore current trip gears)
              || (dataFilter.excludeTripId && trip.id === dataFilter.excludeTripId)) {
              return res;
            }

            return res.concat(trip.gears.map(gear => {
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
          if (dataFilter && dataFilter.vesselId && isNil(dataFilter.tripId)) {
            data.sort(sortByTripDateFn);
          }

          return {data};
        })
      );
  }
}

import {Injectable, Injector} from "@angular/core";
import {BaseDataService} from "../../core/services/base.data-service.class";
import {EditorDataService, LoadResult, TableDataService} from "../../shared/services/data-service.class";
import {AccountService} from "../../core/services/account.service";
import {Observable} from "rxjs";
import {Moment} from "moment";
import {environment} from "../../../environments/environment";
import {ObservedLocation} from "./trip.model";
import gql from "graphql-tag";
import {Fragments} from "./trip.queries";
import {isNil, isNotNil, Person} from "./trip.model";
import {ErrorCodes} from "./trip.errors";
import {map, throttleTime} from "rxjs/operators";
import {FetchPolicy} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";
import {RootDataService} from "./root-data-service.class";


export declare class ObservedLocationFilter {
  programLabel?: string;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationId?: number;
}

export const ObservedLocationFragments = {
  lightObservedLocation: gql`fragment LightObservedLocationFragment on ObservedLocationVO {
    id
    program {
      id
      label
    }
    startDateTime
    endDateTime
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    location {
      ...LocationFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    recorderPerson {
      ...LightPersonFragment
    }
    observers {
      ...LightPersonFragment
    }
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}
  `,
  observedLocation: gql`fragment ObservedLocationFragment on ObservedLocationVO {
    id
    program {
      id
      label
    }
    startDateTime
    endDateTime
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    location {
      ...LocationFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    recorderPerson {
      ...LightPersonFragment
    }
    observers {
      ...LightPersonFragment
    }
    measurementValues
  }
  ${Fragments.lightDepartment}
  ${Fragments.lightPerson}
  ${Fragments.location}
  `
};

// Search query
const LoadAllQuery: any = gql`
  query ObservedLocations($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ObservedLocationFilterVOInput){
    observedLocations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightObservedLocationFragment
    }
    observedLocationCount(filter: $filter)
  }
  ${ObservedLocationFragments.lightObservedLocation}
`;
// Load query
const LoadQuery: any = gql`
  query ObservedLocation($id: Int) {
    observedLocation(id: $id) {
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;
// Save all query
const SaveAllQuery: any = gql`
  mutation SaveObservedLocations($observedLocations:[ObservedLocationVOInput]){
    saveObservedLocations(observedLocations: $observedLocations){
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;
const DeleteByIdsMutation: any = gql`
  mutation DeleteObservedLocations($ids:[Int]){
    deleteObservedLocations(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription UpdateObservedLocation($id: Int, $interval: Int){
    updateObservedLocation(id: $id, interval: $interval) {
      ...ObservedLocationFragment
    }
  }
  ${ObservedLocationFragments.observedLocation}
`;

@Injectable({providedIn: "root"})
export class ObservedLocationService extends RootDataService<ObservedLocation, ObservedLocationFilter>
  implements TableDataService<ObservedLocation, ObservedLocationFilter>,
    EditorDataService<ObservedLocation, ObservedLocationFilter> {

  constructor(
    injector: Injector,
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(injector);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: string, filter?: ObservedLocationFilter, options?: any): Observable<LoadResult<ObservedLocation>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'startDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    this._lastVariables.loadAll = variables;

    let now;
    if (this._debug) {
      now = Date.now();
      console.debug("[observed-location-service] Watching observed locations... using options:", variables);
    }
    return this.graphql.watchQuery<{ observedLocations: ObservedLocation[]; observedLocationsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_OBSERVED_LOCATIONS_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ALL_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'cache-and-network'
    })
      .pipe(
        //throttleTime(200),
        map(res => {
          const data = (res && res.observedLocations || []).map(ObservedLocation.fromObject);
          const total = res && res.observedLocationsCount || 0;
          if (this._debug) {
            if (now) {
              console.debug(`[observed-location-service] Loaded {${data.length || 0}} observed locations in ${Date.now() - now}ms`, data);
              now = undefined;
            } else {
              console.debug(`[observed-location-service] Refreshed {${data.length || 0}} observed locations`);
            }
          }
          return {
            data: data,
            total: total
          };
        }));
  }

  async load(id: number, options?: { fetchPolicy: FetchPolicy }): Promise<ObservedLocation> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    const now = Date.now();
    if (this._debug) console.debug(`[observed-location-service] Loading observed location {${id}}...`);

    const res = await this.graphql.query<{ observedLocation: ObservedLocation }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.LOAD_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.LOAD_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'cache-first'
    });
    const data = res && res.observedLocation && ObservedLocation.fromObject(res.observedLocation);
    if (data && this._debug) console.debug(`[observed-location-service] Observed location #${id} loaded in ${Date.now() - now}ms`, data);

    return data;
  }

  public listenChanges(id: number): Observable<ObservedLocation> {
    if (!id && id !== 0) throw "Missing argument 'id' ";

    if (this._debug) console.debug(`[observed-location-service] [WS] Listening changes for trip {${id}}...`);

    return this.graphql.subscribe<{ updateObservedLocation: ObservedLocation }, { id: number, interval: number }>({
      query: UpdateSubscription,
      variables: {
        id: id,
        interval: 10
      },
      error: {
        code: ErrorCodes.SUBSCRIBE_OBSERVED_LOCATION_ERROR,
        message: 'OBSERVED_LOCATION.ERROR.SUBSCRIBE_ERROR'
      }
    })
      .pipe(
        map(res => {
          const data = res && res.updateObservedLocation && ObservedLocation.fromObject(res.updateObservedLocation);
          if (data && this._debug) console.debug(`[observed-location-service] Observed location {${id}} updated on server !`, data);
          return data;
        })
      );
  }

  async save(entity: ObservedLocation): Promise<ObservedLocation> {

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);
    const isNew = isNil(entity.id);

    const now = Date.now();
    if (this._debug) console.debug("[observed-location-service] Saving observed location...", json);

    // Reset the control date
    entity.controlDate = undefined;
    json.controlDate = undefined;


    const res = await this.graphql.mutate<{ saveObservedLocations: any }>({
      mutation: SaveAllQuery,
      variables: {
        observedLocations: [json]
      },
      error: {code: ErrorCodes.SAVE_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.SAVE_ERROR"},
      update: (proxy, {data}) => {
        const savedEntity = data && data.saveObservedLocations && data.saveObservedLocations[0];
        if (savedEntity !== entity) {
          if (this._debug) console.debug(`[observed-location-service] Observed location saved in ${Date.now() - now}ms`, entity);
          this.copyIdAndUpdateDate(savedEntity, entity);
        }

        // Add to cache
        if (isNew && this._lastVariables.loadAll) {
          this.graphql.addToQueryCache(proxy, {
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'observedLocations', savedEntity);
        }
      }
    });


    return entity;
  }

  async saveAll(entities: ObservedLocation[], options?: any): Promise<ObservedLocation[]> {
    if (!entities) return entities;

    const json = entities.map(t => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(t);
      return this.asObject(t);
    });

    const now = Date.now();
    if (this._debug) console.debug("[observed-location-service] Saving Observed locations...", json);

    const res = await this.graphql.mutate<{ saveObservedLocations: ObservedLocation[] }>({
      mutation: SaveAllQuery,
      variables: {
        trips: json
      },
      error: {code: ErrorCodes.SAVE_OBSERVED_LOCATIONS_ERROR, message: "OBSERVED_LOCATION.ERROR.SAVE_ALL_ERROR"}
    });
    (res && res.saveObservedLocations && entities || [])
      .forEach(entity => {
        const savedEntity = res.saveObservedLocations.find(obj => entity.equals(obj));
        this.copyIdAndUpdateDate(savedEntity, entity);
      });

    if (this._debug) console.debug(`[observed-location-service] Observed locations saved in ${Date.now() - now}ms`, entities);

    return entities;
  }

  async delete(data: ObservedLocation): Promise<any> {
    await this.deleteAll([data]);
  }

  async deleteAll(entities: ObservedLocation[], options?: any): Promise<any> {
    const ids = entities && entities
      .map(t => t.id)
      .filter(isNotNil);

    const now = Date.now();
    if (this._debug) console.debug("[observed-location-service] Deleting observed locations... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeleteByIdsMutation,
      variables: {
        ids: ids
      },
      update: (proxy) => {
        // Update the cache
        if (this._lastVariables.loadAll) {
          this.graphql.removeToQueryCacheByIds(proxy, {
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'observedLocations', ids);
        }

        if (this._debug) console.debug(`[observed-location-service] Observed locations deleted in ${Date.now() - now}ms`);

      }
    });

  }

  /* -- private -- */

  protected asObject(entity: ObservedLocation): any {
    const json = super.asObject(entity);

    // Remove unused properties
    delete json.landings;

    return json;
  }

}

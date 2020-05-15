import {Injectable, Injector} from "@angular/core";
import {EditorDataService, LoadResult, TableDataService} from "../../shared/services/data-service.class";
import {AccountService} from "../../core/services/account.service";
import {Observable} from "rxjs";
import {Moment} from "moment";
import {environment} from "../../../environments/environment";
import gql from "graphql-tag";
import {Fragments} from "./trip.queries";
import {ErrorCodes} from "./trip.errors";
import {map} from "rxjs/operators";
import {FetchPolicy} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";
import {RootDataService} from "./root-data-service.class";
import {
  DataEntityAsObjectOptions,
  isNil,
  isNotNil,
  SAVE_AS_OBJECT_OPTIONS,
  SynchronizationStatus, toDateISOString
} from "./model/base.model";
import {FormErrors} from "../../core/form/form.utils";
import {ObservedLocation} from "./model/observed-location.model";
import {Beans, KeysEnum} from "../../shared/functions";


export class ObservedLocationFilter {
  programLabel?: string;
  startDate?: Date | Moment;
  endDate?: Date | Moment;
  locationId?: number;
  recorderDepartmentId?: number;
  recorderPersonId?: number;
  synchronizationStatus?: SynchronizationStatus;

  static isEmpty(f: ObservedLocationFilter|any): boolean {
    return Beans.isEmpty({...f, synchronizationStatus: null}, ObservedLocationFilterKeys, {
      blankStringLikeEmpty: true
    });
  }
}

export const ObservedLocationFilterKeys: KeysEnum<ObservedLocationFilter> = {
  programLabel: true,
  startDate: true,
  endDate: true,
  locationId: true,
  recorderDepartmentId: true,
  recorderPersonId: true,
  synchronizationStatus: true
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
    observedLocationsCount(filter: $filter)
  }
  ${ObservedLocationFragments.lightObservedLocation}
`;
// Load query
const LoadQuery: any = gql`
  query ObservedLocation($id: Int!) {
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
  subscription UpdateObservedLocation($id: Int!, $interval: Int){
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

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: string, dataFilter?: ObservedLocationFilter, options?: any): Observable<LoadResult<ObservedLocation>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'startDateTime',
      sortDirection: sortDirection || 'asc',
      filter: {
        ...dataFilter,
        // Serialize all dates
        startDate: dataFilter && toDateISOString(dataFilter.startDate),
        endDate: dataFilter && toDateISOString(dataFilter.endDate),
        // Remove fields that not exists in pod
        synchronizationStatus: undefined
      }
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
        map(res => {
          const data = (res && res.observedLocations || []).map(ObservedLocation.fromObject);
          const total = res && isNotNil(res.observedLocationsCount) ? res.observedLocationsCount : undefined;
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
    const now = Date.now();
    if (this._debug) console.debug("[observed-location-service] Saving an observed location...");

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Reset the control date
    entity.controlDate = undefined;

    // If new, create a temporary if (for offline mode)
    const isNew = isNil(entity.id);

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (isNew) delete json.id; // Make to remove temporary id, before sending to graphQL
    if (this._debug) console.debug("[observed-location-service] Using minify object, to send:", json);

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

  /* -- TODO implement this methods -- */
  async synchronize(data: ObservedLocation): Promise<ObservedLocation> { return data; }
  async control(data: ObservedLocation): Promise<FormErrors> { return undefined; }
  async terminate(data: ObservedLocation): Promise<ObservedLocation> { return data; }
  async validate(data: ObservedLocation): Promise<ObservedLocation> { return data; }
  async unvalidate(data: ObservedLocation): Promise<ObservedLocation> { return data; }
  async qualify(data: ObservedLocation, qualityFlagId: number): Promise<ObservedLocation> { return data; }

  /* -- protected methods -- */

  protected asObject(entity: ObservedLocation, opts?: DataEntityAsObjectOptions): any {
    const copy = super.asObject(entity, opts);

    // Remove not saved properties
    delete copy.landings;

    return copy;
  }

}

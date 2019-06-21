import {Injectable, Injector} from "@angular/core";
import {
  EditorDataService,
  LoadEditorDataOptions,
  LoadResult,
  TableDataService
} from "../../shared/services/data-service.class";
import {Observable} from "rxjs";
import {environment} from "../../../environments/environment";
import {Landing} from "./model/landing.model";
import gql from "graphql-tag";
import {DataFragments, Fragments} from "./trip.queries";
import {ErrorCodes} from "./trip.errors";
import {map, throttleTime} from "rxjs/operators";
import {isNil, isNilOrBlank, isNotNil} from "../../shared/functions";
import {RootDataService} from "./root-data-service.class";
import {TripFilter} from "./trip.service";
import {Sample} from "./model/sample.model";
import {EntityUtils} from "../../core/services/model";


export class LandingFilter extends TripFilter {
  observedLocationId?: number;
  tripId?: number;
}

export const LandingFragments = {
  lightLanding: gql`fragment LightLandingFragment on LandingVO {
    id
    program {
      id 
      label
    }
    dateTime
    location {
      ...LocationFragment
    }
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    observedLocationId
    tripId
    vesselFeatures {
      ...VesselFeaturesFragment
    }
    recorderDepartment {
      ...RecorderDepartmentFragment
    }
    recorderPerson {
      ...RecorderPersonFragment
    }
    observers {
      ...LightPersonFragment
    }
  }
  ${Fragments.location}
  ${Fragments.recorderDepartment}
  ${Fragments.recorderPerson}
  ${Fragments.lightPerson}
  ${DataFragments.vesselFeatures}
  `,
  landing: gql`fragment LandingFragment on LandingVO {
    id
    program {
      id 
      label
    }
    dateTime
    location {
      ...LocationFragment
    }
    creationDate
    updateDate
    controlDate
    validationDate
    qualificationDate
    comments
    observedLocationId
    tripId
    vesselFeatures {
      ...VesselFeaturesFragment
    }
    recorderDepartment {
      ...RecorderDepartmentFragment
    }
    recorderPerson {
      ...RecorderPersonFragment
    }
    observers {
      ...LightPersonFragment
    }
    measurementValues
    samples {
      ...SampleFragment
    }
  }
  ${Fragments.location}
  ${Fragments.recorderDepartment}
  ${Fragments.recorderPerson}
  ${Fragments.lightPerson}
  ${DataFragments.vesselFeatures}
  ${DataFragments.sample}
  `
};

// Search query
const LoadAllQuery: any = gql`
  query Landings($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: LandingFilterVOInput){
    landings(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LandingFragment
    }
    landingCount(filter: $filter)
  }
  ${LandingFragments.landing}
`;

const LoadQuery: any = gql`
  query Landing($id: Int){
    landing(id: $id){
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
`;
// Save all query
const SaveAllQuery: any = gql`
  mutation SaveLandings($landings:[LandingVOInput]){
    saveLandings(landings: $landings){
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
`;

const DeleteByIdsMutation: any = gql`
  mutation DeleteLandings($ids:[Int]){
    deleteLandings(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription UpdateLanding($id: Int, $interval: Int){
    updateLanding(id: $id, interval: $interval) {
      ...LandingFragment
    }
  }
  ${LandingFragments.landing}
`;

@Injectable({providedIn: 'root'})
export class LandingService extends RootDataService<Landing, LandingFilter>
  implements TableDataService<Landing, LandingFilter>, EditorDataService<Landing, LandingFilter> {

  constructor(
    injector: Injector
  ) {
    super(injector);

    // FOR DEV ONLY
    this._debug = !environment.production;
  }

  watchAll(offset: number, size: number,
           sortBy?: string, sortDirection?: string,
           filter?: LandingFilter, options?: any): Observable<LoadResult<Landing>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 20,
      sortBy: sortBy || 'dateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    this._lastVariables.loadAll = variables;

    let now;
    if (this._debug) {
      now = Date.now();
      console.debug("[landing-service] Watching landings... using options:", variables);
    }
    return this.graphql.watchQuery<{ landings: Landing[]; landingCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_LANDINGS_ERROR, message: "LANDING.ERROR.LOAD_ALL_ERROR"}
    })
      .pipe(
        throttleTime(200),
        map(res => {
          const data = (res && res.landings || []).map(Landing.fromObject);
          const total = res && res.landingCount || 0;
          if (this._debug) {
            if (now) {
              console.debug(`[landing-service] Loaded {${data.length || 0}} landings in ${Date.now() - now}ms`, data);
              now = undefined;
            } else {
              console.debug(`[landing-service] Refreshed {${data.length || 0}} landings`);
            }
          }
          return {
            data: data,
            total: total
          };
        }));
  }

  async load(id: number, options?: LoadEditorDataOptions): Promise<Landing> {
    if (isNil(id)) throw new Error("Missing argument 'id'");

    const now = Date.now();
    if (this._debug) console.debug(`[landing-service] Loading landing {${id}}...`);

    const res = await this.graphql.query<{ landing: Landing }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.LOAD_LANDING_ERROR, message: "LANDING.ERROR.LOAD_ERROR"},
      fetchPolicy: options && options.fetchPolicy || 'cache-first'
    });
    const data = res && res.landing && Landing.fromObject(res.landing);
    if (data && this._debug) console.debug(`[landing-service] landing #${id} loaded in ${Date.now() - now}ms`, data);

    return data;
  }

  async saveAll(entities: Landing[], options?: any): Promise<Landing[]> {
    if (!entities) return entities;

    const json = entities.map(t => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(t, options);
      return this.asObject(t);
    });

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Saving landings...", json);

    const res = await this.graphql.mutate<{ saveLandings: Landing[] }>({
      mutation: SaveAllQuery,
      variables: {
        trips: json
      },
      error: {code: ErrorCodes.SAVE_LANDINGS_ERROR, message: "LANDING.ERROR.SAVE_ALL_ERROR"}
    });
    (res && res.saveLandings && entities || [])
      .forEach(entity => {
        const savedEntity = res.saveLandings.find(obj => entity.equals(obj));
        this.copyIdAndUpdateDate(savedEntity, entity);
      });

    if (this._debug) console.debug(`[landing-service] Landings saved in ${Date.now() - now}ms`, entities);

    return entities;
  }

  async save(entity: Landing): Promise<Landing> {

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Transform into json
    const json = this.asObject(entity);
    const isNew = isNil(entity.id);

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Saving landing...", json);

    const res = await this.graphql.mutate<{ saveLandings: any }>({
      mutation: SaveAllQuery,
      variables: {
        landings: [json]
      },
      error: {code: ErrorCodes.SAVE_OBSERVED_LOCATION_ERROR, message: "OBSERVED_LOCATION.ERROR.SAVE_ERROR"}
    });

    const savedEntity = res && res.saveLandings && res.saveLandings[0];
    if (savedEntity) {
      this.copyIdAndUpdateDate(savedEntity, entity);

      // Add to cache
      if (isNew && this._lastVariables.loadAll) {
        this.addToQueryCache({
          query: LoadAllQuery,
          variables: this._lastVariables.loadAll
        }, 'landings', savedEntity);
      }
    }

    if (this._debug) console.debug(`[landing-service] Landing saved in ${Date.now() - now}ms`, entity);

    return entity;
  }

  async delete(data: Landing): Promise<any> {
    await this.deleteAll([data]);
  }

  async deleteAll(entities: Landing[], options?: any): Promise<any> {
    const ids = entities && entities
      .map(t => t.id)
      .filter(isNotNil);

    const now = Date.now();
    if (this._debug) console.debug("[landing-service] Deleting landings... ids:", ids);

    const res = await this.graphql.mutate<any>({
      mutation: DeleteByIdsMutation,
      variables: {
        ids: ids
      }
    });

    // Update the cache
    if (this._lastVariables.loadAll) {
      this.removeToQueryCacheByIds({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'landings', ids);
    }

    if (this._debug) console.debug(`[landing-service] Landings deleted in ${Date.now() - now}ms`);

    return res;
  }


  public listenChanges(id: number): Observable<Landing> {
    if (!id && id !== 0) throw "Missing argument 'id' ";

    if (this._debug) console.debug(`[landing-service] [WS] Listening changes for trip {${id}}...`);

    return this.subscribe<{ updateLanding: Landing }, { id: number, interval: number }>({
      query: UpdateSubscription,
      variables: {
        id: id,
        interval: 10
      },
      error: {
        code: ErrorCodes.SUBSCRIBE_LANDING_ERROR,
        message: 'LANDING.ERROR.SUBSCRIBE_ERROR'
      }
    })
      .pipe(
        map(res => {
          const data = res && res.updateLanding && Landing.fromObject(res.updateLanding);
          if (data && this._debug) console.debug(`[landing-service] Landing {${id}} updated on server !`, data);
          return data;
        })
      );
  }

  /* -- private -- */

  protected asObject(entity: Landing): any {
    return super.asObject(entity);
  }

  protected fillDefaultProperties(entity: Landing, options?: any) {
    super.fillDefaultProperties(entity);

    // Fill parent id, if not already set
    if (!entity.tripId && !entity.observedLocationId && options) {
      entity.observedLocationId = options.observedLocationId;
      entity.tripId = options.tripId;
    }

    // Make sure to set all samples attributes
    (entity.samples || []).forEach(s => {
      // Always fill label
      if (isNilOrBlank(s.label)) {
        s.label = `#${s.rankOrder}`;
      }
    });
  }

  protected copyIdAndUpdateDate(source: Landing | undefined, target: Landing) {
    super.copyIdAndUpdateDate(source, target);

    // Update samples (recursively)
    if (target.samples && source.samples) {
      this.copyIdAndUpdateDateOnSamples(source.samples, target.samples);
    }
  }

  /**
   * Copy Id and update, in sample tree (recursively)
   * @param sources
   * @param targets
   */
  copyIdAndUpdateDateOnSamples(sources: (Sample | any)[], targets: Sample[]) {
    // Update samples
    if (sources && targets) {
      targets.forEach(target => {
        const source = sources.find(json => target.equals(json));
        EntityUtils.copyIdAndUpdateDate(source, target);

        // Apply to children
        if (target.children && target.children.length) {
          this.copyIdAndUpdateDateOnSamples(sources, target.children);
        }
      });
    }
  }
}

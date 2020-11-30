import {Injectable} from "@angular/core";
import {gql} from "@apollo/client";
import {Observable} from "rxjs";
import {
  QualityFlagIds
} from "./model/model.enum";
import {
  EntityService, EntityServiceLoadOptions, isNil,
  isNilOrBlank,
  isNotEmptyArray, isNotNil,
  LoadResult,
  EntitiesService
} from "../../shared/shared.module";

import {
  MINIFY_OPTIONS,
  SAVE_AS_OBJECT_OPTIONS,
  SAVE_LOCALLY_AS_OBJECT_OPTIONS
} from "../../core/services/model/referential.model";
import {map} from "rxjs/operators";
import {Moment} from "moment";

import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {ReferentialFragments} from "./referential.fragments";
import {FetchPolicy} from "@apollo/client/core";
import {isEmptyArray} from "../../shared/functions";
import {EntityAsObjectOptions, EntityUtils} from "../../core/services/model/entity.model";
import {LoadFeaturesQuery, VesselFeaturesFragments, VesselFeaturesService} from "./vessel-features.service";
import {LoadRegistrationsQuery, RegistrationFragments, VesselRegistrationService} from "./vessel-registration.service";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {Vessel} from "./model/vessel.model";
import {BaseEntityService} from "../../core/services/base.data-service.class";
import {Person} from "../../core/services/model/person.model";
import {Department} from "../../core/services/model/department.model";
import {StatusIds} from "../../core/services/model/model.enum";
import {VesselSnapshot} from "./model/vessel-snapshot.model";
import {SortDirection} from "@angular/material/sort";

export class VesselFilter {
  date?: Date | Moment;
  vesselId?: number;
  searchText?: string;
  statusId?: number;
  statusIds?: number[];

  static isEmpty(filter: VesselFilter | any): boolean {
    return !filter || (
      !filter.date && isNilOrBlank(filter.vesselId) && isNilOrBlank(filter.searchText) && isNilOrBlank(filter.statusId) && isEmptyArray(filter.statusIds)
    );
  }

  static searchFilter<T extends VesselSnapshot>(f: VesselFilter): (T) => boolean {
    if (this.isEmpty(f)) return undefined; // no filter need
    const searchFilter = EntityUtils.searchTextFilter(['name', 'exteriorMarking', 'registrationCode'], f.searchText);
    const statusIds = (isNotEmptyArray(f.statusIds) && f.statusIds) || (isNotNil(f.statusId) && [f.statusId]);
    return (t: T) => {

      // Vessel id
      if (isNotNil(f.vesselId) && t.id !== f.vesselId) {
        return false;
      }

      // Status
      if (statusIds && statusIds.findIndex(statusId => statusId === t.vesselStatusId) === -1) {
        return false;
      }

      // Search text
      return isNil(searchFilter) || searchFilter(t);
    };
  }
}

export const VesselFragments = {
  lightVessel: gql`fragment VesselFragment on VesselVO {
      id
      comments
      statusId
      #        qualityFlagId
      #        program
      creationDate
      controlDate
      validationDate
      qualificationDate
      qualificationComments
      updateDate
      comments
      vesselType {
        ...ReferentialFragment
      }
      features {
        ...VesselFeaturesFragment
      }
      registration{
        ...RegistrationFragment
      }
      recorderDepartment {
        ...LightDepartmentFragment
      }
      recorderPerson {
        ...LightPersonFragment
      }
    }`,
    vessel: gql`fragment VesselFragment on VesselVO {
        id
        comments
        statusId
        #        qualityFlagId
        #        program
        creationDate
        controlDate
        validationDate
        qualificationDate
        qualificationComments
        updateDate
        comments
        vesselType {
            ...ReferentialFragment
        }
        features {
            ...VesselFeaturesFragment
        }
        registration{
            ...RegistrationFragment
        }
        recorderDepartment {
            ...LightDepartmentFragment
        }
        recorderPerson {
            ...LightPersonFragment
        }
    }`,
};

const LoadAllQuery: any = gql`
    query Vessels($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
        vessels(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
            ...VesselFragment
        }
        vesselsCount(filter: $filter)
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}
`;
const LoadQuery: any = gql`
    query Vessel($vesselId: Int!) {
        vessel(vesselId: $vesselId) {
            ...VesselFragment
        }
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}
`;

const SaveVessels: any = gql`
    mutation SaveVessels($vessels:[VesselVOInput]){
        saveVessels(vessels: $vessels){
            ...VesselFragment
        }
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}
`;

const DeleteVessels: any = gql`
    mutation deleteVessels($ids:[Int]){
        deleteVessels(ids: $ids)
    }
`;


@Injectable({providedIn: 'root'})
export class VesselService
  extends BaseEntityService
  implements EntitiesService<Vessel, VesselFilter>, EntityService<Vessel> {

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected entities: EntitiesStorage,
    private accountService: AccountService,
    private vesselFeatureService: VesselFeaturesService,
    private vesselRegistrationService: VesselRegistrationService,
  ) {
    super(graphql);
  }

  /**
   * Load many vessels
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           filter?: VesselFilter): Observable<LoadResult<Vessel>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'features.exteriorMarking',
      sortDirection: sortDirection || 'asc',
      filter: filter && {
        date: filter.date,
        vesselId: filter.vesselId,
        searchText: filter.searchText,
        statusIds: isNotNil(filter.statusId) ? [filter.statusId] : filter.statusIds
      }
    };

    const now = Date.now();
    if (this._debug) console.debug("[vessel-service] Getting vessels using options:", variables);

    return this.mutableWatchQuery<{ vessels: any[]; vesselsCount: number }>({
      queryName: 'LoadAll',
      query: LoadAllQuery,
      arrayFieldName: 'vessels',
      totalFieldName: 'vesselsCount',
      insertFilterFn: VesselFilter.searchFilter(filter),
      variables: variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    })
      .pipe(
        map(({vessels, vesselsCount}) => {
            const data = (vessels || []).map(Vessel.fromObject);
            const total = vesselsCount || undefined;
            if (this._debug) console.debug(`[vessel-service] Vessels loaded in ${Date.now() - now}ms`, data);
            return {
              data: data,
              total: total
            };
          }
        )
      );
  }

  async load(id: number, opts?: EntityServiceLoadOptions): Promise<Vessel | null> {
    console.debug("[vessel-service] Loading vessel " + id);

    const variables: any = {vesselId: id};

    const data = await this.graphql.query<{ vessel: any }>({
      query: LoadQuery,
      variables: variables,
      fetchPolicy: opts && opts.fetchPolicy || undefined
    });

    if (data && data.vessel) {
      const res = new Vessel();
      res.fromObject(data.vessel);
      return res;
    }
    return null;
  }

  /**
   * Save many vessels
   * @param vessels
   */
  async saveAll(vessels: Vessel[], options?: any): Promise<Vessel[]> {

    if (!vessels) return vessels;

    const json = vessels.map(vessel => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(vessel);
      return this.asObject(vessel);
    });

    const now = Date.now();
    console.debug("[vessel-service] Saving vessels...", json);

    return new Promise<Vessel[]>((resolve, reject) => {
      this.graphql.mutate<{ saveVessels: Vessel[] }>({
        mutation: SaveVessels,
        variables: {
          vessels: json
        },
        error: { reject, code: ErrorCodes.SAVE_VESSELS_ERROR, message: "VESSEL.ERROR.SAVE_VESSELS_ERROR"},
        update: async (proxy, {data}) => {

          if (this._debug) console.debug(`[vessel-service] Vessels saved remotely in ${Date.now() - now}ms`, vessels);

          if (data && data.saveVessels) {
            (vessels || []).forEach(vessel => {
              const savedVessel = data.saveVessels.find(v => vessel.equals(v));
              if (savedVessel && savedVessel !== vessel) {
                this.copyIdAndUpdateDate(savedVessel, vessel);
              }
            });

            // update features history FIXME: marche pas
            if (options && options.isNewFeatures) {
              const lastFeatures = vessels[vessels.length - 1].features;
              this.vesselFeatureService.insertIntoMutableCachedQuery(proxy, {
                query: LoadFeaturesQuery,
                data: lastFeatures
              });
            }

            // update registration history FIXME: marche pas
            if (options && options.isNewRegistration) {
              const lastRegistration = vessels[vessels.length - 1].registration;
              this.vesselRegistrationService.insertIntoMutableCachedQuery(proxy, {
                query: LoadRegistrationsQuery,
                data: lastRegistration
              });
            }

          }

          resolve(vessels);
        }
      });
    });
  }

  /**
   * Save a trip
   * @param entity
   * @param opts
   */
  async save(entity: Vessel, opts?: {
    previousVessel?: Vessel;
    isNewFeatures?: boolean;
    isNewRegistration?: boolean;
  }): Promise<Vessel> {

    const now = Date.now();
    console.debug("[vessel-service] Saving a vessel...");

    // prepare previous vessel to save if present
    if (opts && isNotNil(opts.previousVessel)) {

      // update previous features
      if (opts.isNewFeatures) {
        // set end date = new start date - 1
        const newStartDate = entity.features.startDate.clone();
        newStartDate.subtract(1, "seconds");
        opts.previousVessel.features.endDate = newStartDate;

      } else
      // prepare previous registration period
      if (opts.isNewRegistration) {
        // set registration end date = new registration start date - 1
        const newRegistrationStartDate = entity.registration.startDate.clone();
        newRegistrationStartDate.subtract(1, "seconds");
        opts.previousVessel.registration.endDate = newRegistrationStartDate;
      }

      // save both by calling saveAll
      const savedVessels: Vessel[] = await this.saveAll([opts.previousVessel, entity], opts);
      // return last
      return Promise.resolve(savedVessels.pop());
    }

    // Prepare to save
    this.fillDefaultProperties(entity);
    const isNew = isNil(entity.id);

    // Offline mode
    if (this.network.offline) {
      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      const json = this.asObject(entity, SAVE_LOCALLY_AS_OBJECT_OPTIONS);
      if (this._debug) console.debug('[vessel-service] [offline] Saving vessel locally...', json);

      // Save response locally
      await this.entities.save(json);

      return entity;
    }

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[vessel-service] Using minify object, to send:", json);

    return new Promise<Vessel>((resolve, reject) => {
      this.graphql.mutate<{ saveVessels: any }>({
        mutation: SaveVessels,
        variables: {
          vessels: [json]
        },
        error: {reject, code: ErrorCodes.SAVE_VESSEL_ERROR, message: "VESSEL.ERROR.SAVE_VESSEL_ERROR"},
        update: (proxy, {data}) => {
          const savedVessel = data && data.saveVessels && data.saveVessels[0];

          if (savedVessel) {

            // Copy id and update Date
            this.copyIdAndUpdateDate(savedVessel, entity);

            if (this._debug) console.debug(`[vessel-service] Vessel Feature saved in ${Date.now() - now}ms`, savedVessel);

            // Add to cache
            if (isNew) {
              this.insertIntoMutableCachedQuery(proxy, {
                query: LoadAllQuery,
                data: savedVessel
              });
            }

          }

          resolve(entity);
        }

      });
    });

  }

  delete(data: Vessel, options?: any): Promise<any> {
    return this.deleteAll([data]);
  }

  async deleteAll(vessels: Vessel[]): Promise<any> {
    const ids = vessels && vessels
      .map(t => t.id)
      .filter(id => (id > 0));

    const now = Date.now();
    console.debug("[vessel-service] Deleting vessels... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeleteVessels,
      variables: {
        ids: ids
      },
      update: (proxy, {data}) => {
        // Update the cache
        this.removeFromMutableCachedQueryByIds(proxy, {
          query: LoadAllQuery,
          ids
        });
        if (this._debug) console.debug(`[vessel-service] Vessels deleted in ${Date.now() - now}ms`);
      }
    });
  }

  listenChanges(id: number, options?: any): Observable<Vessel> {
    throw new Error("Method not implemented.");
  }

  /* -- protected methods -- */

  protected asObject(vessel: Vessel, opts?: EntityAsObjectOptions): any {
    return vessel.asObject({...MINIFY_OPTIONS, ...opts} as EntityAsObjectOptions);
  }

  protected fillDefaultProperties(vessel: Vessel) {

    const person: Person = this.accountService.account;

    // Recorder department
    if (person && person.department && (!vessel.recorderDepartment || vessel.recorderDepartment.id !== person.department.id)) {
      if (!vessel.recorderDepartment) {
        vessel.recorderDepartment = new Department();
      }
      vessel.recorderDepartment.id = person.department.id;
      if (vessel.features) {
        if (!vessel.features.recorderDepartment) {
          vessel.features.recorderDepartment = new Department();
        }
        vessel.features.recorderDepartment.id = person.department.id;
      }
    }

    // Recorder person
    if (person && (!vessel.recorderPerson || vessel.recorderPerson.id !== person.id)) {
      if (!vessel.recorderPerson) {
        vessel.recorderPerson = new Person();
      }
      vessel.recorderPerson.id = person.id;
      if (vessel.features) {
        if (!vessel.features.recorderPerson) {
          vessel.features.recorderPerson = new Person();
        }
        vessel.features.recorderPerson.id = person.id;
      }
    }

    // Quality flag (set default)
    if (vessel.features && isNil(vessel.features.qualityFlagId)) {
      vessel.features.qualityFlagId = QualityFlagIds.NOT_QUALIFIED;
    }

  }

  protected async fillOfflineDefaultProperties(entity: Vessel) {
    const isNew = isNil(entity.id);

    // If new, generate a local id
    if (isNew) {
      entity.id =  await this.entities.nextValue(entity);
    }

    // Force status as temporary
    entity.statusId = StatusIds.TEMPORARY;
  }

  copyIdAndUpdateDate(source: Vessel | undefined, target: Vessel) {

    EntityUtils.copyIdAndUpdateDate(source, target);
    EntityUtils.copyIdAndUpdateDate(source.features, target.features);
    EntityUtils.copyIdAndUpdateDate(source.registration, target.registration);

  }
}

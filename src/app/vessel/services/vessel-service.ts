import {Injectable, Injector} from "@angular/core";
import {gql} from "@apollo/client/core";
import {Observable} from "rxjs";
import {QualityFlagIds} from "../../referential/services/model/model.enum";
import {MINIFY_OPTIONS, SAVE_LOCALLY_AS_OBJECT_OPTIONS} from "../../core/services/model/referential.model";
import {map} from "rxjs/operators";
import {Moment} from "moment";
import {ReferentialFragments} from "../../referential/services/referential.fragments";
import {isEmptyArray, isNil, isNotEmptyArray, isNotNil} from "../../shared/functions";
import {EntityAsObjectOptions, EntityUtils} from "../../core/services/model/entity.model";
import {VesselFeatureQueries, VesselFeaturesFragments, VesselFeaturesService} from "./vessel-features.service";
import {VesselRegistrationsQueries, RegistrationFragments, VesselRegistrationService} from "./vessel-registration.service";
import {Vessel} from "./model/vessel.model";
import {Person} from "../../core/services/model/person.model";
import {Department} from "../../core/services/model/department.model";
import {StatusIds} from "../../core/services/model/model.enum";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";
import {SortDirection} from "@angular/material/sort";
import {FilterFn, IEntitiesService, IEntityService, LoadResult} from "../../shared/services/entity-service.class";
import {DataRootEntityUtils} from "../../data/services/model/root-data-entity.model";
import {IDataSynchroService, RootDataSynchroService} from "../../data/services/root-data-synchro-service.class";
import {FormErrors} from "../../core/form/form.utils";
import {BaseEntityGraphqlQueries, EntitySaveOptions} from "../../referential/services/base-entity-service.class";
import {fromDateISOString, toDateISOString} from "../../shared/dates";
import {BaseRootEntityGraphqlMutations} from "../../data/services/root-data-service.class";
import {VESSEL_FEATURE_NAME} from "./config/vessel.config";
import {RootDataEntityFilter} from "../../data/services/model/root-data-filter.model";
import {VesselFilter} from "./filter/vessel.filter";


export const VesselFragments = {
  lightVessel: gql`fragment VesselFragment on VesselVO {
      id
      comments
      statusId
      creationDate
      controlDate
      validationDate
      qualificationDate
      qualificationComments
      updateDate
      comments
      program {
        id
        label
      }
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
        creationDate
        controlDate
        validationDate
        qualificationDate
        qualificationComments
        updateDate
        comments
        program {
          id
          label
        }
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
    }`
};


const VesselQueries: BaseEntityGraphqlQueries = {
  load: gql`query Vessel($id: Int!) {
        data: vessel(vesselId: $id) {
            ...VesselFragment
        }
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}`,

  loadAllWithTotal: gql`query Vessels($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
        data: vessels(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
            ...VesselFragment
        }
        total: vesselsCount(filter: $filter)
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}`,

  loadAll: gql`query Vessels($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
        data: vessels(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
            ...VesselFragment
        }
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}`
};


const VesselMutations: BaseRootEntityGraphqlMutations = {
  saveAll: gql`mutation SaveVessels($data:[VesselVOInput]!){
        data: saveVessels(vessels: $data){
            ...VesselFragment
        }
    }
    ${VesselFragments.vessel}
    ${VesselFeaturesFragments.vesselFeatures}
    ${RegistrationFragments.registration}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}`,

  deleteAll: gql`mutation DeleteVessels($ids:[Int]!){
        deleteVessels(ids: $ids)
    }`
};

export interface VesselSaveOptions extends EntitySaveOptions {
  previousVessel?: Vessel;
  isNewFeatures?: boolean;
  isNewRegistration?: boolean;
}

@Injectable({providedIn: 'root'})
export class VesselService
  extends RootDataSynchroService<Vessel, VesselFilter>
  implements
    IEntitiesService<Vessel, VesselFilter>,
    IEntityService<Vessel>,
    IDataSynchroService<Vessel> {

  constructor(
    injector: Injector,
    private vesselFeatureService: VesselFeaturesService,
    private vesselRegistrationService: VesselRegistrationService,
  ) {
    super(injector, Vessel, VesselFilter, {
      queries: VesselQueries,
      mutations: VesselMutations
    });
    this._featureName = VESSEL_FEATURE_NAME;
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

    // Load offline
    const offline = this.network.offline || filter && filter.synchronizationStatus && filter.synchronizationStatus !== 'SYNC';
    if (offline) {
      return this.watchAllLocally(offset, size, sortBy, sortDirection, filter);
    }

    return super.watchAll(offset, size,  sortBy || 'features.exteriorMarking', sortDirection, filter);
  }

  watchAllLocally(offset: number,
                 size: number,
                 sortBy?: string,
                 sortDirection?: SortDirection,
                 filter?: Partial<VesselFilter>): Observable<LoadResult<Vessel>> {

    filter = this.asFilter(filter);

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'features.exteriorMarking',
      sortDirection: sortDirection || 'asc',
      filter: filter && filter.asFilterFn()
    };

    if (this._debug) console.debug("[vessel-service] Loading local vessels using options:", variables);

    return this.entities.watchAll<Vessel>(Vessel.TYPENAME, variables)
      .pipe(
        map(({data, total}) => {
          const entities = (data || []).map(Vessel.fromObject);
          return {data: entities, total};
        }));
  }

  /**
   * Save many vessels
   * @param vessels
   */
  async saveAll(entities: Vessel[], opts?: VesselSaveOptions): Promise<Vessel[]> {

    return super.saveAll(entities, {
      ...opts,
      update: (proxy, {data}) => {
        if (isEmptyArray(data && data.data)) return; // Skip if empty

        // update features history FIXME: marche pas
        if (opts && opts.isNewFeatures) {
          const lastFeatures = entities[entities.length - 1].features;
          this.vesselFeatureService.insertIntoMutableCachedQuery(proxy, {
            query: VesselFeatureQueries.loadAll,
            data: lastFeatures
          });
        }

        // update registration history FIXME: marche pas
        if (opts && opts.isNewRegistration) {
          const lastRegistration = entities[entities.length - 1].registration;
          this.vesselRegistrationService.insertIntoMutableCachedQuery(proxy, {
            query: VesselRegistrationsQueries.loadAll,
            data: lastRegistration
          });
        }

      }
    });
  }

  /**
   * Save a vessel
   * @param entity
   * @param opts
   */
  async save(entity: Vessel, opts?: VesselSaveOptions): Promise<Vessel> {

    // prepare previous vessel to save if present
    if (opts && isNotNil(opts.previousVessel)) {

      // update previous features
      if (opts.isNewFeatures) {
        // set end date = new start date - 1
        const newStartDate = entity.features.startDate.clone();
        newStartDate.subtract(1, "seconds");
        opts.previousVessel.features.endDate = newStartDate;

      }
      // prepare previous registration period
      else if (opts.isNewRegistration) {
        // set registration end date = new registration start date - 1
        const newRegistrationStartDate = entity.registration.startDate.clone();
        newRegistrationStartDate.subtract(1, "seconds");
        opts.previousVessel.registration.endDate = newRegistrationStartDate;
      }

      // save both by calling saveAll
      const savedVessels: Vessel[] = await this.saveAll([opts.previousVessel, entity], opts);
      // then return last
      return Promise.resolve(savedVessels.pop());
    }

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Save locally, when offline
    const offline = this.network.offline || EntityUtils.isLocal(entity) || (entity.synchronizationStatus && entity.synchronizationStatus !== 'SYNC');
    if (offline) {
      console.debug("[vessel-service] Saving a vessel locally...");

      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      const json = this.asObject(entity, SAVE_LOCALLY_AS_OBJECT_OPTIONS);
      if (this._debug) console.debug('[vessel-service] [offline] Saving vessel locally...', json);

      // Save vessel locally
      await this.entities.save(json);

      // Transform to vesselSnapshot, and add to offline storage
      const vesselSnapshot = VesselSnapshot.fromVessel(entity).asObject(SAVE_LOCALLY_AS_OBJECT_OPTIONS);
      await this.entities.save(vesselSnapshot);

      return entity;
    }

    // Save remotely
    return super.save(entity, opts);
  }

  /**
   * Save a vessel
   * @param entity
   * @param opts
   */
  async saveLocally(entity: Vessel, opts?: VesselSaveOptions): Promise<Vessel> {

    // prepare previous vessel to save if present
    if (opts && isNotNil(opts.previousVessel)) {

      // update previous features
      if (opts.isNewFeatures) {
        // set end date = new start date - 1
        const newStartDate = entity.features.startDate.clone();
        newStartDate.subtract(1, "seconds");
        opts.previousVessel.features.endDate = newStartDate;

      }
      // prepare previous registration period
      else if (opts.isNewRegistration) {
        // set registration end date = new registration start date - 1
        const newRegistrationStartDate = entity.registration.startDate.clone();
        newRegistrationStartDate.subtract(1, "seconds");
        opts.previousVessel.registration.endDate = newRegistrationStartDate;
      }

      // save both by calling saveAll
      const savedVessels: Vessel[] = await this.saveAll([opts.previousVessel, entity], opts);
      // then return last
      return Promise.resolve(savedVessels.pop());
    }

    // Prepare to save
    this.fillDefaultProperties(entity);

    // Save locally, when offline
    const offline = this.network.offline || EntityUtils.isLocal(entity);
    if (offline) {
      console.debug("[vessel-service] Saving a vessel locally...");

      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      const json = this.asObject(entity, SAVE_LOCALLY_AS_OBJECT_OPTIONS);
      if (this._debug) console.debug('[vessel-service] [offline] Saving vessel locally...', json);

      // Save vessel locally
      await this.entities.save(json);

      // Transform to vesselSnapshot, and add to offline storage
      const vesselSnapshot = VesselSnapshot.fromVessel(entity).asObject(SAVE_LOCALLY_AS_OBJECT_OPTIONS);
      await this.entities.save(vesselSnapshot);

      return entity;
    }

    // Save remotely
    return super.save(entity, opts);
  }

  protected async deleteAllLocally(entities: Vessel[], opts?: { trash?: boolean }): Promise<any> {
    // Delete the vessel
    await super.deleteAllLocally(entities, opts);

    // Delete the associated vessel snapshot
    const snapshots = entities.filter(DataRootEntityUtils.isLocal).map(e => e.id);
    if (isEmptyArray(snapshots)) return; // Skip
    await this.entities.deleteMany(snapshots, {entityName: VesselSnapshot.TYPENAME});
  }

  async synchronize(data: Vessel, opts?: any): Promise<Vessel> {
    console.info(`[vessel-service] Synchronizing vessel {${data.id}}...`);
    return data;
  }

  listenChanges(id: number, options?: any): Observable<Vessel> {
    throw new Error("Method not implemented.");
  }

  control(entity: Vessel, opts?: any): Promise<FormErrors> {
    return undefined; // Not implemented
  }

  async terminate(entity: Vessel): Promise<Vessel> {
    return entity; // Not implemented
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
    if (source) {
      EntityUtils.copyIdAndUpdateDate(source.features, target.features);
      EntityUtils.copyIdAndUpdateDate(source.registration, target.registration);
    }

  }
}

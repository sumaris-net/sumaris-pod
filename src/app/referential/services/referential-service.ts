import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { Referential, StatusIds, PmfmStrategy } from "./model";
import { DataService, BaseDataService } from "../../core/services/data-service.class";
import { Apollo } from "apollo-angular";
import { ErrorCodes } from "./errors";
import { AccountService } from "../../core/services/account.service";
import { ReferentialRef } from "../../core/services/model";

export declare class ReferentialFilter {
  entityName: string;
  label?: string;
  name?: string;
  levelId?: number;
  searchText?: string;
  searchAttribute?: string;
}
const LoadAllRefQuery: any = gql`
  query Referenials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      statusId
      entityName
    }
  }
`;
const LoadAllQuery: any = gql`
  query Referenials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      updateDate
      creationDate
      statusId
      levelId
      entityName
    }
  }
`;
export declare class ReferentialType {
  id: string
  level?: string
};
const LoadReferentialTypes: any = gql`
  query ReferentialTypes{
    referentialTypes {
       id
       level
    }
  }
`;

const LoadReferentialLevels: any = gql`
  query ReferentialLevels($entityName: String) {
    referentialLevels(entityName: $entityName){
      id
      label
      name
      entityName
    }
  }
`;

const LoadProgramPmfms: any = gql`
  query LoadProgramPmfms($program: String) {
    programPmfms(program: $program){
      id
      label
      name
      unit
      type
      minValue
      maxValue
      maximumNumberDecimals
      defaultValue
      acquisitionNumber
      isMandatory
      rankOrder    
      acquisitionLevel
      updateDate
      gears
      qualitativeValues {
        id
        label
        name
        statusId
        entityName
      }
    }
  }
`;

const SaveReferentials: any = gql`
  mutation SaveReferentials($referentials:[ReferentialVOInput]){
    saveReferentials(referentials: $referentials){
      id
      label
      name
      updateDate
      creationDate
      statusId
      levelId
      entityName
    }
  }
`;

const DeleteReferentials: any = gql`
  mutation deleteReferentials($entityName: String, $ids:[Int]){
    deleteReferentials(entityName: $entityName, ids: $ids)
  }
`;

@Injectable()
export class ReferentialService extends BaseDataService implements DataService<Referential, ReferentialFilter> {

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);
    this._debug = true;
  }

  loadAllRef(offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: ReferentialFilter,
    options?: any): Observable<ReferentialRef[]> {

    if (!filter || !filter.entityName) {
      console.error("[referential-service] Missing filter.entityName");
      throw { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" };
    }

    const entityName = filter.entityName;
    filter = Object.assign({}, filter);
    delete filter.entityName;

    const variables: any = {
      entityName: entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Loading references on ${entityName}...`, variables);

    return this.watchQuery<{ referentials: any[] }>({
      query: LoadAllRefQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" }
    })
      .pipe(
        map((data) => {
          const res = (data && data.referentials || []).map(ReferentialRef.fromObject);
          if (this._debug) console.debug(`[referential-service] References on ${entityName} loaded in ${new Date().getTime() - now.getTime()}ms`, res);
          return res;
        })
      );
  }

  loadAll(offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: ReferentialFilter,
    options?: any): Observable<Referential[]> {

    if (!filter || !filter.entityName) {
      console.error("[referential-service] Missing filter.entityName");
      throw { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" };
    }

    const entityName = filter.entityName;
    filter = Object.assign({}, filter);
    delete filter.entityName;

    const variables: any = {
      entityName: entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Loading ${entityName}...`, variables);

    if (options.full) {
      this._lastVariables.loadAll = variables;
    }

    return this.watchQuery<{ referentials: any[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" },
      fetchPolicy: 'network-only'
    }).first()
      .pipe(
        map((data) => {
          const res = (data && data.referentials || []).map(Referential.fromObject);
          res.forEach(r => r.entityName = entityName);
          if (this._debug) console.debug(`[referential-service] ${entityName} loaded in ${new Date().getTime() - now.getTime()}ms`, res);
          return res;
        })
      );
  }

  async saveAll(entities: Referential[], options?: any): Promise<Referential[]> {
    if (!entities) return entities;

    // Nothing to save: skip
    if (!entities.length) return;

    const entityName = entities[0].entityName;
    if (!entityName) {
      console.error("[referential-service] Could not save referentials: missing entityName");
      throw { code: ErrorCodes.SAVE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIALS_ERROR" };
    }

    if (entities.length != entities.filter(e => e.entityName === entityName).length) {
      console.error("[referential-service] Could not save referentials: only one entityName is allowed");
      throw { code: ErrorCodes.SAVE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIALS_ERROR" };
    }

    const json = entities.map(t => t.asObject());

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Saving all ${entityName}...`, json);

    const res = await this.mutate<{ saveReferentials: Referential[] }>({
      mutation: SaveReferentials,
      variables: {
        referentials: json
      },
      error: { code: ErrorCodes.SAVE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIALS_ERROR" }
    });

    // Update entites (id and update date)
    (res && res.saveReferentials && entities || [])
      .forEach(entity => {
        const data = res.saveReferentials.find(res => (res.id == entity.id || res.label == entity.label));
        entity.id = data && data.id || entity.id;
        entity.updateDate = data && data.updateDate || entity.updateDate;
        entity.dirty = false;
      });

    if (this._debug) console.debug(`[referential-service] ${entityName} saved in ${new Date().getTime() - now.getTime()}ms`, entities);

    return entities;
  }

  /**
   * Save a referential entity
   * @param entity 
   */
  async save(entity: Referential, options?: any): Promise<Referential> {

    if (!entity.entityName) {
      console.error("[referential-service] Missing entityName");
      throw { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" };
    }

    // Transform into json
    const json = entity.asObject();
    const isNew = !json.id;

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Saving ${entity.entityName}...`, json);

    const data = await this.mutate<{ saveReferentials: any }>({
      mutation: SaveReferentials,
      variables: {
        referentials: [json]
      },
      error: { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" }
    });

    // Update entity
    var res = data && data.saveReferentials && data.saveReferentials[0];
    entity.id = res && res.id || entity.id;
    entity.updateDate = res && res.updateDate || entity.updateDate;
    entity.dirty = false;

    // Update the cache
    if (isNew && this._lastVariables.loadAll) {
      if (this._debug) console.debug(`[referential-service] Updating cache with saved ${entity.entityName}...`);
      this.addToQueryCache({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'referentials', res);
    }

    if (this._debug) console.debug(`[referential-service] ${entity.entityName} saved in ${new Date().getTime() - now.getTime()}ms`, entity);

    return entity;
  }

  /**
   * Delete referential entities
   */
  async deleteAll(entities: Referential[], options?: any): Promise<any> {

    // Filter saved entities
    entities = entities && entities
      .filter(e => !!e.id && !!e.entityName) || [];

    // Nothing to save: skip
    if (!entities.length) return;

    const entityName = entities[0].entityName;
    const ids = entities.filter(e => e.entityName == entityName).map(t => t.id);

    // Check that all entities have the same entityName
    if (entities.length > ids.length) {
      console.error("[referential-service] Could not delete referentials: only one entityName is allowed");
      throw { code: ErrorCodes.DELETE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIALS_ERROR" };
    }

    const now = new Date();
    if (this._debug) console.debug(`[referential-service] Deleting ${entityName}...`, ids);

    const res = await this.mutate<any>({
      mutation: DeleteReferentials,
      variables: {
        entityName: entityName,
        ids: ids
      },
      error: { code: ErrorCodes.DELETE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIALS_ERROR" }
    });

    // Remove from cache
    if (this._lastVariables.loadAll) {
      this.removeToQueryCacheByIds({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'referentials', ids);
    }

    if (this._debug) console.debug(`[referential-service] ${entityName} deleted in ${new Date().getTime() - now.getTime()}ms`);

    return res;
  }

  /**
   * Load referential types
   */
  loadTypes(): Observable<ReferentialType[]> {
    if (this._debug) console.debug("[referential-service] Loading referential types...");
    return this.watchQuery<{ referentialTypes: ReferentialType[] }>({
      query: LoadReferentialTypes,
      variables: null,
      error: { code: ErrorCodes.LOAD_REFERENTIAL_ENTITIES_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ENTITIES_ERROR" }
    })
      .pipe(
        map((data) => {
          const res = (data && data.referentialTypes || []);
          return res;
        })
      );
  }

  /**
   * Load entity levels
   */
  async loadLevels(entityName: string): Promise<Referential[]> {
    if (this._debug) console.debug(`[referential-service] Loading referential levels for ${entityName}...`);
    const data = await this.query<{ referentialLevels: Referential[] }>({
      query: LoadReferentialLevels,
      variables: {
        entityName: entityName
      },
      error: { code: ErrorCodes.LOAD_REFERENTIAL_LEVELS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_LEVELS_ERROR" }
    });

    return (data && data.referentialLevels || []).map(Referential.fromObject);
  }

  /**
   * Load program pmfms
   */
  loadProgramPmfms(program: string, options?: {
    acquisitionLevel: string,
    gear?: string
  }): Observable<PmfmStrategy[]> {
    //console.debug("[referential-service] Getting pmfms for program {" + program + "}");
    return this.watchQuery<{ programPmfms: PmfmStrategy[] }>({
      query: LoadProgramPmfms,
      variables: {
        program: program
      },
      error: { code: ErrorCodes.LOAD_PROGRAM_PMFMS_ERROR, message: "REFERENTIAL.ERROR.LOAD_PROGRAM_PMFMS_ERROR" }
    })
      .pipe(
        map((data) => (data && data.programPmfms || [])
          // Filter on acquisition level and gear
          .filter(p => !options || (
            (!options.acquisitionLevel || p.acquisitionLevel == options.acquisitionLevel)
            && (!options.gear || p.gears && p.gears.findIndex(g => g == options.gear) !== -1)
          ))
          // Sort on rank order
          .sort((p1, p2) => p1.rankOrder - p2.rankOrder)
        )
      );

    // TODO: translate name/label using translate service
  }

  /* -- protected methods -- */


  protected fillDefaultProperties(entity: Referential) {

    entity.statusId = (entity.statusId == undefined) ? 1 : entity.statusId;
  }
}

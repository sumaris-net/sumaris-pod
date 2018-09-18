import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Observable } from "rxjs-compat";
import { map } from "rxjs/operators";
import { Referential, StatusIds, PmfmStrategy } from "./model";
import { DataService, BaseDataService } from "../../core/services/data-service.class";
import { Apollo } from "apollo-angular";

import { ErrorCodes } from "./errors";
import { AccountService } from "../../core/services/account.service";

export declare class ReferentialFilter {
  label?: string;
  name?: string;
  levelId?: number;
  searchText?: string;
}
const LoadAllLightQuery: any = gql`
  query Referenials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      statusId
      levelId
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
  mutation SaveReferentials($entityName: String, $referentials:[ReferentialVOInput]){
    saveReferentials(entityName: $entityName, referentials: $referentials){
      id
      label
      updateDate
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
export class ReferentialService extends BaseDataService implements DataService<Referential, ReferentialFilter>{

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);
  }

  loadAll(offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: ReferentialFilter,
    options?: any): Observable<Referential[]> {

    let query = options && options.full ? LoadAllQuery : LoadAllLightQuery;

    if (!options || !options.entityName) {
      console.error("[referential-service] Missing options.entityName");
      throw { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" };
    }

    const variables: any = {
      entityName: options.entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    this._lastVariables.loadAll = variables;

    //console.debug("[referential-service] Getting data from options:", variables);
    return this.watchQuery<{ referentials: any[] }>({
      query: query,
      variables: variables,
      error: { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" }
    })
      .pipe(
        map((data) => (data && data.referentials || []).map(Referential.fromObject))
      );
  }

  async saveAll(entities: Referential[], options?: any): Promise<Referential[]> {
    if (!entities) return entities;

    if (!options || !options.entityName) {
      console.error("[referential-service] Missing options.entityName");
      throw { code: ErrorCodes.SAVE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIALS_ERROR" };
    }

    const json = entities.map(t => t.asObject());

    console.debug("[referential-service] Saving referentials: ", json);

    const res = await this.mutate<{ saveReferentials: Referential[] }>({
      mutation: SaveReferentials,
      variables: {
        entityName: options.entityName,
        referentials: json
      },
      error: { code: ErrorCodes.SAVE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIALS_ERROR" }
    });
    return (res && res.saveReferentials && entities || [])
      .map(t => {
        const data = res.saveReferentials.find(res => (res.id == t.id || res.label == t.label));
        t.id = data && data.id || t.id;
        t.updateDate = data && data.updateDate || t.updateDate;
        return t;
      });
  }

  /**
   * Save a referential entity
   * @param entity 
   */
  save(entity: Referential, options?: any): Promise<Referential> {

    if (!options || !options.entityName) {
      console.error("[referential-service] Missing options.entityName");
      throw { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" };
    }

    // Transform into json
    const json = entity.asObject();
    const isNew = !json.id;

    console.debug("[referential-service] Saving referential: ", json);

    return this.mutate<{ saveReferentials: any }>({
      mutation: SaveReferentials,
      variables: {
        entityName: options.entityName,
        referentials: [json]
      },
      error: { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" }
    })
      .then(data => {
        var res = data && data.saveReferentials && data.saveReferentials[0];
        entity.id = res && res.id || entity.id;
        entity.updateDate = res && res.updateDate || entity.updateDate;

        // Update the cache
        if (isNew && this._lastVariables.loadAll) {
          const list = this.addToQueryCache({
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'trips', res);
        }

        return entity;
      });
  }

  /**
   * Delete referential entities
   */
  deleteAll(entities: Referential[], options?: any): Promise<any> {

    if (!options || !options.entityName) {
      console.error("[referential-service] Missing options.entityName");
      throw { code: ErrorCodes.DELETE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIALS_ERROR" };
    }

    let ids = entities && entities
      .map(t => t.id)
      .filter(id => (id > 0));

    console.debug("[referential-service] Deleting trips... ids:", ids);

    return this.mutate<any>({
      mutation: DeleteReferentials,
      variables: {
        entityName: options.entityName,
        ids: ids
      },
      error: { code: ErrorCodes.DELETE_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.DELETE_REFERENTIALS_ERROR" }
    });
  }

  /**
   * Load referential types
   */
  loadTypes(): Observable<ReferentialType[]> {
    //console.debug("[referential-service] Getting referential types");
    return this.watchQuery<{ referentialTypes: ReferentialType[] }>({
      query: LoadReferentialTypes,
      variables: null,
      error: { code: ErrorCodes.LOAD_REFERENTIAL_ENTITIES_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ENTITIES_ERROR" }
    })
      .pipe(
        map((data) => (data && data.referentialTypes || []))
      );
  }

  /**
   * Load entity levels
   */
  loadLevels(entityName: string): Observable<Referential[]> {
    //console.debug("[referential-service] Getting referential levels");
    return this.watchQuery<{ referentialLevels: Referential[] }>({
      query: LoadReferentialLevels,
      variables: {
        entityName: entityName
      },
      error: { code: ErrorCodes.LOAD_REFERENTIAL_LEVELS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_LEVELS_ERROR" }
    })
      .pipe(
        map((data) => (data && data.referentialLevels || []))
      );
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

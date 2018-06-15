import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { Referential, StatusIds } from "./model";
import { DataService, BaseDataService } from "./data-service";
import { Apollo } from "apollo-angular";
import { DocumentNode } from "graphql";
import { ErrorCodes } from "./errors";
import { AccountService } from "./account-service";

export declare class ReferentialFilter {
  label?: string;
  name?: string;
  levelId?: number;
  searchText?: string;
}
const LoadAllLightQuery: DocumentNode = gql`
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
const LoadAllQuery: DocumentNode = gql`
  query Referenials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      updateDate
      creationDate
      statusId
      levelId
    }
  }
`;
const LoadReferentialEntities: DocumentNode = gql`
  query ReferentialEntities{
    referentialEntities
  }
`;

const LoadReferentialLevels: DocumentNode = gql`
  query ReferentialLevels($entityName: String) {
    referentialLevels(entityName: $entityName){
      id
      label
      name
    }
  }
`;

const SaveReferentials: DocumentNode = gql`
  mutation SaveReferentials($entityName: String, $referentials:[ReferentialVOInput]){
    saveReferentials(entityName: $entityName, referentials: $referentials){
      id
      label
      updateDate
    }
  }
`;

const DeleteReferentials: DocumentNode = gql`
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

    return this.loadFromQueries(query, offset, size, sortBy, sortDirection, filter, options);
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
   * Load entity names
   */
  loadEntitieNames(): Observable<string[]> {
    console.debug("[referential-service] Getting referential entities");
    return this.watchQuery<{ referentialEntities: string[] }>({
      query: LoadReferentialEntities,
      variables: null,
      error: { code: ErrorCodes.LOAD_REFERENTIAL_ENTITIES_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ENTITIES_ERROR" }
    })
      .pipe(
        map((data) => (data && data.referentialEntities || []))
      );
  }

  /**
   * Load entity levels
   */
  loadLevels(entityName: string): Observable<Referential[]> {
    console.debug("[referential-service] Getting referential levels");
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

  /* -- protected methods -- */


  protected loadFromQueries(
    query: DocumentNode,
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: ReferentialFilter,
    options?: any): Observable<Referential[]> {

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
    console.debug("[referential-service] Getting data from options:", variables);
    return this.watchQuery<{ referentials: any[] }>({
      query: query,
      variables: variables,
      error: { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" }
    })
      .pipe(
        map((data) => (data && data.referentials || []).map(t => {
          const res = new Referential();
          res.fromObject(t);
          return res;
        })
        )
      );
  }

  protected fillDefaultProperties(entity: Referential) {

    entity.statusId = (entity.statusId == undefined) ? 1 : entity.statusId;
  }
}

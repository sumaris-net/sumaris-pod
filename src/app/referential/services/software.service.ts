import {Injectable, InjectionToken} from "@angular/core";
import gql from "graphql-tag";
import {environment} from "../../../environments/environment";
import {Observable, Subject} from "rxjs";
import {ErrorCodes} from "./errors";
import {FormFieldDefinitionMap} from "../../shared/form/field.model";
import {isNotNil} from "../../shared/functions";
import {EditorDataService, EditorDataServiceLoadOptions} from "../../shared/shared.module";
import {Software} from "../../core/services/model";
import {BaseDataService} from "../../core/core.module";
import {GraphqlService} from "../../core/services/graphql.service";
import {ConfigService} from "../../core/services/config.service";

/* ------------------------------------
 * GraphQL queries
 * ------------------------------------*/
export const Fragments = {
  software: gql`
    fragment SoftwareFragment on SoftwareVO {
      id
      label
      name
      properties
      updateDate
      creationDate
      statusId
      __typename
    }
  `
};


const LoadQuery: any = gql`
query Software($id: Int, $label: String) {
  software(id: $id, label: $label){
    ...SoftwareFragment
  }
}
  ${Fragments.software}
`;

// Save (create or update) mutation
const SaveMutation: any = gql`
  mutation SaveConfiguration($software:SoftwareVOInput){
    saveSoftware(software: $software){
       ...SoftwareFragment
    }
  }
  ${Fragments.software}
`;


@Injectable({
  providedIn: 'root'
})
export class SoftwareService<T extends Software<T> = Software<any>> extends BaseDataService<T> implements EditorDataService<T> {

  constructor(
    protected graphql: GraphqlService,
    protected configService: ConfigService
  ) {
    super(graphql);
    console.debug("[software-service] Creating configuration service");

    this._debug = !environment.production;
  }

  async load(
    id: number,
    opts?: EditorDataServiceLoadOptions): Promise<T> {

    return this.loadQuery(
      LoadQuery,
      {id},
      opts
    );
  }

  async existsByLabel(label: string): Promise<boolean> {
    const existingConfig = await this.loadQuery(LoadQuery, {label}, {fetchPolicy: "network-only"});
    return isNotNil(existingConfig && existingConfig.id);
  }

  /**
   * Save a configuration
   * @param entity
   */
  async save(entity: T): Promise<T> {

    console.debug("[software-service] Saving configuration...", entity);

    const json = entity.asObject();

    // Execute mutation
    await this.graphql.mutate<{ saveSoftware: any }>({
      mutation: SaveMutation,
      variables: {
        software: json
      },
      error: {
        code: ErrorCodes.SAVE_SOFTWARE_ERROR,
        message: "ERROR.SAVE_SOFTWARE_ERROR"
      },
      update: (proxy, {data}) => {
        const savedEntity = data && data.saveSoftware;

        // Copy update properties
        entity.id = savedEntity && savedEntity.id || entity.id;
        entity.updateDate = savedEntity && savedEntity.updateDate || entity.updateDate;

        console.debug("[software-service] Software saved!");
      }
    });


    return entity;
  }

  delete(data: T, options?: any): Promise<any> {
    throw new Error("Not implemented yet!");
  }

  listenChanges(id: number, options?: any): Observable<T | undefined> {
    // if (this.$data.getValue() && this.$data.getValue().id === id) {
    //   return this.$data;
    // }
    return new Subject(); // TODO
  }

  /* -- private method -- */

  private async loadQuery(
    query: any,
    variables: any,
    opts?: EditorDataServiceLoadOptions): Promise<T> {

    const now = Date.now();
    console.debug("[software-service] Loading software ...");

    const res = await this.graphql.query<{ software: Software<T> }>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_SOFTWARE_ERROR, message: "ERROR.LOAD_SOFTWARE_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || undefined/*default*/
    });

    const data = res && res.software ? Software.fromObject(res.software) : undefined;
    console.debug(`[software-service] Software loaded in ${Date.now() - now}ms:`, data);
    return data as T;
  }
}



import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {EditorDataService, EditorDataServiceLoadOptions, isNil, isNotNil} from "../../shared/shared.module";
import {BaseDataService, EntityUtils, StatusIds} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {GraphqlService} from "../../core/services/graphql.service";
import {environment} from "../../../environments/environment";
import {ReferentialService} from "./referential.service";
import {Observable, of} from "rxjs";
import {Parameter} from "./model/parameter.model";
import {ReferentialFragments} from "./referential.queries";

const SaveQuery: any = gql`
  mutation SaveParameter($parameter:ParameterVOInput){
    saveParameter(parameter: $parameter){
      ...FullParameterFragment
    }
  }
  ${ReferentialFragments.fullReferential}
  ${ReferentialFragments.fullParameter}
`;

const LoadQuery: any = gql`
  query Parameter($label: String, $id: Int){
    parameter(label: $label, id: $id){
      ...FullParameterFragment
    }
  }
  ${ReferentialFragments.fullReferential}
  ${ReferentialFragments.fullParameter}
`;

@Injectable({providedIn: 'root'})
export class ParameterService extends BaseDataService implements EditorDataService<Parameter> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService,
    protected referentialService: ReferentialService
  ) {
    super(graphql);

    // For DEV only
    this._debug = !environment.production;
  }

  async existsByLabel(label: string, opts?: { excludedId?: number; }): Promise<boolean> {
    if (isNil(label)) return false;
    return await this.referentialService.existsByLabel(label, { ...opts, entityName: 'Parameter' });
  }

  async load(id: number, options?: EditorDataServiceLoadOptions): Promise<Parameter> {

    if (this._debug) console.debug(`[parameter-service] Loading parameter {${id}}...`);

    const res = await this.graphql.query<{ parameter: any }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"}
    });
    const entity = res && Parameter.fromObject(res.parameter);

    if (this._debug) console.debug(`[pmfm-service] Parameter {${id}} loaded`, entity);

    return entity;
  }

  /**
   * Save a parameter entity
   * @param entity
   */
  async save(entity: Parameter, options?: EditorDataServiceLoadOptions): Promise<Parameter> {

    this.fillDefaultProperties(entity);

    // Transform into json
    const json = entity.asObject();
    const isNew = !json.id;

    const now = Date.now();
    if (this._debug) console.debug(`[parameter-service] Saving Parameter...`, json);

    await this.graphql.mutate<{ saveParameter: any }>({
      mutation: SaveQuery,
      variables: {
        parameter: json
      },
      error: { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" },
      update: (proxy, {data}) => {
        // Update entity
        const savedEntity = data && data.saveParameter;
        if (savedEntity) {
          if (this._debug) console.debug(`[parameter-service] Parameter saved in ${Date.now() - now}ms`, entity);
          this.copyIdAndUpdateDate(savedEntity, entity);
        }

        // Update the cache
        if (isNew && this._lastVariables.loadAll) {
          if (this._debug) console.debug(`[parameter-service] Updating cache with saved ${entity.entityName}...`);
          this.graphql.addToQueryCache(proxy, {
            query: LoadQuery,
            variables: this._lastVariables.load
          }, 'parameter', entity.asObject());
        }

      }
    });

    return entity;
  }

  /**
   * Delete parameter entities
   */
  async delete(entity: Parameter, options?: any): Promise<any> {

    entity.entityName = 'Parameter';

    await this.referentialService.deleteAll([entity], {
      update: (proxy) => {
        // Remove from cache
        if (this._lastVariables.load) {
          this.graphql.removeToQueryCacheById(proxy, {
            query: LoadQuery,
            variables: this._lastVariables.loadAll
          }, 'parameter', entity.id);
        }
      }
    });
  }

  listenChanges(id: number, options?: any): Observable<Parameter | undefined> {
    // TODO
    console.warn("TODO: implement listen changes on parameter");
    return of();
  }

  /* -- protected methods -- */


  protected fillDefaultProperties(entity: Parameter) {
    entity.statusId = isNotNil(entity.statusId) ? entity.statusId : StatusIds.ENABLE;
  }

  protected copyIdAndUpdateDate(source: Parameter, target: Parameter) {
    EntityUtils.copyIdAndUpdateDate(source, target);

    // Update strategies
    if (source.qualitativeValues && target.qualitativeValues) {
      target.qualitativeValues.forEach(entity => {

        entity.levelId = source.id;
        const savedQualitativeValue = source.qualitativeValues.find(json => entity.equals(json));
        EntityUtils.copyIdAndUpdateDate(savedQualitativeValue, entity);
      });
    }

  }
}

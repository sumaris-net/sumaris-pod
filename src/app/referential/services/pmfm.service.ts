import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {EntityUtils, isNil, isNotNil, StatusIds} from "./model";
import {EditorDataService, EditorDataServiceLoadOptions} from "../../shared/shared.module";
import {BaseDataService, SAVE_AS_OBJECT_OPTIONS} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {GraphqlService} from "../../core/services/graphql.service";
import {environment} from "../../../environments/environment";
import {ReferentialService} from "./referential.service";
import {Pmfm} from "./model/pmfm.model";
import {Observable, of} from "rxjs";
import {ReferentialFragments} from "./referential.queries";

const LoadQuery: any = gql`
  query Pmfm($label: String, $id: Int){
    pmfm(label: $label, id: $id){
      ...FullPmfmFragment
    }
  }
  ${ReferentialFragments.fullPmfm}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.fullReferential}
  ${ReferentialFragments.fullParameter}
`;

const SaveQuery: any = gql`
  mutation SavePmfm($pmfm:PmfmVOInput){
    savePmfm(pmfm: $pmfm){
      ...FullPmfmFragment
    }
  }
  ${ReferentialFragments.fullPmfm}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.fullReferential}
  ${ReferentialFragments.fullParameter}
`;

@Injectable({providedIn: 'root'})
export class PmfmService extends BaseDataService implements EditorDataService<Pmfm> {

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
    return await this.referentialService.existsByLabel(label, { ...opts, entityName: 'Pmfm' });
  }

  async load(id: number, options?: EditorDataServiceLoadOptions): Promise<Pmfm> {

    if (this._debug) console.debug(`[pmfm-service] Loading pmfm {${id}}...`);

    const res = await this.graphql.query<{ pmfm: any }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"}
    });
    const entity = res && res.pmfm && Pmfm.fromObject(res.pmfm);

    if (this._debug) console.debug(`[pmfm-service] Pmfm {${id}} loaded`, entity);

    return entity;
  }

  /**
   * Save a pmfm entity
   * @param entity
   */
  async save(entity: Pmfm, options?: EditorDataServiceLoadOptions): Promise<Pmfm> {

    this.fillDefaultProperties(entity);

    // Transform into json
    const json = entity.asObject(SAVE_AS_OBJECT_OPTIONS);
    const isNew = !json.id;

    const now = Date.now();
    if (this._debug) console.debug(`[pmfm-service] Saving Pmfm...`, json);

    await this.graphql.mutate<{ savePmfm: any }>({
      mutation: SaveQuery,
      variables: {
        pmfm: json
      },
      error: { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" },
      update: (proxy, {data}) => {
        // Update entity
        const savedEntity = data && data.savePmfm;
        if (savedEntity) {
          if (this._debug) console.debug(`[pmfm-service] Pmfm saved in ${Date.now() - now}ms`, entity);
          this.copyIdAndUpdateDate(savedEntity, entity);
        }

        // Update the cache
        if (isNew && this._lastVariables.loadAll) {
          if (this._debug) console.debug(`[pmfm-service] Updating cache with saved ${entity.entityName}...`);
          this.graphql.addToQueryCache(proxy, {
            query: LoadQuery,
            variables: this._lastVariables.load
          }, 'pmfm', entity.asObject());
        }

      }
    });

    return entity;
  }

  /**
   * Delete pmfm entities
   */
  async delete(entity: Pmfm, options?: any): Promise<any> {

    entity.entityName = 'Pmfm';

    await this.referentialService.deleteAll([entity], {
      update: (proxy) => {
        // Remove from cache
        if (this._lastVariables.load) {
          this.graphql.removeToQueryCacheById(proxy, {
            query: LoadQuery,
            variables: this._lastVariables.loadAll
          }, 'pmfm', entity.id);
        }
      }
    });
  }

  listenChanges(id: number, options?: any): Observable<Pmfm | undefined> {
    // TODO
    console.warn("TODO: implement listen changes on pmfm");
    return of();
  }

  /* -- protected methods -- */

  protected fillDefaultProperties(entity: Pmfm) {
    entity.statusId = isNotNil(entity.statusId) ? entity.statusId : StatusIds.ENABLE;
  }

  protected copyIdAndUpdateDate(source: Pmfm, target: Pmfm) {
    EntityUtils.copyIdAndUpdateDate(source, target);

    // Update Qualitative values
    if (source.qualitativeValues && target.qualitativeValues) {
      target.qualitativeValues.forEach(entity => {
        const savedQualitativeValue = source.qualitativeValues.find(json => entity.equals(json));
        EntityUtils.copyIdAndUpdateDate(savedQualitativeValue, entity);
      });
    }

  }
}

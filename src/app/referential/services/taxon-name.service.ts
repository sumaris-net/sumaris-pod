import {Injectable} from "@angular/core";
import {gql} from "@apollo/client/core";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {ReferentialService} from "./referential.service";
import {Observable, of} from "rxjs";
import {ReferentialFragments} from "./referential.fragments";
import {EntityServiceLoadOptions, IEntityService} from "../../shared/services/entity-service.class";
import {isNil, isNotNil} from "../../shared/functions";
import {BaseGraphqlService} from "../../core/services/base-graphql-service.class";
import {environment} from "../../../environments/environment";
import {StatusIds} from "../../core/services/model/model.enum";
import {EntityUtils} from "../../core/services/model/entity.model";
import {TaxonName} from "./model/taxon-name.model";
import {SAVE_AS_OBJECT_OPTIONS} from "../../core/services/model/referential.model";

const SaveQuery: any = gql`
  mutation saveTaxonName($taxonName:TaxonNameVOInput!){
    saveTaxonName(taxonName: $taxonName){
      ...LightTaxonNameFragment
    }
  }
  ${ReferentialFragments.lightTaxonName}
`;

const LoadQuery: any = gql`
  query taxonName($label: String, $id: Int){
      data: taxonName(label: $label, id: $id){
        ...FullTaxonNameFragment
    }
  }
  ${ReferentialFragments.fullTaxonName}
`;

const ExistsQuery: any = gql`
  query referenceTaxonExists($id: Int){
    data: referenceTaxonExists(id: $id)
  }
`;

@Injectable({providedIn: 'root'})
export class TaxonNameService extends BaseGraphqlService implements IEntityService<TaxonName> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService,
    protected referentialService: ReferentialService
  ) {
    super(graphql, environment);
  }

  async existsByLabel(label: string, opts?: { excludedId?: number; }): Promise<boolean> {
    if (isNil(label)) return false;
    return await this.referentialService.existsByLabel(label, { ...opts, entityName: 'TaxonName' });
  }

  async referenceTaxonExists(referenceTaxonId: number): Promise<boolean> {
    if (isNil(referenceTaxonId)) return false;

    const {data} = await this.graphql.query<{ data: boolean; }>({
      query: ExistsQuery,
      variables : {
        id: referenceTaxonId
      },
      error: { code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR" }
    });

    return data;
  }

  async load(id: number, options?: EntityServiceLoadOptions): Promise<TaxonName> {

    if (this._debug) console.debug(`[taxon-name-service] Loading taxon name {${id}}...`);

    const {data} = await this.graphql.query<{ data: any }>({
      query: LoadQuery,
      variables: {
        id
      },
      error: {code: ErrorCodes.LOAD_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIAL_ERROR"}
    });
    const entity = data && TaxonName.fromObject(data);

    if (this._debug) console.debug(`[taxon-name-service] Taxon Name {${id}} loaded`, entity);

    return entity;
  }

  /**
   * Save a taxon name entity
   * @param entity
   */
  async save(entity: TaxonName, options?: EntityServiceLoadOptions): Promise<TaxonName> {

    this.fillDefaultProperties(entity);

    // Transform into json
    const json = entity.asObject(SAVE_AS_OBJECT_OPTIONS);

    const now = Date.now();
    if (this._debug) console.debug(`[taxon-name-service] Saving Taxon Name ...`, json);

    await this.graphql.mutate<{ saveTaxonName: any }>({
      mutation: SaveQuery,
      variables: {
        taxonName: json
      },
      error: { code: ErrorCodes.SAVE_REFERENTIAL_ERROR, message: "REFERENTIAL.ERROR.SAVE_REFERENTIAL_ERROR" },
      update: (proxy, {data}) => {
        // Update entity
        const savedEntity = data && data.saveTaxonName;
        if (savedEntity) {
          if (this._debug) console.debug(`[taxon-name-service] Taxon Name saved in ${Date.now() - now}ms`, entity);
          this.copyIdAndUpdateDate(savedEntity, entity);
          entity.referenceTaxonId = savedEntity.referenceTaxonId;
        }
      }
    });

    return entity;
  }

  /**
   * Delete parameter entities
   */
  async delete(entity: TaxonName, options?: any): Promise<any> {

    entity.entityName = 'TaxonName';

    await this.referentialService.deleteAll([entity]);
  }

  listenChanges(id: number, options?: any): Observable<TaxonName | undefined> {
    // TODO
    console.warn("TODO: implement listen changes on taxon name");
    return of();
  }

  /* -- protected methods -- */


  protected fillDefaultProperties(entity: TaxonName) {
    entity.statusId = isNotNil(entity.statusId) ? entity.statusId : StatusIds.ENABLE;
  }

  protected copyIdAndUpdateDate(source: TaxonName, target: TaxonName) {
    EntityUtils.copyIdAndUpdateDate(source, target);
  }
}

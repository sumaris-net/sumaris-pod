import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import {DataService, BaseDataService, LoadResult} from "../../core/services/data-service.class";
import { Apollo } from "apollo-angular";
import { ErrorCodes } from "./errors";
import { AccountService } from "../../core/services/account.service";
import { ReferentialRef } from "../../core/services/model";

import { FetchPolicy } from "apollo-client";
import { ReferentialFilter } from "./referential.service";
import { environment } from "src/app/core/core.module";

const LoadAllQuery: any = gql`
  query Referentials($entityName: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(entityName: $entityName, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      statusId
      entityName
    }
  }
`;

@Injectable()
export class ReferentialRefService extends BaseDataService implements DataService<ReferentialRef, ReferentialFilter> {

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);

    // -- For DEV only
    //this._debug = !environment.production;
  }

  loadAll(offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: ReferentialFilter,
    options?: {
      [key: string]: any;
      fetchPolicy?: FetchPolicy;
    }): Observable<LoadResult<ReferentialRef>> {

    if (!filter || !filter.entityName) {
      console.error("[referential-ref-service] Missing filter.entityName");
      throw { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" };
    }

    const entityName = filter.entityName;

    const variables: any = {
      entityName: entityName,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: {
        label: filter.label,
        name: filter.name,
        searchText: filter.searchText,
        searchAttribute: filter.searchAttribute,
        levelId: filter.levelId
      }
    };

    const now = new Date();
    if (this._debug) console.debug(`[referential-ref-service] Loading references on ${entityName}...`, variables);

    return this.watchQuery<{ referentials: any[]; referentialsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR" },
      fetchPolicy: options && options.fetchPolicy || "cache-first"
    })
      .pipe(
        map(({referentials, referentialsCount}) => {
          const data = (referentials || []).map(ReferentialRef.fromObject);
          if (this._debug) console.debug(`[referential-ref-service] References on ${entityName} loaded in ${new Date().getTime() - now.getTime()}ms`, data);
          return {
            data: data,
            total: referentialsCount
          };
        })
      );
  }


  saveAll(entities: ReferentialRef[], options?: any): Promise<ReferentialRef[]> {
    throw 'Not implemented ! Use ReferentialService instead';
  }

  deleteAll(entities: ReferentialRef[], options?: any): Promise<any> {
    throw 'Not implemented ! Use ReferentialService instead';
  }

}

import {Observable} from "rxjs";

import {FetchPolicy, WatchQueryFetchPolicy} from "@apollo/client/core";
import {SortDirection} from "@angular/material/sort";

import {ReferentialFilter} from "./referential.service";
import {Referential, ReferentialUtils} from "../../core/services/model/referential.model";
import {LoadResult} from "../../shared/services/entity-service.class";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {PlatformService} from "../../core/services/platform.service";
import {isNotNil} from "../../shared/functions";
import {Directive} from "@angular/core";
import {BaseEntityService, BaseEntityServiceOptions} from "./base-entity-service.class";


@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class BaseReferentialService<E extends Referential<any>, F extends ReferentialFilter>
  extends BaseEntityService<E, F>  {

  protected constructor(
    protected graphql: GraphqlService,
    protected platform: PlatformService,
    protected dataType: new() => E,
    options: BaseEntityServiceOptions<E, F>
  ) {
    super(graphql, platform, dataType, {
      equalsFn: (e1, e2) => this.equals(e1, e2),
      ...options
    });
  }

  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: F, opts?: { fetchPolicy?: WatchQueryFetchPolicy; withTotal: boolean; toEntity?: boolean }): Observable<LoadResult<E>> {
    // Use search attribute as default sort, is set
    sortBy = sortBy || filter && filter.searchAttribute;
    // Call inherited function
    return super.watchAll(offset, size, sortBy, sortDirection, filter, opts);
  }

  async loadAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: F, opts?: { [p: string]: any; query?: any; fetchPolicy?: FetchPolicy; debug?: boolean; withTotal?: boolean; toEntity?: boolean }): Promise<LoadResult<E>> {
    // Use search attribute as default sort, is set
    sortBy = sortBy || filter && filter.searchAttribute;
    // Call inherited function
    return super.loadAll(offset, size, sortBy, sortDirection, filter, opts);
  }

  async suggest(value: any, filter?: F): Promise<LoadResult<E>> {
    if (ReferentialUtils.isNotEmpty(value)) return {data: [value]};
    value = (typeof value === "string" && value !== '*') && value || undefined;
    return this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        ...filter,
        searchText: value as string
      }
    );
  }

  /* -- protected functions -- */

  /**
   * Workaround to implements CRUD functions using existing queries, when all queries are not been set
   *
   * @protected
   */
  protected createQueriesAndMutationsFallback() {

    super.createQueriesAndMutationsFallback();

    if (this.queries) {
      // load()
      if (!this.queries.load && this.queries.loadAll) {
        this.load = async (id, opts) => {
          const data = await this.loadAll(0, 1, null, null, { id: id } as F, opts);
          return data && data[0];
        };
      }
    }
  }

  protected equals(e1: E, e2: E): boolean {
    return e1 && e2 && ((isNotNil(e1.id) && e1.id === e2.id) || (e1.label && e1.label === e2.label));
  }
}

import {Injectable} from "@angular/core";
import {FetchPolicy, gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {BehaviorSubject, Observable} from 'rxjs';
import {IEntitiesService, isNil, isNotEmptyArray, LoadResult, SuggestService} from "../../shared/shared.module";
import {BaseEntityService} from "../../core/services/base.data-service.class";
import {ErrorCodes} from "./errors";
import {map} from "rxjs/operators";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {EntityUtils} from "../../core/services/model/entity.model";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {environment} from "../../../environments/environment";
import {Beans, KeysEnum} from "../../shared/functions";
import {Person} from "../../core/services/model/person.model";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {StatusIds} from "../../core/services/model/model.enum";
import {SortDirection} from "@angular/material/sort";
import {JobUtils} from "../../shared/services/job.utils";
import {FilterFn} from "../../shared/services/entity-service.class";

export const PersonFragments = {
  person: gql`fragment PersonFragment on PersonVO {
    id
    firstName
    lastName
    email
    pubkey
    avatar
    statusId
    updateDate
    creationDate
    profiles
    department {
      id
      label
      name
      logo
      __typename
    }
    __typename
  }
  `
};

// Load persons query
const LoadAllQuery = gql`
  query Persons($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: PersonFilterVOInput){
    data: persons(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...PersonFragment
    }
  }
  ${PersonFragments.person}
`;

// Load persons query
const LoadAllWithTotalQuery = gql`
  query PersonsWithTotal($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: PersonFilterVOInput){
    data: persons(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...PersonFragment
    }
    total: personsCount(filter: $filter)
  }
  ${PersonFragments.person}
`;

export class PersonFilter {
  email?: string;
  pubkey?: string;
  searchText?: string;
  statusIds?: number[];
  userProfiles?: string[];
  excludedIds?: number[];
  searchAttribute?: string;

  static searchFilter<T extends Person>(f: PersonFilter): (T) => boolean {
    const filterFns: FilterFn<T>[] = [];

    // Filter by status
    if (f.statusIds) {
      filterFns.push((entity) => !!f.statusIds.find(v => entity.statusId === v));
    }

    // Filter excluded ids
    if (isNotEmptyArray(f.excludedIds)) {
      filterFns.push((entity) => isNil(entity.id) || !f.excludedIds.includes(entity.id));
    }

    // Search text
    const searchTextFilter = EntityUtils.searchTextFilter(f.searchAttribute || ['lastName', 'firstName', 'department.name'], f.searchText);
    if (searchTextFilter) filterFns.push(searchTextFilter);

    if (!filterFns.length) return undefined;

    return (entity) => !filterFns.find(fn => !fn(entity));
  }

  static isEmpty(filter: PersonFilter|any): boolean {
    return Beans.isEmpty<PersonFilter>(filter, PersonFilterKeys, {
      blankStringLikeEmpty: true
    });
  }

  static asPodObject(filter: PersonFilter|any) {
    return Beans.copy(filter, PersonFilter, PersonFilterKeys);
  }
}
export const PersonFilterKeys: KeysEnum<PersonFilter> = {
  email: true,
  pubkey: true,
  searchText: true,
  statusIds: true,
  userProfiles: true,
  excludedIds: true,
  searchAttribute: true
};

const SavePersons: any = gql`
  mutation savePersons($persons:[PersonVOInput]){
    savePersons(persons: $persons){
      ...PersonFragment
    }
  }
  ${PersonFragments.person}
`;
const DeletePersons: any = gql`
  mutation deletePersons($ids:[Int]){
    deletePersons(ids: $ids)
  }
`;

@Injectable({providedIn: 'root'})
export class PersonService extends BaseEntityService<Person, PersonFilter>
  implements IEntitiesService<Person, PersonFilter>, SuggestService<Person, PersonFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected entities: EntitiesStorage
  ) {
    super(graphql);

    // for DEV only -----
    this._debug = !environment.production;
  }

  /**
   * Load/search some persons
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   * @param opts
   */
  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: SortDirection,
    filter?: PersonFilter,
    opts?: {
      fetchPolicy?: WatchQueryFetchPolicy;
      withTotal?: boolean;
    }
  ): Observable<LoadResult<Person>> {

    const variables = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'lastName',
      sortDirection: sortDirection || 'asc',
      filter: PersonFilter.asPodObject(filter)
    };

    if (this._debug) console.debug("[person-service] Watching persons, using filter: ", variables);

    const withTotal = (!opts || opts.withTotal !== false);
    return this.mutableWatchQuery<LoadResult<Person>>({
      queryName: withTotal ? 'LoadAllWithTotal' : 'LoadAll',
      query: withTotal ? LoadAllWithTotalQuery : LoadAllQuery,
      arrayFieldName: 'data',
      totalFieldName: (!opts || opts.withTotal !== false) ? 'total' : undefined,
      variables,
      error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || 'cache-and-network'
    })
    .pipe(
      map(({data, total}) => {
        return {
          data: (data || []).map(Person.fromObject),
          total
        };
      })
    );
  }

  async loadAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: PersonFilter, opts?: {
    [key: string]: any;
    fetchPolicy?: FetchPolicy;
    debug?: boolean;
    withTotal?: boolean;
    toEntity?: boolean;
  }): Promise<LoadResult<Person>> {

    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      return this.loadAllLocally(offset, size, sortBy, sortDirection, filter, opts);
    }

    const variables = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'lastName',
      sortDirection: sortDirection || 'asc',
      filter: PersonFilter.asPodObject(filter)
    };

    const debug = this._debug && (!opts || opts.debug !== false);
    if (debug) console.debug("[person-service] Loading persons, using filter: ", variables);

    const query = (!opts || opts.withTotal !== false) ? LoadAllWithTotalQuery : LoadAllQuery;
    const {data, total} = await this.graphql.query<LoadResult<Person>>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || undefined
    });

    const entities = (!opts || opts.toEntity !== false) ?
      (data || []).map(Person.fromObject) :
      (data || []) as Person[];
    return {
      data: entities,
      total
    };
  }

  async loadAllLocally(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: PersonFilter, opts?: {
    [key: string]: any;
    fetchPolicy?: FetchPolicy;
    debug?: boolean;
    withTotal?: boolean;
    toEntity?: boolean;
  }): Promise<LoadResult<Person>> {
    const variables = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'lastName',
      sortDirection: sortDirection || 'asc',
      filter: PersonFilter.searchFilter(filter)
    };

    const {data, total} = await this.entities.loadAll<Person>('PersonVO', variables);

    const entities = (!opts || opts.toEntity !== false) ?
      (data || []).map(Person.fromObject) :
      (data || []) as Person[];
    return {
      data: entities,
      total
    };
  }

  async suggest(value: any, filter?: PersonFilter): Promise<Person[]> {
    if (ReferentialUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        ...filter,
        searchText: value as string,
        statusIds: filter && filter.statusIds || [StatusIds.ENABLE],
        userProfiles: filter && filter.userProfiles
      },
      { withTotal: false /* total not need */ }
      );
    return res.data;
  }

  /**
   * Saving many persons
   * @param data
   */
  async saveAll(data: Person[]): Promise<Person[]> {
    if (!data) return data;

    // Convert as json object
    const json = data.map(this.asObject);

    const now = Date.now();
    if (this._debug) console.debug("[person-service] Saving persons...", data);

    const res = await this.graphql.mutate<{ savePersons: Person[] }>({
      mutation: SavePersons,
      variables: {
        persons: json
      },
      error: {code: ErrorCodes.SAVE_PERSONS_ERROR, message: "REFERENTIAL.ERROR.SAVE_PERSONS_ERROR"}
    });
    (res && res.savePersons && data)
      .forEach(entity => {
        const savedPerson = res.savePersons.find(p => entity.equals(p));
        EntityUtils.copyIdAndUpdateDate(savedPerson, entity);
      });

    if (this._debug) console.debug(`[person-service] Persons saved in ${Date.now() - now}ms`, data);

    return data;
  }

  async deleteAll(entities: Person[]): Promise<any> {

    const ids = entities && entities
      .map(t => t.id)
      .filter(id => (id > 0));

    const now = Date.now();
    if (this._debug) console.debug("[person-service] Deleting persons... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeletePersons,
      variables: {
        ids: ids
      },
      error: {code: ErrorCodes.DELETE_PERSONS_ERROR, message: "REFERENTIAL.ERROR.DELETE_PERSONS_ERROR"},
      update: (proxy) => {
        if (this._debug) console.debug(`[person-service] Trips deleted in ${Date.now() - now}ms`);

        // Update the cache
        this.removeFromMutableCachedQueryByIds(proxy, {
          query: LoadAllQuery,
          ids
        });
      }
    });

  }

  async executeImport(progression: BehaviorSubject<number>,
                opts?: {
                  maxProgression?: number;
                }): Promise<void> {

    const maxProgression = opts && opts.maxProgression || 100;
    const filter = {
      statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
      userProfiles: ['SUPERVISOR', 'USER', 'GUEST']
    };

    console.info("[person-service] Importing persons...");

    const res = await JobUtils.fetchAllPages((offset, size) =>
        this.loadAll(offset, size, 'id', null, filter, {
          debug: false,
          fetchPolicy: "network-only",
          withTotal: (offset === 0), // Compute total only once
          toEntity: false
        }),
      progression,
      {maxProgression: maxProgression * 0.9}
    );

    // Save result locally
    await this.entities.saveAll(res.data, {entityName: 'PersonVO', reset: true});
  }

  /* -- protected methods -- */

  protected asObject(source: Person | any): any {
    if (!source) return undefined;

    if (!(source instanceof Person)) {
      source = Person.fromObject(source);
    }
    const target = source.asObject();

    // Not known in server GraphQL schema
    delete target.mainProfile;

    // Minify the department object
    target.department = source.department && source.department.asObject(true);

    return target;
  }

}

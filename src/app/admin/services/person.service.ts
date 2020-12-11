import {Inject, Injectable} from "@angular/core";
import {FetchPolicy, gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {BehaviorSubject, defer, Observable} from 'rxjs';
import {BaseEntityService} from "../../core/services/base.data-service.class";
import {ErrorCodes} from "./errors";
import {map} from "rxjs/operators";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {EntityUtils} from "../../core/services/model/entity.model";
import {
  EntitiesService,
  fetchAllPagesWithProgress,
  LoadResult,
  SuggestService
} from "../../shared/services/entity-service.class";
import {NetworkService} from "../../core/services/network.service";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {Beans, KeysEnum} from "../../shared/functions";
import {Person} from "../../core/services/model/person.model";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {StatusIds} from "../../core/services/model/model.enum";
import {SortDirection} from "@angular/material/sort";
import {EnvironmentService} from "../../../environments/environment.class";

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
    persons(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...PersonFragment
    }
  }
  ${PersonFragments.person}
`;

// Load persons query
const LoadAllWithTotalQuery = gql`
  query PersonsWithTotal($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: PersonFilterVOInput){
    persons(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...PersonFragment
    }
    personsCount(filter: $filter)
  }
  ${PersonFragments.person}
`;

export class PersonFilter {
  email?: string;
  pubkey?: string;
  searchText?: string;
  statusIds?: number[];
  userProfiles?: string[];

  static isEmpty(personFilter: PersonFilter|any): boolean {
    return Beans.isEmpty<PersonFilter>(personFilter, PersonFilterKeys, {
      blankStringLikeEmpty: true
    });
  }
}
export const PersonFilterKeys: KeysEnum<PersonFilter> = {
  email: true,
  pubkey: true,
  searchText: true,
  statusIds: true,
  userProfiles: true,
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
  implements EntitiesService<Person, PersonFilter>, SuggestService<Person, PersonFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected entities: EntitiesStorage,
    @Inject(EnvironmentService) protected environment
  ) {
    super(graphql, environment);

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
      filter: Beans.copy(filter, PersonFilter, PersonFilterKeys)
    };

    if (this._debug) console.debug("[person-service] Watching persons, using filter: ", variables);

    if ((!opts || opts.withTotal !== false)) {
      return this.mutableWatchQuery<{ persons: Person[]; personsCount?: number }>({
        queryName: 'LoadAllWithTotal',
        query: LoadAllWithTotalQuery,
        arrayFieldName: 'persons',
        totalFieldName: 'personsCount',
        variables,
        error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || 'cache-and-network'
      })
      .pipe(
        map((res) => {
          return {
            data: (res && res.persons || []).map(Person.fromObject),
            total: res && res.personsCount || 0
          };
        })
      );
    }
    else {
      return this.mutableWatchQuery<{ persons: Person[] }>({
        queryName: 'LoadAll',
        query: LoadAllQuery,
        arrayFieldName: 'persons',
        variables,
        error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || 'cache-and-network'
      })
        .pipe(
          map((res) => {
            return {
              data: (res && res.persons || []).map(Person.fromObject),
              total: res && res.persons && res.persons.length || 0
            };
          })
        );
    }
  }

  async loadAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: PersonFilter, opts?: {
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
      filter: Beans.copy(filter, PersonFilter, PersonFilterKeys)
    };

    const debug = this._debug && (!opts || opts.debug !== false);
    const now = debug && Date.now();
    if (debug) console.debug("[person-service] Loading persons, using filter: ", variables);

    let loadResult: { persons: Person[]; personsCount: number };

    // Offline: use local store
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      const res = await this.entities.loadAll<Person>('PersonVO',
        {
          ...variables,
          filter: EntityUtils.searchTextFilter(['lastName', 'firstName', 'department.name'], filter.searchText)
        }
      );
      if (debug) console.debug(`[referential-ref-service] Persons loaded (from offline storage) in ${Date.now() - now}ms`);
      loadResult = {
        persons: res && res.data,
        personsCount: res && res.total
      };
    }

    // Online: use GraphQL
    else {
      const query = (!opts || opts.withTotal !== false) ? LoadAllWithTotalQuery : LoadAllQuery;
      loadResult = await this.graphql.query<{ persons: Person[]; personsCount: number }>({
        query,
        variables,
        error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || undefined
      });
    }

    const data = (!opts || opts.toEntity !== false) ?
      (loadResult && loadResult.persons || []).map(Person.fromObject) :
      (loadResult && loadResult.persons || []) as Person[];
    return {
      data,
      total: loadResult && loadResult.personsCount
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

  executeImport(opts?: {
    maxProgression?: number;
  }): Observable<number>{

    return defer(() => {
      const maxProgression = opts && opts.maxProgression || 100;
      const progression = new BehaviorSubject<number>(0);
      const filter = {
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY],
        userProfiles: ['SUPERVISOR', 'USER', 'GUEST']
      };

      const now = Date.now();
      console.info("[person-service] Importing persons...");

      fetchAllPagesWithProgress((offset, size) =>
          this.loadAll(offset, size, 'id', null, filter, {
            debug: false,
            fetchPolicy: "network-only",
            withTotal: (offset === 0), // Compute total only once
            toEntity: false
          }),
        progression,
        maxProgression
      )
        // Save result locally
        .then(res => this.entities.saveAll(res.data, {entityName: 'PersonVO', reset: true}))
        .then(res => {
          console.info(`[person-service] Successfully import persons in ${Date.now() - now}ms`);
          progression.complete();
        })
        .catch((err: any) => {
          console.error("[person-service] Error during importation: " + (err && err.message || err), err);
          progression.error(err);
        });
      return progression;
    });
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

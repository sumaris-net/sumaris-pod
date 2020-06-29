import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {BehaviorSubject, defer, Observable} from 'rxjs';
import {DataService, LoadResult, SuggestionDataService, TableDataService} from "../../shared/shared.module";
import {BaseDataService} from "../../core/services/base.data-service.class";
import {ErrorCodes} from "./errors";
import {map} from "rxjs/operators";
import {GraphqlService} from "../../core/services/graphql.service";
import {EntityUtils} from "../../core/services/model/entity.model";
import {FetchPolicy, WatchQueryFetchPolicy} from "apollo-client";
import {fetchAllPagesWithProgress} from "../../shared/services/data-service.class";
import {NetworkService} from "../../core/services/network.service";
import {EntityStorage} from "../../core/services/entities-storage.service";
import {environment} from "../../../environments/environment";
import {Beans, KeysEnum} from "../../shared/functions";
import {Person} from "../../core/services/model/person.model";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {StatusIds} from "../../core/services/model/model.enum";

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
export class PersonService extends BaseDataService<Person, PersonFilter>
  implements TableDataService<Person, PersonFilter>, DataService<Person, PersonFilter>,
    SuggestionDataService<Person, PersonFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected entities: EntityStorage
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
    sortDirection?: string,
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

    this._lastVariables.loadAll = variables;
    if (this._debug) console.debug("[person-service] Watching persons, using filter: ", variables);

    const query = (!opts || opts.withTotal !== false) ? LoadAllWithTotalQuery : LoadAllQuery;

    return this.graphql.watchQuery<{ persons: Person[]; personsCount: number }>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || 'cache-and-network'
    })
      .pipe(
        map((res) => {
          return {
            data: res && (res.persons || []).map(Person.fromObject),
            total: res && (res.personsCount || (res.persons && res.persons.length)) || 0
          };
        })
      );
  }

  async loadAll(offset: number, size: number, sortBy?: string, sortDirection?: string, filter?: PersonFilter, opts?: {
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
      this._lastVariables.loadAll = variables;

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
        if (this._lastVariables.loadAll) {
          this.graphql.removeToQueryCacheByIds(proxy, {
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'persons', ids);
        }
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
        .then(res => this.entities.saveAll(res.data, {entityName: 'PersonVO'}))
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

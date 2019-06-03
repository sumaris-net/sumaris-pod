import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from 'rxjs';
import {Person} from './model';
import {DataService, LoadResult, TableDataService} from "../../shared/shared.module";
import {BaseDataService} from "../../core/services/base.data-service.class";
import {ErrorCodes} from "./errors";
import {map} from "rxjs/operators";
import {GraphqlService} from "../../core/services/graphql.service";
import {EntityUtils, StatusIds} from "../../core/services/model";
//import {environment} from "../../../environments/environment";

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

export declare class PersonFilter {
  email?: string;
  pubkey?: string;
  searchText?: string;
  statusIds?: number[];
  userProfileId?: number;
  //profileLabels?: string[];
}

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

@Injectable()
export class PersonService extends BaseDataService implements TableDataService<Person, PersonFilter>, DataService<Person, PersonFilter> {

  constructor(
    protected graphql: GraphqlService
  ) {
    super(graphql);

    // for DEV only -----
    //this._debug = !environment.production;
  }

  /**
   * Load/search some persons
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  public watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: PersonFilter
  ): Observable<LoadResult<Person>> {

    const variables = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'lastName',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    this._lastVariables.loadAll = variables;

    console.debug("[person-service] Watching persons, using filter: ", variables);
    return this.graphql.watchQuery<{ persons: Person[]; personsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"},
      fetchPolicy: 'network-only'
    })
      .pipe(
        map(({persons, personsCount}) => {
          return {
            data: (persons || []).map(Person.fromObject),
            total: personsCount || (persons && persons.length) || 0
          };
        })
      );
  }

  async loadAll(offset: number, size: number, sortBy?: string, sortDirection?: string, filter?: PersonFilter, options?: any): Promise<LoadResult<Person>> {
    const variables = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'lastName',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    this._lastVariables.loadAll = variables;

    console.debug("[person-service] Loading persons, using filter: ", variables);
    const res = await this.graphql.query<{ persons: Person[]; personsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"},
      fetchPolicy: 'network-only'
    });

    const data = res && res.persons;
    return {
      data: (data || []).map(Person.fromObject),
      total: res && res.personsCount || (data && data.length) || 0
    };
  }

  async suggest(value: any, options?: {
    statusIds?: number[],
    userProfileId?: number;
  }): Promise<Person[]> {
    if (EntityUtils.isNotEmpty(value)) return [value];
    value = (typeof value === "string" && value !== '*') && value || undefined;
    const res = await this.loadAll(0, !value ? 30 : 10, undefined, undefined,
      {
        searchText: value as string,
        statusIds: options && options.statusIds || [StatusIds.ENABLE],
        userProfileId: options && options.userProfileId
      });
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
        const savedPerson = res.savePersons.find(res => entity.equals(res));
        EntityUtils.copyIdAndUpdateDate(savedPerson, entity);
      });

    if (this._debug) console.debug(`[person-service] Persons saved in ${Date.now() - now}ms`, data);

    return data;
  }

  async deleteAll(entities: Person[]): Promise<any> {

    let ids = entities && entities
      .map(t => t.id)
      .filter(id => (id > 0));

    const now = new Date();
    if (this._debug) console.debug("[person-service] Deleting persons... ids:", ids);

    const res = await this.graphql.mutate<any>({
      mutation: DeletePersons,
      variables: {
        ids: ids
      },
      error: {code: ErrorCodes.DELETE_PERSONS_ERROR, message: "REFERENTIAL.ERROR.DELETE_PERSONS_ERROR"}
    });

    // Update the cache
    if (this._lastVariables.loadAll) {
      this.removeToQueryCacheByIds({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'persons', ids);
    }

    if (this._debug) console.debug("[person-service] Trips deleted in " + (new Date().getTime() - now.getTime()) + "ms");

    return res;
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

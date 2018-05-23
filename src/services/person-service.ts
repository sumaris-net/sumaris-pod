import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Apollo } from "apollo-angular";
import { ApolloQueryResult } from 'apollo-client';
import { Observable } from 'rxjs/Observable';
import { Person } from './model';
import { DataService, BaseDataService } from "./data-service";
import { ErrorCodes } from "./errors";
import {map} from "rxjs/operators";

// Load persons query
const PersonsQuery = gql`
  query Persons($email: String, $pubkey: String, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    persons(filter: {email: $email, pubkey: $pubkey}, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      id
      firstName
      lastName
      email
      pubkey
      avatar
      statusId
    }
  }
`;
export declare type PersonsQueryResult = {
  persons: Person[];
}
export declare class PersonFilter {
  email?: string;
  pubkey?: string;
};
export declare class PersonsVariables extends PersonFilter{
  offset: number;
  size: number;
  sortBy?: string;
  sortDirection?: string;
};


@Injectable()
export class PersonService extends BaseDataService implements DataService<Person, PersonFilter> {

  constructor(
    protected apollo: Apollo
  ) {
    super(apollo);
  }

  /**
   * Load/search some persons
   * @param offset 
   * @param size 
   * @param sortBy 
   * @param sortDirection 
   * @param filter 
   */
  public loadAll( 
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: PersonFilter
  ): Observable<Person[]> {

    const variables: PersonsVariables = {
      email: filter && filter.email || null,
      pubkey: filter && filter.pubkey || null,
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'lastName',
      sortDirection: sortDirection || 'asc'
    };
    console.debug("[person-service] Loading persons, using filter: ", variables);
    return this.watchQuery<{persons: Person[]}>({
      query: PersonsQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_PERSONS_ERROR, message: "ERROR.LOAD_PERSONS_ERROR"}
    })
    .pipe(
      map(data => (data && data.persons || []).map(t => {
        const res = new Person();
        res.fromObject(t);
        return res;
      }))
    );
  }

  /**
   * Saving many persons
   * @param data 
   */
  saveAll(data: Person[]): Promise<Person[]> {
    console.info("[person-service] Saving persons: ", data);
    console.warn('Not impklemented yet !');
    return Promise.resolve(data);
  }

  deleteAll(data: Person[]):Promise<any>{
    console.info("[person-service] Deleting persons: ", data);
    console.warn('Not impklemented yet !');
    return Promise.resolve();
  }
}

import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Apollo } from "apollo-angular";
import { Observable, Subject } from "rxjs";
import { Person, Operation } from "./model";
import { DataService, BaseDataService } from "../../core/services/data-service.class";
import { map } from "rxjs/operators";
import { Moment } from "moment";
import { DocumentNode } from "graphql";
import { ErrorCodes } from "./errors";
import { AccountService } from "../../core/services/account.service";

export declare class OperationFilter {
  tripId?: number;
}
const LoadAllQuery: DocumentNode = gql`
  query Operations($filter: OperationFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    operations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      id
      startDateTime
      endDateTime
      fishingStartDateTime
      fishingEndDateTime
      rankOrderOnPeriod
      comments
      hasCatch
      updateDate
      recorderDepartment {
        id
        label
        name
      }
      positions {
        id
        dateTime
        latitude
        longitude
        updateDate
      }
    }
  }
`;

const SaveOperations: DocumentNode = gql`
  mutation SaveOperations($operations:[OperationVOInput]){
    saveOperations(operations: $operations){
      id
      startDateTime
      rankOrderOnPeriod
      updateDate
      positions {
        id
        dateTime
        updateDate
      }
    }
  }
`;
const DeleteOperations: DocumentNode = gql`
  mutation deleteOperations($ids:[Int]){
    deleteOperations(ids: $ids)
  }
`;

@Injectable()
export class OperationService extends BaseDataService implements DataService<Operation, OperationFilter>{

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);
  }

  /**
   * Load many operations
   * @param offset 
   * @param size 
   * @param sortBy 
   * @param sortDirection 
   * @param filter 
   */
  loadAll(offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: OperationFilter): Observable<Operation[]> {
    const variables: any = {
      offset: offset || 0,
      size: size || 1000,
      sortBy: sortBy || 'startDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    console.debug("[operation-service] Loading operations... using options:", variables);
    return this.watchQuery<{ operations: Operation[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_OPERATIONS_ERROR, message: "TRIP.ERROR.LOAD_OPERATIONS_ERROR" }
    })
      .pipe(
        map((data) => {
          console.debug("[operation-service] Loaded {" + (data && data.operations && data.operations.length || 0) + "} operations");
          return (data && data.operations || []).map(t => {
            const res = new Operation();
            res.fromObject(t);
            return res;
          });
        }));
  }

  /**
   * Save many operations
   * @param data 
   */
  async saveAll(entities: Operation[]): Promise<Operation[]> {
    if (!entities) return entities;

    const json = entities.map(t => t.asObject());

    console.debug("[operation-service] Saving operations: ", json);

    const res = await this.mutate<{ saveOperations: Operation[] }>({
      mutation: SaveOperations,
      variables: {
        operations: json
      },
      error: { code: ErrorCodes.SAVE_TRIPS_ERROR, message: "TRIP.ERROR.SAVE_TRIPS_ERROR" }
    });
    return (res && res.saveOperations && entities || [])
      .map(t => {
        const data = res.saveOperations.find(res => res.id == t.id); // TODO: get it from start+rank ?
        t.updateDate = data && data.updateDate || t.updateDate;
        return t;
      });
  }


  /**
   * Save many operations
   * @param entities 
   */
  deleteAll(entities: Operation[]): Promise<any> {

    let ids = entities && entities
      .map(t => t.id)
      .filter(id => (id > 0));

    console.debug("[operation-service] Deleting operations... ids:", ids);

    return this.mutate<any>({
      mutation: DeleteOperations,
      variables: {
        ids: ids
      }
    });
  }

  /* -- protected methods -- */

}

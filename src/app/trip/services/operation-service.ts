import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Apollo } from "apollo-angular";
import { Observable, Subject } from "rxjs-compat";
import { Person, Operation, Referential, DataEntity, VesselPosition } from "./model";
import { DataService, BaseDataService } from "../../core/services/data-service.class";
import { map } from "rxjs/operators";
import { Moment } from "moment";

import { ErrorCodes } from "./errors";
import { AccountService } from "../../core/services/account.service";

export declare class OperationFilter {
  tripId?: number;
}
const LoadAllQuery: any = gql`
  query Operations($filter: OperationFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    operations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      id
      startDateTime
      endDateTime
      fishingStartDateTime
      fishingEndDateTime
      rankOrderOnPeriod
      physicalGearId
      tripId
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
        recorderDepartment {
          id
          label
          name
        }
      }
    }
  }
`;

const SaveOperations: any = gql`
  mutation saveOperations($operations:[OperationVOInput]){
    saveOperations(operations: $operations){
      id
      startDateTime
      endDateTime
      fishingStartDateTime
      fishingEndDateTime
      rankOrderOnPeriod
      physicalGearId
      tripId
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
        recorderDepartment {
          id
          label
          name
        }
      }
    }
  }
`;
const DeleteOperations: any = gql`
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
      error: { code: ErrorCodes.LOAD_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATIONS_ERROR" }
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
  async saveAll(entities: Operation[], options?: any): Promise<Operation[]> {
    if (!entities) return entities;


    if (!options || !options.tripId) {
      console.error("[operation-service] Missing options.tripId");
      throw { code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATIONS_ERROR" };
    }

    const json = entities.map(t => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(t, options);
      return t.asObject();
    });

    console.debug("[operation-service] Saving operations: ", json);

    const res = await this.mutate<{ saveOperations: Operation[] }>({
      mutation: SaveOperations,
      variables: {
        operations: json
      },
      error: { code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATIONS_ERROR" }
    });

    return (res && res.saveOperations && entities || [])
      .map(t => {
        const data = res.saveOperations.find(res => t.equals(res));

        // Update (id+updateDate)
        t.updateDate = data && data.updateDate || t.updateDate;
        t.dirty = !data;

        // Update positions (id+updateDate)
        if (data.positions && data.positions.length > 0) {
          t.positions.forEach(p => {
            let savedPos = data.positions.find(res => p.equals(res));
            p.id = savedPos && savedPos.id;
            p.updateDate = savedPos && savedPos.updateDate;
          });
        }

        console.log("TODO: update vesselPosition updateDate ?");
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

  protected fillDefaultProperties(entity: Operation, options?: any) {

    // Recorder department
    this.fillRecorderPartment(entity);

    // Fill positions
    this.fillRecorderPartment(entity.startPosition)
    entity.startPosition.dateTime = entity.fishingStartDateTime || entity.startDateTime;
    this.fillRecorderPartment(entity.endPosition)
    entity.endPosition.dateTime = entity.fishingEndDateTime || entity.endDateTime;
    entity.positions = [entity.startPosition, entity.endPosition];

    // Fill trip ID
    if (!entity.tripId) {
      entity.tripId = options.tripId;
    }

    // Fill physical gear
    if (!entity.physicalGearId) {
      console.log("TODO: FAKE physical gear ID");
      entity.physicalGearId = 1;
    }
  }

  fillRecorderPartment(entity: DataEntity<Operation | VesselPosition>) {
    console.log("fillRecorderPartment: ", entity);
    if (!entity.recorderDepartment || !entity.recorderDepartment.id) {

      const person: Person = this.accountService.account;

      // Recorder department
      if (person && person.department) {
        entity.recorderDepartment.id = person.department.id;
      }
    }
  }
}

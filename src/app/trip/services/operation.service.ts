import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {EMPTY, Observable} from "rxjs";
import {
  Batch,
  DataEntity,
  Department,
  EntityUtils,
  isNil, isNotNil,
  Measurement,
  Operation,
  Person,
  Sample, Trip,
  VesselPosition
} from "./trip.model";
import {map, throttleTime} from "rxjs/operators";
import {LoadResult, TableDataService, toBoolean} from "../../shared/shared.module";
import {BaseDataService, environment} from "../../core/core.module";
import {ErrorCodes} from "./trip.errors";
import {DataFragments, Fragments} from "./trip.queries";
import {FetchPolicy, WatchQueryFetchPolicy} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";
import {isNilOrBlank} from "../../shared/functions";
import {AcquisitionLevelCodes, ReferentialFragments} from "../../referential/referential.module";
import {dataIdFromObject} from "../../core/graphql/graphql.utils";
import {NetworkService} from "../../core/services/network.service";
import {AccountService} from "../../core/services/account.service";
import {
  DataEntityAsObjectOptions,
  MINIFY_OPTIONS,
  OPTIMISTIC_AS_OBJECT_OPTIONS,
  SAVE_AS_OBJECT_OPTIONS
} from "./model/base.model";
import {EntityStorage} from "../../core/services/entities-storage.service";

export const OperationFragments = {
  lightOperation: gql`fragment LightOperationFragment on OperationVO {
    id
    startDateTime
    endDateTime
    fishingStartDateTime
    fishingEndDateTime
    rankOrderOnPeriod
    tripId
    comments
    hasCatch
    updateDate
    physicalGearId
    physicalGear {
        gear {
            ...ReferentialFragment
        }
    }
    metier {
      ...MetierFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    positions {
      ...PositionFragment
    }
  }
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.metier}
  ${ReferentialFragments.referential}
  ${Fragments.position}
  `,
  operation: gql`fragment OperationFragment on OperationVO {
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
    metier {
      ...MetierFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    positions {
      ...PositionFragment
    }
    measurements {
      ...MeasurementFragment
    }
    gearMeasurements {
      ...MeasurementFragment
    }
    samples {
      ...SampleFragment
    }
    batches {
      ...BatchFragment
    }
  }
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.metier}
  ${Fragments.position}
  ${Fragments.measurement}
  ${DataFragments.sample}
  ${DataFragments.batch}
  `
};


export declare class OperationFilter {
  tripId?: number;
}

const LoadAllQuery: any = gql`
  query Operations($filter: OperationFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    operations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightOperationFragment
    }
  }
  ${OperationFragments.lightOperation}
`;
const LoadQuery: any = gql`
  query Operation($id: Int) {
    operation(id: $id) {
      ...OperationFragment
    }
  }
  ${OperationFragments.operation}
`;
const SaveOperations: any = gql`
  mutation saveOperations($operations:[OperationVOInput]){
    saveOperations(operations: $operations){
      ...OperationFragment
    }
  }
  ${OperationFragments.operation}
`;
const DeleteOperations: any = gql`
  mutation deleteOperations($ids:[Int]){
    deleteOperations(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription updateOperation($id: Int, $interval: Int){
    updateOperation(id: $id, interval: $interval) {
      ...OperationFragment
    }
  }
  ${OperationFragments.operation}
`;

const sortByStartDateFn = (n1: Operation, n2: Operation) => {
  return n1.startDateTime.isSame(n2.startDateTime) ? 0 : (n1.startDateTime.isAfter(n2.startDateTime) ? 1 : -1);
};

const sortByEndDateOrStartDateFn = (n1: Operation, n2: Operation) => {
  const d1 = n1.endDateTime || n1.startDateTime;
  const d2 = n2.endDateTime || n2.startDateTime;
  return d1.isSame(d2) ? 0 : (d1.isAfter(d2) ? 1 : -1);
};

@Injectable({providedIn: 'root'})
export class OperationService extends BaseDataService implements TableDataService<Operation, OperationFilter> {

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected entities: EntityStorage
  ) {
    super(graphql);

    // -- For DEV only
    this._debug = !environment.production;
  }

  /**
   * Load many operations
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: string,
           filter?: OperationFilter,
           options?: {
            fetchPolicy?: WatchQueryFetchPolicy
           }
  ): Observable<LoadResult<Operation>> {

    if (!filter || isNil(filter.tripId)) {
      console.warn("[operation-service] Trying to load operations without 'filter.tripId'. Skipping.");
      return EMPTY;
    }

    const variables: any = {
      offset: offset || 0,
      size: size || 1000,
      sortBy: (sortBy != 'id' && sortBy) || 'endDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    this._lastVariables.loadAll = variables;

    if (this._debug) console.debug("[operation-service] Loading operations... using options:", variables);
    return this.graphql.watchQuery<{ operations?: Operation[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATIONS_ERROR"},
      fetchPolicy: options && options.fetchPolicy || undefined
    })
      .pipe(
        throttleTime(200), // avoid multiple call
        map((res) => {
          const data = (res && res.operations || []).map(Operation.fromObject);
          if (this._debug) console.debug(`[operation-service] Loaded ${data.length} operations`);

          // Compute rankOrderOnPeriod, by tripId
          if (filter && filter.tripId) {
            let rankOrderOnPeriod = 1;
            // apply a sorted copy (do NOT change original order), then compute rankOrder
            data.slice().sort(sortByEndDateOrStartDateFn)
              .forEach(o => o.rankOrderOnPeriod = rankOrderOnPeriod++);

            // sort by rankOrderOnPeriod (aka id)
            if (!sortBy || sortBy === 'id') {
              const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
              data.sort((a, b) => {
                const valueA = a.rankOrderOnPeriod;
                const valueB = b.rankOrderOnPeriod;
                return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
              });
            }
          }

          return {
            data: data,
            total: data.length
          };
        }));
  }

  async load(id: number, options?: { fetchPolicy: FetchPolicy }): Promise<Operation | null> {
    if (isNil(id)) throw new Error("Missing argument 'id' ");

    const now = Date.now();
    if (this._debug) console.debug(`[operation-service] Loading operation #${id}...`);

    const res = await this.graphql.query<{ operation: Operation }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.LOAD_OPERATION_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATION_ERROR"},
      fetchPolicy: options && options.fetchPolicy || undefined
    });

    const data = res && res.operation && Operation.fromObject(res.operation);

    if (data && this._debug) console.debug(`[operation-service] Operation #${id} loaded in ${Date.now() - now}ms`, data);
    return data;
  }

  public listenChanges(id: number): Observable<Operation> {
    if (isNil(id)) throw new Error("Missing argument 'id' ");

    if (this._debug) console.debug(`[operation-service] [WS] Listening changes for trip {${id}}...`);

    return this.graphql.subscribe<{ updateOperation: Operation }, { id: number, interval: number }>({
      query: UpdateSubscription,
      variables: {
        id: id,
        interval: 10
      },
      error: {
        code: ErrorCodes.SUBSCRIBE_OPERATION_ERROR,
        message: 'TRIP.OPERATION.ERROR.SUBSCRIBE_OPERATION_ERROR'
      }
    })
      .pipe(
        map(data => {
          if (data && data.updateOperation) {
            const res = Operation.fromObject(data.updateOperation);
            if (this._debug) console.debug(`[operation-service] Operation {${id}} updated on server !`, res);
            return res;
          }
          return null; // deleted ?
        })
      );
  }

  /**
   * Save many operations
   * @param data
   */
  async saveAll(entities: Operation[], options?: any): Promise<Operation[]> {
    if (!entities) return entities;

    if (!options || !options.tripId) {
      console.error("[operation-service] Missing options.tripId");
      throw {code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATIONS_ERROR"};
    }

    // Compute rankOrderOnPeriod
    let rankOrderOnPeriod = 1;
    entities.sort(sortByEndDateOrStartDateFn).forEach(o => o.rankOrderOnPeriod = rankOrderOnPeriod++);

    const json = entities.map(o => {
      // Fill default properties
      this.fillDefaultProperties(o, options);
      return this.asObject(o);
    });

    const now = new Date();
    if (this._debug) console.debug("[operation-service] Saving operations...", json);

    const res = await this.graphql.mutate<{ saveOperations: Operation[] }>({
      mutation: SaveOperations,
      variables: {
        operations: json
      },
      error: {code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATIONS_ERROR"}
    });

    const saveOperations = (res && res.saveOperations);
    if (saveOperations && saveOperations.length) {
      // Copy id and update date
      entities.forEach(entity => {
          const savedOperation = saveOperations.find(json => entity.equals(json));
          this.copyIdAndUpdateDate(savedOperation, entity);
        });
    }

    if (this._debug) console.debug("[operation-service] Operations saved and updated in " + (new Date().getTime() - now.getTime()) + "ms", entities);

    return entities;
  }

  /**
   * Save an operation
   * @param data
   */
  async save(entity: Operation): Promise<Operation> {

    const now = Date.now();
    if (this._debug) console.debug("[operation-service] Saving operation...");

    // Fill default properties (as recorder department and person)
    this.fillDefaultProperties(entity, {});

    // If new, create a temporary if (for offline mode)
    const isNew = isNil(entity.id);

    const offlineResponse = async (context) => {
      if (isNew) {
        entity.id = await this.entities.nextValue('Operation');
        if (this._debug) console.debug(`[operation-service] New  Using local entity id {${entity.id}`);
      }

      // For the query to be tracked (see tracked query link) with a unique serialization key
      context.tracked = true;
      if (isNotNil(entity.id)) context.serializationKey = dataIdFromObject(entity);

      return { saveOperations: [this.asObject(entity, OPTIMISTIC_AS_OBJECT_OPTIONS)] };
    };

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[operation-service] Using minify object, to send:", json);

    return new Promise<Operation>((resolve, reject) => {
      this.graphql.mutate<{ saveOperations: Operation[] }>({
        mutation: SaveOperations,
        variables: {
          operations: [json]
        },
        offlineResponse,
        error: {reject, code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATION_ERROR"},
        update: (proxy, {data}) => {
          const savedEntity = data && data.saveOperations && data.saveOperations[0];

          // Local entity: save it
          if (savedEntity.id < 0) {
            if (this._debug) console.debug('[operation-service] [offline] Saving operation locally...', savedEntity);

            // Save response locally
            this.entities.save(savedEntity);
          }

          // Update the entity and update GraphQL cache
          else {

            // Remove existing entity from the local storage
            if (entity.id < 0) {
              this.entities.delete(entity);
            }

            // Copy id and update Date
            this.copyIdAndUpdateDate(savedEntity, entity);

            // Copy gear
            if (savedEntity.metier) {
              savedEntity.metier.gear = savedEntity.metier.gear || (entity.physicalGear && entity.physicalGear.gear && entity.physicalGear.gear.asObject());
            }

            if (this._debug) console.debug(`[operation-service] Operation saved in ${Date.now() - now}ms`, entity);

            // Update the cache
            if (isNew && this._lastVariables.loadAll) {
              this.graphql.addToQueryCache(proxy, {
                query: LoadAllQuery,
                variables: this._lastVariables.loadAll
              }, 'operations', savedEntity);
            }
            else if (this._lastVariables.load) {
              this.graphql.updateToQueryCache(proxy,{
                query: LoadQuery,
                variables: this._lastVariables.load
              }, 'operation', savedEntity);
            }
          }

          resolve(entity);
        }
      });
    });

  }

  /**
   * Save many operations
   * @param entities
   */
  async deleteAll(entities: Operation[]): Promise<any> {

    let ids = entities && entities
      .map(t => t.id)
      .filter(id => (id > 0));

    const now = Date.now();
    if (this._debug) console.debug("[operation-service] Deleting operations... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeleteOperations,
      variables: {
        ids: ids
      },
      update: (proxy) => {
        // Remove from cache
        if (this._lastVariables.loadAll) {
          this.graphql.removeToQueryCacheByIds(proxy, {
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'operations', ids);
        }

        if (this._debug) console.debug(`[operation-service] Operations deleted in ${Date.now() - now}ms`);
      }
    });
  }

  /* -- protected methods -- */

  protected asObject(entity: Operation, options?: DataEntityAsObjectOptions): any {
    return entity.asObject({ ...MINIFY_OPTIONS, options } as DataEntityAsObjectOptions);
  }

  protected fillDefaultProperties(entity: Operation, options?: any) {

    const department = this.accountService.department;

    // Fill Recorder department
    this.fillRecorderDepartment(entity, department);
    this.fillRecorderDepartment(entity.startPosition, department);
    this.fillRecorderDepartment(entity.endPosition, department);

    // Measurements
    (entity.measurements || []).forEach(m => this.fillRecorderDepartment(m, department));

    // Fill position date s
    entity.startPosition.dateTime = entity.fishingStartDateTime || entity.startDateTime;
    entity.endPosition.dateTime = entity.fishingEndDateTime || entity.endDateTime;

    // Fill trip ID
    if (!entity.tripId && options) {
      entity.tripId = options.tripId;
    }

    // Fill catch batch label
    if (entity.catchBatch && isNilOrBlank(entity.catchBatch.label)) {
      entity.catchBatch.label = AcquisitionLevelCodes.CATCH_BATCH;
    }
  }

  fillRecorderDepartment(entity: DataEntity<Operation | VesselPosition | Measurement>, department?: Department) {
    if (!entity.recorderDepartment || !entity.recorderDepartment.id) {

      department = department || this.accountService.department;

      // Recorder department
      if (department) {
        entity.recorderDepartment = department;
      }
    }
  }

  copyIdAndUpdateDate(source: Operation | undefined | any, target: Operation) {
    if (!source) return;

    // Update (id and updateDate)
    EntityUtils.copyIdAndUpdateDate(source, target);

    // Update positions (id and updateDate)
    if (source.positions && source.positions.length > 0) {
      [target.startPosition, target.endPosition].forEach(targetPos => {
        const savedPos = source.positions.find(srcPos => targetPos.equals(srcPos));
        EntityUtils.copyIdAndUpdateDate(savedPos, targetPos);
      });
    }

    // Update measurements
    if (target.measurements && source.measurements) {
      target.measurements.forEach(targetMeas => {
        const sourceMeas = source.measurements.find(json => targetMeas.equals(json));
        EntityUtils.copyIdAndUpdateDate(sourceMeas, targetMeas);
      });
    }

    // Update samples (recursively)
    if (target.samples && source.samples) {
      this.copyIdAndUpdateDateOnSamples(source.samples, target.samples);
    }

    // Update batches (recursively)
    if (target.catchBatch && source.batches) {
      this.copyIdAndUpdateDateOnBatch(source.batches, [target.catchBatch]);
    }
  }

  /**
   * Copy Id and update, in sample tree (recursively)
   * @param sources
   * @param targets
   */
  copyIdAndUpdateDateOnSamples(sources: (Sample | any)[], targets: Sample[]) {
    // Update samples
    if (sources && targets) {
      targets.forEach(target => {
        const source = sources.find(json => target.equals(json));
        EntityUtils.copyIdAndUpdateDate(source, target);

        // Apply to children
        if (target.children && target.children.length) {
          this.copyIdAndUpdateDateOnSamples(sources, target.children);
        }
      });
    }
  }

  /**
   * Copy Id and update, in batch tree (recursively)
   * @param sources
   * @param targets
   */
  copyIdAndUpdateDateOnBatch(sources: (Batch | any)[], targets: Batch[]) {
    if (sources && targets) {
      targets.forEach(target => {
        const index = sources.findIndex(json => target.equals(json));
        if (index !== -1) {
          EntityUtils.copyIdAndUpdateDate(sources[index], target);
          sources.splice(index, 1); // remove from sources list, as it has been found
        }
        else {
          console.error("Batch NOT found ! ", target);
        }

        // Loop on children
        if (target.children && target.children.length) {
          this.copyIdAndUpdateDateOnBatch(sources, target.children);
        }
      });
    }
  }
}

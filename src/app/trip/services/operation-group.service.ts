import {Injectable} from "@angular/core";
import {
  EditorDataService,
  EditorDataServiceLoadOptions, isNil, isNilOrBlank, isNotNil,
  LoadResult,
  TableDataService
} from "../../shared/shared.module";
import {BaseDataService, Department, EntityUtils, environment} from "../../core/core.module";
import {Operation, OperationGroup} from "./model/trip.model";
import {OperationFilter} from "./operation.service";
import {EMPTY, Observable} from "rxjs";
import {GraphqlService} from "../../core/services/graphql.service";
import {NetworkService} from "../../core/services/network.service";
import {AccountService} from "../../core/services/account.service";
import {EntityStorage} from "../../core/services/entities-storage.service";
import gql from "graphql-tag";
import {ReferentialFragments} from "../../referential/services/referential.queries";
import {DataFragments, Fragments} from "./trip.queries";
import {TripFragments} from "./trip.service";
import {ErrorCodes} from "./trip.errors";
import {map, throttleTime} from "rxjs/operators";
import {
  AcquisitionLevelCodes, DataEntity,
  DataEntityAsObjectOptions,
  MINIFY_OPTIONS, OPTIMISTIC_AS_OBJECT_OPTIONS,
  SAVE_AS_OBJECT_OPTIONS
} from "./model/base.model";
import {Measurement} from "./model/measurement.model";
import {Batch} from "./model/batch.model";
import {dataIdFromObject} from "../../core/graphql/graphql.utils";

export const OperationGroupFragment = {
  operationGroup: gql`fragment OperationGroupFragment on OperationGroupVO {
    id
    rankOrderOnPeriod
    physicalGearId
    tripId
    comments
    hasCatch
    updateDate
    metier {
      ...MetierFragment
    }
    physicalGear {
      ...PhysicalGearFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
    measurementValues
    gearMeasurementValues
    batches {
      ...BatchFragment
    }
  }
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.metier}
  ${DataFragments.batch}
  ${TripFragments.physicalGear}
  `
};

const LoadAllQuery: any = gql`
  query OperationGroups($filter: OperationFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    operationGroups(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...OperationGroupFragment
    }
  }
  ${OperationGroupFragment.operationGroup}
`;
const LoadQuery: any = gql`
  query OperationGroup($id: Int!) {
    operationGroup(id: $id) {
      ...OperationGroupFragment
    }
  }
  ${OperationGroupFragment.operationGroup}
`;
const SaveOperationGroups: any = gql`
  mutation saveOperationGroups($operationGroups:[OperationGroupVOInput]){
    saveOperationGroups(operationGroups: $operationGroups){
      ...OperationGroupFragment
    }
  }
  ${OperationGroupFragment.operationGroup}
`;
const DeleteOperationGroups: any = gql`
  mutation deleteOperationGroups($ids:[Int]){
    deleteOperationGroups(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription updateOperationGroup($id: Int!, $interval: Int){
    updateOperationGroup(id: $id, interval: $interval) {
      ...OperationGroupFragment
    }
  }
  ${OperationGroupFragment.operationGroup}
`;


@Injectable({providedIn: 'root'})
export class OperationGroupService extends BaseDataService
  implements TableDataService<OperationGroup, OperationFilter>,
    EditorDataService<OperationGroup, OperationFilter> {

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
   * Load many operation groups
   *
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   * @param options
   */
  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: string, filter?: OperationFilter, options?: any): Observable<LoadResult<OperationGroup>> {
    if (!filter || isNil(filter.tripId)) {
      console.warn("[operation-service] Trying to load operations without 'filter.tripId'. Skipping.");
      return EMPTY;
    }

    const variables: any = {
      offset: offset || 0,
      size: size || 1000,
      sortBy: (sortBy !== 'id' && sortBy) || 'endDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    this._lastVariables.loadAll = variables;

    if (this._debug) console.debug("[operation-group-service] Loading operation groups... using options:", variables);
    return this.graphql.watchQuery<{ operationGroups?: OperationGroup[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATIONS_ERROR"},
      fetchPolicy: options && options.fetchPolicy || undefined
    })
      .pipe(
        throttleTime(200), // avoid multiple call
        map((res) => {
          const data = (res && res.operationGroups || []).map(OperationGroup.fromObject);
          if (this._debug) console.debug(`[operation-group-service] Loaded ${data.length} operation groups`);

          // Compute rankOrderOnPeriod, by tripId
          // if (filter && filter.tripId) {
          //   let rankOrderOnPeriod = 1;
          //   // apply a sorted copy (do NOT change original order), then compute rankOrder
          //   data.slice().sort(sortByEndDateOrStartDateFn)
          //     .forEach(o => o.rankOrderOnPeriod = rankOrderOnPeriod++);
          //
          //   // sort by rankOrderOnPeriod (aka id)
          //   if (!sortBy || sortBy === 'id') {
          //     const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
          //     data.sort((a, b) => {
          //       const valueA = a.rankOrderOnPeriod;
          //       const valueB = b.rankOrderOnPeriod;
          //       return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
          //     });
          //   }
          // }

          return {
            data: data,
            total: data.length
          };
        }));
  }

  /**
   * Load an operation group
   *
   * @param id
   * @param options
   */
  async load(id: number, options?: EditorDataServiceLoadOptions): Promise<OperationGroup> {
    if (isNil(id)) throw new Error("Missing argument 'id' ");

    const now = Date.now();
    if (this._debug) console.debug(`[operation-group-service] Loading operation group #${id}...`);

    const res = await this.graphql.query<{ operationGroup: OperationGroup }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.LOAD_OPERATION_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATION_ERROR"},
      fetchPolicy: options && options.fetchPolicy || undefined
    });

    const data = res && res.operationGroup && OperationGroup.fromObject(res.operationGroup);

    if (data && this._debug) console.debug(`[operation-group-service] Operation group #${id} loaded in ${Date.now() - now}ms`, data);
    return data;
  }

  listenChanges(id: number, options?: any): Observable<OperationGroup | undefined> {
    if (isNil(id)) throw new Error("Missing argument 'id' ");

    if (this._debug) console.debug(`[operation-group-service] [WS] Listening changes for operation group {${id}}...`);

    return this.graphql.subscribe<{ updateOperationGroup: OperationGroup }, { id: number, interval: number }>({
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
          if (data && data.updateOperationGroup) {
            const res = OperationGroup.fromObject(data.updateOperationGroup);
            if (this._debug) console.debug(`[operation-group-service] Operation group {${id}} updated on server !`, res);
            return res;
          }
          return null; // deleted ?
        })
      );
  }

  /**
   * Save an operation group
   *
   * @param entity
   * @param options
   */
  save(entity: OperationGroup, options?: any): Promise<OperationGroup> {
    const now = Date.now();
    if (this._debug) console.debug("[operation-group-service] Saving operation...");

    // Fill default properties (as recorder department and person)
    this.fillDefaultProperties(entity, {});

    // If new, create a temporary if (for offline mode)
    const isNew = isNil(entity.id);

    const offlineResponse = async (context) => {
      if (isNew) {
        entity.id = await this.entities.nextValue('Operation'); // Keep Operation as reference entity
        if (this._debug) console.debug(`[operation-group-service] New  Using local entity id {${entity.id}`);
      }

      // For the query to be tracked (see tracked query link) with a unique serialization key
      context.tracked = true;
      if (isNotNil(entity.id)) context.serializationKey = dataIdFromObject(entity);

      return { saveOperationGroups: [this.asObject(entity, OPTIMISTIC_AS_OBJECT_OPTIONS)] };
    };

    // Transform into json
    const json = this.asObject(entity, {minify: true, keepEntityName: true});
    if (this._debug) console.debug("[operation-group-service] Using minify object, to send:", json);

    return new Promise<OperationGroup>((resolve, reject) => {
      this.graphql.mutate<{ saveOperationGroups: OperationGroup[] }>({
        mutation: SaveOperationGroups,
        variables: {
          operations: [json]
        },
        offlineResponse,
        error: {reject, code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATION_ERROR"},
        update: (proxy, {data}) => {
          const savedEntity = data && data.saveOperationGroups && data.saveOperationGroups[0];

          // Local entity: save it
          if (savedEntity.id < 0) {
            if (this._debug) console.debug('[operation-group-service] [offline] Saving operation group locally...', savedEntity);

            // Save response locally
            this.entities.save(savedEntity);
          }

          // Update the entity and update GraphQL cache
          else {

            // Remove existing entity from the local storage
            if (entity.id < 0 && savedEntity.updateDate) {
              this.entities.delete(entity);
            }

            // Copy id and update Date
            this.copyIdAndUpdateDate(savedEntity, entity);

            // Copy gear
            if (savedEntity.metier) {
              savedEntity.metier.gear = savedEntity.metier.gear || (entity.physicalGear && entity.physicalGear.gear && entity.physicalGear.gear.asObject());
            }

            if (this._debug) console.debug(`[operation-group-service] Operation group saved in ${Date.now() - now}ms`, entity);

            // Update the cache
            if (isNew && this._lastVariables.loadAll) {
              this.graphql.addToQueryCache(proxy, {
                query: LoadAllQuery,
                variables: this._lastVariables.loadAll
              }, 'operationGroups', savedEntity);
            }
            else if (this._lastVariables.load) {
              this.graphql.updateToQueryCache(proxy, {
                query: LoadQuery,
                variables: this._lastVariables.load
              }, 'operationGroup', savedEntity);
            }
          }

          resolve(entity);
        }
      });
    });
  }

  /**
   * Save many operation groups
   *
   * @param entities
   * @param options
   */
  async saveAll(entities: OperationGroup[], options?: any): Promise<OperationGroup[]> {
    if (!entities) return entities;

    if (!options || !options.tripId) {
      console.error("[operation-group-service] Missing options.tripId");
      throw {code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATIONS_ERROR"};
    }

    // Compute rankOrderOnPeriod
    // let rankOrderOnPeriod = 1;
    // entities.sort(sortByEndDateOrStartDateFn).forEach(o => o.rankOrderOnPeriod = rankOrderOnPeriod++);

    const json = entities.map(o => {
      // Fill default properties
      this.fillDefaultProperties(o, options);
      return this.asObject(o, SAVE_AS_OBJECT_OPTIONS);
    });

    const now = new Date();
    if (this._debug) console.debug("[operation-group-service] Saving operation groups...", json);

    const res = await this.graphql.mutate<{ saveOperationGroups: OperationGroup[] }>({
      mutation: SaveOperationGroups,
      variables: {
        operations: json
      },
      error: {code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATIONS_ERROR"}
    });

    const saveOperationGroups = (res && res.saveOperationGroups);
    if (saveOperationGroups && saveOperationGroups.length) {
      // Copy id and update date
      entities.forEach(entity => {
        const savedOperationGroup = saveOperationGroups.find(o => entity.equals(o));
        this.copyIdAndUpdateDate(savedOperationGroup, entity);
      });
    }

    if (this._debug) console.debug("[operation-group-service] Operation groups saved and updated in " + (new Date().getTime() - now.getTime()) + "ms", entities);

    return entities;
  }

  async delete(entity: OperationGroup, options?: any): Promise<any> {
    await this.deleteAll([entity]);
  }

  async deleteAll(entities: OperationGroup[], options?: any): Promise<any> {
    const ids = entities && entities
      .map(t => t.id)
      .filter(id => (id > 0));

    const now = Date.now();
    if (this._debug) console.debug("[operation-group-service] Deleting operation groups... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeleteOperationGroups,
      variables: {
        ids: ids
      },
      update: (proxy) => {
        // Remove from cache
        if (this._lastVariables.loadAll) {
          this.graphql.removeToQueryCacheByIds(proxy, {
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'operationGroups', ids);
        }

        if (this._debug) console.debug(`[operation-group-service] Operation groups deleted in ${Date.now() - now}ms`);
      }
    });
  }

  /* -- protected methods -- */

  protected asObject(entity: OperationGroup, options?: DataEntityAsObjectOptions): any {
    const copy: any = entity.asObject({ ...MINIFY_OPTIONS, ...options } as DataEntityAsObjectOptions);

    if (options && options.minify) {
      // Clean metier object, before saving
      copy.metier = {id: entity.metier && entity.metier.id};
    }
    return copy;
  }

  protected fillDefaultProperties(entity: OperationGroup, options?: any) {

    const department = this.accountService.department;

    // Fill Recorder department
    this.fillRecorderDepartment(entity, department);

    // TODO ? Measurements
    // (entity.measurements || []).forEach(m => this.fillRecorderDepartment(m, department));

    // Fill trip ID
    if (!entity.tripId && options) {
      entity.tripId = options.tripId;
    }

    // Fill catch batch label
    if (entity.catchBatch && isNilOrBlank(entity.catchBatch.label)) {
      entity.catchBatch.label = AcquisitionLevelCodes.CATCH_BATCH;
    }
  }

  fillRecorderDepartment(entity: DataEntity<OperationGroup | Measurement>, department?: Department) {
    if (!entity.recorderDepartment || !entity.recorderDepartment.id) {

      department = department || this.accountService.department;

      // Recorder department
      if (department) {
        entity.recorderDepartment = department;
      }
    }
  }

  copyIdAndUpdateDate(source: OperationGroup | undefined | any, target: OperationGroup) {
    if (!source) return;

    // Update (id and updateDate)
    EntityUtils.copyIdAndUpdateDate(source, target);

    // TODO Update measurements
    // if (target.measurements && source.measurements) {
    //   target.measurements.forEach(targetMeas => {
    //     const sourceMeas = source.measurements.find(json => targetMeas.equals(json));
    //     EntityUtils.copyIdAndUpdateDate(sourceMeas, targetMeas);
    //   });
    // }

    // TODO ? Update samples (recursively)
    // if (target.samples && source.samples) {
    //   this.copyIdAndUpdateDateOnSamples(source.samples, target.samples);
    // }

    // Update batches (recursively)
    if (target.catchBatch && source.batches) {
      this.copyIdAndUpdateDateOnBatch(source.batches, [target.catchBatch]);
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


import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {EMPTY, Observable} from "rxjs";
import {
  Batch,
  DataEntity,
  Department,
  EntityUtils,
  isNil,
  isNotNil,
  Measurement,
  Operation, QualityFlagIds,
  Sample,
  VesselPosition
} from "./trip.model";
import {filter, map, throttleTime} from "rxjs/operators";
import {
  EditorDataService,
  EditorDataServiceLoadOptions,
  isNotEmptyArray,
  LoadResult,
  TableDataService
} from "../../shared/shared.module";
import {BaseDataService, environment} from "../../core/core.module";
import {ErrorCodes} from "./trip.errors";
import {DataFragments, Fragments} from "./trip.queries";
import {WatchQueryFetchPolicy} from "apollo-client";
import {GraphqlService} from "../../core/services/graphql.service";
import {isEmptyArray, isNilOrBlank, toNumber} from "../../shared/functions";
import {AcquisitionLevelCodes, ReferentialFragments} from "../../referential/referential.module";
import {dataIdFromObject} from "../../core/graphql/graphql.utils";
import {NetworkService} from "../../core/services/network.service";
import {AccountService} from "../../core/services/account.service";
import {
  DataEntityAsObjectOptions,
  MINIFY_OPTIONS,
  SAVE_AS_OBJECT_OPTIONS,
  SAVE_LOCALLY_AS_OBJECT_OPTIONS,
  SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS
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


export class OperationFilter {

  static searchFilter<T extends Operation>(f: OperationFilter): (T) => boolean {
    return (o: T) => {
      // Trip
      if (isNotNil(f.tripId) &&
        ((isNotNil(o.tripId) && f.tripId !== o.tripId) ||
        (isNil(o.tripId) && o.trip && f.tripId !== o.trip.id))) {
        return false;
      }

      return true;
    };
  }

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
  query Operation($id: Int!) {
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

export declare interface OperationSaveOptions {
  tripId?: number;
  computeBatchRankOrder?: boolean;
  computeBatchIndividualCount?: boolean;
}

@Injectable({providedIn: 'root'})
export class OperationService extends BaseDataService
  implements TableDataService<Operation, OperationFilter>,
             EditorDataService<Operation, OperationFilter>{

  loading = false;

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
   * @param dataFilter
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: string,
           dataFilter?: OperationFilter,
           options?: {
            fetchPolicy?: WatchQueryFetchPolicy
           }
  ): Observable<LoadResult<Operation>> {

    if (!dataFilter || isNil(dataFilter.tripId)) {
      console.warn("[operation-service] Trying to load operations without 'filter.tripId'. Skipping.");
      return EMPTY;
    }

    const variables: any = {
      offset: offset || 0,
      size: size || 1000,
      sortBy: (sortBy != 'id' && sortBy) || 'endDateTime',
      sortDirection: sortDirection || 'asc',
      filter: dataFilter
    };

    let $loadResult: Observable<{operations?: Operation[]}>;
    let now = this._debug && Date.now();

    const offlineData = this.network.offline || (dataFilter && dataFilter.tripId < 0) || false;
    if (offlineData) {
      if (this._debug) console.debug("[operation-service] Loading operations locally... using options:", variables);
      $loadResult = this.entities.watchAll<Operation>('OperationVO', {
        ...variables,
        filter: OperationFilter.searchFilter<Operation>(dataFilter)
      })
        .pipe(
          map(res => {
            return res && {operations: res.data};
          }));
    }
    else {
      this._lastVariables.loadAll = variables;

      if (this._debug) console.debug("[operation-service] Loading operations... using options:", variables);
      $loadResult = this.graphql.watchQuery({
        query: LoadAllQuery,
        variables: variables,
        error: {code: ErrorCodes.LOAD_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATIONS_ERROR"},
        fetchPolicy: options && options.fetchPolicy || undefined
      })
        .pipe(
          throttleTime(200), // avoid multiple call
          filter(() => !this.loading)
        );
    }

    return $loadResult
      .pipe(
        map((res) => {
          const data = (res && res.operations || []).map(Operation.fromObject);
          if (now) {
            console.debug(`[operation-service] Loaded ${data.length} operations in ${Date.now() - now}ms`);
            now = undefined;
          }

          // Compute rankOrderOnPeriod, by tripId
          if (dataFilter && dataFilter.tripId) {
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

  async load(id: number, options?: EditorDataServiceLoadOptions): Promise<Operation | null> {
    if (isNil(id)) throw new Error("Missing argument 'id' ");

    const now = this._debug && Date.now();
    if (this._debug) console.debug(`[operation-service] Loading operation #${id}...`);
    this.loading = true;

    try {
      let json: any;

      // Load locally
      if (id < 0) {
        json = await this.entities.load<Operation>(id, Operation.TYPENAME);
      }

      // Load from pod
      else {
        const res = await this.graphql.query<{ operation: Operation }>({
          query: LoadQuery,
          variables: {
            id: id
          },
          error: {code: ErrorCodes.LOAD_OPERATION_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATION_ERROR"},
          fetchPolicy: options && options.fetchPolicy || undefined
        });
        json = res && res.operation;
      }

      // Transform to entity
      const data = Operation.fromObject(json);
      if (data && this._debug) console.debug(`[operation-service] Operation #${id} loaded in ${Date.now() - now}ms`, data);
      return data;
    }
    finally {
      this.loading = false;
    }

  }

  async delete(data: Operation, options?: any): Promise<any> {
    await this.deleteAll([data]);
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
  async saveAll(entities: Operation[], options?: OperationSaveOptions): Promise<Operation[]> {
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
      return this.asObject(o, SAVE_AS_OBJECT_OPTIONS);
    });

    const now = this._debug && Date.now();
    if (this._debug) console.debug("[operation-service] Saving operations...", json);

    await this.graphql.mutate<{ saveOperations: Operation[] }>({
      mutation: SaveOperations,
      variables: {
        operations: json
      },
      error: {code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATIONS_ERROR"},
      update: (proxy, {data}) => {
        const saveOperations = data && data.saveOperations;
        if (saveOperations && saveOperations.length) {
          // Copy id and update date
          entities.forEach(entity => {
            const savedOperation = saveOperations.find(json => entity.equals(json));
            this.copyIdAndUpdateDate(savedOperation, entity);
          });
        }

        if (this._debug) console.debug(`[operation-service] Operations saved and updated in ${Date.now() - now}ms`, entities);
      }
    });

    return entities;
  }

  /**
   * Save an operation
   * @param data
   */
  async save(entity: Operation, options?: OperationSaveOptions): Promise<Operation> {

    const now = Date.now();
    if (this._debug) console.debug("[operation-service] Saving operation...");

    // Fill default properties (as recorder department and person)
    this.fillDefaultProperties(entity, options);

    // If new, create a temporary if (for offline mode)
    const isNew = isNil(entity.id);

    // If parent is a local entity: force a local save
    if (entity.tripId < 0) {
      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      const json = entity.asObject({...SAVE_LOCALLY_AS_OBJECT_OPTIONS, batchAsTree: false});
      if (this._debug) console.debug('[operation-service] [offline] Saving operation locally...', json);

      // Save response locally
      await this.entities.save(json);

      return entity;
    }

    const offlineResponse = async (context) => {
      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      // For the query to be tracked (see tracked query link) with a unique serialization key
      context.tracked = (entity.tripId >= 0);
      if (isNotNil(entity.id)) context.serializationKey = dataIdFromObject(entity);

      return { saveOperations: [this.asObject(entity, SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS)] };
    };

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[operation-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ saveOperations: Operation[] }>({
        mutation: SaveOperations,
        variables: {
          operations: [json]
        },
        offlineResponse,
        error: {code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATION_ERROR"},
        update: (proxy, {data}) => {
          const savedEntity = data && data.saveOperations && data.saveOperations[0];

          // Local entity: save it
          if (savedEntity.id < 0) {
            if (this._debug) console.debug('[operation-service] [offline] Saving operation locally...', savedEntity);

            // Save response locally
            this.entities.save(savedEntity.asObject());
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
        }
      });

    return entity;
  }

  /**
   * Save many operations
   * @param entities
   */
  async deleteAll(entities: Operation[]): Promise<any> {
    // Get local entity ids, then delete id
    const localIds = entities && entities
      .map(t => t.id)
      .filter(id => id < 0);
    if (isNotEmptyArray(localIds)) {
      if (this._debug) console.debug("[trip-service] Deleting trips locally... ids:", localIds);
      await this.entities.deleteMany<Operation>(localIds, 'OperationVO');
    }

    const remoteIds = entities && entities
      .map(t => t.id)
      .filter(id => id >= 0);
    if (isEmptyArray(remoteIds)) return; // stop, if nothing else to do

    const now = Date.now();
    if (this._debug) console.debug("[operation-service] Deleting operations... ids:", remoteIds);

    await this.graphql.mutate<any>({
      mutation: DeleteOperations,
      variables: {
        ids: remoteIds
      },
      update: (proxy) => {
        // Remove from cache
        if (this._lastVariables.loadAll) {
          this.graphql.removeToQueryCacheByIds(proxy, {
            query: LoadAllQuery,
            variables: this._lastVariables.loadAll
          }, 'operations', remoteIds);
        }

        if (this._debug) console.debug(`[operation-service] Operations deleted in ${Date.now() - now}ms`);
      }
    });
  }

  /**
   * Save many operations
   * @param entities
   */
  async deleteLocallyByTripId(tripId: number): Promise<any> {
    if (tripId >= 0) throw new Error('Invalid tripId: must be a local trip (id<0)!');

    try {
      const res = await this.entities.loadAll<Operation>('OperationVO', {
        filter: OperationFilter.searchFilter<Operation>({tripId})
      });

      const ids = (res && res.data || []).map(o => o.id);
      await this.entities.deleteMany(ids, Operation.TYPENAME);
    }
    catch (err) {
      console.error(`[operation-service] Failed to delete operation from trip {${tripId}}`, err);
      throw err;
    }
  }

  /* -- protected methods -- */

  protected asObject(entity: Operation, opts?: DataEntityAsObjectOptions & { batchAsTree?: boolean; }): any {
    opts = { ...MINIFY_OPTIONS, ...opts };
    const copy: any = entity.asObject(opts);

    // Full json optimisation
    if (opts.minify && !opts.keepTypename && !opts.keepEntityName) {
      // Clean metier object, before saving
      copy.metier = {id: entity.metier && entity.metier.id};
    }
    return copy;
  }

  protected fillDefaultProperties(entity: Operation, options?: Partial<OperationSaveOptions>) {

    const department = this.accountService.department;

    // Fill Recorder department
    this.fillRecorderDepartment(entity, department);
    this.fillRecorderDepartment(entity.startPosition, department);
    this.fillRecorderDepartment(entity.endPosition, department);

    // Measurements
    (entity.measurements || []).forEach(m => this.fillRecorderDepartment(m, department));

    // Fill position dates
    entity.startPosition.dateTime = entity.fishingStartDateTime || entity.startDateTime;
    entity.endPosition.dateTime = entity.fishingEndDateTime || entity.endDateTime || entity.startPosition.dateTime;

    // Fill trip ID
    if (isNil(entity.tripId) && options) {
      entity.tripId = options.tripId;
    }

    // Fill catch batch label
    if (entity.catchBatch) {
      // Fill catch batch label
      if (isNilOrBlank(entity.catchBatch.label)) {
        entity.catchBatch.label = AcquisitionLevelCodes.CATCH_BATCH;
      }

      // Fill sum and rank order
      this.fillBatchTreeDefaults(entity.catchBatch, options);
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

  protected async fillOfflineDefaultProperties(entity: Operation) {
    const isNew = isNil(entity.id);

    // If new, generate a local id
    if (isNew) {
      entity.id =  await this.entities.nextValue(entity);
    }

    // Fill all sample id
    const samples = entity.samples && EntityUtils.listOfTreeToArray(entity.samples) || [];
    await EntityUtils.fillLocalIds(samples, (_, count) => this.entities.nextValues('SampleVO', count));
    entity.samples = samples;

    // Fill all batches id
    const batches = entity.catchBatch && EntityUtils.treeToArray(entity.catchBatch) || [];
    await EntityUtils.fillLocalIds(batches, (_, count) => this.entities.nextValues('BatchVO', count));
    //if (this._debug) BatchUtils.logTree(entity.catchBatch);
  }

  protected fillBatchTreeDefaults(source: Batch,
                                  options?: Partial<OperationSaveOptions>) {

    if (!source.label || !source.children ) return; // skip
    if (!options || (options.computeBatchRankOrder !== true && options.computeBatchIndividualCount !== true)) return; // skip (nothing to do)

    let sumChildrenIndividualCount: number = null;

    // Sort by id and rankOrder (new batch at the end)
    if (options.computeBatchRankOrder === true && source.children) {
      source.children = source.children
        .sort((b1, b2) => ((b1.id || 0) * 10000 + (b1.rankOrder || 0)) - ((b2.id || 0) * 10000 + (b2.rankOrder || 0)));
    }

    source.children.forEach((b, index) => {
      b.rankOrder = options.computeBatchRankOrder ? index + 1 : b.rankOrder;

      // Sampling batch
      if (b.label.endsWith(Batch.SAMPLING_BATCH_SUFFIX)) {
        b.label = source.label + Batch.SAMPLING_BATCH_SUFFIX;
      }
      // Individual measure batch
      else if (b.label.startsWith(AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL)) {
        b.label = `${AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL}#${b.rankOrder}`;
      }

      this.fillBatchTreeDefaults(b, options); // Recursive call

      // Update sum of individual count
      if (options.computeBatchIndividualCount && b.label.startsWith(AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL)) {
        sumChildrenIndividualCount = toNumber(sumChildrenIndividualCount, 0) + toNumber(b.individualCount, 1);
      }
    });

    if (options.computeBatchIndividualCount) {

      // Parent batch is a sampling batch: update individual count
      if (source.label.endsWith(Batch.SAMPLING_BATCH_SUFFIX)) {
        source.individualCount = sumChildrenIndividualCount || null;
      }

      // Parent is NOT a sampling batch
      else if (isNotNil(sumChildrenIndividualCount) && source.label.startsWith(AcquisitionLevelCodes.SORTING_BATCH)){
        if (isNotNil(source.individualCount) && source.individualCount < sumChildrenIndividualCount) {
          console.warn(`[operation-service] Fix batch {${source.label}} individual count  ${source.individualCount} => ${sumChildrenIndividualCount}`);
          //source.individualCount = childrenIndividualCount;
          source.qualityFlagId = QualityFlagIds.BAD;
        }
        else if (isNil(source.individualCount) || source.individualCount > sumChildrenIndividualCount) {
          // Create a sampling batch, to hold the sampling individual count
          const samplingBatch = new Batch();
          samplingBatch.label = source.label + Batch.SAMPLING_BATCH_SUFFIX;
          samplingBatch.rankOrder = 1;
          samplingBatch.individualCount = sumChildrenIndividualCount;
          samplingBatch.children = source.children;
          source.children = [samplingBatch];
        }
      }
    }

  }

  protected copyIdAndUpdateDate(source: Operation | undefined | any, target: Operation) {
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
  protected copyIdAndUpdateDateOnSamples(sources: (Sample | any)[], targets: Sample[]) {
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
  protected copyIdAndUpdateDateOnBatch(sources: (Batch | any)[], targets: Batch[]) {
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

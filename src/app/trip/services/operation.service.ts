import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {EMPTY, Observable} from "rxjs";
import {filter, first, map} from "rxjs/operators";
import {
  EntitiesService,
  EntityService,
  EntityServiceLoadOptions,
  isNil,
  isNotEmptyArray,
  isNotNil,
  LoadResult
} from "../../shared/shared.module";
import {BaseEntityService, Department, EntityUtils, environment} from "../../core/core.module";
import {ErrorCodes} from "./trip.errors";
import {DataFragments, Fragments} from "./trip.queries";
import {GraphqlService} from "../../core/graphql/graphql.service";
import {isEmptyArray, isNilOrBlank} from "../../shared/functions";
import {dataIdFromObject} from "../../core/graphql/graphql.utils";
import {NetworkService} from "../../core/services/network.service";
import {AccountService} from "../../core/services/account.service";
import {
  DataEntity,
  DataEntityAsObjectOptions,
  SAVE_AS_OBJECT_OPTIONS,
  SAVE_LOCALLY_AS_OBJECT_OPTIONS,
  SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS
} from "../../data/services/model/data-entity.model";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {Operation, OperationFromObjectOptions, VesselPosition} from "./model/trip.model";
import {Measurement} from "./model/measurement.model";
import {Batch, BatchUtils} from "./model/batch.model";
import {Sample} from "./model/sample.model";
import {ReferentialFragments} from "../../referential/services/referential.fragments";
import {MINIFY_OPTIONS} from "../../core/services/model/referential.model";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {EntitiesServiceWatchOptions, FilterFn} from "../../shared/services/entity-service.class";
import {QueryVariables} from "../../core/services/base.data-service.class";
import {SortDirection} from "@angular/material/sort";
import {concatPromises, firstNotNilPromise} from "../../shared/observables";
import {FetchPolicy} from "@apollo/client/core";

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
      id
      rankOrder
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
    physicalGear {
      id
      rankOrder
      gear {
        ...ReferentialFragment
      }
    }
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
    fishingAreas {
      ...FishingAreaFragment
    }
  }
  ${ReferentialFragments.lightDepartment}
  ${ReferentialFragments.metier}
  ${ReferentialFragments.referential}
  ${Fragments.position}
  ${Fragments.measurement}
  ${DataFragments.sample}
  ${DataFragments.batch}
  ${DataFragments.fishingArea}
  `
};


export class OperationFilter {

  static searchFilter<T extends Operation>(f: OperationFilter): (T) => boolean {
    if (!f) return undefined;

    const filterFns: FilterFn<T>[] = [];

    // Exclude id
    if (isNotNil(f.excludeId)) {
      const excludeId = +(f.excludeId);
      filterFns.push(o => o.id !== excludeId);
    }

    // Trip
    if (isNotNil(f.tripId)) {
      const tripId = +(f.tripId);
      filterFns.push((o => (isNotNil(o.tripId) && o.tripId === tripId)
        || (o.trip && o.trip.id === tripId)));
    }


    if (!filterFns.length) return undefined;

    return (entity) => !filterFns.find(fn => !fn(entity));
  }

  tripId?: number;
  excludeId?: number;
}

const LoadAllLightQuery: any = gql`
  query Operations($filter: OperationFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    operations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightOperationFragment
    }
  }
  ${OperationFragments.lightOperation}
`;
const LoadAllFullQuery: any = gql`
  query Operations($filter: OperationFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    operations(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...OperationFragment
    }
  }
  ${OperationFragments.operation}
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
  subscription updateOperation($id: Int!, $interval: Int){
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

export declare interface OperationServiceWatchOptions extends
  OperationFromObjectOptions, EntitiesServiceWatchOptions {

  computeRankOrder?: boolean;
  fullLoad?: boolean;
  fetchPolicy?: FetchPolicy;
}

@Injectable({providedIn: 'root'})
export class OperationService extends BaseEntityService<Operation, OperationFilter>
  implements EntitiesService<Operation, OperationFilter, OperationServiceWatchOptions>,
             EntityService<Operation>{

  static LIGHT_EXCLUDED_ATTRIBUTES = ["measurements", "samples", "batches"];

  loading = false;

  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected entities: EntitiesStorage
  ) {
    super(graphql);

    this._mutableWatchQueriesMaxCount = 2;

    // -- For DEV only
    this._debug = !environment.production;
  }

  async loadAllByTrip(filter?: OperationFilter & { tripId: number; }, opts?: OperationServiceWatchOptions): Promise<LoadResult<Operation>> {
    return firstNotNilPromise(this.watchAllByTrip(filter, opts));
  }

  watchAllByTrip(filter?: OperationFilter & { tripId: number; }, opts?: OperationServiceWatchOptions): Observable<LoadResult<Operation>> {
      return this.watchAll(0, -1, null, null, filter, opts);
  }

  async loadAll(offset: number,
                size: number,
                sortBy?: string,
                sortDirection?: SortDirection,
                dataFilter?: OperationFilter,
                opts?: OperationServiceWatchOptions): Promise<Operation[]> {
     return firstNotNilPromise(
       this.watchAll(offset, size, sortBy, sortDirection, dataFilter, opts)
         .pipe(
           map(res => res.data)
         ));
  }

  /**
   * Load many operations
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param dataFilter
   * @param opts
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           dataFilter?: OperationFilter,
           opts?: OperationServiceWatchOptions
  ): Observable<LoadResult<Operation>> {

    // Load offline
    const offlineData = this.network.offline || (dataFilter && dataFilter.tripId < 0) || false;
    if (offlineData) {
      return this.watchAllLocally(offset, size, sortBy, sortDirection, dataFilter, opts);
    }

    if (!dataFilter || isNil(dataFilter.tripId)) {
      console.warn("[operation-service] Trying to load operations without 'filter.tripId'. Skipping.");
      return EMPTY;
    }

    const variables: QueryVariables<OperationFilter> = {
      offset: offset || 0,
      size: size >= 0 ? size : 1000,
      sortBy: (sortBy != 'id' && sortBy) || 'endDateTime',
      sortDirection: sortDirection || 'asc',
      trash: opts && opts.trash || false,
      filter: dataFilter
    };

    let now = this._debug && Date.now();
    if (this._debug) console.debug("[operation-service] Loading operations... using options:", variables);

    const query = (!opts || opts.fullLoad !== true) ? LoadAllLightQuery : LoadAllFullQuery;
    return this.mutableWatchQuery<{operations: any[], operationsCount: number}>({
      queryName: 'LoadAll',
      query: query,
      arrayFieldName: 'operations',
      totalFieldName: 'operationsCount',
      insertFilterFn: OperationFilter.searchFilter<Operation>(dataFilter),
      variables: variables,
      error: {code: ErrorCodes.LOAD_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATIONS_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || undefined
    })
    .pipe(
      // Skip update during load()
      filter(() => !this.loading),

      map((res) => {
        const data = (res && res.operations || []).map(source => Operation.fromObject(source, opts));
        if (now) {
          console.debug(`[operation-service] Loaded ${data.length} operations in ${Date.now() - now}ms`);
          now = undefined;
        }

        // Compute rankOrder and re-sort (if enable AND all data fetched)
        if (offset === 0 && size === -1 && (!opts || opts.computeRankOrder !== false)) {
          this.computeRankOrderAndSort(data, sortBy, sortDirection, dataFilter);
        }

        return {
          data,
          total: data.length
        };
      }));
  }

  async load(id: number, options?: EntityServiceLoadOptions): Promise<Operation | null> {
    if (isNil(id)) throw new Error("Missing argument 'id' ");

    const now = this._debug && Date.now();
    if (this._debug) console.debug(`[operation-service] Loading operation #${id}...`);
    this.loading = true;

    try {
      let json: any;

      // Load locally
      if (id < 0) {
        json = await this.entities.load<Operation>(id, Operation.TYPENAME);
        if (!json) throw {code: ErrorCodes.LOAD_OPERATION_ERROR, message: "TRIP.OPERATION.ERROR.LOAD_OPERATION_ERROR"};
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

    if (this._debug) console.debug(`[operation-service] [WS] Listening changes for operation {${id}}...`);

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
   * @param entities
   * @param opts
   */
  async saveAll(entities: Operation[], opts?: OperationSaveOptions): Promise<Operation[]> {
    if (isEmptyArray(entities)) return entities;

    if (this._debug) console.debug(`[operation-service] Saving ${entities.length} operations...`);
    const jobsFactories = (entities || []).map(entity => () => this.save(entity, {...opts}));
    return concatPromises<Operation>(jobsFactories);
  }

  /**
   * Save an operation
   * @param data
   */
  async save(entity: Operation, opts?: OperationSaveOptions): Promise<Operation> {

    // If parent is a local entity: force a local save
    // Save response locally
    if (entity.tripId < 0) {
      return await this.saveLocally(entity, opts);
    }

    const now = Date.now();
    if (this._debug) console.debug("[operation-service] Saving operation...");

    // Fill default properties (as recorder department and person)
    this.fillDefaultProperties(entity, opts);

    // If new, create a temporary if (for offline mode)
    const isNew = isNil(entity.id);

    // Transform into json
    const json = this.asObject(entity, SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[operation-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ saveOperations: Operation[] }>({
        mutation: SaveOperations,
        variables: {
          operations: [json]
        },
        error: {code: ErrorCodes.SAVE_OPERATIONS_ERROR, message: "TRIP.OPERATION.ERROR.SAVE_OPERATION_ERROR"},
        offlineResponse: async (context) => {
          // Make sure to fill id, with local ids
          await this.fillOfflineDefaultProperties(entity);

          // For the query to be tracked (see tracked query link) with a unique serialization key
          context.tracked = (entity.tripId >= 0);
          if (isNotNil(entity.id)) context.serializationKey = dataIdFromObject(entity);

          return { saveOperations: [this.asObject(entity, SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS)] };
        },
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

            // Insert into cached queries
            if (isNew) {
              this.insertIntoMutableCachedQuery(proxy, {
                query: LoadAllLightQuery,
                data: savedEntity
              });
            }

            if (this._debug) console.debug(`[operation-service] Operation saved in ${Date.now() - now}ms`, entity);
          }
        }
      });

    return entity;
  }

  /**
   * Save many operations
   * @param entities
   * @param opts
   */
  async deleteAll(entities: Operation[], opts?: {
    trash?: boolean; // True by default
  }): Promise<any> {

    // Delete local entities
    const localEntities = (entities || []).filter(EntityUtils.isLocal);
    if (isNotEmptyArray(localEntities)) {
      const trash = !opts || opts.trash !== false;
      const localIds = localEntities.map(e => e.id);
      if (this._debug) console.debug(`[operation-service] Deleting local operations... {trash: ${trash}}`);
      if (trash) {
        await this.entities.moveManyToTrash<Operation>(localIds, {entityName: Operation.TYPENAME});
      }
      else {
        await this.entities.deleteMany<Operation>(localIds, {entityName: Operation.TYPENAME});
      }
    }

    // Get remote ids, then delete remotely
    const remoteEntities = (entities || []).filter(EntityUtils.isRemote)
    if (isNotEmptyArray(remoteEntities)) {

      const remoteIds = remoteEntities.map(e => e.id);
      const now = Date.now();
      if (this._debug) console.debug("[operation-service] Deleting operations... ids:", remoteIds);

      await this.graphql.mutate({
        mutation: DeleteOperations,
        variables: {
          ids: remoteIds
        },
        update: (proxy) => {
          // Remove from cached queries
          this.removeFromMutableCachedQueryByIds(proxy, {
            query: LoadAllLightQuery,
            ids: remoteIds
          });

          if (this._debug) console.debug(`[operation-service] Operations deleted in ${Date.now() - now}ms`);
        }
      });
    }
  }

  /**
   * Delete operation locally (from the entity storage)
   * @param tripId
   * @param opts
   */
  async deleteLocally(filter: OperationFilter & { tripId: number; }): Promise<Operation[]> {
    if (!filter || isNil(filter.tripId)) throw new Error("Missing arguments 'filter.tripId'");

    try {
      // Find operations to delete
      const res = await this.entities.loadAll<Operation>('OperationVO', {
        filter: OperationFilter.searchFilter<Operation>(filter)
      });
      const ids = (res && res.data || []).map(o => o.id);
      if (isEmptyArray(ids)) return undefined; // Skip

      // Apply deletion
      return await this.entities.deleteMany(ids, {entityName: Operation.TYPENAME});
    }
    catch (err) {
      console.error(`[operation-service] Failed to delete operation {tripId: ${filter.tripId}}`, err);
      throw err;
    }
  }

  /**
   * Load many local operations
   */
  watchAllLocally(offset: number,
                 size: number,
                 sortBy?: string,
                 sortDirection?: SortDirection,
                 dataFilter?: OperationFilter,
                 opts?: OperationServiceWatchOptions): Observable<LoadResult<Operation>> {

    if (!dataFilter || isNil(dataFilter.tripId)) {
      console.warn("[operation-service] Trying to load operations without 'filter.tripId'. Skipping.");
      return EMPTY;
    }
    if (dataFilter.tripId >= 0) throw new Error("Invalid 'filter.tripId': must be a local trip (id<0)!");

    const variables: any = {
      offset: offset || 0,
      size: size >= 0 ? size : 1000,
      sortBy: (sortBy !== 'id' && sortBy) || 'endDateTime',
      sortDirection: sortDirection || 'asc',
      trash: opts && opts.trash || false,
      filter: dataFilter
    };

    if (this._debug) console.debug("[operation-service] Loading operations locally... using options:", variables);
    return this.entities.watchAll<Operation>(Operation.TYPENAME, {
      ...variables,
      filter: OperationFilter.searchFilter<Operation>(dataFilter)
    }, {fullLoad: opts && opts.fullLoad})
      .pipe(map(res => {
        const data = (res && res.data || []).map(source => Operation.fromObject(source, opts));

        // Compute rankOrder and re-sort (if enable AND all data fetched)
        if (offset === 0 && size === -1 && (!opts || opts.computeRankOrder !== false)) {
          this.computeRankOrderAndSort(data, sortBy, sortDirection, dataFilter);
        }

        return {
          data,
          total: data.length
        };
      }))
  }

  /**
   * Compute rank order of the given operation. This function will load all operations, to compute the rank order.
   * Please use opts={fetchPolicy: 'cache-first'} when possible
   * @param source
   * @param opts
   */
  computeRankOrder(source: Operation, opts?: { fetchPolicy?: FetchPolicy; } ): Promise<number> {
    return this.watchRankOrder(source, opts)
      .pipe(first())
      .toPromise();
  }

  /**
   * Compute rank order of the operation
   * @param source
   * @param opts
   */
  watchRankOrder(source: Operation, opts?: OperationServiceWatchOptions): Observable<number> {
    console.debug(`[operation-service] Loading rankOrder of operation #${source.id}...`);
    const tripId = isNotNil(source.tripId) ? source.tripId : source.trip && source.trip.id;
    return this.watchAllByTrip({tripId}, opts)
      .pipe(
        map(res => {
          const existingOperation = (res && res.data ||[]).find(o => o.id === source.id);
          return existingOperation ? existingOperation.rankOrderOnPeriod : null;
        })
      );
  }

  /* -- protected methods -- */

  /**
   * Save an operation on the local storage
   * @param data
   */
  protected async saveLocally(entity: Operation, opts?: OperationSaveOptions): Promise<Operation> {
    if (entity.tripId >= 0) throw new Error('Must be a local entity');

    const now = Date.now();
    if (this._debug) console.debug("[operation-service] Saving operation locally...");

    // Fill default properties (as recorder department and person)
    this.fillDefaultProperties(entity, opts);

    // Make sure to fill id, with local ids
    await this.fillOfflineDefaultProperties(entity);

    const jsonLocal = this.asObject(entity, {...SAVE_LOCALLY_AS_OBJECT_OPTIONS, batchAsTree: false, sampleAsTree: false});
    if (this._debug) console.debug('[operation-service] [offline] Saving operation locally...', jsonLocal);

    // Save response locally
    await this.entities.save(jsonLocal);

    return entity;
  }

  protected asObject(entity: Operation, opts?: DataEntityAsObjectOptions & { batchAsTree?: boolean; sampleAsTree?: boolean; }): any {
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
    (entity.measurements || []).forEach(m => this.fillRecorderDepartment(m, department));

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

  protected fillRecorderDepartment(entity: DataEntity<Operation | VesselPosition | Measurement>, department?: Department) {
    if (!entity.recorderDepartment || !entity.recorderDepartment.id) {

      department = department || this.accountService.department;

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

    // Fill all batches id
    const batches = entity.catchBatch && EntityUtils.treeToArray(entity.catchBatch) || [];
    if (isNotEmptyArray(batches)) {
      await EntityUtils.fillLocalIds(batches, (_, count) => this.entities.nextValues('BatchVO', count));
      if (this._debug) {
        console.debug("[Operation-service] Preparing batches to be saved locally:");
        BatchUtils.logTree(entity.catchBatch);
      }
    }
  }

  protected fillBatchTreeDefaults(catchBatch: Batch, options?: Partial<OperationSaveOptions>) {

    if (!options) return;

    // Compute rankOrder (and label)
    if (options.computeBatchRankOrder) BatchUtils.computeRankOrder(catchBatch);

    // Compute individual count
    if (options.computeBatchIndividualCount) BatchUtils.computeIndividualCount(catchBatch);
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

  protected computeRankOrderAndSort(data: Operation[],
                                    sortBy: string,
                                    sortDirection: string,
                                    filter?: OperationFilter) {
    // Compute rankOrderOnPeriod, by tripId
    if (filter && isNotNil(filter.tripId)) {
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
  }
}

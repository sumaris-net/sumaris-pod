import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {BehaviorSubject, Observable, of, Subject} from "rxjs";
import {filter, map} from "rxjs/operators";
import {
  AcquisitionLevelCodes,
  EntityUtils,
  isNil,
  isNotNil,
  IWithProgramEntity,
  PmfmStrategy,
  Program,
  StatusIds
} from "./model";
import {
  BaseDataService,
  environment,
  IReferentialRef,
  LoadResult,
  ReferentialRef,
  TableDataService
} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {ReferentialFragments} from "../services/referential.queries";
import {GraphqlService} from "../../core/services/graphql.service";
import {
  EditorDataService,
  EditorDataServiceLoadOptions,
  fetchAllPagesWithProgress
} from "../../shared/services/data-service.class";
import {TaxonGroupIds, TaxonGroupRef, TaxonNameRef} from "./model/taxon.model";
import {isNilOrBlank, isNotEmptyArray, suggestFromArray} from "../../shared/functions";
import {CacheService} from "ionic-cache";
import {ReferentialRefService} from "./referential-ref.service";
import {firstNotNilPromise} from "../../shared/observables";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {FetchPolicy, WatchQueryFetchPolicy} from "apollo-client";
import {EntityStorage} from "../../core/services/entities-storage.service";

export declare class ProgramFilter {
  searchText?: string;
  statusIds?: number[];
  withProperty?: string;
}

const ProgramFragments = {
  lightProgram: gql`
    fragment LightProgramFragment on ProgramVO {
      id
      label
      name
      description
      comments
      updateDate
      creationDate
      statusId
      properties
    }
    `,
  programRef: gql`
    fragment ProgramRefFragment on ProgramVO {
      id
      label
      name
      description
      comments
      updateDate
      creationDate
      statusId
      properties
      strategies {
        ...StrategyRefFragment
      }
    }`,
  program: gql`
    fragment ProgramFragment on ProgramVO {
      id
      label
      name
      description
      comments
      updateDate
      creationDate
      statusId
      properties
      strategies {
        ...StrategyFragment
      }
    }
    `,
  strategyRef: gql`
    fragment StrategyRefFragment on StrategyVO {
      id
      label
      name
      description
      comments
      updateDate
      creationDate
      statusId
      gears {
        ...ReferentialFragment
      }
      taxonGroups {
        ...TaxonGroupStrategyFragment
      }
      taxonNames {
        ...TaxonNameStrategyFragment
      }
      pmfmStrategies {
        ...PmfmStrategyRefFragment
      }
    }
  `,
  strategy: gql`
    fragment StrategyFragment on StrategyVO {
      id
      label
      name
      description
      comments
      updateDate
      creationDate
      statusId
      programId
      gears {
        ...ReferentialFragment
      }
      taxonGroups {
        ...TaxonGroupStrategyFragment
      }
      taxonNames {
        ...TaxonNameStrategyFragment
      }
      pmfmStrategies {
        ...PmfmStrategyFragment
      }
    }
  `,
  pmfmStrategyRef: gql`
    fragment PmfmStrategyRefFragment on PmfmStrategyVO {
      id
      pmfmId
      methodId
      label
      name
      unit
      type
      minValue
      maxValue
      maximumNumberDecimals
      defaultValue
      acquisitionNumber
      isMandatory
      rankOrder
      acquisitionLevel
      updateDate
      gears
      taxonGroupIds
      referenceTaxonIds
      qualitativeValues {
        id
        label
        name
        statusId
        entityName
        __typename
      }
      __typename
  }`,
  pmfmStrategy: gql`
    fragment PmfmStrategyFragment on PmfmStrategyVO {
      acquisitionLevel
      rankOrder
      isMandatory
      acquisitionNumber
      defaultValue
      pmfmId
      pmfm {
        id
        label
        name
        minValue
        maxValue
        unit
        defaultValue
        maximumNumberDecimals
        __typename
      }
      gears
      taxonGroupIds
      referenceTaxonIds
      __typename
  }`,
  taxonGroupStrategy: gql`
    fragment TaxonGroupStrategyFragment on TaxonGroupStrategyVO {
      strategyId
      priorityLevel
      taxonGroup {
          id
          label
          name
          entityName
          taxonNames {
              ...TaxonNameFragment
          }
      }
      __typename
    }
  `,
  taxonNameStrategy: gql`
    fragment TaxonNameStrategyFragment on TaxonNameStrategyVO {
      strategyId
      priorityLevel
      taxonName {
          ...TaxonNameFragment
      }
      __typename
    }
  `
};
const LoadRefQuery: any = gql`
  query ProgramRef($id: Int, $label: String){
      program(id: $id, label: $label){
        ...ProgramRefFragment
      }
  }
  ${ProgramFragments.programRef}
  ${ProgramFragments.strategyRef}
  ${ProgramFragments.pmfmStrategyRef}
  ${ProgramFragments.taxonGroupStrategy}
  ${ProgramFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}
`;

const LoadQuery: any = gql`
  query Program($id: Int, $label: String){
      program(id: $id, label: $label){
        ...ProgramFragment
      }
  }
  ${ProgramFragments.program}
  ${ProgramFragments.strategy}
  ${ProgramFragments.pmfmStrategy}
  ${ProgramFragments.taxonGroupStrategy}
  ${ProgramFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}
`;

const LoadAllQuery: any = gql`
  query Programs($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ProgramFilterVOInput){
    programs(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightProgramFragment
    }
  }
  ${ProgramFragments.lightProgram}
`;

const LoadAllWithCountQuery: any = gql`
  query Programs($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ProgramFilterVOInput){
    programs(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightProgramFragment
    }
    referentialsCount(entityName: "Program")
  }
  ${ProgramFragments.lightProgram}
`;

const SaveQuery: any = gql`
  mutation SaveProgram($program:ProgramVOInput){
    saveProgram(program: $program){
      ...ProgramFragment
    }
  }
  ${ProgramFragments.program}
  ${ProgramFragments.strategy}
  ${ProgramFragments.pmfmStrategy}
  ${ProgramFragments.taxonGroupStrategy}
  ${ProgramFragments.taxonNameStrategy}
  ${ReferentialFragments.referential}
  ${ReferentialFragments.taxonName}
`;

const ProgramCacheKeys = {
  CACHE_GROUP: 'program',

  PROGRAM_BY_LABEL: 'programByLabel',
  PMFMS: 'programPmfms',
  GEARS: 'programGears',
  TAXON_GROUPS: 'programTaxonGroups',
  TAXON_GROUP_ENTITIES: 'programTaxonGroupEntities',
  TAXON_NAME_BY_GROUP: 'programTaxonNameByGroup',
  TAXON_NAMES: 'taxonNameByGroup'
};

const cacheBuster$ = new Subject<void>();

@Injectable({providedIn: 'root'})
export class ProgramService extends BaseDataService
  implements TableDataService<Program, ProgramFilter>,
    EditorDataService<Program, ProgramFilter> {


  constructor(
    protected graphql: GraphqlService,
    protected network: NetworkService,
    protected accountService: AccountService,
    protected referentialRefService: ReferentialRefService,
    protected cache: CacheService,
    protected entities: EntityStorage
  ) {
    super(graphql);

    // Clear cache
    network.onResetNetworkCache.subscribe(() => this.clearCache());

    // -- For DEV only
    this._debug = !environment.production;
  }

  /**
   * Load programs
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
           filter?: ProgramFilter,
           opts?: {
             fetchPolicy?: WatchQueryFetchPolicy;
             withTotal?: boolean;
           }): Observable<LoadResult<Program>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    const now = Date.now();
    if (this._debug) console.debug("[program-service] Watching programs using options:", variables);

    const query = (!opts || opts.withTotal !== false) ? LoadAllWithCountQuery : LoadAllQuery;
    return this.graphql.watchQuery<{ programs: any[], referentialsCount?: number }>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_PROGRAMS_ERROR, message: "PROGRAM.ERROR.LOAD_PROGRAMS_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || undefined
    })
      .pipe(
        map(res => {
            const data = (res && res.programs || []).map(Program.fromObject);
            if (this._debug) console.debug(`[program-service] Programs loaded in ${Date.now() - now}ms`, data);
            return {
              data: data,
              total: res.referentialsCount
            };
          }
        )
      );
  }

  /**
   * Load programs
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  async loadAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: string,
           filter?: ProgramFilter,
           opts?: {
             fetchPolicy: FetchPolicy;
             withTotal?: boolean;
             toEntity?: boolean;
             debug?: boolean;
           }): Promise<LoadResult<Program>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    const now = Date.now();
    const debug = this._debug && (!opts || opts.debug !== false);
    if (debug) console.debug("[program-service] Loading programs using options:", variables);

    // Offline mode
    const offline = this.network.offline && (!opts || opts.fetchPolicy !== 'network-only');
    if (offline) {
      const res = await this.entities.loadAll('ProgramVO',
        {
          ...variables,
          filter: EntityUtils.searchTextFilter('label', filter.searchText)
        }
      );
      const data = (!opts || opts.toEntity !== false) ?
        (res && res.data || []).map(Program.fromObject) :
        (res && res.data || []) as Program[];
      if (debug) console.debug(`[referential-ref-service] Programs loaded (from offline storage) in ${Date.now() - now}ms`);
      return {
        data: data,
        total: res.total
      };
    }

    // Online mode
    else {

      const query = opts && opts.withTotal ? LoadAllWithCountQuery : LoadAllQuery;
      const res = await this.graphql.query<{ programs: any[], referentialsCount?: number }>({
        query,
        variables,
        error: {code: ErrorCodes.LOAD_PROGRAMS_ERROR, message: "PROGRAM.ERROR.LOAD_PROGRAMS_ERROR"},
        fetchPolicy: opts && opts.fetchPolicy || undefined
      });

      const data = (!opts || opts.toEntity !== false) ?
        (res && res.programs || []).map(Program.fromObject) :
        (res && res.programs || []) as Program[];
      if (debug) console.debug(`[program-service] Programs loaded in ${Date.now() - now}ms`);
      return {
        data: data,
        total: res.referentialsCount
      };
    }
  }

  async saveAll(entities: Program[], options?: any): Promise<Program[]> {
    if (!entities) return entities;
    return await Promise.all(entities.map((program) => this.save(program, options)));
  }

  deleteAll(data: Program[], options?: any): Promise<any> {
    throw new Error("Not implemented yet");
  }

  /**
   * Watch program by label
   * @param label
   * @param toEntity
   */
  watchByLabel(label: string, opts?: {
    toEntity?: boolean;
    debug?: boolean;
  }): Observable<Program> {

    const debug = this._debug && (!opts || opts !== false);
    if (debug) console.debug(`[program-service] Watch program {${label}}...`);
    const cacheKey = [ProgramCacheKeys.PROGRAM_BY_LABEL, label].join('|');
    return this.cache.loadFromObservable(cacheKey,
      this.graphql.watchQuery<{ program: any }>({
        query: LoadRefQuery,
        variables: {
          label: label
        },
        error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.ERROR.LOAD_PROGRAM_ERROR"}
      }).pipe(filter(isNotNil)),
      ProgramCacheKeys.CACHE_GROUP
    )
      .pipe(
        map(({program}) => {
          if (debug) console.debug(`[program-service] Program loaded {${label}}`, program);
          return (!opts || opts.toEntity !== false) ? Program.fromObject(program) : program;
        })
      );
  }

  async existsByLabel(label: string): Promise<Boolean> {
    if (isNilOrBlank(label)) return false;

    const program = await this.loadByLabel(label, {toEntity: false});
    return EntityUtils.isNotEmpty(program);
  }

  async loadByLabel(label: string, opts?: {
    toEntity?: boolean;
  }): Promise<Program> {

    if (this._debug) console.debug(`[program-service] Loading program {${label}}...`);
    const cacheKey = [ProgramCacheKeys.PROGRAM_BY_LABEL, label].join('|');

    const res = await this.cache.getOrSetItem<{ program: any }>(cacheKey,
      () => this.graphql.query<{ program: any }>({
          query: LoadRefQuery,
          variables: {
            label: label
          },
          error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.ERROR.LOAD_PROGRAM_ERROR"}
        }), ProgramCacheKeys.CACHE_GROUP);
    const program = res && res.program || undefined;
    if (this._debug) console.debug(`[program-service] Program loaded {${label}}`, program);
    return (!opts || opts.toEntity !== false) ? Program.fromObject(program) : program;
  }

  /**
   * Watch program pmfms
   */
  watchProgramPmfms(programLabel: string, options: {
    acquisitionLevel: string;
    gear?: string;
    taxonGroupId?: number;
    referenceTaxonId?: number;
  }, debug?: boolean): Observable<PmfmStrategy[]> {

    const cacheKey = [ProgramCacheKeys.PMFMS, programLabel, JSON.stringify(options)].join('|');

    return this.cache.loadFromObservable(cacheKey,
      this.watchByLabel(programLabel, {toEntity: false, debug: false}) // Watch the program
        .pipe(
          filter(isNotNil),
          map(program => {
              // TODO: select valid strategy (from date and location)
              const strategy = program && program.strategies && program.strategies[0];

              const pmfmIds = []; // used to avoid duplicated pmfms
              const res = (strategy && strategy.pmfmStrategies || [])
              // Filter on acquisition level and gear
                .filter(p =>
                  pmfmIds.indexOf(p.pmfmId) === -1
                  && (
                    !options || (
                      (!options.acquisitionLevel || p.acquisitionLevel === options.acquisitionLevel)
                      // Filter on gear (if PMFM has gears = compatible with all gears)
                      && (!options.gear || !p.gears || !p.gears.length || p.gears.findIndex(g => g === options.gear) !== -1)
                      // Filter on taxon group
                      && (!options.taxonGroupId || !p.taxonGroupIds || !p.taxonGroupIds.length || p.taxonGroupIds.findIndex(g => g === options.taxonGroupId) !== -1)
                      // Filter on reference taxon
                      && (!options.referenceTaxonId || !p.referenceTaxonIds || !p.referenceTaxonIds.length || p.referenceTaxonIds.findIndex(g => g === options.referenceTaxonId) !== -1)
                      // Add to list of IDs
                      && pmfmIds.push(p.pmfmId)
                    )
                  ))

                // Sort on rank order
                .sort((p1, p2) => p1.rankOrder - p2.rankOrder);

              if (debug) console.debug(`[program-service] PMFM for ${options.acquisitionLevel} (filtered):`, res);

              // TODO: translate name/label using translate service ?
              return res;
            }
          )), ProgramCacheKeys.CACHE_GROUP)
      .pipe(
        map(res => res && res.map(PmfmStrategy.fromObject))
      );
  }

  /**
   * Load program pmfms
   */
  loadProgramPmfms(programLabel: string, options?: {
    acquisitionLevel: string;
    gear?: string;
    taxonGroupId?: number;
    referenceTaxonId?: number;
  }, debug?: boolean): Promise<PmfmStrategy[]> {

    return firstNotNilPromise(
      this.watchProgramPmfms(programLabel, options, debug)
    );
  }

  /**
   * Watch program gears
   */
  watchGears(programLabel: string): Observable<ReferentialRef[]> {
    const cacheKey = [ProgramCacheKeys.GEARS, programLabel].join('|');
    return this.cache.loadFromObservable(cacheKey,
      this.watchByLabel(programLabel, {toEntity: false}) // Load the program
        .pipe(
          filter(isNotNil),
          map(program => {
            // TODO: select valid strategy (from date and location)
            const strategy = program && program.strategies && program.strategies[0];
            const res = (strategy && strategy.gears || []);
            if (this._debug) console.debug(`[program-service] Gears for program ${program}: `, res);
            return res;
          })
        ), ProgramCacheKeys.CACHE_GROUP)
      // Convert into model (after cache)
      .pipe(map(res => res.map(ReferentialRef.fromObject)));
  }

  /**
   * Load program gears
   */
  loadGears(programLabel: string): Promise<ReferentialRef[]> {
    return firstNotNilPromise(this.watchGears(programLabel));
  }

  /**
   * Watch program taxon groups
   */
  watchTaxonGroups(programLabel: string): Observable<TaxonGroupRef[]> {
    const cacheKey = [ProgramCacheKeys.TAXON_GROUPS, programLabel].join('|');
    return this.cache.loadFromObservable(cacheKey,
      this.watchByLabel(programLabel, {toEntity: false})
        .pipe(
          map(program => {
            // TODO: select the valid strategy (from date and location)
            // For now: select the first one
            const strategy = program && program.strategies && program.strategies[0];

            const res = (strategy && strategy.taxonGroups || [])
              // FIXME Priority level not always set in DB
              //.sort(propertyComparator('priorityLevel'))
              .map(v => v.taxonGroup);
            if (this._debug) console.debug(`[program-service] Taxon groups for program ${program}: `, res);
            return res;
          })
        ),
      ProgramCacheKeys.CACHE_GROUP
    )
      // Convert into model (after cache)
      .pipe(map(res => res.map(TaxonGroupRef.fromObject)));
  }

  /**
   * Load program taxon groups
   */
  loadTaxonGroups(programLabel: string): Promise<TaxonGroupRef[]> {
    const mapCacheKey = [ProgramCacheKeys.TAXON_GROUP_ENTITIES, programLabel].join('|');
    return this.cache.getOrSetItem(mapCacheKey,
      () => firstNotNilPromise(this.watchTaxonGroups(programLabel)),
      ProgramCacheKeys.CACHE_GROUP);
  }

  /**
   * Suggest program taxon groups
   */
  async suggestTaxonGroups(value: any, options: {
    program?: string;
    searchAttribute?: string;
  }): Promise<IReferentialRef[]> {
    // Search on program's taxon groups
    if (isNotNil(options.program)) {
      const values = await this.loadTaxonGroups(options.program);
      if (isNotEmptyArray(values)) {
        return suggestFromArray(values, value, {
          searchAttribute: options.searchAttribute
        });
      }
    }

    // If nothing found in program, or species defined
    return await this.referentialRefService.suggest(value, {
      entityName: 'TaxonGroup',
      levelId: TaxonGroupIds.FAO,
      searchAttribute: options.searchAttribute
    });
  }

  /**
   * Load program taxon groups
   */
  async suggestTaxonNames(value: any, options: {
    program?: string;
    taxonomicLevelId?: number;
    taxonomicLevelIds?: number[]
    searchAttribute?: string;
    taxonGroupId?: number;
  }): Promise<TaxonNameRef[]> {

    // Search on taxon group's taxon'
    if (isNotNil(options.program) && isNotNil(options.taxonGroupId)) {

      // Get map from program
      const taxonNamesByTaxonGroupId = this.loadTaxonNamesByTaxonGroupIdMap(options.program);
      const values = taxonNamesByTaxonGroupId[options.taxonGroupId];
      if (isNotEmptyArray(values)) {
        return suggestFromArray<TaxonNameRef>(values, value, {
          searchAttribute: options.searchAttribute
        });
      }
    }

    // If nothing found in program, or species defined
    const res = await this.referentialRefService.suggestTaxonNames(value, {
        taxonomicLevelId: options.taxonomicLevelId,
        taxonomicLevelIds: options.taxonomicLevelIds,
        taxonGroupId: options.taxonGroupId,
        searchAttribute: options.searchAttribute
      });

    // If there result, use it
    if (res && res.length) return res;

    // Then, retry without the taxon groups (e.g. when link in DB is missing)
    if (isNotNil(options.taxonGroupId)) {
      return await this.referentialRefService.suggestTaxonNames(value, {
        taxonomicLevelId: options.taxonomicLevelId,
        taxonomicLevelIds: options.taxonomicLevelIds,
        searchAttribute: options.searchAttribute
      });
    }

    // Nothing found
    return [];
  }

  async loadTaxonNamesByTaxonGroupIdMap(program: string): Promise<{ [key: number]: TaxonNameRef[] } | undefined> {
    const mapCacheKey = [ProgramCacheKeys.TAXON_NAME_BY_GROUP, program].join('|');

    return await this.cache.getOrSetItem(mapCacheKey,
      async (): Promise<{ [key: number]: TaxonNameRef[] }> => {
        const taxonGroups = await this.loadTaxonGroups(program);
        return (taxonGroups || []).reduce((res, taxonGroup) => {
          if (isNotEmptyArray(taxonGroup.taxonNames)) {
            res[taxonGroup.id] = taxonGroup.taxonNames;
            //empty = false;
          }
          return res;
        }, {});
      }, ProgramCacheKeys.CACHE_GROUP);
  }

  async load(id: number, options?: EditorDataServiceLoadOptions): Promise<Program> {

    if (this._debug) console.debug(`[program-service] Loading program {${id}}...`);

    const res = await this.graphql.query<{ program: any }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.ERROR.LOAD_PROGRAM_ERROR"}
    });
    return res && res.program && Program.fromObject(res.program);
  }

  async delete(data: Program, options?: any): Promise<any> {
    // TODO
    throw new Error("TODO: implement programService.delete()");
  }


  async save(data: Program, options?: any): Promise<Program> {
    if (!data) return data;

    // Clean cache
    this.clearCache();

    // Fill default properties
    this.fillDefaultProperties(data);
    const json = data.asObject({minify: false /* keep all properties */});

    const now = Date.now();
    if (this._debug) console.debug("[program-service] Saving program...", json);

    const res = await this.graphql.mutate<{ saveProgram: Program }>({
      mutation: SaveQuery,
      variables: {
        program: json
      },
      error: {code: ErrorCodes.SAVE_PROGRAM_ERROR, message: "PROGRAM.ERROR.SAVE_PROGRAM_ERROR"}
    });
    const savedProgram = res && res.saveProgram;
    this.copyIdAndUpdateDate(savedProgram, data);

    if (this._debug) console.debug(`[pogram-service] Program saved and updated in ${Date.now() - now}ms`, data);

    return data;
  }

  listenChanges(id: number, options?: any): Observable<Program | undefined> {
    // TODO
    console.warn("TODO: implement listen changes on program");
    return of();
  }

  canUserWrite(data: IWithProgramEntity<any>): boolean {
    if (!data) return false;

    // If the user is the recorder: can write
    if (data.recorderPerson && this.accountService.isLogin() && this.accountService.account.equals(data.recorderPerson)) {
      return true;
    }

    // TODO: check rights on program (need model changes)

    // Check same department
    return this.accountService.canUserWriteDataForDepartment(data.recorderDepartment);
  }

  executeImport(opts?: {
    maxProgression?: number;
  }): Observable<number>{

    const maxProgression = opts && opts.maxProgression || 100;
    const progression = new BehaviorSubject<number>(0);
    this.doExecuteImport(progression, maxProgression)
      .then(() => progression.complete())
      .catch(err => progression.error(err));
    return progression;
  }

  /* -- protected methods -- */

  protected async doExecuteImport(progression: BehaviorSubject<number>,
                                  maxProgression: number,
                                  opts?: {
                                    acquisitionLevels?: string[];
                                  }) {

    const acquisitionLevels: string[] = opts && opts.acquisitionLevels || Object.keys(AcquisitionLevelCodes).map(key => AcquisitionLevelCodes[key]);

    const stepCount = 1; //5; // programs, pmfms, gears, taxon groups, taxon names
    const progressionStep = maxProgression ? maxProgression / stepCount : undefined;
    const progressionRest = progressionStep && (maxProgression - progressionStep * stepCount) || undefined;

    const now = Date.now();
    console.info("[program-service] Importing programs...");

    try {
      // Clear cache
      await this.clearCache();

      // Step 1. load all programs
      let programLabels: string[] = [];
      {
        const loadFilter = {
          statusIds:  [StatusIds.ENABLE, StatusIds.TEMPORARY]
        };
        const res = await fetchAllPagesWithProgress((offset, size) =>
            this.loadAll(offset, size, 'id', null, loadFilter, {
              debug: false,
              fetchPolicy: "network-only",
              withTotal: (offset === 0), // Compute total only once
              toEntity: false
            }),
          progression,
          progressionStep,
          (page) => {
            const pageLabels = (page && page.data || []).map(p => p.label) as string[];
            programLabels.push(...pageLabels);
          }
        );

        // Saving locally
        await this.entities.saveAll(res.data, {entityName: 'ProgramVO'});
      }

      // Step 2. load pmfms
      // {
      //   await Promise.all(programLabels.map(programLabel => {
      //       return Promise.all(acquisitionLevels.map((acquisitionLevel) =>
      //         this.loadProgramPmfms(programLabel, {acquisitionLevel})
      //       ));
      //     }));
      //   progression.next(progression.getValue() + progressionStep);
      // }
      //
      // // Step 3. load gears
      // {
      //   await Promise.all(programLabels.map(programLabel => this.loadGears(programLabel)));
      //   progression.next(progression.getValue() + progressionStep);
      // }
      //
      // // Step 4. load taxon groups
      // {
      //   await Promise.all(programLabels.map(programLabel => this.loadTaxonGroups(programLabel)));
      //   progression.next(progression.getValue() + progressionStep);
      // }
      //
      // // Step 5. load taxon name
      // {
      //   await Promise.all(programLabels.map(programLabel => this.loadTaxonNamesByTaxonGroupIdMap(programLabel)));
      //   progression.next(progression.getValue() + progressionStep);
      // }

      // Make sure to fill the progression at least once
      if (progressionRest) {
        progression.next(progression.getValue() + progressionRest);
      }
      else if (isNil(progressionStep) && isNotNil(maxProgression)) {
        progression.next(progression.getValue() + maxProgression);
      }

      console.info(`[program-service] Successfully import programs in ${Date.now() - now}ms`);
    }
    catch (err) {
      console.error(`[program-service] Error during programs importation (at step #${progressionStep && progression.getValue() / progressionStep || '?'})`, err);
      throw err;
    }
  }

  protected fillDefaultProperties(program: Program) {
    program.statusId = isNotNil(program.statusId) ? program.statusId : StatusIds.ENABLE;

    // Update strategies
    (program.strategies || []).forEach(strategy => {

      strategy.statusId = isNotNil(strategy.statusId) ? strategy.statusId : StatusIds.ENABLE;

      // Force a valid programId
      // (because a bad copy can leave an old value)
      strategy.programId = isNotNil(program.id) ? program.id : undefined;
    });

  }

  protected copyIdAndUpdateDate(source: Program, target: Program) {
    EntityUtils.copyIdAndUpdateDate(source, target);

    // Update strategies
    if (target.strategies && source.strategies) {
      target.strategies.forEach(entity => {

        // Make sure program id is copy (need by equals)
        entity.programId = source.id;

        const savedStrategy = source.strategies.find(json => entity.equals(json));
        EntityUtils.copyIdAndUpdateDate(savedStrategy, entity);

        // Update pmfm strategy
        // TODO

      });
    }

  }

  protected async clearCache() {
    console.info("[program-service] Clearing program cache...");
    await this.cache.clearGroup(ProgramCacheKeys.CACHE_GROUP);
  }
}

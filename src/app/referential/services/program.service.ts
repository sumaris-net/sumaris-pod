import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable, Subject} from "rxjs";
import {filter, first, map} from "rxjs/operators";
import {EntityUtils, isNotNil, IWithProgramEntity, PmfmStrategy, Program} from "./model";
import {
  AccountService,
  BaseDataService,
  environment,
  LoadResult,
  ReferentialRef,
  TableDataService
} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {ReferentialFragments} from "../services/referential.queries";
import {GraphqlService} from "../../core/services/graphql.service";
import {EditorDataService, EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {TaxonGroupRef} from "./model/taxon.model";
import {isNilOrBlank} from "../../shared/functions";
import {CacheService} from "ionic-cache";

export declare class ProgramFilter {
  searchText?: string;
  withProperty?: string;
  //acquisitionLevel?: string;
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
    }`,
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
      id
      label   
      name
      priorityLevel
      taxonNames {
        ...TaxonNameFragment
      }
      __typename
    }
  `,
  taxonNameStrategy: gql`
    fragment TaxonNameStrategyFragment on TaxonNameStrategyVO {
      id
      label   
      name
      priorityLevel
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
    referentialsCount(entityName: "Program")
  }
  ${ProgramFragments.lightProgram}
`;

const ProgramCacheKeys = {
  GROUP: 'program',
  PMFMS: 'programPmfms',
  GEARS: 'programGears',
};

const cacheBuster$ = new Subject<void>();

@Injectable()
export class ProgramService extends BaseDataService
  implements TableDataService<Program, ProgramFilter>,
    EditorDataService<Program, ProgramFilter> {


  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService,
    protected cache: CacheService
  ) {
    super(graphql);

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
           options?: any): Observable<LoadResult<Program>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    const now = Date.now();
    if (this._debug) console.debug("[program-service] Loading programs using options:", variables);

    return this.graphql.watchQuery<{ programs: any[], referentialsCount: number }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_PROGRAMS_ERROR, message: "PROGRAM.ERROR.LOAD_PROGRAMS_ERROR"}
    })
      .pipe(
        map(({programs, referentialsCount}) => {
            const data = (programs || []).map(Program.fromObject);
            if (this._debug) console.debug(`[program-service] Programs loaded in ${Date.now() - now}ms`, data);
            return {
              data: data,
              total: referentialsCount
            };
          }
        )
      );
  }

  async saveAll(data: Program[], options?: any): Promise<Program[]> {
    throw new Error("Not implemented yet");
  }

  deleteAll(data: Program[], options?: any): Promise<any> {
    throw new Error("Not implemented yet");
  }

  /**
   * Watch program by label
   * @param label
   * @param toEntity
   */
  watchByLabel(label: string, toEntity: boolean): Observable<Program> {

    if (this._debug) console.debug(`[program-service] Watch program {${label}}...`);

    return this.graphql.watchQuery<{ program: any }>({
        query: LoadRefQuery,
        variables: {
          label: label
        },
        error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.ERROR.LOAD_PROGRAM_ERROR"}
      })
        .pipe(
          filter(isNotNil),
          map(({program}) => {
            if (this._debug) console.debug(`[program-service] Program loaded {${label}}`, program);
            return toEntity ? Program.fromObject(program) : program;
          })
        );
  }

  async existsByLabel(label: string): Promise<Boolean> {
    if (isNilOrBlank(label)) return false;

    const program = await this.watchByLabel(label, false).toPromise();
    return EntityUtils.isNotEmpty(program);
  }

  loadByLabel(label: string): Promise<Program> {
    return this.watchByLabel(label, true)
      .pipe(
        filter(isNotNil),
        first(),
        map(Program.fromObject)
      )
      .toPromise();
  }

  /**
   * Watch program pmfms
   */
  watchProgramPmfms(programLabel: string, options: {
    acquisitionLevel: string;
    gear?: string;
    taxonGroupId?: number;
    referenceTaxonId?: number;
    debug?: boolean;
  }): Observable<PmfmStrategy[]> {

    //const cacheKey = [ProgramCacheKeys.PMFMS, options.acquisitionLevel, options.gear, options.taxonGroupId, options.referenceTaxonId].join('|');

    return this.watchByLabel(programLabel, false) // Watch the program
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
            .map(PmfmStrategy.fromObject)

            // Sort on rank order
            .sort((p1, p2) => p1.rankOrder - p2.rankOrder);

          if (options.debug) console.debug(`[program-service] PMFM for ${options.acquisitionLevel} (filtered):`, res);
          console.debug(`TODO [program-service] PMFM for ${options.acquisitionLevel} (filtered):`, res);

          // TODO: translate name/label using translate service ?
          return res;
        }));
  }

  /**
   * Load program pmfms
   */
  loadProgramPmfms(programLabel: string, options?: {
    acquisitionLevel: string;
    gear?: string;
    taxonGroupId?: number;
    referenceTaxonId?: number;
    debug?: boolean;
  }): Promise<PmfmStrategy[]> {

    return this.watchProgramPmfms(programLabel, options)
      .pipe(filter(isNotNil), first())
      .toPromise();
  }

  /**
   * Watch program gears
   */
  watchGears(programLabel: string): Observable<ReferentialRef[]> {
    return this.cache.loadFromObservable(
      ProgramCacheKeys.GEARS,
      this.watchByLabel(programLabel, false) // Load the program
        .pipe(
          filter(isNotNil),
          map(program => {
            // TODO: select valid strategy (from date and location)
            const strategy = program && program.strategies && program.strategies[0];
            const res = (strategy && strategy.gears || []);
            if (this._debug) console.debug(`[program-service] Gears for program ${program}: `, res);
            return res ;
          })
        ))
      // Convert into model (after cache)
      .pipe(map(res => res.map(ReferentialRef.fromObject)));
  }

  /**
   * Load program gears
   */
  loadGears(programLabel: string): Promise<ReferentialRef[]> {
    // Load the program
    return this.watchGears(programLabel)
      .pipe(filter(isNotNil), first())
      .toPromise();
  }

  /**
   * Watch program taxon groups
   */
  watchTaxonGroups(programLabel: string): Observable<TaxonGroupRef[]> {

    return this.watchByLabel(programLabel, false)
      .pipe(
        map(program => {
          // TODO: select valid strategy (from date and location)
          const strategy = program && program.strategies && program.strategies[0];
          const res = (strategy && strategy.taxonGroups || []);
          if (this._debug) console.debug(`[program-service] Taxon groups for program ${program}: `, res);
          return res;
        })
      )
      // Convert into model (after cache)
      .pipe(map(res => res.map(TaxonGroupRef.fromObject)));
  }

  /**
   * Load program taxon groups
   */
  loadTaxonGroups(programLabel: string): Promise<TaxonGroupRef[]> {
    return this.watchTaxonGroups(programLabel)
      .pipe(filter(isNotNil), first())
      .toPromise();
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
    if (this._debug) console.debug(`[program-service] Saving program {${data.label}}...`, data);

    // Clean cache
    this.cache.clearGroup(ProgramCacheKeys.GROUP);

    // TODO
    throw new Error("TODO: implement programService.save()");
  }

  listenChanges(id: number, options?: any): Observable<Program | undefined> {
    // TODO
    console.warn("TODO: implement listen changes on program");
    return Observable.of();
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
}

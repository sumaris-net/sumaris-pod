import {concat, defer, Observable, of, timer} from "rxjs";
import {catchError, map, switchMap, tap} from "rxjs/operators";
import {RootDataEntity, SynchronizationStatusEnum} from "./model/root-data-entity.model";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {RootDataService, RootEntityMutations} from "../../trip/services/root-data-service.class";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ProgramService} from "../../referential/services/program.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {PersonService} from "../../admin/services/person.service";
import {Injector} from "@angular/core";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {NetworkService} from "../../core/services/network.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Moment} from "moment";
import {isNil} from "../../shared/functions";
import {SAVE_LOCALLY_AS_OBJECT_OPTIONS} from "./model/data-entity.model";
import {JobUtils} from "../../shared/services/job.utils";
import {ProgramRefService} from "../../referential/services/program-ref.service";


export interface IDataSynchroService<T extends RootDataEntity<T>, O = EntityServiceLoadOptions> {

  load(id: number, opts?: O): Promise<T>;

  executeImport(opts?: {
    maxProgression?: number;
  }): Observable<number>;

  synchronizeById(id: number): Promise<T>;

  synchronize(data: T, opts?: any): Promise<T>;

  hasOfflineData(): Promise<boolean>;

  lastUpdateDate(): Promise<Moment>;
}

const DataSynchroServiceFnName: (keyof IDataSynchroService<any>)[] = ['load', 'executeImport', 'synchronizeById', 'synchronize', 'lastUpdateDate'];

export function isDataSynchroService(object: any): object is IDataSynchroService<any> {
  return object && DataSynchroServiceFnName.filter(fnName => (typeof object[fnName] === 'function'))
    .length === DataSynchroServiceFnName.length || false;
}

export const DEFAULT_FEATURE_NAME = 'synchro';

export abstract class RootDataSynchroService<T extends RootDataEntity<T>, F = any, O = EntityServiceLoadOptions>
  extends RootDataService<T, F>
  implements IDataSynchroService<T, O> {

  protected referentialRefService: ReferentialRefService;
  protected personService: PersonService;
  protected vesselSnapshotService: VesselSnapshotService;
  protected programRefService: ProgramRefService;
  protected entities: EntitiesStorage;
  protected network: NetworkService;
  protected settings: LocalSettingsService;

  protected $importationProgress: Observable<number>;

  protected featureName: string;

  protected constructor (
    injector: Injector,
    mutations: RootEntityMutations
  ) {
    super(injector, mutations);

    this.referentialRefService = injector.get(ReferentialRefService);
    this.personService = injector.get(PersonService);
    this.vesselSnapshotService = injector.get(VesselSnapshotService);
    this.programRefService = injector.get(ProgramRefService);
    this.entities = injector.get(EntitiesStorage);
    this.network = injector.get(NetworkService);
    this.settings = injector.get(LocalSettingsService);
  }

  executeImport(opts?: {
    maxProgression?: number;
  }): Observable<number>{
    if (this.$importationProgress) return this.$importationProgress; // Skip to may call

    const maxProgression = opts && opts.maxProgression || 100;

    const featuresName = this.featureName || DEFAULT_FEATURE_NAME;

    const jobOpts = {maxProgression: undefined};
    const jobDefers: Observable<number>[] = [
      // Clear caches
      defer(() => timer()
        .pipe(
          switchMap(() => this.network.clearCache()),
          map(() => jobOpts.maxProgression as number)
        )
      ),

      // Import Jobs
      ...this.getImportJobs(jobOpts),

      // Save data to local storage, then set progression to the max
      defer(() => timer()
        .pipe(
          switchMap(() => this.entities.persist()),
          map(() => jobOpts.maxProgression as number)
        ))
    ];
    const jobCount = jobDefers.length;
    jobOpts.maxProgression = Math.trunc(maxProgression / jobCount);

    const now = Date.now();
    console.info(`[synchro-service] Starting ${featuresName} importation (${jobDefers.length} jobs)...`);

    // Execute all jobs, one by one
    let jobIndex = 0;
    this.$importationProgress = concat(
      ...jobDefers.map((jobDefer: Observable<number>, index) => {
        return jobDefer
          .pipe(
            //switchMap(() => jobDefer),
            map(jobProgression => {
              jobIndex = index;
              if (this._debug && jobProgression > jobOpts.maxProgression) {
                console.warn(`[synchro-service] WARN job #${jobIndex} return a jobProgression > maxProgression (${jobProgression} > ${jobOpts.maxProgression})!`);
              }
              // Compute total progression
              return index * jobOpts.maxProgression + Math.min(jobProgression || 0, jobOpts.maxProgression);
            })
          );
      }),

      // Finish (force to reach max value)
      of(maxProgression)
        .pipe(
          tap(() => {
            this.$importationProgress = null;
            console.info(`[synchro-service] Importation finished in ${Date.now() - now}ms`);
            this.settings.registerOfflineFeature(featuresName);
          })
        ))

      .pipe(
        catchError((err) => {
          this.$importationProgress = null;
          console.error(`[synchro-service] Error during importation (job #${jobIndex + 1}): ${err && err.message || err}`, err);
          throw err;
        }),
        // Compute total progression (= job offset + job progression)
        // (and make ti always <= maxProgression)
        map((progression) =>  Math.min(progression, maxProgression))
      );

    return this.$importationProgress;
  }

  async synchronizeById(id: number): Promise<T> {
    const entity = await this.load(id);

    if (!entity || entity.id >= 0) return; // skip

    return await this.synchronize(entity);
  }

  /**
   * Check if there is offline data.
   * Can be override by subclasses (e.g. to check in the entities storage)
   */
  async hasOfflineData(): Promise<boolean> {
    const featuresName = this.featureName || DEFAULT_FEATURE_NAME;
    return this.settings.hasOfflineFeature(featuresName);
  }

  /**
   * Get remote last update date. By default, check on referential tables.
   * Can be override by subclasses (e.g. to check in the entities storage)
   */
  lastUpdateDate(): Promise<Moment> {
    return this.referentialRefService.lastUpdateDate();
  }

  abstract load(id: number, opts?: O): Promise<T>;

  abstract synchronize(data: T, opts?: any): Promise<T>;

  async terminate(entity: T): Promise<T> {
    // If local entity: save locally
    const offline = entity && entity.id < 0;
    if (offline) {

      // Make sure to fill id, with local ids
      await this.fillOfflineDefaultProperties(entity);

      // Update sync status
      entity.synchronizationStatus = 'READY_TO_SYNC';

      const json = this.asObject(entity, SAVE_LOCALLY_AS_OBJECT_OPTIONS);
      if (this._debug) console.debug(`[trip-service] Terminate {${entity.id}} locally...`, json);

      // Save response locally
      await this.entities.save(json);

      return entity;
    }

    return super.terminate(entity);
  }

  /* -- protected methods -- */

  protected async fillOfflineDefaultProperties(entity: T) {
    const isNew = isNil(entity.id);

    // If new, generate a local id
    if (isNew) {
      entity.id =  await this.entities.nextValue(entity);
    }

    // Fill default synchronization status
    entity.synchronizationStatus = entity.synchronizationStatus || SynchronizationStatusEnum.DIRTY;

  }

  /**
   * List of importation jobs. Can be override by subclasses, to add or remove some jobs
   * @param opts
   * @protected
   */
  protected getImportJobs(opts: {maxProgression: undefined}): Observable<number>[] {
    return JobUtils.defers([
      (p, o) => this.referentialRefService.executeImport(p, o),
      (p, o) => this.personService.executeImport(p, o),
      (p, o) => this.vesselSnapshotService.executeImport(p, o),
      (p, o) => this.programRefService.executeImport(p, o)
    ], opts);
  }
}

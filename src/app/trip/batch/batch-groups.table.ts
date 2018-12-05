import {Component} from "@angular/core";
import {Observable} from "rxjs";
import {ValidatorService} from "angular4-material-table";
import {AccountService} from "../../core/core.module";
import {referentialToString, PmfmStrategy, Batch, MeasurementUtils, getPmfmName} from "../services/trip.model";
import {ModalController, Platform} from "@ionic/angular";
import {Router, ActivatedRoute} from "@angular/router";
import {Location} from "@angular/common";
import {ReferentialRefService, ProgramService} from "../../referential/referential.module";
import {TranslateService} from "@ngx-translate/core";
import {environment} from "../../../environments/environment";
import {isNotNil, isNil} from "../../core/services/model";
import {
  MeasurementsValidatorService,
  BatchGroupsValidatorService,
  BatchValidatorService
} from "../services/trip.validators";
import {PmfmLabelPatterns} from "src/app/referential/services/model";
import {FormBuilder} from "@angular/forms";
import {getControlFromPath} from "src/app/core/form/form.utils";
import {BatchesTable} from "./batches.table";
import {LoadResult} from "../../core/services/data-service.class";

@Component({
    selector: 'table-batch-groups',
    templateUrl: 'batch-groups.table.html',
    styleUrls: ['batch-groups.table.scss'],
    providers: [
        { provide: ValidatorService, useClass: BatchGroupsValidatorService }
    ]
})
export class BatchGroupsTable extends BatchesTable {

    qvPmfm: PmfmStrategy;
    defaultWeightPmfm: PmfmStrategy;
    weightPmfmsByMethod: {[key: string]: PmfmStrategy};

    constructor(
        route: ActivatedRoute,
        router: Router,
        platform: Platform,
        location: Location,
        modalCtrl: ModalController,
        accountService: AccountService,
        validatorService: BatchValidatorService,
        measurementsValidatorService: MeasurementsValidatorService,
        referentialRefService: ReferentialRefService,
        programService: ProgramService,
        translate: TranslateService,
        formBuilder: FormBuilder
    ) {
        super(route, router, platform, location, modalCtrl, accountService,
            validatorService, measurementsValidatorService, referentialRefService, programService, translate, formBuilder
        );
        // -- For DEV only
        this.debug = !environment.production;
    };

    loadAll(
        offset: number,
        size: number,
        sortBy?: string,
        sortDirection?: string,
        filter?: any,
        options?: any
    ): Observable<LoadResult<Batch>> {
        if (!this.data) {
            if (this.debug) console.debug("[batch-table] Unable to load row: value not set (or not started)");
            return Observable.empty(); // Not initialized
        }
        sortBy = (sortBy !== 'id') && sortBy || 'rankOrder'; // Replace id by rankOrder

        const now = Date.now();
        if (this.debug) console.debug("[batch-table] Loading rows..", this.data);

        this.pmfms
            .filter(pmfms => pmfms && pmfms.length > 0)
            .first()
            .subscribe(pmfms => {
                // Transform entities into object array
                const data = this.data.map(batch => {

                    const json = batch.asObject();
                    if (isNotNil(this.qvPmfm)) {
                        const measurementValues = {};
                        this.qvPmfm.qualitativeValues.forEach((qv, qvIndex) => {
                            const child = (batch.children || []).find(child => child.label === `${batch.label}.${qv.label}`);
                            if (child) {
                                let i = qvIndex * 5;
                                measurementValues[i++] = child && isNotNil(child.individualCount) ? child.individualCount : null;
                                measurementValues[i++] = child && child.measurementValues[this.defaultWeightPmfm.pmfmId];

                                if (child.children && child.children.length == 1) {
                                    const samplingChild = child.children[0];
                                    measurementValues[i++] = isNotNil(samplingChild.samplingRatio) ? samplingChild.samplingRatio * 100 : null;
                                    measurementValues[i++] = isNotNil(samplingChild.individualCount) ? samplingChild.individualCount : null;
                                    measurementValues[i++] = samplingChild.measurementValues[this.defaultWeightPmfm.pmfmId];
                                }
                            }
                        });
                        json.measurementValues = MeasurementUtils.normalizeFormValues(measurementValues, pmfms);
                    }

                    return json;
                });

                // Sort
                this.sortBatches(data, sortBy, sortDirection);
                if (this.debug) console.debug(`[batch-table] Rows loaded in ${Date.now() - now}ms`, data);

                this.dataSubject.next({data: data});
            });

        return this.dataSubject.asObservable();
    }

    async saveAll(data: Batch[], options?: any): Promise<Batch[]> {
        if (!this.data) throw new Error("[batch-table] Could not save table: value not set (or not started)");

        if (this.debug) console.debug("[batch-table] Updating data from rows...");

        this.data = data.map(json => {
            const batch: Batch = json.id && this.data.find(b => b.id === json.id) || Batch.fromObject(json);
            //const batch: Batch = Batch.fromObject(json);
            const measurementValues = json.measurementValues;
            batch.measurementValues = {}; // TODO: compute total weight and nb indiv ?

            if (isNotNil(this.qvPmfm)) {
                batch.children = this.qvPmfm.qualitativeValues.reduce((res, qv, qvIndex:number) => {
                    let i = qvIndex * 5;
                    const individualCount = measurementValues[i++];
                    const weight = measurementValues[i++];
                    const samplingRatio = measurementValues[i++];
                    const samplingIndividualCount = measurementValues[i++];
                    const samplingWeight = measurementValues[i++];

                    const childLabel = `${batch.label}.${qv.label}`;
                    const child: Batch = batch.id && (batch.children || []).find(b => b.label === childLabel) || new Batch();
                    child.rankOrder = qvIndex + 1;
                    child.measurementValues[this.qvPmfm.pmfmId] = qv.id.toString();
                    child.measurementValues[this.defaultWeightPmfm.pmfmId] = weight;
                    child.individualCount = individualCount;
                    child.label = childLabel;

                    // If sampling
                    if (isNotNil(samplingRatio) || isNotNil(samplingIndividualCount) || isNotNil(samplingWeight)) {
                        const samplingLabel = `${childLabel}.%`;
                        const samplingChild: Batch = child.id && (child.children || []).find(b => b.label === samplingLabel) || new Batch();
                        samplingChild.rankOrder = 1;
                        samplingChild.label = samplingLabel;
                        samplingChild.samplingRatio = isNotNil(samplingRatio) ? samplingRatio / 100 : undefined;
                        samplingChild.samplingRatioText = isNotNil(samplingRatio) ? `${samplingRatio}%` : undefined;
                        samplingChild.measurementValues[this.defaultWeightPmfm.pmfmId] = samplingWeight;
                        samplingChild.individualCount = samplingIndividualCount;
                        child.children = [samplingChild];
                    }
                    // Remove children
                    else {
                        child.children = [];
                    }

                    return res.concat(child);
                }, []);
            }
            return batch;
        });

        return data;
    }

    deleteAll(dataToRemove: Batch[], options?: any): Promise<any> {
        this._dirty = true;
        // Noting else to do (make no sense to delete in this.data, will be done in saveAll())
        return Promise.resolve();
    }

    /* -- protected methods -- */

    protected async onNewBatch(batch: Batch, rankOrder?: number): Promise<void> {
        // Set computed values
        batch.rankOrder = isNotNil(rankOrder) ? rankOrder : ((await this.getMaxRankOrder()) + 1);
        batch.label = this.acquisitionLevel + "#" + batch.rankOrder;
    }

    protected async refreshPmfms(event?: any): Promise<PmfmStrategy[]> {
        const candLoadPmfms = isNotNil(this.program) && isNotNil(this.acquisitionLevel);
        if (!candLoadPmfms) return undefined;

        this.loading = true;
        this.loadingPmfms = true;

        // Load pmfms
        const pmfms = (await this.programService.loadProgramPmfms(
            this.program,
            {
                acquisitionLevel: this.acquisitionLevel
            })) || [];

        if (!pmfms.length && this.debug) {
            console.debug(`[batch-group-table] No pmfm found (program=${this.program}, acquisitionLevel=${this.acquisitionLevel}). Please fill program's strategies !`);
        }

        let weightMinRankOrder: number = undefined;
        let defaultWeightPmfm: PmfmStrategy = undefined;
        this.weightPmfmsByMethod = pmfms.reduce((res, p) => {
            const matches = PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label);
            if (matches) {
                const methodId = p.methodId;
                res[methodId] = p;
                if (isNil(weightMinRankOrder)) weightMinRankOrder = p.rankOrder;
                if (isNil(defaultWeightPmfm)) defaultWeightPmfm = p;
            }
            return res;
        }, {});
        this.defaultWeightPmfm = defaultWeightPmfm;

        this.qvPmfm = pmfms.find(p => p.type == 'qualitative_value');
        if (isNil(weightMinRankOrder) || weightMinRankOrder < this.qvPmfm.rankOrder) {
            throw new Error(`[bacth-group-table] Unable to construct the table. No qualitative value found (before weight) in program PMFMs on acquisition level ${this.acquisitionLevel}`);
        }

        const translations = this.translate.instant([
            'TRIP.BATCH.TABLE.TOTAL_INDIVIDUAL_COUNT',
            'TRIP.BATCH.TABLE.TOTAL_WEIGHT',
            'TRIP.BATCH.TABLE.SAMPLING_RATIO',
            'TRIP.BATCH.TABLE.SAMPLING_INDIVIDUAL_COUNT',
            'TRIP.BATCH.TABLE.SAMPLING_WEIGHT']);
        const columnPmfms: PmfmStrategy[] = this.qvPmfm.qualitativeValues.reduce((res, qv, index) => {
            return res.concat(
                [
                    // Column on total (nb indiv, weight)
                    {
                        type: 'double', label: qv.label + '_TOTAL_INDIVIDUAL_COUNT', id: index,
                        name: translations['TRIP.BATCH.TABLE.TOTAL_INDIVIDUAL_COUNT'],
                        minValue: 0,
                        maxValue: 10000,
                        maximumNumberDecimals: 0
                    },
                    Object.assign({}, this.defaultWeightPmfm, {
                        type: 'double', label: qv.label + '_TOTAL_WEIGHT', id: index,
                        name: translations['TRIP.BATCH.TABLE.TOTAL_WEIGHT'],
                        minValue: 0,
                        maxValue: 10000,
                        maximumNumberDecimals: 1
                    }),
                    // Column on sampling (ratio, nb indiv, weight)
                    {
                        type: 'integer', label: qv.label + '_SAMPLING_RATIO', id: index,
                        name: translations['TRIP.BATCH.TABLE.SAMPLING_RATIO'],
                        unit: '%',
                        minValue: 0,
                        maxValue: 100,
                        maximumNumberDecimals: 0
                    },
                    {
                        type: 'integer', label: qv.label + '_SAMPLING_INDIVIDUAL_COUNT', id: index,
                        name: translations['TRIP.BATCH.TABLE.SAMPLING_INDIVIDUAL_COUNT'],
                        minValue: 0,
                        maxValue: 1000,
                        maximumNumberDecimals: 0
                    },
                    Object.assign({}, this.defaultWeightPmfm, {
                        type: 'double', label: 'SAMPLING_WEIGHT', id: index,
                        name: translations['TRIP.BATCH.TABLE.SAMPLING_WEIGHT'],
                        minValue: 0,
                        maxValue: 1000,
                        maximumNumberDecimals: 1
                    })
                ]
            )
        }, [])
            .map((pmfm, index) => {
                // Set pmfmId (as index in array)
                pmfm.pmfmId = index;
                return PmfmStrategy.fromObject(pmfm);
            });

        this.loadingPmfms = false;

        this.pmfms.next(columnPmfms);

        return pmfms;
    }

    protected isEven(pmfm: PmfmStrategy) {
        const qvIndex = Math.trunc(pmfm.pmfmId / 5);
        return (qvIndex % 2 === 0);
    }

    protected isOdd(pmfm: PmfmStrategy) {
        const qvIndex = Math.trunc(pmfm.pmfmId / 5);
        return (qvIndex % 2 !== 0);
    }

    referentialToString = referentialToString;
    getPmfmColumnHeader = getPmfmName;
    getControlFromPath = getControlFromPath;
}


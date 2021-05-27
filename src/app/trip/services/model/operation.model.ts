import {EntityClass} from "../../../core/services/model/entity.decorators";
import {DataEntity, DataEntityAsObjectOptions} from "../../../data/services/model/data-entity.model";
import {Moment} from "moment";
import {Metier} from "../../../referential/services/model/taxon.model";
import {Measurement, MeasurementFormValues, MeasurementModelValues, MeasurementUtils, MeasurementValuesUtils} from "./measurement.model";
import {Sample} from "./sample.model";
import {Batch} from "./batch.model";
import {FishingArea} from "./fishing-area.model";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";
import {NOT_MINIFY_OPTIONS, ReferentialAsObjectOptions} from "../../../core/services/model/referential.model";
import {isNotNil} from "../../../shared/functions";
import {IWithProductsEntity, Product} from "./product.model";
import {IWithPacketsEntity, Packet} from "./packet.model";
import {PhysicalGear, VesselPosition} from "./trip.model";
import {RootDataEntity} from "../../../data/services/model/root-data-entity.model";

const sortByDateTimeFn = (n1: VesselPosition, n2: VesselPosition) => {
    return n1.dateTime.isSame(n2.dateTime) ? 0 : (n1.dateTime.isAfter(n2.dateTime) ? 1 : -1);
};

export interface OperationAsObjectOptions extends DataEntityAsObjectOptions {
    batchAsTree?: boolean;
    sampleAsTree?: boolean;
}

export interface OperationFromObjectOptions {
    withSamples?: boolean;
    withBatchTree?: boolean;
}

@EntityClass({typename: 'OperationVO'})
export class Operation extends DataEntity<Operation, number, OperationAsObjectOptions, OperationFromObjectOptions> {

    static fromObject: (source: any, opts?: OperationFromObjectOptions) => Operation;

    startDateTime: Moment = null;
    endDateTime: Moment = null;
    fishingStartDateTime: Moment = null;
    fishingEndDateTime: Moment = null;
    comments: string = null;
    rankOrderOnPeriod: number = null;
    hasCatch: boolean = null;
    positions: VesselPosition[] = null;
    startPosition: VesselPosition = new VesselPosition();
    endPosition: VesselPosition = new VesselPosition();

    metier: Metier = null;
    physicalGear: PhysicalGear = null;
    tripId: number = null;
    trip: RootDataEntity<any> = null;

    measurements: Measurement[] = [];
    samples: Sample[] = null;
    catchBatch: Batch = null;
    fishingAreas: FishingArea[] = [];

    constructor() {
        super(Operation.TYPENAME);
    }

    asObject(opts?: OperationAsObjectOptions): any {
        const target = super.asObject(opts);
        target.startDateTime = toDateISOString(this.startDateTime);
        target.endDateTime = toDateISOString(this.endDateTime);
        target.fishingStartDateTime = toDateISOString(this.fishingStartDateTime);
        target.fishingEndDateTime = toDateISOString(this.fishingEndDateTime);

        target.endDateTime = target.endDateTime || target.startDateTime;

        // If end position is valid (has latitude AND longitude)
        if (target.endPosition && target.endPosition.latitude && target.endPosition.longitude) {

            // Fill end date, using start date (if on FIELD mode, can be null, but Pod has NOT NULL constraint)
            if (!target.endPosition.dateTime) {
                // Create a copy
                target.endPosition = target.endPosition.clone();
                target.endPosition.dateTime = target.endPosition.dateTime || target.fishingEndDateTime || target.endDateTime;
            }
        }
        // Invalid position (missing latitude or longitude - allowed in on FIELD mode): remove it
        else {
            delete target.endPosition;
        }

        // Create an array of position, instead of start/end
        target.positions = [target.startPosition, target.endPosition]
            .filter(p => p && p.dateTime)
            .map(p => p && p.asObject(opts)) || undefined;
        delete target.startPosition;
        delete target.endPosition;

        // Physical gear
        target.physicalGear = this.physicalGear.asObject({...opts, ...NOT_MINIFY_OPTIONS /*Avoid minify, to keep gear for operations tables cache*/});
        delete target.physicalGear.measurementValues;
        target.physicalGearId = this.physicalGear && this.physicalGear.id;
        if (opts && opts.keepLocalId === false && target.physicalGearId < 0) {
            delete target.physicalGearId; // Remove local id
        }

        // Metier
        target.metier = this.metier && this.metier.asObject({...opts, ...NOT_MINIFY_OPTIONS /*Always minify=false, because of operations tables cache*/}) || undefined;

        // Measurements
        target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(opts)) || undefined;

        // Samples
        {
            // Serialize samples into a tree (will keep only children arrays, and removed parentId and parent)
            if (!opts || opts.sampleAsTree !== false) {
                target.samples = this.samples && this.samples
                    .filter(s => isNotNil(s.parentId) && isNotNil(s.parent))
                    .map(s => s.asObject({...opts, withChildren: true})) || undefined;
            } else {
                // Serialize as samples array (this will fill parentId, and remove children and parent properties)
                target.samples = Sample.treeAsObjectArray(this.samples, opts);
            }
        }

        // Batch
        if (target.catchBatch) {
            // Serialize batches into a tree (will keep only children arrays, and removed parentId and parent)
            if (!opts || opts.batchAsTree !== false) {
                target.catchBatch = this.catchBatch && this.catchBatch.asObject({
                    ...opts,
                    withChildren: true
                }) || undefined;
            }
            // Serialize as batches array (this will fill parentId, and remove children and parent properties)
            else {
                target.batches = Batch.treeAsObjectArray(target.catchBatch, opts);
                delete target.catchBatch;
            }
        }

        // Fishing areas
        target.fishingAreas = this.fishingAreas && this.fishingAreas.map(value => value.asObject(opts)) || undefined;

        return target;
    }

    fromObject(source: any, opts?: OperationFromObjectOptions) {
        super.fromObject(source, opts);
        this.hasCatch = source.hasCatch;
        this.comments = source.comments;
        this.tripId = source.tripId;
        this.physicalGear = (source.physicalGear || source.physicalGearId) ? PhysicalGear.fromObject(source.physicalGear || {id: source.physicalGearId}) : undefined;
        this.startDateTime = fromDateISOString(source.startDateTime);
        this.endDateTime = fromDateISOString(source.endDateTime);

        this.fishingStartDateTime = fromDateISOString(source.fishingStartDateTime);
        this.fishingEndDateTime = fromDateISOString(source.fishingEndDateTime);
        this.rankOrderOnPeriod = source.rankOrderOnPeriod;
        this.metier = source.metier && Metier.fromObject(source.metier, {useChildAttributes: 'TaxonGroup'}) || undefined;
        if (source.startPosition || source.endPosition) {
            this.startPosition = source.startPosition && VesselPosition.fromObject(source.startPosition);
            this.endPosition = source.endPosition && VesselPosition.fromObject(source.endPosition);
            this.positions = undefined;
        } else if (source.positions) {
            const positions = source.positions.map(VesselPosition.fromObject).sort(sortByDateTimeFn) || undefined;
            if (positions.length >= 1 && positions.length <= 2) {
                this.startPosition = positions[0];
                if (positions.length > 1) {
                    this.endPosition = positions.pop(); // last
                }
                this.positions = undefined;
            } else {
                this.startPosition = undefined;
                this.endPosition = undefined;
                this.positions = positions;
            }
        }
        this.measurements = [
            ...(source.measurements && source.measurements.map(Measurement.fromObject) || []),
            ...(source.gearMeasurements && source.gearMeasurements.map(Measurement.fromObject) || [])
        ];

        // Remove fake dates (e.g. if endDateTime = startDateTime)
        if (this.endDateTime && this.endDateTime.isSameOrBefore(this.startDateTime)) {
            this.endDateTime = undefined;
        }
        if (this.fishingEndDateTime && this.fishingEndDateTime.isSameOrBefore(this.fishingStartDateTime)) {
            this.fishingEndDateTime = undefined;
        }
        if (this.endPosition && this.endPosition.dateTime && this.startPosition && this.endPosition.dateTime.isSameOrBefore(this.startPosition.dateTime)) {
            this.endPosition.dateTime = undefined;
        }

        // Fishing areas
        this.fishingAreas = source.fishingAreas && source.fishingAreas.map(FishingArea.fromObject) || undefined;

        // Samples
        if (!opts || opts.withSamples !== false) {
            this.samples = source.samples && source.samples.map(source => Sample.fromObject(source, {withChildren: true})) || undefined;
        }

        // Batches
        if (!opts || opts.withBatchTree !== false) {
            this.catchBatch = source.catchBatch && !source.batches ?
                // Reuse existing catch batch (useful for local entity)
                Batch.fromObject(source.catchBatch, {withChildren: true}) :
                // Convert list to tree (useful when fetching from a pod)
                Batch.fromObjectArrayAsTree(source.batches);
        }
    }

    equals(other: Operation): boolean {
        return super.equals(other)
            || ((this.startDateTime === other.startDateTime
                    || (!this.startDateTime && !other.startDateTime && this.fishingStartDateTime === other.fishingStartDateTime))
                && ((!this.rankOrderOnPeriod && !other.rankOrderOnPeriod) || (this.rankOrderOnPeriod === other.rankOrderOnPeriod))
            );
    }
}

export class OperationGroup extends DataEntity<OperationGroup>
    implements IWithProductsEntity<OperationGroup>, IWithPacketsEntity<OperationGroup> {

    static TYPENAME = 'OperationGroupVO';

    static fromObject(source: any): OperationGroup {
        const res = new OperationGroup();
        res.fromObject(source);
        return res;
    }

    comments: string;
    rankOrderOnPeriod: number;
    hasCatch: boolean;

    metier: Metier;
    physicalGear: PhysicalGear;
    tripId: number;
    trip: RootDataEntity<any>;

    measurements: Measurement[];
    gearMeasurements: Measurement[];

    // all measurements in table
    measurementValues: MeasurementModelValues | MeasurementFormValues;

    products: Product[];
    samples: Sample[];
    packets: Packet[];
    fishingAreas: FishingArea[];

    constructor() {
        super();
        this.__typename = OperationGroup.TYPENAME;
        this.metier = null;
        this.physicalGear = null;
        this.measurementValues = {};
        this.measurements = [];
        this.gearMeasurements = [];
        this.products = [];
        this.samples = [];
        this.packets = [];
        this.fishingAreas = [];
    }

    clone(): OperationGroup {
        const target = new OperationGroup();
        target.fromObject(this.asObject());
        return target;
    }

    asObject(opts?: DataEntityAsObjectOptions & { batchAsTree?: boolean }): any {
        const target = super.asObject(opts);

        target.metier = this.metier && this.metier.asObject({...opts, ...NOT_MINIFY_OPTIONS /*Always minify=false, because of operations tables cache*/} as ReferentialAsObjectOptions) || undefined;

        // Physical gear
        target.physicalGear = this.physicalGear && this.physicalGear.asObject({...opts, ...NOT_MINIFY_OPTIONS /*Avoid minify, to keep gear for operations tables cache*/});
        if (target.physicalGear)
            delete target.physicalGear.measurementValues;

        // Measurements
        target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(opts)) || undefined;
        target.gearMeasurements = this.gearMeasurements && this.gearMeasurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(opts)) || undefined;
        target.measurementValues = MeasurementValuesUtils.asObject(this.measurementValues, opts);
        delete target.gearMeasurementValues; // all measurements are stored only measurementValues

        // Products
        target.products = this.products && this.products.map(product => {
            const p = product.asObject(opts);
            // Affect parent link
            p.operationId = target.id;
            return p;
        }) || undefined;

        // Samples
        target.samples = this.samples && this.samples.map(sample => {
            const s = sample.asObject({...opts, withChildren: true});
            // Affect parent link
            s.operationId = target.id;
            return s;
        }) || undefined;

        // Packets
        target.packets = this.packets && this.packets.map(packet => {
            const p = packet.asObject(opts);
            // Affect parent link
            p.operationId = target.id;
            return p;
        }) || undefined;

        // Fishing areas
        target.fishingAreas = this.fishingAreas && this.fishingAreas.map(value => value.asObject(opts)) || undefined;

        return target;
    }

    fromObject(source: any): OperationGroup {
        super.fromObject(source);
        this.hasCatch = source.hasCatch;
        this.comments = source.comments;
        this.tripId = source.tripId;
        this.rankOrderOnPeriod = source.rankOrderOnPeriod;
        this.metier = source.metier && Metier.fromObject(source.metier) || undefined;
        this.physicalGear = (source.physicalGear || source.physicalGearId) ? PhysicalGear.fromObject(source.physicalGear || {id: source.physicalGearId}) : undefined;

        // Measurements
        this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];
        this.gearMeasurements = source.gearMeasurements && source.gearMeasurements.map(Measurement.fromObject) || [];
        this.measurementValues = {
            ...MeasurementUtils.toMeasurementValues(this.measurements),
            ...MeasurementUtils.toMeasurementValues(this.gearMeasurements),
            ...(this.physicalGear && this.physicalGear.measurementValues),
            ...source.measurementValues // important: keep at last assignment
        };
        if (Object.keys(this.measurementValues).length === 0) {
            console.warn("Source as no measurement. Should never occur! ", source);
        }

        // Products
        this.products = source.products && source.products.map(Product.fromObject) || [];
        // Affect parent
        this.products.forEach(product => {
            product.parent = this;
            product.operationId = this.id;
        });

        // Samples
        this.samples = source.samples && source.samples.map(source => Sample.fromObject(source, {withChildren: true})) || [];
        // Affect parent
        this.samples.forEach(sample => {
            sample.operationId = this.id;
        });

        // Packets
        this.packets = source.packets && source.packets.map(Packet.fromObject) || [];
        // Affect parent
        this.packets.forEach(packet => {
            packet.parent = this;
        });

        // Fishing areas
        this.fishingAreas = source.fishingAreas && source.fishingAreas.map(FishingArea.fromObject) || undefined;

        return this;
    }

    equals(other: OperationGroup): boolean {
        return super.equals(other)
            || (
                this.metier.equals(other.metier) && ((!this.rankOrderOnPeriod && !other.rankOrderOnPeriod) || (this.rankOrderOnPeriod === other.rankOrderOnPeriod))
            );
    }
}

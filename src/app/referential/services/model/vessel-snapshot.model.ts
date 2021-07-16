import {Moment} from 'moment';
import {Department, Entity, EntityAsObjectOptions, EntityClass, fromDateISOString, IEntity, Person, ReferentialAsObjectOptions, ReferentialRef, toDateISOString} from '@sumaris-net/ngx-components';
import {Vessel} from '@app/vessel/services/model/vessel.model';
import {NOT_MINIFY_OPTIONS} from '@app/core/services/model/referential.model';


export interface IWithVesselSnapshotEntity<T, ID = number> extends IEntity<T, ID> {
  vesselSnapshot: VesselSnapshot;
}

@EntityClass({typename: 'VesselSnapshotVO', fromObjectReuseStrategy: 'clone'})
export class VesselSnapshot extends Entity<VesselSnapshot> {

  static fromObject: (source: any, opts?: any) => VesselSnapshot;

  static fromVessel(source: Vessel | any): VesselSnapshot {
    if (!source) return undefined;
    const target = new VesselSnapshot();
    target.fromObject({
      id: source.id,
      vesselType: source.vesselType,
      vesselStatusId: source.statusId,
      name: source.features && source.features.name,
      startDate: source.features && source.features.startDate,
      endDate: source.features && source.features.endDate,
      exteriorMarking: source.features && source.features.exteriorMarking,
      basePortLocation: source.features && source.features.basePortLocation,
      registrationId: source.registration && source.registration.id,
      registrationCode: source.registration && source.registration.registrationCode,
      registrationStartDate: source.registration && source.registration.startDate,
      registrationEndDate: source.registration && source.registration.endDate,
      registrationLocation: source.registration && source.registration.registrationLocation
    });
    return target;
  }

  program: ReferentialRef;
  vesselType: ReferentialRef;
  vesselStatusId: number;
  name: string;
  startDate: Moment;
  endDate: Moment;
  exteriorMarking: string;
  registrationId: number; // TODO remove this ?
  registrationCode: string;
  registrationStartDate: Moment;
  registrationEndDate: Moment;
  administrativePower: number;
  lengthOverAll: number;
  grossTonnageGt: number;
  grossTonnageGrt: number;
  basePortLocation: ReferentialRef;
  registrationLocation: ReferentialRef;
  creationDate: Moment;
  recorderDepartment: Department;
  recorderPerson: Person;
  comments: string;

  constructor() {
    super(VesselSnapshot.TYPENAME);
    this.vesselType = null;
    this.basePortLocation = null;
    this.registrationLocation = null;
    this.recorderDepartment = null;
    this.recorderPerson = null;
    this.program = null;
  }

 // TODO: Check if clone is needed
  clone(): VesselSnapshot {
    const target = new VesselSnapshot();
    target.fromObject(this);
    target.program = this.program && this.program.clone() || undefined;
    target.vesselType = this.vesselType && this.vesselType.clone() || undefined;
    target.basePortLocation = this.basePortLocation && this.basePortLocation.clone() || undefined;
    target.registrationLocation = this.registrationLocation && this.registrationLocation.clone() || undefined;
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.clone() || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.clone() || undefined;
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);

    target.program = this.program && this.program.asObject({ ...options, ...NOT_MINIFY_OPTIONS /*always keep for table*/ } as ReferentialAsObjectOptions) || undefined;
    target.vesselType = this.vesselType && this.vesselType.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.basePortLocation = this.basePortLocation && this.basePortLocation.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.registrationLocation = this.registrationLocation && this.registrationLocation.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);
    target.registrationStartDate = toDateISOString(this.registrationStartDate);
    target.registrationEndDate = toDateISOString(this.registrationEndDate);
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(options) || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(options) || undefined;

    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.exteriorMarking = source.exteriorMarking;
    this.registrationCode = source.registrationCode;
    this.name = source.name;
    this.comments = source.comments || undefined;
    this.vesselStatusId = source.vesselStatusId;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.registrationId = source.registrationId;
    this.registrationStartDate = fromDateISOString(source.registrationStartDate);
    this.registrationEndDate = fromDateISOString(source.registrationEndDate);
    this.administrativePower = source.administrativePower || undefined;
    this.lengthOverAll = source.lengthOverAll || undefined;
    this.grossTonnageGt = source.grossTonnageGt || undefined;
    this.grossTonnageGrt = source.grossTonnageGrt || undefined;
    this.creationDate = fromDateISOString(source.creationDate);
    this.vesselType = source.vesselType && ReferentialRef.fromObject(source.vesselType);
    this.basePortLocation = source.basePortLocation && ReferentialRef.fromObject(source.basePortLocation);
    this.registrationLocation = source.registrationLocation && ReferentialRef.fromObject(source.registrationLocation);
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    this.recorderPerson = source.recorderPerson && Person.fromObject(source.recorderPerson);
    this.program = source.program && ReferentialRef.fromObject(source.program);
  }
}

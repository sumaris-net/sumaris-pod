import {Moment} from "moment";
import {isNil, isNilOrBlank, isNotNil} from "../../../shared/functions";
import {NOT_MINIFY_OPTIONS, ReferentialAsObjectOptions, ReferentialRef, ReferentialUtils} from "../../../core/services/model/referential.model";
import {Entity, EntityAsObjectOptions, EntityUtils} from "../../../core/services/model/entity.model";
import {Department} from "../../../core/services/model/department.model";
import {Person} from "../../../core/services/model/person.model";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";
import {RootDataEntity} from "../../../data/services/model/root-data-entity.model";
import {EntityClass} from "../../../core/services/model/entity.decorators";

@EntityClass({typename: 'VesselVO'})
export class Vessel extends RootDataEntity<Vessel> {

  static fromObject: (source: any, opts?: any) => Vessel;

  vesselType: ReferentialRef = null;
  statusId: number = null;
  features: VesselFeatures = null;
  registration: VesselRegistration = null;

  constructor() {
    super(Vessel.TYPENAME);
  }

  clone(): Vessel {
    const target = new Vessel();
    this.copy(target);
    target.vesselType = this.vesselType && this.vesselType.clone() || undefined;
    target.program = this.program && this.program.clone() || undefined;
    target.features = this.features && this.features.clone() || undefined;
    target.registration = this.registration && this.registration.clone() || undefined;
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.clone() || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.clone() || undefined;

    return target;
  }

  copy(target: Vessel): Vessel {
    target.fromObject(this);
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.statusId = source.statusId;
    this.vesselType = source.vesselType && ReferentialRef.fromObject(source.vesselType);
    this.features = source.features && VesselFeatures.fromObject(source.features);
    this.registration = source.registration && VesselRegistration.fromObject(source.registration);
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
  }

  asObject(options?: ReferentialAsObjectOptions): any {
    const target: any = super.asObject(options);
    target.vesselType = this.vesselType && this.vesselType.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.features = this.features && !this.features.empty && this.features.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.registration = this.registration && !this.registration.empty && this.registration.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(options) || undefined;
    return target;
  }

  equals(other: Vessel): boolean {
    return super.equals(other)
      && (this.features.id === other.features.id || this.features.startDate.isSame(other.features.startDate))
      && (this.registration.id === other.registration.id || this.registration.startDate.isSame(other.registration.startDate));
  }
}

@EntityClass({typename: 'VesselFeaturesVO'})
export class VesselFeatures extends Entity<VesselFeatures> {

  static fromObject: (source: any, opts?: any) => VesselFeatures;

  name: string;
  startDate: Moment;
  endDate: Moment;
  exteriorMarking: string;
  administrativePower: number;
  lengthOverAll: number;
  grossTonnageGt: number;
  grossTonnageGrt: number;
  basePortLocation: ReferentialRef;
  creationDate: Moment;
  recorderDepartment: Department;
  recorderPerson: Person;
  comments: string;
  qualityFlagId: number;

  // Parent
  vesselId: number;

  constructor() {
    super(VesselFeatures.TYPENAME);
    this.basePortLocation = null;
    this.recorderDepartment = null;
    this.recorderPerson = null;
  }

  clone(): VesselFeatures {
    const target = new VesselFeatures();
    this.copy(target);
    target.basePortLocation = this.basePortLocation && this.basePortLocation.clone() || undefined;
    target.vesselId = this.vesselId || undefined;
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.clone() || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.clone() || undefined;
    return target;
  }

  copy(target: VesselFeatures): VesselFeatures {
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);

    target.vesselId = this.vesselId;
    target.basePortLocation = this.basePortLocation && this.basePortLocation.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(options) || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(options) || undefined;
    target.qualityFlagId = isNotNil(this.qualityFlagId) ? this.qualityFlagId : undefined;
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.exteriorMarking = source.exteriorMarking;
    this.name = source.name;
    this.comments = source.comments || undefined;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.administrativePower = source.administrativePower || undefined;
    this.lengthOverAll = source.lengthOverAll || undefined;
    this.grossTonnageGt = source.grossTonnageGt || undefined;
    this.grossTonnageGrt = source.grossTonnageGrt || undefined;
    this.creationDate = fromDateISOString(source.creationDate);
    this.vesselId  = source.vesselId;
    this.qualityFlagId = source.qualityFlagId;
    this.basePortLocation = source.basePortLocation && ReferentialRef.fromObject(source.basePortLocation);
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    this.recorderPerson = source.recorderPerson && Person.fromObject(source.recorderPerson);
  }

  get empty(): boolean {
    return isNil(this.id) && isNilOrBlank(this.exteriorMarking) && isNilOrBlank(this.name) && isNil(this.startDate);
  }
}

@EntityClass({typename: 'VesselRegistrationVO'})
export class VesselRegistration extends Entity<VesselRegistration> {

  static fromObject: (source: any, opts?: any) => VesselRegistration;

  vesselId: number = null;
  startDate: Moment = null;
  endDate: Moment = null;
  registrationCode: string = null;
  intRegistrationCode: string = null;
  registrationLocation: ReferentialRef = null;

  constructor() {
    super(VesselRegistration.TYPENAME);
  }

  clone(): VesselRegistration {
    const target = new VesselRegistration();
    this.copy(target);
    target.registrationLocation = this.registrationLocation && this.registrationLocation.clone() || undefined;
    return target;
  }

  copy(target: VesselRegistration): VesselRegistration {
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);

    target.registrationLocation = this.registrationLocation && this.registrationLocation.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);

    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.registrationCode = source.registrationCode;
    this.intRegistrationCode = source.intRegistrationCode;
    this.vesselId = source.vesselId;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.registrationLocation = source.registrationLocation && ReferentialRef.fromObject(source.registrationLocation) || undefined;
  }

  get empty(): boolean {
    return isNil(this.id) && isNilOrBlank(this.registrationCode) && isNilOrBlank(this.intRegistrationCode)
      && ReferentialUtils.isEmpty(this.registrationLocation)
      && isNil(this.startDate);
  }
}

import {Moment} from "moment/moment";
import {fromDateISOString, isNil, isNilOrBlank, isNotNil, toDateISOString} from "../../../shared/functions";
import {
  NOT_MINIFY_OPTIONS,
  ReferentialAsObjectOptions, ReferentialRef,
  ReferentialUtils
} from "../../../core/services/model/referential.model";
import {Entity, EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {Department} from "../../../core/services/model/department.model";
import {Person} from "../../../core/services/model/person.model";

export class Vessel extends Entity<Vessel> {

  static fromObject(source: any): Vessel {
    if (!source || source instanceof Vessel) return source;
    const res = new Vessel();
    res.fromObject(source);
    return res;
  }

  vesselType: ReferentialRef;
  statusId: number;
  // TODO? program
  features: VesselFeatures;
  registration: VesselRegistration;
  creationDate: Moment;
  recorderDepartment: Department;
  recorderPerson: Person;

  constructor() {
    super();
    this.__typename = 'VesselVO';
    this.vesselType = null;
    this.features = null;
    this.registration = null;
    this.recorderDepartment = null;
    this.recorderPerson = null;
  }

  clone(): Vessel {
    const target = new Vessel();
    this.copy(target);
    target.vesselType = this.vesselType && this.vesselType.clone() || undefined;
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

  asObject(options?: ReferentialAsObjectOptions): any {
    const target: any = super.asObject(options);

    target.vesselType = this.vesselType && this.vesselType.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.features = this.features && !this.features.empty && this.features.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.registration = this.registration && !this.registration.empty && this.registration.asObject({ ...options,  ...NOT_MINIFY_OPTIONS }) || undefined;
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(options) || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(options) || undefined;

    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.statusId = source.statusId;
    this.creationDate = fromDateISOString(source.creationDate);
    this.vesselType = source.vesselType && ReferentialRef.fromObject(source.vesselType);
    this.features = source.features && VesselFeatures.fromObject(source.features);
    this.registration = source.registration && VesselRegistration.fromObject(source.registration);
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    this.recorderPerson = source.recorderPerson && Person.fromObject(source.recorderPerson);
  }

  equals(other: Vessel): boolean {
    return super.equals(other)
      && (this.features.id === other.features.id || this.features.startDate.isSame(other.features.startDate))
      && (this.registration.id === other.registration.id || this.registration.startDate.isSame(other.registration.startDate));
  }
}

export class VesselFeatures extends Entity<VesselFeatures> {

  static fromObject(source: any): VesselFeatures {
    if (!source || source instanceof VesselFeatures) return source;
    const res = new VesselFeatures();
    res.fromObject(source);
    return res;
  }

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
    super();
    this.__typename = 'VesselFeaturesVO';
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

export class VesselRegistration extends Entity<VesselRegistration> {

  static fromObject(source: any): VesselRegistration {
    if (!source || source instanceof VesselRegistration) return source;
    const res = new VesselRegistration();
    res.fromObject(source);
    return res;
  }

  vesselId: number;
  startDate: Moment;
  endDate: Moment;
  registrationCode: string;
  intRegistrationCode: string;
  registrationLocation: ReferentialRef;

  constructor() {
    super();
    this.__typename = 'VesselRegistrationVO';
    this.registrationLocation = null;
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

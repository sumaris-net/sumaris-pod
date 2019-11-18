import {DataEntity} from "./model/base.model";

export { TripService } from './trip.service';
export { OperationService } from './operation.service';
export { ObservedLocationService } from './observed-location.service';
export { LandingService } from './landing.service';


export interface DataQualityService<T extends DataEntity<T>> {
  control(data: T): Promise<T>;
  validate(data: T): Promise<T>;
  unvalidate(data: T): Promise<T>;
  qualify(data: T, qualityFlagId: number): Promise<T>;

  canUserWrite(data: T): boolean;
}

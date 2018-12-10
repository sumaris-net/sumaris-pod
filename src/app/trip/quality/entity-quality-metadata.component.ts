import {Component, EventEmitter, Input, Output} from '@angular/core';
import {DataRootEntity, isNil, isNotNil, Trip} from '../services/trip.model';

// import fade in animation
import {fadeInAnimation} from '../../shared/material/material.animations';
import {AccountService} from "../../core/services/account.service";
import {TripService} from "../services/trip.service";

@Component({
  selector: 'entity-quality-metadata',
  templateUrl: './entity-quality-metadata.component.html',
  styleUrls: ['./entity-quality-metadata.component.scss'],
  animations: [fadeInAnimation]
})
export class EntityQualityMetadataComponent {

  data: DataRootEntity<any>;
  enable: boolean;
  canControl: boolean;
  canValidate: boolean;
  canUnvalidate: boolean;
  canQualify: boolean;

  @Input("value")
  set value(value: DataRootEntity<any>) {
    this.data = value;
    this.onValueChange();
  }
  get value(): DataRootEntity<any> {
    return this.data;
  }

  @Output()
  onChange = new EventEmitter<any>();

  constructor(
    protected accountService: AccountService,
    protected tripService: TripService
  ) {
    this.accountService.onLogin.subscribe(() => this.onValueChange());
  }

  async control() {
    if (this.data instanceof Trip) {
      await this.tripService.controlTrip(this.data);
      this.onChange.emit();
    }
  }

  async validate() {
    if (this.data instanceof Trip) {
      await this.tripService.validateTrip(this.data);
      this.onChange.emit();
    }
  }

  async unvalidate() {
    if (this.data instanceof Trip) {
      await this.tripService.unvalidateTrip(this.data);
      this.onChange.emit();
    }
  }

  qualify() {
    // TODO
  }

  protected onValueChange() {
    this.enable = this.data && isNotNil(this.data.id);
    if (!this.enable) {
      this.canControl = false;
      this.canValidate = false;
      this.canUnvalidate = false;
      this.canQualify = false;
    }
    else if (this.data instanceof Trip) {
      const canWrite = this.tripService.canUserWrite(this.data);
      const isSupervisor = this.accountService.isSupervisor();
      this.canControl = canWrite && isNil(this.data.controlDate);
      this.canValidate = canWrite && isSupervisor && isNotNil(this.data.controlDate) && isNil(this.data.validationDate);
      this.canUnvalidate = canWrite && isSupervisor && isNotNil(this.data.controlDate) && isNotNil(this.data.validationDate);
      this.canQualify = false;
    }
  }

}

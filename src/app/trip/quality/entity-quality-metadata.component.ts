import {Component, Input} from '@angular/core';
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
  canShow: boolean;
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

  constructor(
    protected accountService: AccountService,
    protected tripService: TripService
  ) {
    this.accountService.onLogin.subscribe(() => this.onValueChange());
  }

  async control() {
    if (this.data instanceof Trip) {
      await this.tripService.controlTrip(this.data);
      this.onValueChange();
    }
  }

  async validate() {
    if (this.data instanceof Trip) {
      await this.tripService.validateTrip(this.data);
      this.onValueChange();
    }
  }

  async unvalidate() {
    if (this.data instanceof Trip) {
      await this.tripService.unvalidateTrip(this.data);
      this.onValueChange();
    }
  }

  qualify() {
    // TODO
  }

  protected onValueChange() {
    this.canShow = this.data && isNotNil(this.data.id);
    if (!this.canShow) {
      this.canControl = false;
      this.canValidate = false;
      this.canUnvalidate = false;
      this.canQualify = false;
      return
    }

    if (this.data instanceof Trip) {
      const canWrite = this.tripService.canUserWrite(this.data);
      const isSupervisor = this.accountService.isSupervisor();
      this.canControl = canWrite && isNil(this.data.controlDate);
      this.canValidate = canWrite && isSupervisor && isNotNil(this.data.controlDate) && isNil(this.data.validationDate);
      this.canUnvalidate = canWrite && isSupervisor && isNotNil(this.data.controlDate) && isNotNil(this.data.validationDate);
      this.canQualify = false;
    }
  }

}

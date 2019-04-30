import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from '@angular/core';
import {DataRootEntity, isNil, isNotNil, Trip} from '../services/trip.model';
// import fade in animation
import {fadeInAnimation} from '../../shared/shared.module';
import {AccountService} from "../../core/core.module";
import {TripService} from "../services/trip.service";

@Component({
  selector: 'entity-quality-form',
  templateUrl: './entity-quality-form.component.html',
  styleUrls: ['./entity-quality-form.component.scss'],
  animations: [fadeInAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityQualityFormComponent{

  data: DataRootEntity<any>;
  loading: boolean = true;
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
  onChange = new EventEmitter<any>(true);

  @Output()
  onControl = new EventEmitter<Event>();

  constructor(
    protected accountService: AccountService,
    protected tripService: TripService,
    protected cd: ChangeDetectorRef
  ) {
    this.accountService.onLogin.subscribe(() => this.onValueChange());
  }

  async control(event: Event) {
    this.onControl.emit(event);

    if (event.defaultPrevented) return;

    if (this.data instanceof Trip) {
      console.debug("[quality] Mark trip as controlled...");
      await this.tripService.controlTrip(this.data);
      this.onChange.emit();
      this.markForCheck();
    }
  }

  async validate(event: Event) {
    this.onControl.emit(event);

    if (event.defaultPrevented) return;

    if (this.data instanceof Trip) {
      console.debug("[quality] Mark trip as validated...");
      await this.tripService.validateTrip(this.data);
      this.onChange.emit();
      this.markForCheck();
    }
  }

  async unvalidate(event) {
    if (this.data instanceof Trip) {
      await this.tripService.unvalidateTrip(this.data);
      this.onChange.emit();
      this.markForCheck();
    }
  }

  qualify(event) {
    // TODO
  }

  /* -- protected method -- */

  protected onValueChange() {
    this.loading = isNil(this.data) || isNil(this.data.id);
    if (this.loading) {
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
    this.markForCheck();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

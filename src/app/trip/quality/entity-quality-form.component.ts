import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import {DataRootEntity, isNil, isNotNil, QualityFlagIds, ReferentialRef, StatusIds, Trip} from '../services/trip.model';
// import fade in animation
import {fadeInAnimation, LoadResult} from '../../shared/shared.module';
import {AccountService} from "../../core/services/account.service";
import {DataQualityService} from "../services/trip.services";
import {QualityFlags, qualityFlagToColor} from "../../referential/services/model";
import * as moment from "moment";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {Observable} from "rxjs";
import {first, map} from "rxjs/operators";

@Component({
  selector: 'entity-quality-form',
  templateUrl: './entity-quality-form.component.html',
  styleUrls: ['./entity-quality-form.component.scss'],
  animations: [fadeInAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityQualityFormComponent implements OnInit {

  data: DataRootEntity<any>;
  loading = true;
  canControl: boolean;
  canValidate: boolean;
  canUnvalidate: boolean;
  canQualify: boolean;
  canUnqualify: boolean;

  $qualityFlags: Observable<ReferentialRef[]>;

  @Input("value")
  set value(value: DataRootEntity<any>) {
    this.data = value;
    this.onValueChange();
  }
  get value(): DataRootEntity<any> {
    return this.data;
  }

  @Input() dataService: DataQualityService<any>;

  @Output()
  onChange = new EventEmitter<any>(true);

  @Output()
  onControl = new EventEmitter<Event>();

  constructor(
    protected accountService: AccountService,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef
  ) {
    this.accountService.onLogin.subscribe(() => this.onValueChange());
  }

  ngOnInit(): void {

    if (!this.dataService) {
      throw new Error("Missing mandatory 'dataService' input!");
    }

    this.$qualityFlags = this.referentialRefService.watchAll(0, 100, 'id', 'asc', {
      entityName: 'QualityFlag',
      statusId: StatusIds.ENABLE
    }, {
      fetchPolicy: "cache-first"
    }).pipe(
        first(),
        map((res) => {
          const items = res && res.data || [];

          // Try to get i18n key instead of label
          items.forEach(flag => {
            flag.label = this.getI18nQualityFlag(flag.id) || flag.label;
          });

          return items;
        })

    );
  }

  async control(event: Event) {
    this.onControl.emit(event);

    if (event.defaultPrevented) return;

    if (this.data instanceof Trip) {
      console.debug("[quality] Mark trip as controlled...");
      await this.dataService.control(this.data);
      this.onChange.emit();
      this.markForCheck();
    }
  }

  async validate(event: Event) {
    this.onControl.emit(event);

    if (event.defaultPrevented) return;

    if (this.data instanceof Trip) {
      console.debug("[quality] Mark trip as validated...");
      await this.dataService.validate(this.data);
      this.onChange.emit();
      this.markForCheck();
    }
  }

  async unvalidate(event) {
    if (this.data instanceof Trip) {
      await this.dataService.unvalidate(this.data);
      this.onChange.emit();
      this.markForCheck();
    }
  }

  async qualify(event, qualityFlagId: number ) {
    if (this.data instanceof Trip) {
      await this.dataService.qualify(this.data, qualityFlagId);
      this.onChange.emit();
      this.markForCheck();
    }
  }

  getI18nQualityFlag(qualityFlagId: number, qualityFlags?: ReferentialRef[]) {
    // Get label from the input list, if any
    let qualityFlag: any = qualityFlags && qualityFlags.find(qf => qf.id === qualityFlagId);
    if (qualityFlag && qualityFlag.label) return qualityFlag.label;

    // Or try to compute a label from the model enumeration
    qualityFlag = qualityFlag || QualityFlags.find(qf => qf.id === qualityFlagId);
    return qualityFlag ? ('QUALITY.QUALITY_FLAGS.' + qualityFlag.label) : undefined;
  }

  qualityFlagToColor = qualityFlagToColor;

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
      const canWrite = this.dataService.canUserWrite(this.data);
      const isSupervisor = this.accountService.isSupervisor();
      this.canControl = canWrite && isNil(this.data.controlDate);
      this.canValidate = canWrite && isSupervisor && isNotNil(this.data.controlDate) && isNil(this.data.validationDate);
      this.canUnvalidate = canWrite && isSupervisor && isNotNil(this.data.controlDate) && isNotNil(this.data.validationDate);
      this.canQualify = canWrite && isSupervisor /*TODO && isQualifier */ && isNotNil(this.data.validationDate) && isNil(this.data.qualificationDate);
      this.canUnqualify = canWrite && isSupervisor && isNotNil(this.data.validationDate) && isNotNil(this.data.qualificationDate);
    }
    this.markForCheck();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

import {AppForm, Referential, StatusIds} from "../../core/core.module";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {ReferentialValidatorService} from "../services/referential.validator";
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";

export declare interface StatusValue {
  id: number;
  icon: string;
  label: string;
}

@Component({
  selector: 'app-referential-form',
  templateUrl: './referential.form.html',
  providers: [
    {provide: ValidatorService, useExisting: ReferentialValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReferentialForm extends AppForm<Referential> implements OnInit {

  private _statusList: StatusValue[] = [
    {
      id: StatusIds.ENABLE,
      icon: 'checkmark',
      label: 'REFERENTIAL.STATUS_ENUM.ENABLE'
    },
    {
      id: StatusIds.DISABLE,
      icon: 'close',
      label: 'REFERENTIAL.STATUS_ENUM.DISABLE'
    },
    {
      id: StatusIds.TEMPORARY,
      icon: 'warning',
      label: 'REFERENTIAL.STATUS_ENUM.TEMPORARY'
    }
  ];
  statusById: { [id: number]: StatusValue; };

  @Input() showError = true;
  @Input() entityName;

  @Input()
  set statusList(values: StatusValue[]) {
    this._statusList = values;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
  }

  get statusList(): StatusValue[] {
    return this._statusList;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: ValidatorService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getRowValidator());

  }

  ngOnInit() {
    super.ngOnInit();

    // Fill statusById
    if (this._statusList && !this.statusById) {
      this.statusById = {};
      this._statusList.forEach((status) => this.statusById[status.id] = status);
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

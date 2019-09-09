import {AppForm, Referential, StatusIds} from "../../core/core.module";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {ReferentialValidatorService} from "../services/referential.validator";
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";

@Component({
  selector: 'app-referential-form',
  templateUrl: './referential.form.html',
  providers: [
    {provide: ValidatorService, useExisting: ReferentialValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReferentialForm extends AppForm<Referential> implements OnInit {

  statusList: any[] = [
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
  statusById: any;

  @Input() showError = true;
  @Input() entityName;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: ValidatorService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getRowValidator());

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
  }

  ngOnInit() {
    super.ngOnInit();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

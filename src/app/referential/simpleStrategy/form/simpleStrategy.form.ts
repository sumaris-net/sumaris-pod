import {AppForm, Referential} from "../../../core/core.module";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {ReferentialValidatorService} from "../../services/validator/referential.validator";
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {DefaultStatusList, StatusValue} from "../../../core/services/model/referential.model";
import {ValidatorService} from "@e-is/ngx-material-table";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import { Program } from '../../services/model/program.model';
import {FormBuilder, FormGroup} from "@angular/forms";

@Component({
  selector: 'app-simpleStrategy-form',
  templateUrl: './simpleStrategy.form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: ValidatorService,
      useExisting: ReferentialValidatorService
    }
  ]
})
export class SimpleStrategyForm extends AppForm<Referential> implements OnInit {

  protected formBuilder: FormBuilder;
  private _statusList = DefaultStatusList;
  statusById: { [id: number]: StatusValue; };

  simpleStrategyForm: FormGroup;

  @Input() showError = true;
  @Input() entityName;
  @Input() program: Program;

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
    protected settings?: LocalSettingsService,
    protected cd?: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getRowValidator(), settings);
    this._enable = true;
  }

  ngOnInit() {
    super.ngOnInit();

    // Fill statusById
    if (this._statusList && !this.statusById) {
      this.statusById = {};
      this._statusList.forEach((status) => this.statusById[status.id] = status);
    }
  }

  setValue(data: Referential, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    super.setValue(data, opts);

    // Make sure to set entityName if set from Input()
    const entityNameControl = this.form.get('entityName');
    if (entityNameControl && this.entityName && entityNameControl.value !== this.entityName) {
      entityNameControl.setValue(this.entityName, opts);
    }
  }
  protected markForCheck() {
    if (this.cd) this.cd.markForCheck();
  }
}

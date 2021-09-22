import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {ReferentialValidatorService} from "../services/validator/referential.validator";
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {StatusList, Referential, IStatus, splitById} from '@sumaris-net/ngx-components';
import {ValidatorService} from "@e-is/ngx-material-table";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {AppForm}  from "@sumaris-net/ngx-components";

@Component({
  selector: 'app-referential-form',
  templateUrl: './referential.form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: ValidatorService,
      useExisting: ReferentialValidatorService
    }
  ]
})
export class ReferentialForm extends AppForm<Referential> implements OnInit {

  private _statusList = StatusList;
  statusById: { [id: number]: IStatus; };

  @Input() showError = true;
  @Input() showDescription = true;
  @Input() showComments = true;
  @Input() entityName;

  @Input()
  set statusList(values: IStatus[]) {
    this._statusList = values;

    // Fill statusById
    this.statusById = splitById(values);
  }

  get statusList(): IStatus[] {
    return this._statusList as IStatus[];
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: ValidatorService,
    protected settings?: LocalSettingsService,
    protected cd?: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getRowValidator(), settings);

  }

  ngOnInit() {
    super.ngOnInit();

    // Fill statusById, if not set by input
    if (this._statusList && !this.statusById) {
      this.statusById = splitById(this._statusList);
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

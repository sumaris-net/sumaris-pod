import {Component, Injector, OnInit, ViewChild} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AbstractControl, FormGroup, ValidationErrors} from "@angular/forms";
import {EditorDataServiceLoadOptions, isNil, isNotNil} from "../../../shared/shared.module";
import {AppEditorPage} from "../../../core/core.module";
import {Program, referentialToString} from "../../services/model";
import {ReferentialValidatorService} from "../../services/referential.validator";
import {ProgramService} from "../../services/program.service";
import {ReferentialForm} from "../../form/referential.form";
import {isNotNilOrBlank} from "../../../shared/functions";

@Component({
  selector: 'app-program',
  templateUrl: 'program.page.html',
  providers: [
    {provide: ValidatorService, useClass: ReferentialValidatorService}
  ],
})
export class ProgramPage extends AppEditorPage<Program> implements OnInit {

  @ViewChild('referentialForm') referentialForm: ReferentialForm;

  protected get form(): FormGroup {
    return this.referentialForm.form;
  }

  constructor(
    protected injector: Injector,
    protected programService: ProgramService
  ) {
    super(injector,
      Program,
      programService);

    this.defaultBackHref = "/referential/list?entity=Program";
  }

  ngOnInit() {
    super.ngOnInit();

    // Set entity name (required for referential form validator)
    this.referentialForm.entityName = 'Program';

    // Check label is unique
    this.form.get('label')
      .setAsyncValidators(async (control: AbstractControl) => {
        const label = control.enabled && control.value;
        return (await this.programService.existsByLabel(label)) ? {unique: true} : null;
      });
  }

  /* -- protected methods -- */

  updateViewState() {
    super.enable();

    if (!this.isNewData) {
      this.form.get('label').disable();
    }

    this.form.get('entityName').disable();
  }

  protected registerFormsAndTables() {
    this.registerForm(this.referentialForm);
  }

  protected setValue(data: Program) {
    this.referentialForm.value = data;
  }

  protected async computeTitle(data: Program): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return await this.translate.get('PROGRAM.NEW.TITLE').toPromise();
    }

    // Existing data
    return await this.translate.get('PROGRAM.EDIT.TITLE', data).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    if (this.referentialForm.invalid) return 0;
    return 0;
  }

  referentialToString = referentialToString;
}


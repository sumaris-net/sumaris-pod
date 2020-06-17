import {ValidatorService} from "angular4-material-table";
import {FormGroup} from "@angular/forms";

export interface IValidatorService<T> extends ValidatorService {

  getFormGroup(data?: T): FormGroup;
}

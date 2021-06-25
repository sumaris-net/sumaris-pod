import {ChangeDetectionStrategy, Component, Injector} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {LandingValidatorService} from "../services/validator/landing.validator";
import {LandingsTable} from "./landings.table";

@Component({
  selector: 'app-editable-landings-table',
  templateUrl: 'landings.table.html',
  styleUrls: ['landings.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: LandingValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditableLandingsTable extends LandingsTable {

  constructor(
    injector: Injector
  ) {
    super(injector);
  }
}


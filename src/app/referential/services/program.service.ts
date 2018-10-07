import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { PmfmStrategy } from "./model";
import { BaseDataService } from "../../core/services/data-service.class";
import { ErrorCodes } from "./errors";
import { Apollo } from "apollo-angular";

const LoadProgramPmfms: any = gql`
  query LoadProgramPmfms($program: String) {
    programPmfms(program: $program){
      id
      label
      name
      unit
      type
      minValue
      maxValue
      maximumNumberDecimals
      defaultValue
      acquisitionNumber
      isMandatory
      rankOrder    
      acquisitionLevel
      updateDate
      gears
      qualitativeValues {
        id
        label
        name
        statusId
        entityName
      }
    }
  }
`;


@Injectable()
export class ProgramService extends BaseDataService {

  constructor(
    protected apollo: Apollo
  ) {
    super(apollo);
  }

  /**
   * Load program pmfms
   */
  loadProgramPmfms(program: string, options?: {
    acquisitionLevel: string,
    gear?: string
  }): Observable<PmfmStrategy[]> {
    if (this._debug) console.debug(`[referential-service] Getting pmfms for program ${program}`);
    return this.watchQuery<{ programPmfms: PmfmStrategy[] }>({
      query: LoadProgramPmfms,
      variables: {
        program: program
      },
      error: { code: ErrorCodes.LOAD_PROGRAM_PMFMS_ERROR, message: "REFERENTIAL.ERROR.LOAD_PROGRAM_PMFMS_ERROR" }
    })
      .pipe(
        map((data) => (data && data.programPmfms || [])
          // Filter on acquisition level and gear
          .filter(p => !options || (
            (!options.acquisitionLevel || p.acquisitionLevel == options.acquisitionLevel)
            // Filter on gear (if PMFM has gears = compatible with all gears)
            && (!options.gear || !p.gears || !p.gears.length || p.gears.findIndex(g => g == options.gear) !== -1)
          ))
          // Convert into model
          .map(PmfmStrategy.fromObject)
          // Sort on rank order
          .sort((p1, p2) => p1.rankOrder - p2.rankOrder)
        )
      );

    // TODO: translate name/label using translate service ?
  }

}

import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { PmfmStrategy } from "./model";
import { BaseDataService } from "../../core/services/data-service.class";
import { ErrorCodes } from "./errors";
import { Apollo } from "apollo-angular";
import { ReferentialRef } from "../../core/services/model";
import { Fragments } from "../../trip/services/trip.queries";

const LoadProgramPmfms: any = gql`
  query LoadProgramPmfms($program: String) {
    programPmfms(program: $program){
      id
      pmfmId
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

const LoadProgramGears: any = gql`
  query LoadProgramGears($program: String) {
    programGears(program: $program){
      ...ReferentialFragment
    }
  }
  ${Fragments.referential}
`;

@Injectable()
export class ProgramService extends BaseDataService {

  constructor(
    protected apollo: Apollo
  ) {
    super(apollo);

    // -- For DEV only
    //this._debug = !environment.production;
  }

  /**
   * Watch program pmfms
   * @deprecated - use loadProgramPmfms
   */
  watchProgramPmfms(program: string, options?: {
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
        map((data) => {
          const pmfmIds = []; // used to avoid duplicated pmfms
          //if (options.acquisitionLevel == "SURVIVAL_TEST") console.debug("data.programPmfms:", data && data.programPmfms);
          const res = (data && data.programPmfms || [])
            // Filter on acquisition level and gear
            .filter(p =>
              pmfmIds.indexOf(p.pmfmId) == -1
              && (
                !options || (
                  (!options.acquisitionLevel || p.acquisitionLevel == options.acquisitionLevel)
                  // Filter on gear (if PMFM has gears = compatible with all gears)
                  && (!options.gear || !p.gears || !p.gears.length || p.gears.findIndex(g => g == options.gear) !== -1)
                  // Add to list of IDs
                  && pmfmIds.push(p.pmfmId)
                )
              ))
            // Convert into model
            .map(PmfmStrategy.fromObject);
          // Sort on rank order
          res.sort((p1, p2) => p1.rankOrder - p2.rankOrder);
          //if (options.acquisitionLevel == "SURVIVAL_TEST") console.debug("PMFM for " + options.acquisitionLevel, res);

          if (this._debug && res.length) console.debug(`[referential-service] ${res.length} pmfms found`, res);

          return res;
        })
      );

    // TODO: translate name/label using translate service ?
  }

  /**
   * Load program pmfms
   */
  async loadProgramPmfms(program: string, options?: {
    acquisitionLevel: string,
    gear?: string
  }): Promise<PmfmStrategy[]> {
    if (this._debug) console.debug(`[referential-service] Getting pmfms (program=${program}, acquisitionLevel=${options && options.acquisitionLevel}, gear=${options && options.gear})`);
    const data = await this.query<{ programPmfms: PmfmStrategy[] }>({
      query: LoadProgramPmfms,
      variables: {
        program: program
      },
      error: { code: ErrorCodes.LOAD_PROGRAM_PMFMS_ERROR, message: "REFERENTIAL.ERROR.LOAD_PROGRAM_PMFMS_ERROR" }
    });
    const pmfmIds = []; // used to avoid duplicated pmfms
    //if (options.acquisitionLevel == "SURVIVAL_TEST") console.debug("data.programPmfms:", data && data.programPmfms);
    const res = (data && data.programPmfms || [])
      // Filter on acquisition level and gear
      .filter(p =>
        pmfmIds.indexOf(p.pmfmId) == -1
        && (
          !options || (
            (!options.acquisitionLevel || p.acquisitionLevel == options.acquisitionLevel)
            // Filter on gear (if PMFM has gears = compatible with all gears)
            && (!options.gear || !p.gears || !p.gears.length || p.gears.findIndex(g => g == options.gear) !== -1)
            // Add to list of IDs
            && pmfmIds.push(p.pmfmId)
          )
        ))
      // Convert into model
      .map(PmfmStrategy.fromObject);
    // Sort on rank order
    res.sort((p1, p2) => p1.rankOrder - p2.rankOrder);
    //if (options.acquisitionLevel == "SURVIVAL_TEST") console.debug("PMFM for " + options.acquisitionLevel, res);

    return res;
    // TODO: translate name/label using translate service ?
  }


  /**
   * Load program gears
   */
  async loadGears(program: string): Promise<ReferentialRef[]> {
    if (this._debug) console.debug(`[referential-service] Getting gears for program ${program}`);
    const data = await this.query<{ programGears: ReferentialRef[] }>({
      query: LoadProgramGears,
      variables: {
        program: program
      },
      error: { code: ErrorCodes.LOAD_PROGRAM_GEARS_ERROR, message: "REFERENTIAL.ERROR.LOAD_PROGRAM_GEARS_ERROR" }
    })
    return (data && data.programGears || []).map(ReferentialRef.fromObject);
  }
}

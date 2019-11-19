import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {Department, EntityUtils, isNil, Person, VesselFeatures} from "./model";
import {
  EditorDataService,
  EditorDataServiceLoadOptions,
  LoadResult,
  TableDataService
} from "../../shared/shared.module";
import {BaseDataService} from "../../core/core.module";
import {map} from "rxjs/operators";
import {ErrorCodes} from "./errors";
import {GraphqlService} from "../../core/services/graphql.service";
import {ReferentialFragments} from "./referential.queries";
import {VesselFilter} from "./vessel-service";
import {AccountService} from "../../core/services/account.service";

export const VesselFeaturesFragments = {
    lightVesselFeatures: gql`fragment LightVesselFeaturesFragment on VesselFeaturesVO {
        id
        startDate
        endDate
        name
        exteriorMarking
        registrationCode
        administrativePower
        lengthOverAll
        grossTonnageGt
        grossTonnageGrt
        creationDate
        updateDate
        comments
        vesselId
        vesselType {
            ...ReferentialFragment
        }
        vesselStatusId
        basePortLocation {
            ...LocationFragment
        }
        registrationLocation {
            ...LocationFragment
        }
        recorderDepartment {
            ...LightDepartmentFragment
        }
        entityName
    }`,
    vesselFeatures: gql`fragment VesselFeaturesFragment on VesselFeaturesVO {
        id
        startDate
        endDate
        name
        exteriorMarking
        registrationCode
        registrationId
        registrationStartDate
        registrationEndDate
        administrativePower
        lengthOverAll
        grossTonnageGt
        grossTonnageGrt
        creationDate
        updateDate
        comments
        vesselId
        vesselType {
            ...ReferentialFragment
        }
        vesselStatusId
        basePortLocation {
            ...LocationFragment
        }
        registrationLocation {
            ...LocationFragment
        }
        recorderDepartment {
            ...LightDepartmentFragment
        }
        recorderPerson {
            ...LightPersonFragment
        }
        entityName
    }`,
};

const LoadAllQuery: any = gql`
    query VesselFeaturesHistory($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $vesselId: Int){
        vesselFeaturesHistory(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, vesselId: $vesselId){
            ...LightVesselFeaturesFragment
        }
    }
    ${VesselFeaturesFragments.lightVesselFeatures}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.referential}
`;
const SaveVessels: any = gql`
    mutation saveVessels($vessels:[VesselFeaturesVOInput]){
        saveVessels(vessels: $vessels){
            ...VesselFeaturesFragment
        }
    }
    ${VesselFeaturesFragments.vesselFeatures}
    ${ReferentialFragments.location}
    ${ReferentialFragments.lightDepartment}
    ${ReferentialFragments.lightPerson}
    ${ReferentialFragments.referential}
`;

@Injectable({providedIn: 'root'})
export class VesselFeaturesService
  extends BaseDataService
  implements TableDataService<VesselFeatures, VesselFilter>, EditorDataService<VesselFeatures, VesselFilter> {

  constructor(
    protected graphql: GraphqlService,
    private accountService: AccountService
  ) {
    super(graphql);
  }

  load(id: number, options?: EditorDataServiceLoadOptions): Promise<VesselFeatures> {
    throw new Error("Method not implemented.");
  }

  async save(vesselFeatures: VesselFeatures, options?: any): Promise<VesselFeatures> {

    // Prepare to save
    this.fillDefaultProperties(vesselFeatures);
    const isNew = isNil(vesselFeatures.id);

    // Transform into json
    const json = vesselFeatures.asObject();

    const now = Date.now();
    console.debug("[vessel-service] Saving vessel: ", json);

    await this.graphql.mutate<{ saveVessels: any }>({
      mutation: SaveVessels,
      variables: {
        vessels: [json]
      },
      error: {code: ErrorCodes.SAVE_VESSEL_ERROR, message: "VESSEL.ERROR.SAVE_VESSEL_ERROR"},
      update: (proxy, {data}) => {
        const savedVesselFeature = data && data.saveVessels && data.saveVessels[0];

        if (savedVesselFeature) {

          // Copy id and update Date
          if (savedVesselFeature !== vesselFeatures) {
            this.copyIdAndUpdateDate(savedVesselFeature, vesselFeatures);
            if (this._debug) console.debug(`[vessel-features-service] Vessel Feature saved in ${Date.now() - now}ms`, savedVesselFeature);
          }

          // Add to cache
          if (isNew && this._lastVariables.loadAll) {
            this.graphql.addToQueryCache(proxy, {
              query: LoadAllQuery,
              variables: this._lastVariables.loadAll
            }, 'vesselFeaturesHistory', savedVesselFeature);
          }

        }
      }
    });

    return vesselFeatures;

  }
  delete(data: VesselFeatures, options?: any): Promise<any> {
    throw new Error("Method not implemented.");
  }
  listenChanges(id: number, options?: any): Observable<VesselFeatures> {
    throw new Error("Method not implemented.");
  }

  /**
   * Load vessel features history
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: string,
           filter?: VesselFilter): Observable<LoadResult<VesselFeatures>> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'startDate',
      sortDirection: sortDirection || 'asc',
      vesselId: filter.vesselId
    };

    this._lastVariables.loadAll = variables;

    const now = Date.now();
    if (this._debug) console.debug("[vessel-features-history-service] Getting vessel features history using options:", variables);

    return this.graphql.watchQuery<{ vesselFeaturesHistory: any[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    })
      .pipe(
        map(({vesselFeaturesHistory}) => {
            const data = (vesselFeaturesHistory || []).map(VesselFeatures.fromObject);
            if (this._debug) console.debug(`[vessel-features-history-service] Vessel features history loaded in ${Date.now() - now}ms`, data);
            return {
              data: data
            };
          }
        )
      );
  }

  deleteAll(data: VesselFeatures[], options?: any): Promise<any> {
    throw new Error("Method not implemented.");
  }

  saveAll(data: VesselFeatures[], options?: any): Promise<VesselFeatures[]> {
    throw new Error("Method not implemented.");
  }

  /* -- protected methods -- */

  protected fillDefaultProperties(vessel: VesselFeatures): void {

    // If new
    // if (!vessel.id || vessel.id < 0) {

    const person: Person = this.accountService.account;

    // Recorder department
    if (person && person.department && (!vessel.recorderDepartment || vessel.recorderDepartment.id !== person.department.id)) {
      if (!vessel.recorderDepartment) {
        vessel.recorderDepartment = new Department();
      }
      vessel.recorderDepartment.id = person.department.id;
    }

    // Recorder person
    if (person && (!vessel.recorderPerson || vessel.recorderPerson.id !== person.id)) {
      if (!vessel.recorderPerson) {
        vessel.recorderPerson = new Person();
      }
      vessel.recorderPerson.id = person.id;
    }

    // }
  }

  copyIdAndUpdateDate(source: VesselFeatures | undefined, target: VesselFeatures) {

    EntityUtils.copyIdAndUpdateDate(source, target);

    target.vesselId = source && source.vesselId || target.vesselId;

  }
}

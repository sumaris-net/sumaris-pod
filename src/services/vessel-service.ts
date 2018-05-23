import { Injectable } from "@angular/core";
import gql from "graphql-tag";
import { Apollo } from "apollo-angular";
import { Observable } from "rxjs";
import { Trip, VesselFeatures, Person } from "./model";
import { DataService, BaseDataService } from "./data-service";
import { map } from "rxjs/operators";
import { Moment } from "moment";
import { DocumentNode } from "graphql";
import { ErrorCodes } from "./errors";
import { AccountService } from "./account-service";

export declare class VesselFilter {
  date?: Date|Moment;
  vesselId?: number;
  searchText?: string
}
const LoadAllQuery: DocumentNode = gql`
  query Vessels($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: VesselFilterVOInput){
    vessels(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      name
      exteriorMarking
      administrativePower
      lengthOverAll
      creationDate
      updateDate
      comments
      vesselId
      vesselTypeId
      basePortLocation {
        id
        label
        name
      }
    }
  }
`;
const LoadQuery: DocumentNode = gql`
  query Vessel($id: Int) {
    vessels(filter: {vesselId: $id}) {
      id
      name
      exteriorMarking
      administrativePower
      lengthOverAll
      creationDate
      updateDate
      comments
      vesselId
      vesselTypeId
      basePortLocation {
        id
        label
        name
      }
      recorderDepartment {
        id
        label
        name
      }
      recorderPerson {
        id
        firstName
        lastName
        department {
          id
          label
          name
        }
      }
    }
  }
`;

const SaveVessels: DocumentNode = gql`
  mutation saveVessels($vessels:[VesselFeaturesVOInput]){
    saveVessels(vessels: $vessels){
      id
      updateDate
    }
  }
`;

const DeleteVessels: DocumentNode = gql`
  mutation deleteVessels($ids:[Int]){
    deleteVessels(ids: $ids)
  }
`;

@Injectable()
export class VesselService extends BaseDataService implements DataService<VesselFeatures, VesselFilter>{

  constructor(
    protected apollo: Apollo,
    private accountService: AccountService
  ) {
    super(apollo);
  }

  /**
   * Load many vessels
   * @param offset 
   * @param size 
   * @param sortBy 
   * @param sortDirection 
   * @param filter 
   */
  loadAll(offset: number,
          size: number,
          sortBy?: string,
          sortDirection?: string,
          filter?: VesselFilter): Observable<VesselFeatures[]> {
    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'exteriorMarking',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    console.debug("[vessel-service] Getting vessels using options:", variables);
    return this.watchQuery<{vessels: any[]}>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_VESSELS_ERROR, message: "VESSEL.ERROR.LOAD_VESSELS_ERROR"}
    })
    .pipe(
      map((data) => (data && data.vessels || []).map(t => {
          const res = new VesselFeatures();
          res.fromObject(t);
          return res;
        })
      )
    );
  }

  load(id: number): Promise<VesselFeatures|null> {
    console.debug("[vessel-service] Loading vessel " + id);

    return this.query<{vessels: any}>({
      query: LoadQuery,
      variables: {
        id: id
      }
    })
    .then(data => {
      if (data && data.vessels) {
        const res = new VesselFeatures();
        res.fromObject(data.vessels[0]);
        return res;
      }
      return null;
    });
  }

  /**
   * Save many vessels
   * @param data 
   */
  saveAll(vessels: VesselFeatures[]): Promise<VesselFeatures[]> {

    if (!vessels) return Promise.resolve(vessels);

    // Fill default properties (as recorder department and person)
    vessels.forEach(t => this.fillDefaultProperties(t));

    let json = vessels.map(t => this.asObject(t));
    console.debug("[vessel-service] Saving vessels: ", json);

    return this.mutate<{saveVessels: any}>({
        mutation: SaveVessels,
        variables: {
          vessels: json
        },
        error: {code: ErrorCodes.SAVE_VESSELS_ERROR, message: "VESSEL.ERROR.SAVE_VESSELS_ERROR"}
      })
      .then(data => (data && data.saveVessels && vessels || Trip[0]).map(t => {
        const res = data.saveVessels.find(res => res.id == t.id);
        t.updateDate = res && res.updateDate || t.updateDate;
        return t;
      }) );
  }

  /**
   * Save a trip
   * @param data 
   */
  save(vessel: VesselFeatures): Promise<VesselFeatures> {

    // Prepare to save
    this.fillDefaultProperties(vessel);

    // Transform into json
    const json = this.asObject(vessel);

    console.debug("[vessel-service] Saving vessel: ", json);

    return this.mutate<{saveVessels: any}>({
        mutation: SaveVessels,
        variables: {
          vessels: [json]
        },
        error: {code: ErrorCodes.SAVE_VESSEL_ERROR, message: "VESSEL.ERROR.SAVE_VESSEL_ERROR"}
      })
      .then(data => {
        var res = data && data.saveVessels && data.saveVessels[0];
        vessel.updateDate = res && res.updateDate || vessel.updateDate;
        return vessel;
      });
  }

  deleteAll(vessels: VesselFeatures[]): Promise<any> {
    let ids = vessels && vessels
    .map(t => t.id)
    .filter(id => (id > 0));

    console.debug("[vessel-service] Deleting vessels... ids:", ids);

    return this.mutate<any>({
        mutation: DeleteVessels,
        variables: {
          ids: ids
        }
      });
  }

   /* -- protected methods -- */

   protected asObject(vessel: VesselFeatures): any {
    const copy:any = vessel.asObject();

    // If no vessel: set the default vessel type
    copy.vesselTypeId = !copy.vesselId ? 1/*TODO ?*/ : undefined;

    return copy;
  }

  protected fillDefaultProperties(vessel: VesselFeatures): void {

    // If new
    if (!vessel.id || vessel.id < 0) {

      const person: Person = this.accountService.account;

      // Recorder department
      if (person && !vessel.recorderDepartment.id && person.department) {
        vessel.recorderDepartment.id = person.department.id;
      }

      // Recorder person
      if (person && !vessel.recorderPerson.id) {
        vessel.recorderPerson.id = person.id;
      }
      
    }
  }
}

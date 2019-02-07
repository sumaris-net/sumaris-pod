import { Injectable } from "@angular/core";
 import gql from "graphql-tag";
 import { Apollo } from "apollo-angular";
 
 import { BaseDataService } from "./base.data-service.class";
 import { environment } from "../../../environments/environment";
import { Department } from "src/app/trip/services/trip.model";

 

/* ------------------------------------
 * GraphQL queries
 * ------------------------------------*/
// New auth challenge query
const DepartmentsQuery: any = gql`
query Departments {
  departments {
    logo
    name
    hasLogo
    siteUrl
    label
  }
}
`; 


@Injectable()
export class PodConfigService extends BaseDataService {
 
  constructor(
    protected apollo: Apollo,
  ) {
    super(apollo);

    //console.log("You've written a TypeScript class ");
   
  } 

  /**
  * get Logos for all Departments configured 
  */
   async getDepartments(): Promise<Department[]> {

    const defaultDep = environment.defaultDepartmentId;
    //TODO: filter 

    const data = await this.query<{ departments: Department[] } >({
      query: DepartmentsQuery,
      variables: {
      }
    }); 
    return data.departments;
  };
}



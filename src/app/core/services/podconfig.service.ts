import { Injectable } from "@angular/core";
 import gql from "graphql-tag";
 import { Apollo } from "apollo-angular";
 import { BaseDataService } from "./base.data-service.class";
 import { Referential, Configuration } from "./model";
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

const ConfigurationQuery: any = gql`
query confs {
  configuration{
    
    name
    label
    properties{
      name
      label
    }
    backGroundImages
  }
}
`;  


@Injectable()
export class PodConfigService extends BaseDataService {
 
  configuration: Configuration;

  constructor(
    protected apollo: Apollo,
  ) {
    super(apollo);

    //console.log("You've written a TypeScript class ");
   
  } 
  


  async getConfs() : Promise<Configuration> {
    const data = await this.query<{ configuration: Configuration} >({
      query: ConfigurationQuery,
      variables: { },
    }); 

    const res = Configuration.fromObject(data.configuration);

    return res;
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



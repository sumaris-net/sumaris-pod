import { Injectable } from "@angular/core";
 import gql from "graphql-tag";
 import { Apollo } from "apollo-angular";
 import { BaseDataService } from "./base.data-service.class";
 import { Referential, Configuration } from "./model";
 import { environment } from "../../../environments/environment";
import { Department } from "src/app/trip/services/trip.model";
import {ErrorCodes} from "./errors";
 
/* ------------------------------------
 * GraphQL queries
 * ------------------------------------*/
// New auth challenge query
const DepartmentsQuery: any = gql`
query Departments {
  departments {
    id
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
    id
    label
    name    
    logo
    defaultProgram
    backgroundImages
    partners {
      id
      logo
      siteUrl
    }
    properties
  }
}
`;  

const DeletePartner: any = gql`
  mutation deletePartner($app:String, $partner:String){
    deletePartner(app: $app,  partner: $partner)
  }
`;

const DeleteBackground: any = gql`
mutation deleteBG($app:String, $bg:String){
  deleteBG(app: $app,  bg: $bg)
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

  // async removeBackGround(){
  //   const data = await this.mutate<any>({
  //     mutation:  DeleteBackground,
  //     variables: { app: "ADAP", bg: "2"}
  //   }); 

  // }

  // async removePartnerLogo() : Promise<Configuration> {
  //   const data = await this.mutate<any>({
  //     mutation:  DeletePartner,
  //     variables: { app: "ADAP", partner: "2"}
  //   }); 
 
  //   return this.getConfs();
  // }

  // /**
  // * get Logos for all Departments configured 
  // */
  //  async getDepartments(): Promise<Department[]> {

  //   const defaultDep = environment.defaultDepartmentId;
  //   //TODO: filter 

  //   const data = await this.query<{ departments: Department[] } >({
  //     query: DepartmentsQuery,
  //     variables: {
  //     }
  //   }); 
  //   return data.departments;
  // };
}



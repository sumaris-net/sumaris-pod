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

const LoadQuery: any = gql`
query Configuration {
  configuration{
    id
    label
    name    
    smallLogo
    largeLogo
    backgroundImages
    partners {
      id
      label
      name
      logo
      siteUrl
    }
    properties
  }
}
`;  


@Injectable()
export class PodConfigService extends BaseDataService {
 
  data: Configuration;
   

  constructor(
    protected apollo: Apollo,
  ) {
    super(apollo);

    //console.log("You've written a TypeScript class ");
   
  } 
  


  async getConfs() : Promise<Configuration> {

    if (this.data) return Promise.resolve(this.data);

    try {
      const res = await this.query<{ configuration: Configuration} >({
        query: LoadQuery,
        variables: { },
      });

      if (res && res.configuration) {
        this.data = Configuration.fromObject(res && res.configuration);
        console.info("[config] Configuration loaded (from pod): ", this.data);
      }
    }
    catch(err) {
      console.error(err && err.message || err);
    }

    if (!this.data) this.data = Configuration.fromObject(environment as any);

    // Make sure name if filled
    this.data.label = this.data.label || environment.name;

    // Reset name if same
    if (this.data.name === this.data.label) this.data.name = undefined;

    return this.data;
  }

}



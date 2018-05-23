import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {Referential} from "./model";
import {DataService, BaseDataService} from "./data-service";
import {Apollo} from "apollo-angular";
import { DocumentNode } from "graphql";
import { ErrorCodes } from "./errors";

export declare class ReferentialFilter {
  entityName: string;
  label?: string;
  name?: string;
  levelId?: number;
  searchText?: string;
}
const LoadAllQuery: DocumentNode = gql`
  query Referenials($offset: Int, $size: Int, $sortBy: String, $sortDirection: String, $filter: ReferentialFilterVOInput){
    referentials(offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection, filter: $filter){
      id
      label
      name
      updateDate
    }
  }
`;

@Injectable()
export class ReferentialService extends BaseDataService implements DataService<Referential, ReferentialFilter>{

  constructor(
    protected apollo: Apollo
  ) {
    super(apollo);
  }

  loadAll( offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    filter?: ReferentialFilter): Observable<Referential[]> {

    const variables: any = {
      offset: offset || 0,
      size: size || 100,
      sortBy: sortBy || 'label',
      sortDirection: sortDirection || 'asc',
      filter: filter  
    };
    console.debug("[referential-service] Getting data from options:", variables);
    return this.watchQuery<{referentials: any[]}>({
      query: LoadAllQuery,
      variables: variables,
      error: {code: ErrorCodes.LOAD_REFERENTIALS_ERROR, message: "REFERENTIAL.ERROR.LOAD_REFERENTIALS_ERROR"}
    })
    .pipe(
      map((data) => (data && data.referentials || []).map(t => {
          const res = new Referential();
          res.fromObject(t);
          return res;
        })
      )
    );
  }

  saveAll(data: Referential[]): Promise<Referential[]> {
    return Promise.resolve(data);
  }

  deleteAll(data: Referential[]): Promise<any> {
    return Promise.resolve();
  }
}

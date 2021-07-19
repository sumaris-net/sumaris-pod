import {EntityClass}  from "@sumaris-net/ngx-components";
import {ReferentialRefFilter} from "./referential-ref.filter";
import {Metier} from "../model/taxon.model";
import {Moment} from "moment";
import {fromDateISOString, toDateISOString} from "@sumaris-net/ngx-components";
import {EntityAsObjectOptions}  from "@sumaris-net/ngx-components";

@EntityClass({typename: 'MetierFilterVO'})
export class MetierFilter extends ReferentialRefFilter {

  static fromObject: (source: any, opts?: any) => MetierFilter;

  constructor() {
      super();
      this.entityName = Metier.ENTITY_NAME;
  }

  // Add predoc properties
  programLabel: string = null;
  startDate: Moment = null;
  endDate: Moment = null;
  vesselId: number = null;
  excludedTripId: number = null;

  fromObject(source: any) {
    super.fromObject(source);
    this.entityName = source.entityName || Metier.ENTITY_NAME;
    this.programLabel = source.programLabel;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.vesselId = source.vesselId;
    this.excludedTripId = source.excludedTripId;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.programLabel = this.programLabel;
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);
    target.vesselId = this.vesselId;
    target.excludedTripId = this.excludedTripId;

    return target;
  }
}

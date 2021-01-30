import {Strategy} from "../services/model/strategy.model";
import {Moment} from "moment";
import {fromDateISOString} from "../../shared/dates";

export class DenormalizedStrategy extends Strategy<DenormalizedStrategy> {

  parameterGroups: string[];
  efforts: StrategyEffort[];
  effortByQuarter: {
    1?: StrategyEffort;
    2?: StrategyEffort;
    3?: StrategyEffort;
    4?: StrategyEffort;
  };

  constructor() {
    super();
    this.effortByQuarter = {}; // Init, for easier use in UI
  }

  clone(): DenormalizedStrategy {
    const target = new DenormalizedStrategy();
    target.fromObject(this);
    return target;
  }

}


export class StrategyEffort {

  static fromObject(value: any): StrategyEffort {
    if (!value || value instanceof StrategyEffort) return value;
    const target = new StrategyEffort();
    target.fromObject(value);
    return target;
  }

  strategyLabel: string;
  startDate: Moment;
  endDate: Moment;
  quarter: number;
  expectedEffort: number;
  realizedEffort: number;

  constructor() {
  }

  clone(): StrategyEffort {
    const target = new StrategyEffort();
    target.fromObject(this);
    return target;
  }

  fromObject(source: any) {
    if (!source) return;
    this.strategyLabel = source.strategy || source.strategyLabel;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.expectedEffort = source.expectedEffort;
    this.realizedEffort = source.realizedEffort;

    // Compute quarter (if possible = is same between start/end date)
    const startQuarter = this.startDate && this.startDate.quarter();
    const endQuarter = this.endDate && this.endDate.quarter();
    this.quarter = startQuarter === endQuarter ? startQuarter : undefined;
  }

  get realized(): boolean {
    return (!this.expectedEffort || (this.realizedEffort && this.realizedEffort >= this.expectedEffort));
  }

  get missingEffort(): number {
    return !this.expectedEffort ? undefined :
      // Avoid negative missing effort (when realized > expected)
      Math.max(0, this.expectedEffort - (this.realizedEffort || 0));
  }

}

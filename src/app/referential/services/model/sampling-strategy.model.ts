import { Strategy } from './strategy.model';
import { Moment } from 'moment';
import { EntityClass, fromDateISOString, isNil, ReferentialAsObjectOptions, toNumber } from '@sumaris-net/ngx-components';

export interface SamplingStrategyAsObjectOptions extends ReferentialAsObjectOptions {
  keepEffort: boolean; // fa  lse by default
}

@EntityClass({typename: 'SamplingStrategyVO'})
export class SamplingStrategy extends Strategy<SamplingStrategy, SamplingStrategyAsObjectOptions> {

  static fromObject: (source: any, opts?: any) => SamplingStrategy;

  static clone(source: any): SamplingStrategy {
    if (source instanceof SamplingStrategy) return source.clone();
    const res = new SamplingStrategy();
    res.fromObject(source);
    return res;
  }

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
    this.efforts = [];
    this.effortByQuarter = {}; // Init, for easier use in UI
  }

  // TODO : Check if clone is needed
  clone(): SamplingStrategy {
    const target = new SamplingStrategy();
    target.fromObject(this);
    return target;
  }

  fromObject(source: any) {
    const target = super.fromObject(source);
    this.efforts = source.efforts && source.efforts.map(StrategyEffort.fromObject) || [];
    this.effortByQuarter = source.effortByQuarter && Object.assign({}, source.effortByQuarter) || {};
    this.parameterGroups = source.parameterGroups || undefined;
    return target;
  }

  asObject(opts?: SamplingStrategyAsObjectOptions): any {
    const target = super.asObject(opts);

    // Remove effort
    if (!opts || opts.keepEffort !== true) {
      delete target.efforts;
      delete target.effortByQuarter;
      delete target.parameterGroups;
    }
    return target;
  }

  get hasRealizedEffort(): boolean {
    return (this.efforts || []).findIndex(e => e.hasRealizedEffort) !== -1;
  }

  get hasExpectedEffort(): boolean {
    return (this.efforts || []).findIndex(e => e.hasExpectedEffort) !== -1;
  }

  get hasLanding(): boolean {
    return (this.efforts || []).findIndex(e => e.hasLanding) !== -1;
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
  landingCount: number;

  constructor() {
  }

// TODO : Check if clone is needed
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
    this.expectedEffort = toNumber(source.expectedEffort);
    this.realizedEffort = toNumber(source.realizedEffort);
    this.landingCount = toNumber(source.landingCount);

    // Compute quarter (if possible = is same between start/end date)
    const startQuarter = this.startDate && this.startDate.quarter();
    const endQuarter = this.endDate && this.endDate.quarter();
    this.quarter = startQuarter === endQuarter ? startQuarter : undefined;
  }

  get realized(): boolean {
    return (!this.expectedEffort || (this.realizedEffort && this.realizedEffort >= this.expectedEffort));
  }

  get missingEffort(): number {
    return isNil(this.expectedEffort) ? undefined :
      // Avoid negative missing effort (when realized > expected)
      Math.max(0, this.expectedEffort - (this.realizedEffort || 0));
  }

  get hasRealizedEffort(): boolean {
    return (this.realizedEffort && this.realizedEffort > 0);
  }

  get hasExpectedEffort(): boolean {
    return (this.expectedEffort && this.expectedEffort > 0);
  }

  get hasLanding(): boolean {
    return (this.landingCount && this.landingCount > 0);
  }
}

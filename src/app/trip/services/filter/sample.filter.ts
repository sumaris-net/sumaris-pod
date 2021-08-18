import { DataEntityFilter } from '@app/data/services/model/data-filter.model';
import { Sample } from '../model/sample.model';
import { EntityAsObjectOptions, FilterFn, isNil } from '@sumaris-net/ngx-components';

export class SampleFilter extends DataEntityFilter<SampleFilter, Sample> {
  operationId?: number;
  landingId?: number;
  observedLocationId?: number;
  observedLocationIds?: number[];
  parent?: Sample;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.operationId = source.operationId;
    this.landingId = source.landingId;
    this.observedLocationId = source.observedLocationId;
    this.observedLocationIds = source.observedLocationIds;
    this.parent = source.parent;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    if (opts && opts.minify) {
      target.parentId = this.parent ? this.parent.id : undefined;
      delete target.parent;
    } else {
      target.parent = this.parent ? { id: this.parent.id, label: this.parent.label } : undefined;
    }
    return target;
  }

  asFilterFn<E extends Sample>(): FilterFn<E> {
    const filterFns: FilterFn<E>[] = [];

    const inheritedFn = super.asFilterFn();
    if (inheritedFn) filterFns.push(inheritedFn);

    // Landing
    if (isNil(this.landingId)) {
      filterFns.push((t) => t.landingId === this.landingId);
    }

    // Operation
    if (isNil(this.operationId)) {
      filterFns.push((t) => t.operationId === this.operationId);
    }

    // Parent
    if (isNil(this.parent)) {
      filterFns.push((t) => t.parentId === this.parent.id || this.parent.equals(t.parent));
    }

    if (!filterFns.length) return undefined;

    return (entity) => !filterFns.find((fn) => !fn(entity));
  }
}

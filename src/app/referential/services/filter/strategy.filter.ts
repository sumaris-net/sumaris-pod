import {EntityAsObjectOptions, EntityClass, EntityUtils, FilterFn, fromDateISOString, ReferentialRef, ReferentialUtils, toDateISOString} from '@sumaris-net/ngx-components';
import {BaseReferentialFilter} from '@app/referential/services/filter/referential.filter';
import {Strategy} from '@app/referential/services/model/strategy.model';
import {Moment} from 'moment';
import {TaxonNameRef} from '@app/referential/services/model/taxon-name.model';

@EntityClass({typename: 'StrategyFilterVO'})
export class StrategyFilter extends BaseReferentialFilter<StrategyFilter, Strategy> {

  static fromObject: (source: any, opts?: any) => StrategyFilter;

  startDate: Moment;
  endDate: Moment;
  department: ReferentialRef;
  location: ReferentialRef;
  taxonName: TaxonNameRef;
  analyticReference: ReferentialRef;

  parameterIds?: number[];
  periods?: any[];
  programId: number;

  fromObject(source: any) {
    super.fromObject(source);
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.department = source.department && ReferentialRef.fromObject(source.department) || undefined;
    this.location = source.location && ReferentialRef.fromObject(source.location) || undefined;
    this.taxonName = source.taxonName && TaxonNameRef.fromObject(source.taxonName) || undefined;
    this.analyticReference = source.analyticReference && ReferentialRef.fromObject(source.analyticReference) || undefined;

    this.parameterIds = source.parameterIds;
    this.periods = source.periods;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);

    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);

    if (opts && opts.minify) {
      target.departmentIds = ReferentialUtils.isNotEmpty(this.department) ? [this.department.id] : undefined;
      target.locationIds = ReferentialUtils.isNotEmpty(this.location) ? [this.location.id] : undefined;
      target.referenceTaxonIds = EntityUtils.isNotEmpty(this.taxonName, 'referenceTaxonId') ? [this.taxonName.referenceTaxonId] : undefined;
      target.analyticReferences = EntityUtils.isNotEmpty(this.analyticReference, 'label') ? [this.analyticReference.label] : undefined;
      delete target.department;
      delete target.location;
      delete target.taxonName;
      delete target.analyticReference;
    }
    else {
      target.department = this.department && this.department.asObject(opts);
      target.location = this.location && this.location.asObject(opts);
      target.taxonName = this.taxonName && this.taxonName.asObject(opts);
      target.analyticReference = this.analyticReference && this.analyticReference.asObject(opts);
    }

    return target;
  }

  buildFilter(): FilterFn<Strategy>[] {
    this.levelId = this.programId || this.levelId;
    const filterFns = super.buildFilter();

    // Filter by reference taxon
    /*if (isNotEmptyArray(this.referenceTaxonIds)) {
      console.warn('TODO: filter local strategy by reference taxon IDs: ', this.referenceTaxonIds);
      //filterFns.push(t => (t.appliedStrategies...includes(entity.statusId));
    }*/

    // TODO: any other attributes


    return filterFns;
  }
}

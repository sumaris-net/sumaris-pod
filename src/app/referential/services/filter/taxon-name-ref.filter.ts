import {BaseReferentialFilter} from './referential.filter';
import {TaxonNameRef} from '../model/taxon.model';
import {EntityAsObjectOptions, EntityClass, FilterFn, isNotEmptyArray, isNotNil} from '@sumaris-net/ngx-components';
import {StoreObject} from '@apollo/client/core';

@EntityClass({typename: 'TaxonNameFilterVO'})
export class TaxonNameRefFilter extends BaseReferentialFilter<TaxonNameRefFilter, TaxonNameRef> {

    static fromObject: (source: any, opts?: any) => TaxonNameRefFilter;

    taxonGroupId?: number;
    taxonGroupIds?: number[];

    fromObject(source: any, opts?: any) {
        super.fromObject(source);
        this.taxonGroupIds = source.taxonGroupIds;
        this.taxonGroupId = source.taxonGroupId;
    }

    asObject(opts?: EntityAsObjectOptions): StoreObject {
        const target = super.asObject(opts);
        if (opts && opts.minify) {
            target.taxonGroupIds = isNotNil(this.taxonGroupId) ? [this.taxonGroupId] : this.taxonGroupIds;
            delete target.taxonGroupId;
        } else {
            target.taxonGroupId = this.taxonGroupId;
            target.taxonGroupIds = this.taxonGroupIds;
        }
        return target;
    }

    asFilterFn<E extends TaxonNameRef>(): FilterFn<E> {
        const filterFns: FilterFn<E>[] = [];

        const inheritedFn = super.asFilterFn();
        if (inheritedFn) filterFns.push(inheritedFn);

        // Filter by taxon group id, or list of id
        if (isNotNil(this.taxonGroupId)) {
            filterFns.push(entity => entity.taxonGroupIds && entity.taxonGroupIds.includes(this.taxonGroupId));
        } else if (isNotEmptyArray(this.taxonGroupIds)) {
            const taxonGroupIds = this.taxonGroupIds;
            filterFns.push(entity => entity.taxonGroupIds && entity.taxonGroupIds.findIndex(id => taxonGroupIds.includes(id)) !== -1);
        }

        if (!filterFns.length) return undefined;

        return entity => !filterFns.find(fn => !fn(entity));
    }
}

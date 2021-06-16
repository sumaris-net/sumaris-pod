import {Injectable} from "@angular/core";
import {gql, WatchQueryFetchPolicy} from "@apollo/client/core";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {ErrorCodes} from "./trip.errors";
import {DataFragments, Fragments} from "./trip.queries";
import {GraphqlService}  from "@sumaris-net/ngx-components";
import {AccountService}  from "@sumaris-net/ngx-components";
import {SAVE_AS_OBJECT_OPTIONS} from "../../data/services/model/data-entity.model";
import {VesselSnapshotFragments} from "../../referential/services/vessel-snapshot.service";
import {Sale} from "./model/sale.model";
import {Sample} from "./model/sample.model";
import {SortDirection} from "@angular/material/sort";
import {BaseGraphqlService}  from "@sumaris-net/ngx-components";
import {FilterFn, IEntitiesService, LoadResult} from "@sumaris-net/ngx-components";
import {EntityUtils}  from "@sumaris-net/ngx-components";
import {environment} from "../../../environments/environment";
import {DataEntityFilter} from "../../data/services/model/data-filter.model";
import {RootDataEntityFilter} from "../../data/services/model/root-data-filter.model";
import {ReferentialRef}  from "@sumaris-net/ngx-components";
import {isNotNil} from "@sumaris-net/ngx-components";
import {ReferentialRefFilter} from "../../referential/services/filter/referential-ref.filter";

export const SaleFragments = {
  lightSale: gql`fragment LightSaleFragment_PENDING on SaleVO {
    id
    startDateTime
    endDateTime
    tripId
    comments
    updateDate
    saleLocation {
      ...LocationFragment
    }
    vesselSnapshot {
      ...LightVesselSnapshotFragment
    }
    recorderDepartment {
      ...LightDepartmentFragment
    }
  }
  ${Fragments.location}
  ${Fragments.lightDepartment}
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${Fragments.referential}
  `,
  sale: gql`fragment SaleFragment_PENDING on SaleVO {
    id
    startDateTime
    endDateTime
    tripId
    comments
    updateDate
    saleLocation {
      ...LocationFragment
    }
    measurements {
      ...MeasurementFragment
    }
    samples {
      ...SampleFragment
    }
    vesselSnapshot {
      ...LightVesselSnapshotFragment
    }
    recorderPerson {
        ...LightPersonFragment
    }
    recorderDepartment {
        ...LightDepartmentFragment
    }
    observers {
      ...LightPersonFragment
    }
  }
  ${Fragments.lightPerson}
  ${Fragments.lightDepartment}
  ${Fragments.measurement}
  ${Fragments.location}
  ${DataFragments.sample}
  ${VesselSnapshotFragments.lightVesselSnapshot}
  ${Fragments.referential}
  `
};


export class SaleFilter extends RootDataEntityFilter<SaleFilter, Sale> {

  static fromObject(source: any): SaleFilter {
    if (!source || source instanceof SaleFilter) return source;
    const target = new SaleFilter();
    target.fromObject(target);
    return target;
  }

  observedLocationId?: number;
  tripId?: number;

  fromObject(source: any) {
    super.fromObject(source);
    this.observedLocationId = source.observedLocationId;
    this.tripId = source.tripId;
  }

  asFilterFn<E extends Sale>(): FilterFn<E> {
    const filterFns: FilterFn<E>[] = [];

    const inheritedFn = super.asFilterFn();
    if (inheritedFn) filterFns.push(inheritedFn);

    if (isNotNil(this.observedLocationId)) {
      filterFns.push(t => t.observedLocationId === this.observedLocationId);
    }
    if (isNotNil(this.tripId)) {
      filterFns.push(t => t.tripId === this.tripId);
    }

    if (!filterFns.length) return undefined;

    return entity => !filterFns.find(fn => !fn(entity));
  }
}

const LoadAllQuery: any = gql`
  query Sales($filter: SaleFilterVOInput, $offset: Int, $size: Int, $sortBy: String, $sortDirection: String){
    sales(filter: $filter, offset: $offset, size: $size, sortBy: $sortBy, sortDirection: $sortDirection){
      ...LightSaleFragment
    }
  }
  ${SaleFragments.lightSale}
`;
const LoadQuery: any = gql`
  query Sale($id: Int) {
    sale(id: $id) {
      ...SaleFragment
    }
  }
  ${SaleFragments.sale}
`;
const SaveSales: any = gql`
  mutation saveSales($sales:[SaleVOInput]){
    saveSales(sales: $sales){
      ...SaleFragment
    }
  }
  ${SaleFragments.sale}
`;
const DeleteSales: any = gql`
  mutation deleteSales($ids:[Int]){
    deleteSales(ids: $ids)
  }
`;

const UpdateSubscription = gql`
  subscription updateSale($id: Int, $interval: Int){
    updateSale(id: $id, interval: $interval) {
      ...SaleFragment
    }
  }
  ${SaleFragments.sale}
`;

const sortByStartDateFn = (n1: Sale, n2: Sale) => { return n1.startDateTime.isSame(n2.startDateTime) ? 0 : (n1.startDateTime.isAfter(n2.startDateTime) ? 1 : -1); };

const sortByEndDateOrStartDateFn = (n1: Sale, n2: Sale) => {
  const d1 = n1.endDateTime || n1.startDateTime;
  const d2 = n2.endDateTime || n2.startDateTime;
  return d1.isSame(d2) ? 0 : (d1.isAfter(d2) ? 1 : -1);
};

@Injectable({providedIn: 'root'})
export class SaleService extends BaseGraphqlService<Sale, SaleFilter> implements IEntitiesService<Sale, SaleFilter>{

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql, environment);

    // -- For DEV only
    //this._debug = !environment.production;
  }

  /**
   * Load many sales
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           filter?: SaleFilter,
           options?: {
             fetchPolicy?: WatchQueryFetchPolicy
           }): Observable<LoadResult<Sale>> {

    const variables: any = {
      offset: offset || 0,
      size: size >= 0 ? size : 1000,
      sortBy: (sortBy !== 'id' && sortBy) || 'startDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };

    if (this._debug) console.debug("[sale-service] Loading sales... using options:", variables);
    return this.mutableWatchQuery<{ sales: Sale[] }>({
      queryName: 'LoadAll',
      arrayFieldName: 'sales',
      //TODO implement SaleFilter.searchFilter()
      // mutationFilterFn: SaleFilter.searchFilter(filter),
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_SALES_ERROR, message: "TRIP.SALE.ERROR.LOAD_SALES_ERROR" },
      fetchPolicy: options && options.fetchPolicy || 'cache-and-network'
    })
      .pipe(
        map((res) => {
          const data = (res && res.sales || []).map(Sale.fromObject);
          if (this._debug) console.debug(`[sale-service] Loaded ${data.length} sales`);

          // Compute rankOrderOnPeriod, when loading by parent entity
          if (offset === 0 && (size === -1) && filter && filter.observedLocationId) {
            if (offset === 0 && (size === -1)) {
              let rankOrder = 1;
              // apply a sorted copy (do NOT change original order), then compute rankOrder
              data.slice().sort(sortByEndDateOrStartDateFn)
                .forEach(o => o.rankOrder = rankOrder++);

              // sort by rankOrder (aka id)
              if (!sortBy || sortBy == 'id') {
                const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
                data.sort((a, b) => {
                  const valueA = a.rankOrder;
                  const valueB = b.rankOrder;
                  return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
                });
              }
            }
          }

          return {
            data: data,
            total: data.length
          };
        }));
  }

  load(id: number): Observable<Sale | null> {
    if (this._debug) console.debug("[sale-service] Loading sale {" + id + "}...");

    return this.graphql.watchQuery<{ sale: Sale }>({
      query: LoadQuery,
      variables: {
        id: id
      },
      error: { code: ErrorCodes.LOAD_SALE_ERROR, message: "TRIP.SALE.ERROR.LOAD_SALE_ERROR" }
    })
      .pipe(
        map(data => {
          if (data && data.sale) {
            const res = Sale.fromObject(data.sale);
            if (this._debug) console.debug("[sale-service] Sale {" + id + "} loaded", res);
            return res;
          }
          return null;
        })
      );
  }

  public listenChanges(id: number): Observable<Sale> {
    if (!id && id !== 0) throw "Missing argument 'id' ";

    if (this._debug) console.debug(`[sale-service] [WS] Listening changes for trip {${id}}...`);

    return this.graphql.subscribe<{ updateSale: Sale }, { id: number, interval: number }>({
      query: UpdateSubscription,
      variables: {
        id: id,
        interval: 10
      },
      error: {
        code: ErrorCodes.SUBSCRIBE_SALE_ERROR,
        message: 'TRIP.SALE.ERROR.SUBSCRIBE_SALE_ERROR'
      }
    })
      .pipe(
        map(data => {
          if (data && data.updateSale) {
            const res = Sale.fromObject(data.updateSale);
            if (this._debug) console.debug(`[sale-service] Sale {${id}} updated on server !`, res);
            return res;
          }
          return null; // deleted ?
        })
      );
  }

  /**
   * Save many sales
   * @param data
   */
  async saveAll(entities: Sale[], options?: any): Promise<Sale[]> {
    if (!entities) return entities;

    if (!options || !options.tripId) {
      console.error("[sale-service] Missing options.tripId");
      throw { code: ErrorCodes.SAVE_SALES_ERROR, message: "TRIP.SALE.ERROR.SAVE_SALES_ERROR" };
    }
    const now = Date.now();
    if (this._debug) console.debug("[sale-service] Saving sales...");

    // Compute rankOrder
    let rankOrder = 1;
    entities.sort(sortByEndDateOrStartDateFn).forEach(o => o.rankOrder = rankOrder++);

    // Transform to json
    const json = entities.map(t => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(t, options);
      return t.asObject(SAVE_AS_OBJECT_OPTIONS);
    });
    if (this._debug) console.debug("[sale-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ saveSales: Sale[] }>({
      mutation: SaveSales,
      variables: {
        sales: json
      },
      error: { code: ErrorCodes.SAVE_SALES_ERROR, message: "TRIP.SALE.ERROR.SAVE_SALES_ERROR" },
      update: (proxy, {data}) => {
        // Copy id and update date
        (data && data.saveSales && entities || [])
          .forEach(entity => {
            const savedSale = data.saveSales.find(res => entity.equals(res));
            this.copyIdAndUpdateDate(savedSale, entity);
          });

        if (this._debug) console.debug(`[sale-service] Sales saved and updated in ${Date.now() - now}ms`, entities);
      }
    });

    return entities;
  }

  /**
     * Save an sale
     * @param data
     */
  async save(entity: Sale): Promise<Sale> {
    const now = Date.now();
    if (this._debug) console.debug("[sale-service] Saving a sale...");

    // Fill default properties (as recorder department and person)
    this.fillDefaultProperties(entity, {});

    const isNew = !entity.id && entity.id !== 0;

    // Transform into json
    const json = entity.asObject(SAVE_AS_OBJECT_OPTIONS);
    if (this._debug) console.debug("[sale-service] Using minify object, to send:", json);

    await this.graphql.mutate<{ saveSales: Sale[] }>({
      mutation: SaveSales,
      variables: {
        sales: [json]
      },
      error: { code: ErrorCodes.SAVE_SALES_ERROR, message: "TRIP.SALE.ERROR.SAVE_SALE_ERROR" },
      update: (proxy, {data}) => {
        const savedEntity = data && data.saveSales && data.saveSales[0];
        if (savedEntity && savedEntity !== entity) {
          // Copy id and update Date
          this.copyIdAndUpdateDate(savedEntity, entity);
          if (this._debug) console.debug(`[sale-service] Sale saved and updated in ${Date.now() - now}ms`, entity);
        }

        // Update the cache
        if (isNew) {
          this.insertIntoMutableCachedQuery(proxy, {
            query: LoadAllQuery,
            data: savedEntity
          });
        }
      }
    });

    return entity;
  }

  /**
   * Save many sales
   * @param entities
   */
  async deleteAll(entities: Sale[]): Promise<any> {

    let ids = entities && entities
      .map(t => t.id)
      .filter(id => (id > 0));

    const now = Date.now();
    if (this._debug) console.debug("[sale-service] Deleting sales... ids:", ids);

    await this.graphql.mutate<any>({
      mutation: DeleteSales,
      variables: {
        ids
      },
      update: (proxy) => {
        // Remove from cache
        this.removeFromMutableCachedQueryByIds(proxy,{
          query: LoadAllQuery,
          ids
        });

        if (this._debug) console.debug(`[sale-service] Sale deleted in ${Date.now() - now}ms`);
      }
    });
  }


  asFilter(filter: Partial<SaleFilter>): SaleFilter {
    return SaleFilter.fromObject(filter);
  }

  /* -- protected methods -- */

  protected fillDefaultProperties(entity: Sale, options?: any) {

    // If new trip
    if (!entity.id || entity.id < 0) {

      // Fill Recorder department and person
      this.fillRecorderPersonAndDepartment(entity);

    }

    // Fill parent entity id
    if (!entity.observedLocationId && options) {
      entity.observedLocationId = options.observedLocationId;
    }
  }

  fillRecorderPersonAndDepartment(entity: Sale) {
    const person = this.accountService.person;

    // Recorder department
    if (person && person.department && !entity.recorderDepartment) {
      entity.recorderDepartment = person.department;
    }

    // Recorder person
    if (person && person.id && !entity.recorderPerson) {
      entity.recorderPerson = person;
    }
  }

  copyIdAndUpdateDate(source: Sale | undefined | any, target: Sale) {
    if (!source) return;

    // Update (id and updateDate)
    EntityUtils.copyIdAndUpdateDate(source, target);

    // Update samples (recursively)
    if (target.samples && source.samples) {
      this.copyIdAndUpdateDateOnSamples(source.samples, target.samples);
    }
  }

  /**
   * Copy Id and update, in sample tree (recursively)
   * @param sources
   * @param targets
   */
  copyIdAndUpdateDateOnSamples(sources: (Sample | any)[], targets: Sample[]) {
    // Update samples
    if (sources && targets) {
      targets.forEach(target => {
        const source = sources.find(json => target.equals(json));
        EntityUtils.copyIdAndUpdateDate(source, target);

        // Apply to children
        if (target.children && target.children.length) {
          this.copyIdAndUpdateDateOnSamples(sources, target.children);
        }
      });
    }
  }

  /* -- private -- */

}

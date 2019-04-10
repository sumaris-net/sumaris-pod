import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Apollo} from "apollo-angular";
import {Observable} from "rxjs-compat";
import {Person, Sale, Sample} from "./trip.model";
import {map} from "rxjs/operators";
import {TableDataService, LoadResult} from "../../shared/shared.module";
import {AccountService, BaseDataService, environment} from "../../core/core.module";
import {ErrorCodes} from "./trip.errors";
import {DataFragments, Fragments} from "./trip.queries";

export const SaleFragments = {
  lightSale: gql`fragment LightSaleFragment on SaleVO {
    id
    startDateTime
    endDateTime
    observedLocationId
    comments
    updateDate 
    location {
      ...LocationFragment
    } 
    vesselFeatures {
      ...VesselFeaturesFragment
    }
    recorderDepartment {
      ...RecorderDepartmentFragment
    }    
  }
  ${Fragments.location}
  ${Fragments.recorderDepartment}
  ${DataFragments.vesselFeatures}
  `,
  sale: gql`fragment SaleFragment on SaleVO {
    id
    startDateTime
    endDateTime
    observedLocationId
    comments
    updateDate   
    location {
      ...LocationFragment
    } 
    recorderPerson {
      ...RecorderPersonFragment
    }
    recorderDepartment {
      ...RecorderDepartmentFragment
    }
    measurements {
      ...MeasurementFragment
    }
    samples {
      ...SampleFragment
    }
    vesselFeatures {
      ...VesselFeaturesFragment
    }
    observers {
      ...RecorderPersonFragment
    }
  }
  ${Fragments.recorderPerson}
  ${Fragments.recorderDepartment}
  ${Fragments.measurement}
  ${Fragments.location}
  ${DataFragments.sample} 
  ${DataFragments.vesselFeatures} 
  `
};


export declare class SaleFilter {
  observedLocationId?: number;
  tripId?: number
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

@Injectable()
export class SaleService extends BaseDataService implements TableDataService<Sale, SaleFilter>{

  constructor(
    protected apollo: Apollo,
    protected accountService: AccountService
  ) {
    super(apollo);

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
           sortDirection?: string,
           filter?: SaleFilter): Observable<LoadResult<Sale>> {

    // Mock
    if (environment.mock) return Observable.of(this.getMockData());

    const variables: any = {
      offset: offset || 0,
      size: size || 1000,
      sortBy: (sortBy != 'id' && sortBy) || 'startDateTime',
      sortDirection: sortDirection || 'asc',
      filter: filter
    };
    this._lastVariables.loadAll = variables;

    if (this._debug) console.debug("[sale-service] Loading sales... using options:", variables);
    return this.watchQuery<{ sales?: Sale[] }>({
      query: LoadAllQuery,
      variables: variables,
      error: { code: ErrorCodes.LOAD_SALES_ERROR, message: "TRIP.SALE.ERROR.LOAD_SALES_ERROR" },
      fetchPolicy: 'cache-and-network'
    })
      .pipe(
        map((res) => {
          const data = (res && res.sales || []).map(Sale.fromObject);
          if (this._debug) console.debug(`[sale-service] Loaded ${data.length} sales`);

          // Compute rankOrderOnPeriod, by parent entity
          if (filter && filter.observedLocationId) {
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

          return {
            data: data,
            total: data.length
          };
        }));
  }

  load(id: number): Observable<Sale | null> {
    if (this._debug) console.debug("[sale-service] Loading sale {" + id + "}...");

    return this.watchQuery<{ sale: Sale }>({
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

    return this.subscribe<{ updateSale: Sale }, { id: number, interval: number }>({
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

    // Compute rankOrder
    let rankOrder = 1;
    entities.sort(sortByEndDateOrStartDateFn).forEach(o => o.rankOrder = rankOrder++);

    const json = entities.map(t => {
      // Fill default properties (as recorder department and person)
      this.fillDefaultProperties(t, options);
      return t.asObject(true/*minify*/);
    });

    const now = new Date();
    if (this._debug) console.debug("[sale-service] Saving sales...", json);

    const res = await this.mutate<{ saveSales: Sale[] }>({
      mutation: SaveSales,
      variables: {
        sales: json
      },
      error: { code: ErrorCodes.SAVE_SALES_ERROR, message: "TRIP.SALE.ERROR.SAVE_SALES_ERROR" }
    });

    // Copy id and update date
    (res && res.saveSales && entities || [])
      .forEach(entity => {
        const savedSale = res.saveSales.find(res => entity.equals(res));
        this.copyIdAndUpdateDate(savedSale, entity);
      });

    if (this._debug) console.debug("[sale-service] Sales saved and updated in " + (new Date().getTime() - now.getTime()) + "ms", entities);

    return entities;
  }

  /**
     * Save an sale
     * @param data
     */
  async save(entity: Sale): Promise<Sale> {


    // Fill default properties (as recorder department and person)
    this.fillDefaultProperties(entity, {});

    // Transform into json
    const json = entity.asObject(true/*minify*/);
    const isNew = !entity.id && entity.id !== 0;

    const now = new Date();
    if (this._debug) console.debug("[sale-service] Saving sale...", json);

    const res = await this.mutate<{ saveSales: Sale[] }>({
      mutation: SaveSales,
      variables: {
        sales: [json]
      },
      error: { code: ErrorCodes.SAVE_SALES_ERROR, message: "TRIP.SALE.ERROR.SAVE_SALE_ERROR" }
    });

    const savedSale = res && res.saveSales && res.saveSales[0];
    if (savedSale) {
      // Copy id and update Date
      this.copyIdAndUpdateDate(savedSale, entity);

      // Update the cache
      if (isNew && this._lastVariables.loadAll) {
        this.addToQueryCache({
          query: LoadAllQuery,
          variables: this._lastVariables.loadAll
        }, 'sales', entity.asObject());
      }
    }

    if (this._debug) console.debug("[sale-service] Sale saved and updated in " + (new Date().getTime() - now.getTime()) + "ms", entity);

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

    const now = new Date();
    if (this._debug) console.debug("[sale-service] Deleting sales... ids:", ids);

    await this.mutate<any>({
      mutation: DeleteSales,
      variables: {
        ids: ids
      }
    });

    // Remove from cache
    if (this._lastVariables.loadAll) {
      this.removeToQueryCacheByIds({
        query: LoadAllQuery,
        variables: this._lastVariables.loadAll
      }, 'sales', ids);
    }

    if (this._debug) console.debug("[sale-service] Sale deleted in " + (new Date().getTime() - now.getTime()) + "ms");
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
    const person: Person = this.accountService.account;

    if (!entity.recorderDepartment || !entity.recorderDepartment.id) {
      // Recorder department
      if (person && person.department) {
        entity.recorderDepartment.id = person.department.id;
      }
    }

    // Recorder person
    if (!entity.recorderPerson || !entity.recorderPerson.id) {
      entity.recorderPerson.id = person.id;
    }
  }

  copyIdAndUpdateDate(source: Sale | undefined | any, target: Sale) {
    if (!source) return;

    // Update (id and updateDate)
    target.id = source.id || target.id;
    target.updateDate = source.updateDate || target.updateDate;
    target.dirty = false;

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
        target.id = source && source.id || target.id;
        target.updateDate = source && source.updateDate || target.updateDate;
        target.creationDate = source && source.creationDate || target.creationDate;
        target.dirty = false;

        // Apply to children
        if (target.children && target.children.length) {
          this.copyIdAndUpdateDateOnSamples(sources, target.children);
        }
      });
    }
  }

  /* -- private -- */

  getMockData(): LoadResult<Sale> {
    const recorderPerson = {id: 1, firstName:'Jacques', lastName: 'Dupond'};
    const observers = [recorderPerson];
    const location = {id: 30, label:'DZ', name:'Douarnenez'};

    const data = [
      Sale.fromObject({
        id:2,
        program:  {id: 11, label:'ADAP-CONTROLE', name:'Contrôle en criée'},
        vesselFeatures: {vesselId:1, exteriorMarking: 'FRA000141001', name: 'Navire 1'},
        startDateTime: '2019-01-01T03:50:00Z',
        saleLocation: location,
        saleType: {id: 1, label: 'Criée', name: 'Criée'},
        recorderPerson: recorderPerson,
        observers: observers
      }),
      Sale.fromObject({
        id:3,
        program:  {id: 11, label:'ADAP-CONTROLE', name:'Contrôle en criée'},
        vesselFeatures: {vesselId:1, exteriorMarking: 'FRA000141002', name: 'Navire 2'},
        startDateTime: '2019-01-01T04:15:00Z',
        saleLocation: location,
        saleType: {id: 1, label: 'Criée', name: 'Criée'},
        recorderPerson: recorderPerson,
        observers: observers
      })
    ];

    return {
      total: data.length,
      data
    };
  }
}

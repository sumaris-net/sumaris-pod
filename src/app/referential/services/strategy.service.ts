import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Observable} from "rxjs";
import {LoadResult, EntitiesService, EntityService} from "../../shared/shared.module";
import {BaseEntityService} from "../../core/core.module";
import {ErrorCodes} from "./errors";

import {GraphqlService} from "../../core/services/graphql.service";
import {SortDirection} from "@angular/material/sort";
import { Strategy } from './model/strategy.model';
import { StrategyFilter } from '../strategy/strategies.table';
import { EntitiesServiceWatchOptions, EntityServiceLoadOptions } from 'src/app/shared/services/entity-service.class';

const StrategyFragments = {
  strategyNextLabel:  gql`fragment StrategyNextLabelFragment on String {
    id
    label
    name
    entityName
    __typename
  }`,
}

const FindStrategyNextLabel: any = gql`
  query SuggestedStrategyNextLabelQuery($id: Int, $labelPrefix: String, $nbDigit: Int){
    suggestedStrategyNextLabel(programId: $programId, labelPrefix: $labelPrefix, nbDigit: $nbDigit){
      ...StrategyNextLabelFragment
    }
  }
  ${StrategyFragments.strategyNextLabel}
`;

@Injectable({providedIn: 'root'})
export class StrategyService extends BaseEntityService implements EntitiesService<Strategy, StrategyFilter>, EntityService<Strategy> {

  constructor(
    protected graphql: GraphqlService,
  ) {
    super(graphql);
    if (this._debug) console.debug('[program-service] Creating service');
  }

  load(id: number, options?: EntityServiceLoadOptions): Promise<Strategy> {
    throw new Error('Method not implemented.');
  }
  save(data: Strategy, options?: any): Promise<Strategy> {
    throw new Error('Method not implemented.');
  }
  delete(data: Strategy, options?: any): Promise<any> {
    throw new Error('Method not implemented.');
  }
  listenChanges(id: number, options?: any): Observable<Strategy> {
    throw new Error('Method not implemented.');
  }
  watchAll(offset: number, size: number, sortBy?: string, sortDirection?: SortDirection, filter?: StrategyFilter, options?: EntitiesServiceWatchOptions): Observable<LoadResult<Strategy>> {
    throw new Error('Method not implemented.');
  }
  saveAll(data: Strategy[], options?: any): Promise<Strategy[]> {
    throw new Error('Method not implemented.');
  }
  deleteAll(data: Strategy[], options?: any): Promise<any> {
    throw new Error('Method not implemented.');
  }

  async findStrategyNextLabel() {
    if (this._debug) console.debug(`[strategy-service] Loading strategy next label...`);

    const res = await this.graphql.query<{ label: any }>({
      query: FindStrategyNextLabel,
      variables: {
        id: id
      },
      error: {code: ErrorCodes.LOAD_PROGRAM_ERROR, message: "PROGRAM.STRATEGY.ERROR.LOAD_STRATEGY_LABEL_ERROR"}
    });

    return res && res.label && Strategy.fromObject(res.label);
  }
}

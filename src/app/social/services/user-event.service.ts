import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {EntitiesService, EntityServiceLoadOptions, isNil, isNilOrBlank, LoadResult} from "../../shared/shared.module";
import {BaseEntityService, Entity, EntityUtils} from "../../core/core.module";
import {ErrorCodes} from "./errors";
import {AccountService} from "../../core/services/account.service";
import {GraphqlService} from "../../core/services/graphql.service";
import {environment} from "../../../environments/environment";
import {Observable, of} from "rxjs";
import {UserEvent, UserEventTypes} from "./model/user-event.model";
import {SocialFragments} from "./social.fragments";
import {SortDirection} from "@angular/material/sort";
import {EntitiesServiceWatchOptions, Page} from "../../shared/services/entity-service.class";
import {map} from "rxjs/operators";
import {toNumber} from "../../shared/functions";
import {IEntity} from "../../core/services/model/entity.model";

export class UserEventFilter {
  issuer?: string;
  recipient?: string;
}

const SaveQuery: any = gql`
  mutation SaveUserEvent($userEvent: UserEventVOInput){
    saveUserEvent(userEvent: $userEvent){
      ...UserEventFragment
    }
  }
  ${SocialFragments.userEvent}
`;

const LoadAllQuery: any = gql`
  query UserEvents($filter: UserEventFilterVOInput, $page: PageInput){
    userEvents(filter: $filter, page: $page){
      ...LightUserEventFragment
    }
  }
  ${SocialFragments.lightUserEvent}
`;


const LoadAllWithContentQuery: any = gql`
  query UserEventsWithContent($filter: UserEventFilterVOInput, $page: PageInput){
    userEvents(filter: $filter, page: $page){
      ...UserEventFragment
    }
  }
  ${SocialFragments.userEvent}
`;

export declare interface UserEventWatchOptions extends EntitiesServiceWatchOptions {
  withContent?: boolean; // Default to false
}

@Injectable({providedIn: 'root'})
export class UserEventService extends BaseEntityService<UserEvent>
  implements EntitiesService<UserEvent, UserEventFilter, UserEventWatchOptions> {

  constructor(
    protected graphql: GraphqlService,
    protected accountService: AccountService
  ) {
    super(graphql);

    // For DEV only
    this._debug = !environment.production;
  }

  saveAll(data: UserEvent[], options?: any): Promise<UserEvent[]> {
    return Promise.all(data
      .map(entity => this.save(entity, options))
    );
  }

  deleteAll(data: UserEvent[], options?: any): Promise<any> {
    return Promise.all(data
      .map(entity => this.delete(entity, options))
    );
  }

  /**
   *
   * @param offset
   * @param size
   * @param sortBy
   * @param sortDirection
   * @param filter
   * @param options
   * @deprecated use watchPage() instead
   */
  watchAll(offset: number,
           size: number,
           sortBy?: string,
           sortDirection?: SortDirection,
           filter?: UserEventFilter,
           options?: UserEventWatchOptions): Observable<LoadResult<UserEvent>> {
    return this.watchPage(<Page>{offset, size, sortBy, sortDirection}, filter, options);
  }

  watchPage(page: Page,
            filter?: UserEventFilter,
            options?: UserEventWatchOptions): Observable<LoadResult<UserEvent>> {

    let now = this._debug && Date.now();
    if (this._debug) console.debug("[user-event-service] Loading user events...", filter);

    filter = filter || {};

    // Force recipient to current issuer, if not admin and not specified
    if (isNilOrBlank(filter.recipient) || !this.accountService.isAdmin()) {
      const recipient = this.accountService.account.pubkey;
      if (recipient !== filter.recipient) {
        console.warn("[user-events-service] Force user event filter.recipient="+ recipient);
        filter.recipient = recipient;
      }
    }

    const withContent = options && options.withContent === true;

    return this.mutableWatchQuery<{userEvents: any; userEventCount: number}>({
      queryName: 'LoadAll',
      query: withContent ? LoadAllWithContentQuery : LoadAllQuery,
      variables: {
        page: {
          sortBy: 'updateDate',
          sortDirection: 'DESC',
          ...page
        },
        filter
      },
      arrayFieldName: "userEvents",
      totalFieldName: "userEventCount",
      error: {code: ErrorCodes.LOAD_USER_EVENTS_ERROR, message: "SOCIAL.ERROR.LOAD_USER_EVENTS_ERROR"},
      fetchPolicy: options && options.fetchPolicy || undefined
    })
    .pipe(
      map(res => {
        const data = res && res.userEvents.map(UserEvent.fromObject);
        if (now) {
          console.debug(`[user-event-service] ${data.length} user events loaded in ${Date.now() - now}ms`);
          now = null;
        }
        return {
          data,
          total: res && toNumber(res.userEventCount, data.length)
        };
      })
    );
  }

  /**
   * Save a userEvent entity
   * @param entity
   */
  async save(entity: UserEvent, options?: EntityServiceLoadOptions): Promise<UserEvent> {

    this.fillDefaultProperties(entity);

    // Transform into json
    const isNew = isNil(entity.id);
    const json = entity.asObject();

    const now = Date.now();
    if (this._debug) console.debug(`[user-event-service] Saving user event...`, json);

    await this.graphql.mutate<{ saveUserEvent: any }>({
      mutation: SaveQuery,
      variables: {
        userEvent: json
      },
      error: { code: ErrorCodes.SAVE_USER_EVENT_ERROR, message: "SOCIAL.ERROR.SAVE_USER_EVENT_ERROR" },
      update: (proxy, {data}) => {
        // Update entity
        const savedEntity = data && data.saveUserEvent;
        if (savedEntity) {
          if (this._debug) console.debug(`[user-event-service] User event saved in ${Date.now() - now}ms`, entity);
          this.copyIdAndUpdateDate(savedEntity, entity);

          // Add to cache
          if (isNew) {
            this.insertIntoMutableCachedQuery(proxy,{
              query: LoadAllQuery,
              data: savedEntity
            });
          }
        }
      }
    });

    return entity;
  }

  /**
   * Delete userEvent entities
   */
  async delete(entity: UserEvent, options?: any): Promise<any> {
    throw new Error('Not implemented yet');
  }

  listenChanges(id: number, options?: any): Observable<UserEvent | undefined> {
    // TODO
    console.warn("TODO: implement listen changes on user events");
    return of();
  }

  sendDataForDebug<T extends IEntity<any>>(data: T): Promise<UserEvent> {
    const userEvent = new UserEvent();
    userEvent.eventType = UserEventTypes.DEBUG_DATA;

    if (typeof data === 'string'){
      userEvent.content = data;
    }
    // Serialize content into string
    else if (typeof data === 'object'){
      if (data instanceof Entity) {
        userEvent.content = JSON.stringify(data.asObject({keepTypename: true, keepLocalId: true, minify: false}));
      }
      else {
        userEvent.content = JSON.stringify(data);
      }
    }
    return this.save(userEvent);
  }

  /* -- protected methods -- */

  protected fillDefaultProperties(entity: UserEvent) {
    entity.issuer = this.accountService.account.pubkey;

    // TODO: compute hash (using cryptoService)
    // TODO: compute sign
    console.warn('TODO: sign user event before sending');
  }

  protected copyIdAndUpdateDate(source: UserEvent, target: UserEvent) {
    EntityUtils.copyIdAndUpdateDate(source, target);
  }
}

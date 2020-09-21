import {ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {LandingValidatorService} from "../../trip/services/validator/landing.validator";
import {fadeInOutAnimation} from "../../shared/material/material.animations";
import {UserEventService} from "../services/user-event.service";
import {AccountService} from "../../core/services/account.service";
import {BehaviorSubject, Observable, Subject} from "rxjs";
import {UserEvent, UserEventTypes} from "../services/model/user-event.model";
import {map, takeUntil, tap} from "rxjs/operators";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {TranslateService} from "@ngx-translate/core";
import {firstNotNilPromise} from "../../shared/observables";
import {isNil, toBoolean} from "../../shared/functions";
import {UserProfileLabel} from "../../core/services/model/person.model";
import {Moment} from "moment";

const DEFAULTS_BY_EVENT_TYPE: {[key: string]: Partial<UserEventItem>} = {
  "DEBUG_DATA": {icon: "pin"},
  "INBOX_MESSAGE": {icon: "pin"},
}
const DEFAULTS_BY_ICON_BY_CONTENT_TYPENAME = {
  "TripVO": {
    icon: null,
    matIcon: "explore",
    action: 'importDebugData'
  }
}

export interface UserEventItem {
  title: string;
  path?: string;
  action?: string | any;
  icon?: string;
  matIcon?: string;
  color?: string,
  cssClass?: string;
  time?: Moment;

  // A config property, to override the title
  titleProperty?: string;
  titleArgs?: {[key: string]: string};
}

@Component({
  selector: 'app-user-events-list',
  //styleUrls: ['user-events.component.scss'],
  templateUrl: './user-events.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
  })
export class UserEventsComponent implements OnInit, OnDestroy{

  loading = true;
  $events = new BehaviorSubject<UserEventItem[]>([]);
  $stopWatching = new Subject<void>();
  dateTimePattern: string;


  @Input() recipient: string;

  @Input() showContent: boolean;

  constructor(
    private accountService: AccountService,
    private userEventService: UserEventService,
    private translate: TranslateService
  ) {

  }

  async ngOnInit() {
    // Load date/time pattern
    this.dateTimePattern = this.translate.instant('COMMON.DATE_TIME_PATTERN');

    this.showContent = toBoolean(this.showContent, false);

    this.start();
  }

  ngOnDestroy() {
    this.$stopWatching.next();
    this.$stopWatching.unsubscribe();
  }

  async start() {
    // Waiting account to be ready
    await this.accountService.ready();

    // Load data
    await this.load();
  }

  async load() {
    this.loading = true;
    this.$stopWatching.next();

    const page = {offset: 0, size: 20};
    const filter = {recipient: this.recipient};

    // TODO: manage more
    this.userEventService.watchPage(page,filter, {
      withContent: this.showContent
    })
      .pipe(
        takeUntil(this.$stopWatching),
        map(res => (res && res.data || []).map(e => this.toUserEventItem(e))),
      ).subscribe(events => this.$events.next(events));

    //this.userEventService.listenChanges(filter)

    await firstNotNilPromise(this.$events);
    this.loading = false;
  }

  doAction(action: string, item: UserEventItem) {
    console.log("TODO do action")
  }

  /* */

  toUserEventItem(source: UserEvent): UserEventItem {
    if (!source) return undefined;

    const defaultsFromType = DEFAULTS_BY_EVENT_TYPE[source.eventType]
    let defaultsFromContent: Partial<UserEventItem>;
    if (source.content && source.eventType === UserEventTypes.DEBUG_DATA && this.showContent) {
      try {
        if (source.content && source.content.startsWith('{')) {
          const object = JSON.parse(source.content);
          if (object && object.__typename) {
            defaultsFromContent = DEFAULTS_BY_ICON_BY_CONTENT_TYPENAME[object.__typename];
          }
        }
      }
      catch(err) {
        console.error(err);
      }
    }
    return {
      title: 'SOCIAL.EVENT_TYPE_ENUM.' + source.eventType,
      time: source.updateDate||source.creationDate,
      ...defaultsFromType,
      ...defaultsFromContent
    };
  }
}

import {Entity, EntityAsObjectOptions, fromDateISOString} from "../../../core/core.module";
import {Moment} from "moment/moment";

export const UserEventTypes = {
  DEBUG_DATA: 'DEBUG_DATA',
  INBOX_MESSAGE: 'INBOX_MESSAGE'
}

export class UserEvent extends Entity<UserEvent> {

  static TYPE_NAME = "UserEventVO";

  static fromObject(source: any): UserEvent {
    if (!source || source instanceof UserEvent) return source;
    const target = new UserEvent();
    target.fromObject(source);
    return target;
  }

  eventType: string;
  issuer: string;
  recipient: string;
  updateDate: Moment;
  creationDate: Moment;

  content: string;
  signature: string;
  readSignature: string;

  constructor() {
    super();
    this.__typename = UserEvent.TYPE_NAME;
  }

  clone(): UserEvent {
    const target = new UserEvent();
    target.fromObject(this);
    return target;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    return target;
  }

  fromObject(source: any) {
    Object.assign(this, source); // Copy all properties
    super.fromObject(source);
    this.creationDate = fromDateISOString(source.creationDate);
  }
}


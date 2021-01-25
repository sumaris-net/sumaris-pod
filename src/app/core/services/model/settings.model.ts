import {isNil} from "../../../shared/functions";
import {Entity, EntityAsObjectOptions} from "./entity.model";
import {Moment} from "moment";
import {LatLongPattern} from "../../../shared/material/latlong/latlong.utils";
import {PropertiesMap, Property} from "../../../shared/types";
import {InjectionToken} from "@angular/core";

export type UsageMode = 'DESK' | 'FIELD';

export declare interface LocaleConfig extends Property {
  country?: string;
}

export const APP_LOCALES = new InjectionToken<LocaleConfig[]>('locales');

export declare interface LocalSettings {
  pages?: any;
  peerUrl?: string;
  latLongFormat: 'DDMMSS' | 'DDMM' | 'DD';
  accountInheritance?: boolean;
  locale: string;
  usageMode?: UsageMode;
  mobile?: boolean;
  touchUi?: boolean;
  properties?: PropertiesMap;
  pageHistory?: HistoryPageReference[];
  offlineFeatures?: string[];
  pageHistoryMaxSize: number;
}


export interface HistoryPageReference {
  title: string;
  subtitle?: string;
  path: string;
  time?: Moment|string;
  icon?: string;
  matIcon?: string;

  children?: HistoryPageReference[];

  // Manage visibility and data access
  offline?: boolean;
  onlinePeer?: string;

}

export class UserSettings extends Entity<UserSettings> {
  locale: string;
  latLongFormat: LatLongPattern;
  content: {};
  nonce: string;

  clone(): UserSettings {
    const target = new UserSettings();
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const res: any = super.asObject(options);
    res.content = this.content && JSON.stringify(res.content) || undefined;
    return res;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.locale = source.locale;
    this.latLongFormat = source.latLongFormat as LatLongPattern;
    if (isNil(source.content) || typeof source.content === 'object') {
      this.content = source.content || {};
    } else {
      this.content = source.content && JSON.parse(source.content) || {};
    }
    this.nonce = source.nonce;
  }
}

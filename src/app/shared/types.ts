export declare interface ObjectMap<O = any> {
  [key: string]: O;
}

export declare interface ObjectMapEntry<O = any> {
  key: string;
  value?: O;
}

export declare type PropertiesMap = ObjectMap<string>;

export declare type Property = ObjectMapEntry<string>;

export declare type PropertiesArray = Property[];

export declare type DisplayFn = (obj: any) => string;

export declare interface IconRef {
  icon?: string; // An ion-icon name
  matIcon?: string; // A mat icon
}

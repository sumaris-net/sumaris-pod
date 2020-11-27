export declare type ObjectMap<O = any> = {
  [key: string]: O;
}

export declare type ObjectMapEntry<O = any> = {
  key: string;
  value?: O;
}

export declare type PropertiesMap = ObjectMap<string>;

export declare type Property = ObjectMapEntry<string>;

export declare type PropertiesArray = Property[];

export declare type DisplayFn = (obj: any) => string;

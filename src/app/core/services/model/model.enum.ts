
// TODO: rename to STATUS_ID_MAP
// then declare a type like this :
// > export declare type StatusIds = keyof typeof STATUS_ID_MAP;
export const StatusIds = {
  DISABLE: 0,
  ENABLE: 1,
  TEMPORARY: 2,
  DELETED: 3,
  ALL: 99
};

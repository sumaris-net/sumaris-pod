import {FetchPolicy} from "apollo-client";
export class Environment {
  name: string;
  version: string;
  production: boolean;
  baseUrl: string;
  defaultLocale: string;
  defaultLatLongFormat?: 'DD' | 'DDMM' | 'DDMMSS';
  defaultDepartmentId?: number;
  apolloFetchPolicy?:  FetchPolicy;
  mock?: boolean;
  listenRemoteChanges?: boolean;

  // A peer to use at startup (useful on a web site deployment)
  defaultPeer?: { host: string; port: number; useSsl?: boolean; path?: string; } | undefined | null;

  // A list of peers, to select as peer, in settings
  defaultPeers?: { host: string; port: number; useSsl?: boolean; path?: string; }[];

  // Enable cache persistence ?
  persistCache?: boolean;

  // Force offline mode ? For DEV only
  offline?: boolean;
}

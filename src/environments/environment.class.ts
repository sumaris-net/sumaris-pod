export class Environment {
  name: string;
  version: string;
  production: boolean;
  baseUrl: string;
  remoteBaseUrl: string;
  defaultLocale: string;
  defaultLatLongFormat?: 'DD' | 'DDMM' | 'DDMMSS';
  defaultDepartmentId?: number;
  apolloFetchPolicy?: 'cache-first' | 'cache-and-network' | 'network-only' | 'cache-only' | 'no-cache' | 'standby';
  mock?: boolean;
  listenRemoteChanges?: boolean;

  // A peer to use at startup (useful on a web site deployment)
  defaultPeer?: { host: string; port: number; useSsl?: boolean; } | undefined | null;

  // A list of peers, to select as peer, in settings
  defaultPeers?: { host: string; port: number; useSsl?: boolean; }[];
}

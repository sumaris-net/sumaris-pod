import {FetchPolicy} from "apollo-client";
import {StorageConfig} from "@ionic/storage";
export class Environment {
  name: string;
  version: string;
  production: boolean;
  baseUrl: string;
  mock?: boolean;
  listenRemoteChanges?: boolean;

  // A peer to use at startup (useful on a web site deployment)
  defaultPeer?: { host: string; port: number; useSsl?: boolean; path?: string; } | undefined | null;

  // A list of peers, to select as peer, in settings
  defaultPeers?: { host: string; port: number; useSsl?: boolean; path?: string; }[];

  // Enable cache persistence ?
  persistCache?: boolean;

  // Force offline mode ? /!\ For DEV only
  offline?: boolean;

  // Apollo (graphQL) config
  apolloFetchPolicy?:  FetchPolicy;

  // Default values
  defaultLocale: string;
  defaultLatLongFormat?: 'DD' | 'DDMM' | 'DDMMSS';
  defaultDepartmentId?: number;
  defaultAppName?: string;
  defaultAndroidInstallUrl?: string;

  // Storage
  storage?: Partial<StorageConfig>
}

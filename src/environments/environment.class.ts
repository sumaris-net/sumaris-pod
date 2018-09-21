export class Environment {
    production: boolean;
    baseUrl: string;
    remoteBaseUrl: string;
    defaultLocale: string;
    version: string;
    defaultProgram: string;
    apolloFetchPolicy?: 'cache-first' | 'cache-and-network' | 'network-only' | 'cache-only' | 'no-cache' | 'standby';
    mock?: boolean;
};

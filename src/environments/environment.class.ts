export class Environment {
    production: boolean;
    baseUrl: string;
    remoteBaseUrl: string;
    defaultLocale: string;
    version: string;
    defaultProgram: string;

    // debugging
    mock?: boolean;
};

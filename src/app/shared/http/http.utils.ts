import {HttpClient, HttpHeaders, HttpParams} from "@angular/common/http";
import {HTTP} from "@ionic-native/http/ngx";

export interface NodeInfo {
  softwareName: string;
  softwareVersion: string;
  nodeLabel?: string;
  nodeName?: string;
}

export class HttpUtils {
  static getResource = getResource;
}

export async function getResource<T>(http: HttpClient | HTTP,
                                     uri: string,
                                     opts?: {
    headers?: HttpHeaders | {
      [header: string]: string | string[];
    };
    params?: HttpParams | {
      [param: string]: string | string[];
    };
    reportProgress?: boolean;
    responseType?: 'json';
    withCredentials?: boolean;
  }): Promise<T> {

  // Add headers
  opts = {
    /*headers: new HttpHeaders()
      .set('Content-Type', 'application/json;charset=UTF-8')
      .set('Access-Control-Allow-Headers', 'Content-Type')
      .set('Access-Control-Allow-Methods', 'GET')
      .set('Access-Control-Allow-Origin', '*'),*/
    ...opts
  };

  try {

    // Use native http client
    if (http instanceof HTTP) {
      // Execute the request
      const response = await http.get(uri, opts.params, opts.headers);
      console.info("[network] response: " + response.data);
      return response.data as T;
    }

    // Using web http client
    return (await http.get(uri, opts).toPromise()) as T;
  }
  catch (err) {
    if (err && err.message) {
      console.error(`[network] Error on get request ${uri}: ${err.message}`, err);
    }
    else {
      console.error(`[network] Error on get request ${uri}: ${err && err.statusText}`);
    }
    throw {code: err.status, message: "ERROR.UNKNOWN_NETWORK_ERROR"};
  }
}


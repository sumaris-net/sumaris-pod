import {HttpClient, HttpHeaders} from "@angular/common/http";
import {HTTP} from "@ionic-native/http/ngx";
import {HttpUtils} from "../../shared/http/http.utils";

export interface NodeInfo {
  softwareName: string;
  softwareVersion: string;
  nodeLabel?: string;
  nodeName?: string;
}

export class NetworkUtils {
  static getNodeInfo = getNodeInfo;
}

export async function getNodeInfo(
  http: HTTP | HttpClient,
  peerUrl: string,
  opts?: {
  headers?: HttpHeaders | {
    [header: string]: string | string[];
  };
}): Promise<NodeInfo> {

  // Remove trailing slash
  if (peerUrl.endsWith('/')) {
    peerUrl = peerUrl.substr(0, peerUrl.length - 1);
  }

  return HttpUtils.getResource(http, peerUrl + '/api/node/info', opts);
}

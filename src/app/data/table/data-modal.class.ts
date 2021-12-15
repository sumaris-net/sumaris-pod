import {IModalDetailOptions}  from "@sumaris-net/ngx-components";
import {Observable} from "rxjs";
import {DenormalizedPmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {Entity}  from "@sumaris-net/ngx-components";
import {UsageMode}  from "@sumaris-net/ngx-components";
import {IPmfm} from "../../referential/services/model/pmfm.model";

export interface IDataEntityModalOptions<T = any> extends IModalDetailOptions<T> {
  // Program (or PMFMs, to avoid loading PMFMs by program)
  programLabel: string;
  acquisitionLevel: string;
  pmfms: Observable<IPmfm[]> | IPmfm[];

  // UI options
  usageMode: UsageMode;
}

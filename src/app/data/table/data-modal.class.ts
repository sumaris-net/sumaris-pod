import {IModalDetailOptions} from "../../core/table/table.class";
import {Observable} from "rxjs";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {Entity} from "../../core/services/model/entity.model";
import {UsageMode} from "../../core/services/model/settings.model";

export interface IDataEntityModalOptions<T extends Entity<T>> extends IModalDetailOptions<T> {
  // Program (or PMFMs, to avoid loading PMFMs by program)
  program: string;
  acquisitionLevel: string;
  pmfms: Observable<PmfmStrategy[]> | PmfmStrategy[];

  // UI options
  usageMode: UsageMode;
}

import { Trip } from '@app/trip/services/model/trip.model';
import { ContextService } from '@app/shared/context.service';
import { Injectable } from '@angular/core';
import { UsageMode } from '@sumaris-net/ngx-components';

export type TripContext = {
  trip?: Trip;
  usageMode?: UsageMode;
}

@Injectable({providedIn: 'root'})
export class TripContextService extends ContextService<TripContext> {

  constructor() {
    super(<TripContext>{});
  }
}

import { Trip } from '@app/trip/services/model/trip.model';
import { ContextService } from '@app/shared/context.service';
import { Injectable } from '@angular/core';

export type TripContext = {
  trip?: Trip;
}

@Injectable({providedIn: 'root'})
export class TripContextService<S extends TripContext = TripContext> extends ContextService<S> {

  constructor() {
    super(<S>{});
  }
}

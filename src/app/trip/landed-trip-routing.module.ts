import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LandingPage } from './landing/landing.page';
import { SharedRoutingModule } from '@sumaris-net/ngx-components';
import { ObservedLocationsPage } from './observedlocation/observed-locations.page';
import { ObservedLocationPage } from './observedlocation/observed-location.page';
import { AuctionControlPage } from './landing/auctioncontrol/auction-control.page';
import { LandedTripPage } from './landedtrip/landed-trip.page';
import { LandedTripModule } from './landed-trip.module';
import { SamplingLandingPage } from './landing/sampling/sampling-landing.page';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: ObservedLocationsPage,
  },
  {
    path: ':observedLocationId',
    runGuardsAndResolvers: 'pathParamsChange',
    data: {
      pathIdParam: 'observedLocationId',
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: ObservedLocationPage,
        runGuardsAndResolvers: 'pathParamsChange',
      },
      {
        path: 'landing/:landingId',
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          pathIdParam: 'landingId',
        },
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: LandingPage,
            runGuardsAndResolvers: 'pathParamsChange',
          },
        ],
      },
      {
        path: 'control/:controlId',
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          pathIdParam: 'controlId',
        },
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: AuctionControlPage,
            runGuardsAndResolvers: 'pathParamsChange',
          },
        ],
      },
      {
        path: 'sampling/:samplingId',
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          pathIdParam: 'samplingId',
        },
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: SamplingLandingPage,
            runGuardsAndResolvers: 'pathParamsChange',
          },
        ],
      },
      {
        path: 'trip/:tripId',
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          pathIdParam: 'tripId',
        },
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: LandedTripPage,
            runGuardsAndResolvers: 'pathParamsChange',
          },
        ],
      },
    ],
  },
];

@NgModule({
  imports: [SharedRoutingModule, LandedTripModule, RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class LandedTripRoutingModule {}

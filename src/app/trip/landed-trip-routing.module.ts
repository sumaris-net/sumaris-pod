import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LandingPage } from './landing/landing.page';
import { ComponentDirtyGuard, SharedRoutingModule } from '@sumaris-net/ngx-components';
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
    component: ObservedLocationsPage
  },
  {
    path: ':observedLocationId',
    runGuardsAndResolvers: 'pathParamsChange',
    data: {
      pathIdParam: 'observedLocationId'
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: ObservedLocationPage,
        runGuardsAndResolvers: 'pathParamsChange',
        canDeactivate: [ComponentDirtyGuard]
      },
      {
        path: 'landing/:landingId',
        data: {
          pathIdParam: 'landingId'
        },
        pathMatch: 'full',
        component: LandingPage,
        runGuardsAndResolvers: 'pathParamsChange',
        canDeactivate: [ComponentDirtyGuard]
      },
      {
        path: 'control/:controlId',
        data: {
          pathIdParam: 'controlId'
        },
        pathMatch: 'full',
        component: AuctionControlPage,
        runGuardsAndResolvers: 'pathParamsChange',
        canDeactivate: [ComponentDirtyGuard]
      },
      {
        path: 'sampling/:samplingId',
        data: {
          pathIdParam: 'samplingId'
        },
        pathMatch: 'full',
        component: SamplingLandingPage,
        runGuardsAndResolvers: 'pathParamsChange',
        canDeactivate: [ComponentDirtyGuard]
      },
      {
        path: 'trip/:tripId',
        data: {
          pathIdParam: 'tripId'
        },
        pathMatch: 'full',
        component: LandedTripPage,
        runGuardsAndResolvers: 'pathParamsChange',
        canDeactivate: [ComponentDirtyGuard]
      }
    ]
  }
];


@NgModule({
  imports: [
    SharedRoutingModule,
    LandedTripModule,
    RouterModule.forChild(routes)
  ],
  exports: [
    RouterModule
  ]
})
export class LandedTripRoutingModule {
}

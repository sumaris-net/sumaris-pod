import {Injectable, NgModule} from '@angular/core';
import {ActivatedRouteSnapshot, ExtraOptions, RouteReuseStrategy, RouterModule, Routes} from '@angular/router';
import {HomePage} from './core/home/home';
import {RegisterConfirmPage} from './core/register/confirm/confirm';
import {AccountPage} from './core/account/account';
import {TripPage, TripTable} from './trip/trip.module';
import {OperationPage} from './trip/operation/operation.page';
import {ObservedLocationPage} from "./trip/observedlocation/observed-location.page";
import {ObservedLocationsPage} from "./trip/observedlocation/observed-locations.page";
import {SettingsPage} from "./core/settings/settings.page";
import {LandingPage} from "./trip/landing/landing.page";
import {AuctionControlPage} from "./trip/auctioncontrol/auction-control.page";
import {AuthGuardService} from "./core/services/auth-guard.service";
import {LandedTripPage} from "./trip/landedtrip/landed-trip.page";
import {SHARED_ROUTE_OPTIONS, SharedRoutingModule} from "./shared/shared-routing.module";

const routes: Routes = [
  // Core path
  {
    path: '',
    component: HomePage
  },

  {
    path: 'home/:action',
    component: HomePage
  },
  {
    path: 'confirm/:email/:code',
    component: RegisterConfirmPage
  },
  {
    path: 'account',
    pathMatch: 'full',
    component: AccountPage,
    canActivate: [AuthGuardService]
  },
  {
    path: 'settings',
    pathMatch: 'full',
    component: SettingsPage
  },

  // Admin
  {
    path: 'admin',
    canActivate: [AuthGuardService],
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule)
  },

  // Referential
  {
    path: 'referential',
    canActivate: [AuthGuardService],
    loadChildren: () => import('./referential/referential.module').then(m => m.ReferentialModule)
  },

  // Trip path
  {
    path: 'trips',
    canActivate: [AuthGuardService],
    data: {
      profile: 'USER'
    },
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: TripTable
      },
      {
        path: ':tripId',
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          pathIdParam: 'tripId'
        },
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: TripPage,
            runGuardsAndResolvers: 'pathParamsChange'
          },
          {
            path: 'operations/:operationId',
            runGuardsAndResolvers: 'pathParamsChange',
            data: {
              pathIdParam: 'operationId'
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                component: OperationPage,
                runGuardsAndResolvers: 'pathParamsChange'
              }
            ]
          }
        ]
      },

      {
        path: ':tripId/landing/:landingId',
        component: LandingPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER',
          pathIdParam: 'landingId'
        }
      }
    ]
  },

  // Observations path
  {
    path: 'observations',
    canActivate: [AuthGuardService],
    data: {
      profile: 'USER'
    },
    children: [
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
            runGuardsAndResolvers: 'pathParamsChange'
          },
          // {
          //   path: 'batches',
          //   component: SubBatchesModal,
          //   runGuardsAndResolvers: 'pathParamsChange'
          // },
          {
            path: 'landing/:landingId',
            runGuardsAndResolvers: 'pathParamsChange',
            data: {
              pathIdParam: 'landingId'
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                component: LandingPage,
                runGuardsAndResolvers: 'pathParamsChange'
              }
            ]
          },
          {
            path: 'control/:controlId',
            runGuardsAndResolvers: 'pathParamsChange',
            data: {
              pathIdParam: 'controlId'
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                component: AuctionControlPage,
                runGuardsAndResolvers: 'pathParamsChange'
              }
            ]
          },
          {
            path: 'trip/:tripId',
            runGuardsAndResolvers: 'pathParamsChange',
            data: {
              pathIdParam: 'tripId'
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                component: LandedTripPage,
                runGuardsAndResolvers: 'pathParamsChange'
              }
            ]
          }
        ]
      }
    ]
  },

  // Extraction path
  {
    path: 'extraction',
    canActivate: [AuthGuardService],
    loadChildren: () => import('./trip/extraction/extraction.module').then(m => m.ExtractionModule)
  },

  // Test module (disable in menu, by default - can be enable by the Pod configuration page)
  {
    path: 'testing',
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'shared',
      },
      // Shared module
      {
        path: 'shared',
        loadChildren: () => import('./shared/shared.testing.module').then(m => m.SharedTestingModule)
      },
      // Trip module
      {
        path: 'trip',
        loadChildren: () => import('./trip/trip.testing.module').then(m => m.TripTestingModule)
      }
    ]
  },

  // Other route redirection (should at the end of the array)
  {
    path: "**",
    redirectTo: '/'
  }
];


@NgModule({
  imports: [
    SharedRoutingModule,
    RouterModule.forRoot(routes, SHARED_ROUTE_OPTIONS)
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule {
}

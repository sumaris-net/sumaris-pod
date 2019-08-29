import {NgModule} from '@angular/core';
import {ActivatedRouteSnapshot, ExtraOptions, RouteReuseStrategy, RouterModule, Routes} from '@angular/router';
import {HomePage} from './core/home/home';
import {RegisterConfirmPage} from './core/register/confirm/confirm';
import {AccountPage} from './core/account/account';
import {AuthGuardService} from './core/core.module';
import {UsersPage} from './admin/users/list/users';
import {VesselsPage} from './referential/vessel/list/vessels';
import {VesselPage} from './referential/vessel/page/page-vessel';
import {ReferentialsPage} from './referential/list/referentials';
import {TripPage, TripsPage} from './trip/trip.module';
import {OperationPage} from './trip/operation/operation.page';
import {ExtractionTablePage} from "./trip/extraction/extraction-table-page.component";
import {RemoteConfigPage} from './admin/config/config.component';
import {ObservedLocationPage} from "./trip/observedlocation/observed-location.page";
import {ObservedLocationsPage} from "./trip/observedlocation/observed-locations.page";
import {SettingsPage} from "./core/settings/settings.page";
import {ExtractionMapPage} from "./trip/extraction/extraction-map-page.component";
import {LandingPage} from "./trip/landing/landing.page";
import {AuctionControlLandingPage} from "./trip/landing/auctioncontrol/auction-control-landing.page";
import {SubBatchesModal} from "./trip/batch/sub-batches.modal";
import {IonicRouteStrategy} from "@ionic/angular";
import {ProgramPage} from "./referential/program/program.page";

const routeOptions: ExtraOptions = {
  enableTracing: false,
  //enableTracing: !environment.production,
  useHash: false
};

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
    path: 'admin/users',
    pathMatch: 'full',
    component: UsersPage,
    canActivate: [AuthGuardService],
    data: {
      profile: 'ADMIN'
    }
  },
  {
    path: 'admin/config',
    pathMatch: 'full',
    component: RemoteConfigPage,
    canActivate: [AuthGuardService],
    data: {
      profile: 'ADMIN'
    }
  },

  // Referential path
  {
    path: 'referential',
    canActivate: [AuthGuardService],
    children: [
      {
        path: 'list',
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: ReferentialsPage,
            data: {
              profile: 'ADMIN'
            }
          }
        ]
      },
      {
        path: 'vessels',
        children: [
          {
            path: '',
            component: VesselsPage,
            data: {
              profile: 'USER'
            }
          },
          {
            path: ':id',
            component: VesselPage,
            data: {
              profile: 'USER'
            }
          }
        ]
      },
      {
        path: 'program/:id',
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: ProgramPage,
            data: {
              profile: 'ADMIN'
            }
          }
        ]
      },
      {
        path: 'software/:id',
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: RemoteConfigPage,
            data: {
              profile: 'ADMIN'
            }
          }
        ]
      }
    ]
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
        component: TripsPage
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
            path: 'operations/:id',
            runGuardsAndResolvers: 'pathParamsChange',
            data: {
              pathIdParam: 'id'
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                component: OperationPage,
                runGuardsAndResolvers: 'pathParamsChange'
              },
              {
                path: 'batches',
                component: SubBatchesModal,
                runGuardsAndResolvers: 'pathParamsChange'
              }
            ]
          }
        ]
      },

      {
        path: ':tripId/landing/:id',
        component: LandingPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER'
        }
      }
    ]
  },

  // Observations path
  {
    path: 'observations',
    canActivate: [AuthGuardService],
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: ObservedLocationsPage,
        data: {
          profile: 'USER'
        }
      },
      {
        path: ':id',
        component: ObservedLocationPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER',
          pathIdParam: 'id'
        }
      },
      {
        path: ':observedLocationId/landing/:id',
        component: LandingPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER',
          pathIdParam: 'id'
        }
      },
      {
        path: ':observedLocationId/control/:id',
        component: AuctionControlLandingPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER'
        }
      }
    ]
  },

  {
    path: 'extraction',
    canActivate: [AuthGuardService],
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: ExtractionTablePage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'SUPERVISOR'
        }
      }
    ]
  },

  {
    path: 'map',
    canActivate: [AuthGuardService],
    children: [
      {
        path: '',
        //pathMatch: 'full',
        component: ExtractionMapPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER'
        }
      }
    ]
  },

  {
    path: "**",
    redirectTo: '/'
  },
];

export class CustomReuseStrategy extends IonicRouteStrategy {

  shouldReuseRoute(future: ActivatedRouteSnapshot, curr: ActivatedRouteSnapshot): boolean {
    const res = super.shouldReuseRoute(future, curr);

    // Reuse the route if path change from [/new] -> [/:id]
    if (!res && future.routeConfig && future.routeConfig === curr.routeConfig) {
      const pathIdParam = future.routeConfig.data && future.routeConfig.data.pathIdParam || 'id';
      const futureId = future.params[pathIdParam] === 'new' ?
        (future.queryParams[pathIdParam] || future.queryParams['id']) : future.params[pathIdParam];
      const currId = curr.params[pathIdParam] === 'new' ?
        (curr.queryParams[pathIdParam] || curr.queryParams['id']) : curr.params[pathIdParam];
      //if (futureId !== currId) console.log("TODO: shouldReuseRoute -> NOT same page. Will not reused");
      return futureId === currId;
    }
    return res;
  }
}

@NgModule({
  imports: [
    RouterModule.forRoot(routes, routeOptions)
  ],
  exports: [
    RouterModule
  ],
  providers: [
    { provide: RouteReuseStrategy, useClass: CustomReuseStrategy }
  ]
})
export class AppRoutingModule {
}

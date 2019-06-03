import {NgModule} from '@angular/core';
import {ExtraOptions, RouterModule, Routes} from '@angular/router';
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
import {ConfigPage} from './admin/config/config.component';
import {ObservedLocationPage} from "./trip/observedlocation/observed-location.page";
import {ObservedLocationsPage} from "./trip/observedlocation/observed-locations.page";
import {SettingsPage} from "./core/settings/settings.page";
import {ExtractionMapPage} from "./trip/extraction/extraction-map-page.component";
import {LandingPage} from "./trip/landing/landing.page";

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
    component: ConfigPage,
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
        path: 'list',
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: '/referential/list/Location',
            data: {
              profile: 'ADMIN'
            }
          },
          {
            path: ':entityName',
            component: ReferentialsPage,
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
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: TripsPage,
        data: {
          profile: 'USER'
        }
      },
      {
        path: ':tripId',
        pathMatch: 'full',
        component: TripPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER'
        }
      },
      {
        path: ':tripId/operations/:id',
        component: OperationPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER'
        }
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
          profile: 'USER'
        }
      },
      {
        path: ':observedLocationId/landings/:id',
        component: LandingPage,
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
        redirectTo: '/extraction/product/p01_rdb',
        data: {
          profile: 'SUPERVISOR'
        }
      },
      {
        path: ':category',
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: '/extraction/product/p01_rdb',
            data: {
              profile: 'SUPERVISOR'
            }
          },
          {
            path: ':label',
            component: ExtractionTablePage,
            data: {
              profile: 'SUPERVISOR'
            }
          }
        ]
      }
    ]
  },

  {
    path: 'map',
    canActivate: [AuthGuardService],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: '/map/product/p01_rdb',
        data: {
          profile: 'SUPERVISOR'
        }
      },
      {
        path: ':category',
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: '/map/product/p01_rdb',
            data: {
              profile: 'SUPERVISOR'
            }
          },
          {
            path: ':label',
            component: ExtractionMapPage,
            data: {
              profile: 'SUPERVISOR'
            }
          }
        ]
      }
    ]
  },

  {
    path: "**",
    redirectTo: '/'
  },
];
@NgModule({
  imports: [
    RouterModule.forRoot(routes, routeOptions)
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule { }

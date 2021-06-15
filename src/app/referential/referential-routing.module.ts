import {RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {ReferentialsPage} from "./list/referentials.page";
import {ProgramPage} from "./program/program.page";
import {SoftwarePage} from "./software/software.page";
import {ParameterPage} from "./pmfm/parameter.page";
import {PmfmPage} from "./pmfm/pmfm.page";
import {SharedRoutingModule} from "../shared/shared-routing.module";
import {ReferentialModule} from "./referential.module";
import {StrategyPage} from "./strategy/strategy.page";
import {ProgramsPage} from "./program/programs.page";
import {SamplingStrategyPage} from "./strategy/sampling/sampling-strategy.page";
import {TaxonNamePage} from "./taxon/taxon-name.page";

const routes: Routes = [
  {
    path: 'list',
    pathMatch: 'full',
    component: ReferentialsPage,
    runGuardsAndResolvers: 'pathParamsChange',
    data: {
      profile: 'ADMIN'
    }
  },
  {
    path: 'programs',
    children: [
      {
        path: '',
        component: ProgramsPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'SUPERVISOR'
        }
      },
      {
        path: ':programId',
        runGuardsAndResolvers: 'pathParamsChange',
        children: [
          {
            path: '',
            pathMatch: 'full',
            component: ProgramPage,
            data: {
              profile: 'SUPERVISOR',
              pathIdParam: 'programId'
            }
          },
          {
            path: 'strategy/legacy/:strategyId',
            data: {
              profile: 'SUPERVISOR',
              pathIdParam: 'strategyId'
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                component: StrategyPage
              }
            ]
          },

          {
            path: 'strategy/sampling/:strategyId',
            component: SamplingStrategyPage,
            data: {
              profile: 'SUPERVISOR',
              pathIdParam: 'strategyId'
            },
            children: [
              {
                path: '',
                pathMatch: 'full',
                component: StrategyPage
              }
            ]
          }
        ]
      }
    ]
  },
  {
    path: 'software/:id',
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: SoftwarePage,
        data: {
          profile: 'ADMIN'
        }
      }
    ]
  },
  {
    path: 'parameter/:id',
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: ParameterPage,
        data: {
          profile: 'ADMIN'
        }
      }
    ]
  },
  {
    path: 'pmfm/:id',
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: PmfmPage,
        data: {
          profile: 'ADMIN'
        }
      }
    ]
  },
  {
    path: 'taxonName/:id',
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: TaxonNamePage,
        data: {
          profile: 'ADMIN'
        }
      }
    ]
  }
];

@NgModule({
  imports: [
    SharedRoutingModule,
    ReferentialModule,
    RouterModule.forChild(routes)
  ],
  exports: [RouterModule]
})
export class ReferentialRoutingModule { }

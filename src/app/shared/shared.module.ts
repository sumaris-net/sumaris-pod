import {ModuleWithProviders, NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {Environment, SharedModule} from '@sumaris-net/ngx-components';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
  ],
  exports: [
    SharedModule,
    RouterModule,
    TranslateModule
  ]
})
export class AppSharedModule {

  static forRoot(environment: Environment): ModuleWithProviders<AppSharedModule> {

    console.info('[shared] Creating module (root)');
    return {
      ngModule: AppSharedModule,
      providers: [
        ...SharedModule.forRoot(environment).providers
      ]
    };
  }
}

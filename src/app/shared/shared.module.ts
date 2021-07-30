import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {SharedModule} from '@sumaris-net/ngx-components';
import { Context, ContextService } from './context.service';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
  ],
  exports: [
    SharedModule,
    RouterModule,
    TranslateModule,
  ],
  providers: [
    {
      provide: ContextService,
      useFactory: () => (new ContextService<Context>({}))
    },
  ],
})
export class AppSharedModule {

}

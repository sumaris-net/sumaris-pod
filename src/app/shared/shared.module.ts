import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {SharedModule} from '@sumaris-net/ngx-components';

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

}

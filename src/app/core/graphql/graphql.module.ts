import {NgModule} from '@angular/core';
import {HttpClientModule} from '@angular/common/http';
// Apollo
import {HttpLinkModule} from 'apollo-angular-link-http';

@NgModule({
  imports: [
    HttpClientModule,
    HttpLinkModule
  ],
  exports: [
    HttpClientModule,
    HttpLinkModule
  ]
})
export class AppGraphQLModule {

  constructor() {
  }
}


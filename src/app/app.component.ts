import { Component } from '@angular/core';
import { Platform } from "@ionic/angular";
import { MenuItem } from './core/menu/menu.component';
import { HomePage } from './core/home/home';
import { AccountService, DataService } from './core/core.module';
import { ReferentialService } from './referential/referential.module';
// import { StatusBar } from "@ionic-native/status-bar";
// import { SplashScreen } from "@ionic-native/splash-screen";
// import { Keyboard } from "@ionic-native/keyboard";
// import { ReferentialService, ReferentialFilter } from './referential/referential.module';
// import { AccountFieldDef, AccountService } from './core/core.module';
// import { Referential } from './core/services/model';
// import { DataService } from './core/services/data-service.class';


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {

  root: any = HomePage;
  menuItems: Array<MenuItem> = [
    { title: 'MENU.HOME', path: '/', icon: 'home' },
    { title: 'MENU.TRIPS', path: '/trips', icon: 'pin' },
    { title: 'MENU.ADMINISTRATION_DIVIDER', requiredProfiles: ['ADMIN', 'SUPERVISOR', 'USER'] },
    { title: 'MENU.USERS', path: '/admin/users', icon: 'people', requiredProfiles: ['ADMIN'] },
    { title: 'MENU.VESSELS', path: '/referential/vessels', icon: 'boat', requiredProfiles: ['ADMIN', 'SUPERVISOR', 'USER'] },
    { title: 'MENU.REFERENTIALS', path: '/referential/list', icon: 'list', requiredProfiles: ['ADMIN'] }
  ];

  constructor(
    private platform: Platform,
    private accountService: AccountService,
    private referentialService: ReferentialService
    // TODO: waiting ionic-native release
    // private statusBar: StatusBar, 
    // private splashScreen: SplashScreen,
    // private keyboard: Keyboard
  ) {

    platform.ready().then(() => {
      console.info("[app] Setting cordova plugins...");

      /*
      statusBar.styleDefault();
      splashScreen.hide();

      statusBar.overlaysWebView(false);

      // Control Keyboard
      keyboard.disableScroll(true);
      */

      this.addAccountFields();
    });

  }

  public onActivate(event) {
    // Make sure to scroll on top before changing state
    // See https://stackoverflow.com/questions/48048299/angular-5-scroll-to-top-on-every-route-click
    let scrollToTop = window.setInterval(() => {
      let pos = window.pageYOffset;
      if (pos > 0) {
        window.scrollTo(0, pos - 20); // how far to scroll on each step
      } else {
        window.clearInterval(scrollToTop);
      }
    }, 16);
  }

  protected addAccountFields() {

    console.debug("[app] Add additional account fields...");

    // Add account field: department
    this.accountService.addAdditionalAccountField({
      name: 'department',
      label: 'USER.DEPARTMENT',
      required: true,
      dataService: this.referentialService as DataService<any, any>,
      dataServiceOptions: { entityName: 'Department' }
    });
  }
}


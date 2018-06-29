import { Component } from '@angular/core';
import { Platform } from "@ionic/angular";
import { MenuItem } from './core/menu/menu.component';
import { HomePage } from './core/home/home';


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
    { title: 'MENU.ADMINISTRATION_DIVIDER' },
    { title: 'MENU.USERS', path: '/admin/users', icon: 'people' },
    { title: 'MENU.VESSELS', path: '/referential/vessels', icon: 'boat' },
    { title: 'MENU.REFERENTIALS', path: '/referential/list', icon: 'list' }
  ];

  constructor(
    private platform: Platform
    /* TODO: waiting ionic-native release,
    private statusBar: StatusBar,
    private splashScreen: SplashScreen,
    private keyboard: Keyboard*/
  ) {

    platform.ready().then(() => {
      console.info("[app] Setting cordova plugins...");

      /*statusBar.styleDefault();
      splashScreen.hide();

      statusBar.overlaysWebView(false);

      //*** Control Keyboard
      keyboard.disableScroll(true);*/
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

}


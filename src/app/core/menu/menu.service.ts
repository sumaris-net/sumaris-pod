import { Injectable } from '@angular/core';
import {Subject} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MenuService {

  menuToggled$ = new Subject();
  menuVisible$ = new Subject<boolean>();

  constructor() { }

  toggleMenu() {
    this.menuToggled$.next();
  }

  menuVisible(value: boolean) {
    this.menuVisible$.next(value);
  }

}

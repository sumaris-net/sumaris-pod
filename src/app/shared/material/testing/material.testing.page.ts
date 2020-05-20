import {Component} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-material-testing',
  templateUrl: './material.testing.html'
})
export class MaterialTestingPage {

  constructor(
      protected route: ActivatedRoute,
      protected router: Router
    ) {
  }

  async openPage(page): Promise<boolean> {
    return this.router.navigate(['./' + page], {
      relativeTo: this.route
    });
  }
}


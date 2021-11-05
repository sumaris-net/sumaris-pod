import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {ExtractionProduct} from "../../services/model/extraction-product.model";
import { Observable, Subscription } from 'rxjs';
import { first, map } from 'rxjs/operators';
import {TranslateService} from "@ngx-translate/core";
import {ExtractionProductService} from "../../services/extraction-product.service";
import {ExtractionProductFilter} from "../../services/filter/extraction-product.filter";
import { capitalizeFirstLetter, isNil, propertyComparator } from '@sumaris-net/ngx-components';
import { ExtractionTypeUtils } from '@app/extraction/services/model/extraction-type.model';

@Component({
  selector: 'app-select-product-modal',
  templateUrl: './select-product.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectProductModal implements OnInit {

  _subscription = new Subscription();
  loading = true;
  types$: Observable<ExtractionProduct[]>;

  @Input() filter: Partial<ExtractionProductFilter> = {};
  @Input() program: string;

  constructor(
    protected viewCtrl: ModalController,
    protected service: ExtractionProductService,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {

  }

  ngOnInit() {

    // Load items
    this.types$ = this.service.watchAll(this.filter, {})
      .pipe(
        map(({data}) =>
          // Compute i18n name
          data.map(t => ExtractionTypeUtils.computeI18nName(this.translate, t))
            // Then sort by name
            .sort(propertyComparator('name'))
        )
      );

    // Update loading indicator
    this.types$.pipe(first()).subscribe((_) => this.loading = false);
  }

  selectType(type: ExtractionProduct) {

    this.close(type);
  }

  async close(event?: any) {

    await this.viewCtrl.dismiss(event);
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

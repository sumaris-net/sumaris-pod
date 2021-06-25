import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {ExtractionProduct} from "../../services/model/extraction-product.model";
import {Observable} from "rxjs";
import {first} from "rxjs/operators";
import {TranslateService} from "@ngx-translate/core";
import {ExtractionProductService} from "../../services/extraction-product.service";
import {ExtractionProductFilter} from "../../services/filter/extraction-product.filter";

@Component({
  selector: 'app-select-product-modal',
  templateUrl: './select-product.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectProductModal implements OnInit {

  loading = true;
  $types: Observable<ExtractionProduct[]>;

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
    this.$types = this.service.watchAll(this.filter, {});

    // Update loading indicator
    this.$types.pipe(first()).subscribe((_) => this.loading = false);
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

  getI18nTypeName(type: ExtractionProduct) {
    if (type.name) return type.name;
    const format = type.label && type.label.split('-')[0].toUpperCase();
    const key = `EXTRACTION.PRODUCT.${format}.TITLE`;

    const message = this.translate.instant(key);
    if (message !== key) return message;

    // No i18n message: compute a new one
    return type.name;
  }


  protected markForCheck() {
    this.cd.markForCheck();
  }

}

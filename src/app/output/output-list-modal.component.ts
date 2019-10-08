import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {AggregationTypeFilter, ExtractionService} from "../trip/services/extraction.service";
import {AggregationType} from "../trip/services/extraction.model";
import {Observable} from "rxjs";
import {first} from "rxjs/operators";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'app-map-select-modal',
  templateUrl: './output-list-modal.component.html',
  //styleUrls: ['./extraction-list-modal.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OutputSelectTypeModal implements OnInit {

  loading = true;
  $types: Observable<AggregationType[]>;

  @Input() filter: AggregationTypeFilter = {};
  @Input() program: string;

  constructor(
    protected viewCtrl: ModalController,
    protected service: ExtractionService,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {

  }

  ngOnInit() {

    // Load items
    this.$types = this.service.loadAggregationTypes(this.filter, {});

    // Update loading indicator
    this.$types.pipe(first()).subscribe((_) => this.loading = false);
  }

  selectType(type: AggregationType) {

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

  protected getI18nTypeName(type: AggregationType) {
    if (type.name) return type.name;
    const format = type.label && type.label.split('-')[0].toUpperCase();
    const key = `EXTRACTION.PRODUCT.${format}.TITLE`;
    let message = this.translate.instant(key);

    if (message !== key) {
      return message;
    }

    // No i18n message: compute a new one

    return type.name;
  }
}

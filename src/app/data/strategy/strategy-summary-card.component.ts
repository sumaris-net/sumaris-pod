import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
// import fade in animation
import {merge, Subscription} from "rxjs";
import {Router} from "@angular/router";
import {environment} from "../../../environments/environment";
import {AppRootDataEditor} from "../form/root-data-editor.class";
import {isNil, isNotNil} from "../../shared/functions";
import {fadeInAnimation} from "../../shared/material/material.animations";
import {Strategy} from "../../referential/services/model/strategy.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {ProgramService} from "../../referential/services/program.service";
import {StrategyService} from "../../referential/services/strategy.service";
import {ProgramProperties} from "../../referential/services/config/program.config";

export const STRATEGY_SUMMARY_DEFAULT_I18N_PREFIX = 'PROGRAM.STRATEGY.SUMMARY.';

@Component({
  selector: 'app-strategy-summary-card',
  templateUrl: './strategy-summary-card.component.html',
  styleUrls: ['./strategy-summary-card.component.scss'],
  animations: [fadeInAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategySummaryCardComponent<T extends Strategy<T> = Strategy<any>> implements OnInit, OnDestroy {

  private _debug = false;
  private _subscription = new Subscription();

  data: T = null;
  loading = true;
  displayAttributes: { [key: string]: string[]; } = {
    location: undefined,
    taxonName: undefined,
    taxonGroup: undefined,
  };

  @Input() i18nPrefix = STRATEGY_SUMMARY_DEFAULT_I18N_PREFIX;
  @Input() title: string;
  @Input() canOpen = false;

  @Input("value")
  set value(value: T) {
    this.updateView(value);
  }
  get value(): T {
    return this.data;
  }

  @Input() editor: AppRootDataEditor<any, any>;

  constructor (
    protected router: Router,
    protected localSettings: LocalSettingsService,
    protected programService: ProgramService,
    protected strategyService: StrategyService,
    protected cd: ChangeDetectorRef
  ) {

    Object.keys(this.displayAttributes).forEach(fieldName => {
      const defaultAttributes = fieldName === 'taxonName' ? ['name'] : ['label', 'name'];
      this.displayAttributes[fieldName] = localSettings.getFieldDisplayAttributes(fieldName, defaultAttributes);
    });
    this._debug = !environment.production;
  }

  ngOnInit(): void {

    // Check editor exists
    if (!this.editor) throw new Error("Missing mandatory 'editor' input!");

    this.title = this.title || (this.i18nPrefix + 'TITLE');

    // Subscribe to refresh events
    this._subscription
        .add(
            merge(
                this.editor.onUpdateView
            )
            .subscribe(() => this.updateView())
        );
  }

  ngOnDestroy(): void {
    this._subscription.unsubscribe();
  }

  /* -- protected method -- */

  protected updateView(data?: T) {
    data = data || this.data || (this.editor && this.editor.strategy as T)

    if (isNil(data) || isNil(data.id)) {
      this.loading = true;
      this.data = null;
      this.canOpen = false;
      this.markForCheck();
    }
    else if (this.data !== data){
      console.debug('[strategy-summary-card] updating view using strategy:', data);
      this.data = data;
      this.canOpen = this.strategyService.canUserWrite(data);
      this.loading = false;
      this.markForCheck();
    }
  }

  async open(event?: UIEvent): Promise<boolean> {

    const programId = this.data && this.data.programId;
    if (isNil(programId) || isNil(this.data.id)) return; // Skip

    const program = await this.programService.load(programId, {fetchPolicy: "cache-first"});
    const strategyEditor = program.getProperty(ProgramProperties.LANDING_EDITOR);
    return this.router.navigateByUrl(`/referential/programs/${programId}/strategies/${strategyEditor}/${this.data.id}`);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

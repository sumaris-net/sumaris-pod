import {Directive, OnDestroy, OnInit, Optional, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {MatTabChangeEvent, MatTabGroup} from "@angular/material/tabs";
import {Entity} from '../services/model/entity.model';
import {AlertController, ToastController} from '@ionic/angular';
import {TranslateService} from '@ngx-translate/core';
import {Subscription} from 'rxjs';
import {isNotNil, ToolbarComponent} from '../../shared/shared.module';
import {AppTable} from '../table/table.class';
import {AppForm} from './form.class';
import {FormButtonsBarComponent} from './form-buttons-bar.component';
import {AppFormUtils, IAppForm} from "./form.utils";
import {ToastOptions} from "@ionic/core";
import {Toasts} from "../../shared/toasts";
import {HammerSwipeAction, HammerSwipeEvent} from "../../shared/gesture/hammer.utils";

export class AppTabFormOptions {
  /**
   * Number of tab. 1 by default
   */
  tabCount?: number;

  /**
   * '200ms' by default
   */
  tabGroupAnimationDuration?: string;

  /**
   * Should enable swipe between tab, on touch screen
   */
  enableSwipe?: boolean; // true by default
}

@Directive()
export abstract class AppTabForm<T extends Entity<T>, O = any> implements IAppForm, OnInit, OnDestroy {

  private _children: IAppForm[];
  private _subscription = new Subscription();
  protected _enabled = false;
  protected _dirty = false;

  // From options
  tabCount: number;
  enableSwipe: boolean;
  tabGroupAnimationDuration = '200ms';

  debug = false;
  data: T;
  previousDataId: number;
  selectedTabIndex = 0;

  submitted = false;
  error: string;
  loading = true;
  queryParams: {
    tab?: number;
    subtab?: number;
    [key: string]: any
  };

  protected toastController: ToastController;

  @ViewChild('tabGroup', { static: true }) tabGroup: MatTabGroup;
  @ViewChild(ToolbarComponent, { static: true }) appToolbar: ToolbarComponent;
  @ViewChild(FormButtonsBarComponent, { static: true }) formButtonsBar: FormButtonsBarComponent;

  get isNewData(): boolean {
    return !this.data || this.data.id === undefined || this.data.id === null;
  }

  get tables(): AppTable<any>[] {
    return this._children && (this._children.filter(c => c instanceof AppTable) as AppTable<any>[]);
  }

  get dirty(): boolean {
    return this._dirty || (this._children && this._children.find(c => c.dirty) && true);
  }

  /**
   * Is valid (tables and forms)
   */
  get valid(): boolean {
    // Important: Should be not invalid AND not pending, so use '!valid' (DO NOT use 'invalid')
    return (!this._children || !this._children.find(form => !form.valid));
  }

  get invalid(): boolean {
    return this._children && this._children.find(c => c.invalid) && true;
  }

  get pending(): boolean {
    return this._children && this._children.find(c => c.pending) && true;
  }

  get enabled(): boolean {
    return this._enabled;
  }

  get disabled(): boolean {
    return !this._enabled;
  }

  get children(): IAppForm[] {
    return this._children;
  }

  get empty(): boolean {
    return this._enabled;
  }

  protected constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    @Optional() options?: AppTabFormOptions
  ) {
    options = {
      tabCount: 1,
      tabGroupAnimationDuration: '200ms',
      enableSwipe: true,
      ...options
    };
    this.tabCount = options.tabCount;
    this.tabGroupAnimationDuration = options.tabGroupAnimationDuration;
    this.enableSwipe = options.enableSwipe;
  }

  ngOnInit() {
    // Read route query parameters
    const queryParams = this.route.snapshot.queryParams;

    // Copy original queryParams, for reuse in onTabChange()
    this.queryParams = Object.assign({}, queryParams);

    if (this.tabGroup) {
      // Parse tab param
      if (this.tabCount > 1) {
        const tabIndex = queryParams["tab"];
        this.queryParams.tab = tabIndex && parseInt(tabIndex) || undefined;
        if (isNotNil(this.queryParams.tab)) {
          this.selectedTabIndex = this.queryParams.tab;
        }
      }
      this.tabGroup.realignInkBar();
    }

    // Catch back click events
    if (this.appToolbar) {
      this.registerSubscription(this.appToolbar.onBackClick.subscribe(event => this.onBackClick(event)));
    }
    if (this.formButtonsBar) {
      this.registerSubscription(this.formButtonsBar.onBack.subscribe(event => this.onBackClick(event)));
    }
  }

  ngOnDestroy() {
    this._subscription.unsubscribe();
  }

  abstract async load(id?: number, options?: O);

  abstract async save(event, options?: any): Promise<any>;

  addChildForm(form: IAppForm): AppTabForm<T> {
    if (!form) throw new Error('Trying to register an invalid form');
    this._children = this._children || [];
    this._children.push(form);
    return this;
  }

  addChildForms(forms: IAppForm[]): AppTabForm<T> {
    forms.forEach(form => this.addChildForm(form));
    return this;
  }

  disable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this._enabled = false;
    this._children && this._children.forEach(c => c.disable(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  enable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this._enabled = true;
    this._children && this._children.forEach(c => c.enable(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  markAsPristine(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this.error = null;
    this.submitted = false;
    this._dirty = false;
    this._children && this._children.forEach(c => c.markAsPristine(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  markAsUntouched(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    // TODO: check if need to pass opts or not ?
    //this._children && this._children.forEach(c => c.markAsUntouched(opts));
    this._children && this._children.forEach(c => c.markAsUntouched());

    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  markAsTouched(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this._children && this._children.forEach(c => c.markAsTouched(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  markAsDirty(opts?: {onlySelf?: boolean, emitEvent?: boolean; }){
    this._dirty = true;
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  onTabChange(event: MatTabChangeEvent, queryParamName?: string): boolean {
    queryParamName = queryParamName || 'tab';
    if (!this.queryParams || +this.queryParams[queryParamName] !== event.index) {

      this.queryParams = this.queryParams || {};
      this.queryParams[queryParamName] = event.index;

      if (queryParamName === 'tab' && isNotNil(this.queryParams.subtab)) {
        delete this.queryParams.subtab; // clean subtab
      }
      this.router.navigate(['.'], {
        relativeTo: this.route,
        queryParams: this.queryParams,
        replaceUrl: true
      });
      return true;
    }
    return false;
  }

  /**
   * Action triggered when user swipes
   */
  onSwipeTab(event: HammerSwipeEvent) {
    // DEBUG
    // if (this.debug) console.debug("[tab-page] onSwipeTab()");

    // Skip, if not a valid swipe event
    if (!this.enableSwipe || !event
      || event.defaultPrevented || (event.srcEvent && event.srcEvent.defaultPrevented)
      || event.pointerType !== 'touch'
    ) {
      return false;
    }

    // DEBUG
    //if (this.debug)
    //console.debug("[tab-page] Detected swipe: " + event.type, event);

    let selectTabIndex = this.selectedTabIndex;
    switch (event.type) {
      // Open next tab
      case "swipeleft":
        const isLast = selectTabIndex >= (this.tabCount - 1);
        selectTabIndex = isLast ? 0 : selectTabIndex + 1;
        break;

      // Open previous tab
      case "swiperight":
        const isFirst = selectTabIndex <= 0;
        selectTabIndex = isFirst ? this.tabCount : selectTabIndex - 1;
        break;

      // Other case
      default:
        console.error("[tab-page] Unknown swipe action: " + event.type);
        return false;
    }

    setTimeout(() => {
      this.selectedTabIndex = selectTabIndex;
      this.markForCheck();
    });

    return true;
  }

  onSubTabChange(event: MatTabChangeEvent) {
    this.onTabChange(event, 'subtab');
  }

  async cancel(event?: Event) {
    if (!this.dirty) return;
    await this.reload();
  }

  /**
   * Unload the page (remove all data). Useful when reusing angular cache a cancelled page
   * @param opts
   */
  async unload(opts?: {emitEvent?: boolean; }) {
    console.debug("[tab-page] Unloading data...");
    this.loading = true;
    this.selectedTabIndex = 0;
    this._children.forEach(f => {
      if (f instanceof AppForm) {
        f.reset(null, opts);
      }
      else if (f instanceof AppTable) {
        f.dataSource.disconnect();
      }
    });
    // TODO: find a way to remove current page from the navigation history
  }

  onBackClick(event: Event) {
    if (event.defaultPrevented) return;

    // Stop the go back event, to be able to override it
    event.preventDefault();

    setTimeout(async () => {
      const dirty = this.dirty;
      let confirm = !dirty;
      let save = false;

      if (dirty) {

        let alert;
        // Ask user before
        if (this.valid) {
          const translations = this.translate.instant(['COMMON.BTN_CANCEL', 'COMMON.BTN_SAVE', 'COMMON.BTN_ABORT_CHANGES',
            'CONFIRM.SAVE_BEFORE_CLOSE', 'CONFIRM.ALERT_HEADER']);
          alert = await this.alertCtrl.create({
            header: translations['CONFIRM.ALERT_HEADER'],
            message: translations['CONFIRM.SAVE_BEFORE_CLOSE'],
            buttons: [
              {
                text: translations['COMMON.BTN_CANCEL'],
                role: 'cancel',
                cssClass: 'secondary',
                handler: () => {
                }
              },
              {
                text: translations['COMMON.BTN_ABORT_CHANGES'],
                cssClass: 'secondary',
                handler: () => {
                  confirm = true;
                }
              },
              {
                text: translations['COMMON.BTN_SAVE'],
                handler: () => {
                  save = true;
                  confirm = true;
                }
              }
            ]
          });
        } else {
          const translations = this.translate.instant(['COMMON.BTN_ABORT_CHANGES', 'COMMON.BTN_CANCEL', 'CONFIRM.CANCEL_CHANGES', 'CONFIRM.ALERT_HEADER']);

          alert = await this.alertCtrl.create({
            header: translations['CONFIRM.ALERT_HEADER'],
            message: translations['CONFIRM.CANCEL_CHANGES'],
            buttons: [
              {
                text: translations['COMMON.BTN_ABORT_CHANGES'],
                cssClass: 'secondary',
                handler: () => {
                  confirm = true; // update upper value
                }
              },
              {
                text: translations['COMMON.BTN_CANCEL'],
                role: 'cancel',
                handler: () => {
                }
              }
            ]
          });
        }
        await alert.present();
        await alert.onDidDismiss();

      }


      if (confirm) {
        if (save) {
          await this.save(event); // sync save
        } else if (dirty) {
          if (this.isNewData) {
            this.unload(); // async reset
          }
          else {
            this.doReload(); // async reload
          }
        }

        // Execute the action
        this.appToolbar.goBack();
      }

    }, 300);
  }

  async reload(confirm?: boolean) {
    const needConfirm = this.dirty;
    // if not confirm yet: ask confirmation
    if (!confirm && needConfirm) {
      const translations = this.translate.instant(['COMMON.BTN_ABORT_CHANGES', 'COMMON.BTN_CANCEL', 'CONFIRM.CANCEL_CHANGES', 'CONFIRM.ALERT_HEADER']);
      const alert = await this.alertCtrl.create({
        header: translations['CONFIRM.ALERT_HEADER'],
        message: translations['CONFIRM.CANCEL_CHANGES'],
        buttons: [
          {
            text: translations['COMMON.BTN_ABORT_CHANGES'],
            cssClass: 'secondary',
            handler: () => {
              confirm = true; // update upper value
            }
          },
          {
            text: translations['COMMON.BTN_CANCEL'],
            role: 'cancel',
            handler: () => {
            }
          }
        ]
      });
      await alert.present();
      await alert.onDidDismiss();
    }

    // If confirm: execute the reload
    if (confirm || !needConfirm) {
      this.scrollToTop();
      this.disable();
      return await this.doReload();
    }
  }

  async doReload() {
    this.loading = true;
    await this.load(this.data && this.data.id);
  }



  /* -- protected methods -- */

  protected async scrollToTop() {
    // TODO: FIXME (not working as the page is not the window)
    const scrollToTop = window.setInterval(() => {
      const pos = window.pageYOffset;
      if (pos > 0) {
        window.scrollTo(0, pos - 20); // how far to scroll on each step
      } else {
        window.clearInterval(scrollToTop);
      }
    }, 16);
  }

  protected registerSubscription(sub: Subscription) {
    this._subscription.add(sub);
  }

  protected async saveIfDirtyAndConfirm(): Promise<boolean> {
    if (!this.dirty) return true;

    let confirm = false;
    let cancel = false;
    const translations = this.translate.instant(['COMMON.BTN_SAVE', 'COMMON.BTN_CANCEL', 'COMMON.BTN_ABORT_CHANGES', 'CONFIRM.SAVE', 'CONFIRM.ALERT_HEADER']);
    const alert = await this.alertCtrl.create({
      header: translations['CONFIRM.ALERT_HEADER'],
      message: translations['CONFIRM.SAVE'],
      buttons: [
        {
          text: translations['COMMON.BTN_CANCEL'],
          role: 'cancel',
          cssClass: 'secondary',
          handler: () => {
            cancel = true;
          }
        },
        {
          text: translations['COMMON.BTN_ABORT_CHANGES'],
          cssClass: 'secondary',
          handler: () => {
          }
        },
        {
          text: translations['COMMON.BTN_SAVE'],
          handler: () => {
            confirm = true; // update upper value
          }
        }
      ]
    });
    await alert.present();
    await alert.onDidDismiss();

    if (!confirm) {
      return !cancel;
    }

    const saved = await this.save(event);
    return saved;
  }

  protected async showToast(opts: ToastOptions & { error?: boolean; }) {
    if (!this.toastController) throw new Error("Missing toastController in component's constructor");
    await Toasts.show(this.toastController, this.translate, opts);
  }

  protected logFormErrors() {
    if (this.debug) console.debug("[root-editor-form] Page not valid. Checking where (forms, tables)...");
    this._children.forEach(c => {
      // If form
      if (c instanceof AppForm) {
        if (!c.empty && !c.valid) {
          if (c.pending) {
            console.warn( `[root-editor-form] [${c.constructor.name.toLowerCase()}] - pending form state`);
            AppFormUtils.waitWhilePending(c)
              .then(() => c.invalid && AppFormUtils.logFormErrors(c.form, `[root-editor-form] [${c.constructor.name.toLowerCase()}] `));
          }
          else {
            AppFormUtils.logFormErrors(c.form, `[root-editor-form] [${c.constructor.name.toLowerCase()}] `);
          }
        }
      }
      // If table
      else if (c instanceof AppTable) {
        if (c.invalid && c.editedRow && c.editedRow.validator) {
          AppFormUtils.logFormErrors(c.editedRow.validator, `[root-editor-form] [${c.constructor.name.toLowerCase()}] `);
        }
      }
    });
  }

  protected markForCheck() {
    // Should be override by subclasses, if change detection is Push
  }
}

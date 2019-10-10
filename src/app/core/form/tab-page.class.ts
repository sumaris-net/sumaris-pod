import {OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {MatTabChangeEvent} from "@angular/material";
import {Entity} from '../services/model';
import {AlertController} from '@ionic/angular';
import {TranslateService} from '@ngx-translate/core';
import {Subscription} from 'rxjs';
import {isNotNil, ToolbarComponent} from '../../shared/shared.module';
import {AppTable} from '../table/table.class';
import {AppForm} from './form.class';
import {FormButtonsBarComponent} from './form-buttons-bar.component';
import {AppFormUtils} from "./form.utils";

export abstract class AppTabPage<T extends Entity<T>, F = any> implements OnInit, OnDestroy {

  private _forms: AppForm<any>[];
  private _tables: AppTable<any, any>[];
  private _subscription = new Subscription();

  debug = false;
  data: T;
  selectedTabIndex = 0;
  submitted = false;
  error: string;
  loading = true;
  enabled = false;
  queryParams: {
    tab?: number;
    subtab?: number;
    [key: string]: any
  };

  @ViewChild(ToolbarComponent) appToolbar: ToolbarComponent;
  @ViewChild(FormButtonsBarComponent) formButtonsBar: FormButtonsBarComponent;

  get isNewData(): boolean {
    return !this.data || this.data.id === undefined || this.data.id === null;
  }

  get dirty(): boolean {
    return (this._forms && !!this._forms.find(form => form.dirty)) || (this._tables && !!this._tables.find(table => table.dirty));
  }

  /**
   *
   */
  get valid(): boolean {
    // Important: Should be not invalid AND not pending, so use '!valid' (and NOT 'invalid')
    return (!this._forms || !this._forms.find(form => !form.valid)) && (!this._tables || !this._tables.find(table => !table.valid));
  }

  get invalid(): boolean {
    return (this._forms && this._forms.find(form => form.invalid) && true) || (this._tables && this._tables.find(table => table.invalid) && true);
  }

  get pending(): boolean {
    return (this._forms && !!this._forms.find(form => form.pending)) || (this._tables && !!this._tables.find(table => table.pending));
  }

  protected get tables(): AppTable<any, any>[] {
    return this._tables;
  }

  protected constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected alertCtrl: AlertController,
    protected translate: TranslateService
  ) {

  }

  ngOnInit() {
    // Listen route parameters

    const queryParams = this.route.snapshot.queryParams;

    // Copy original queryParams, for reuse in onTabChange()
    this.queryParams = Object.assign({}, queryParams);

    // Parse tab
    const tabIndex = queryParams["tab"];
    this.queryParams.tab = tabIndex && parseInt(tabIndex) || undefined;

    if (isNotNil(this.queryParams.tab)) {
      this.selectedTabIndex = this.queryParams.tab;
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

  abstract async load(id?: number, options?: F);

  abstract async save(event): Promise<any>;

  registerForm(form: AppForm<any>): AppTabPage<T, F> {
    if (!form) throw 'Trying to register an invalid form';
    this._forms = this._forms || [];
    this._forms.push(form);
    return this;
  }

  registerForms(forms: AppForm<any>[]): AppTabPage<T, F> {
    forms.forEach(form => this.registerForm(form));
    return this;
  }

  registerTable(table: AppTable<any, any>): AppTabPage<T, F> {
    if (!table) throw new Error('Trying to register an invalid table');
    this._tables = this._tables || [];
    this._tables.push(table);
    return this;
  }

  registerTables(tables: AppTable<any, any>[]): AppTabPage<T, F> {
    tables
      .filter(table => isNotNil(table)) // Skip not found tables
      .forEach(table => this.registerTable(table));
    return this;
  }

  disable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this.enabled = false;
    this._forms && this._forms.forEach(form => form.disable(opts));
    this._tables && this._tables.forEach(table => table.disable(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  enable(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this.enabled = true;
    this._forms && this._forms.forEach(form => form.enable(opts));
    this._tables && this._tables.forEach(table => table.enable(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  markAsPristine(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this.error = null;
    this.submitted = false;
    this._forms && this._forms.forEach(form => form.markAsPristine(opts));
    this._tables && this._tables.forEach(table => table.markAsPristine(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  markAsUntouched(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this._forms && this._forms.forEach(form => form.markAsUntouched());
    this._tables && this._tables.forEach(table => table.markAsUntouched(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  markAsTouched(opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    this._forms && this._forms.forEach(form => form.markAsTouched(opts));
    this._tables && this._tables.forEach(table => table.markAsTouched(opts));
    if (!this.loading && (!opts || opts.emitEvent !== false)) this.markForCheck();
  }

  onTabChange(event: MatTabChangeEvent, queryParamName?: string): boolean {
    queryParamName = queryParamName || 'tab';
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

  onSubTabChange(event: MatTabChangeEvent) {
    this.onTabChange(event, 'subtab');
  }

  async cancel() {
    if (!this.dirty) return;
    await this.reload();
  }

  onBackClick(event: Event) {
    if (event.defaultPrevented) return;

    // Stop the go back event, to be able to override it
    event.preventDefault();

    setTimeout(async () => {
      let confirm = !this.dirty;
      let save = false;

      if (!confirm) {

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
        } else if (this.dirty && this.data.id) {
          this.doReload(); // async reload
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
      let pos = window.pageYOffset;
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

    if (!confirm) return !cancel;

    const saved = await this.save(event);
    return saved;
  }

  protected logFormErrors() {
    if (this.debug) console.debug("[root-editor-form] Page not valid. Checking where (forms, tables)...");
    (this._forms || []).forEach(appForm => {
      if (!appForm.empty && appForm.invalid) {
        AppFormUtils.logFormErrors(appForm.form, `"[root-editor-form] [${appForm.constructor.name.toLowerCase()}] `);
      }
    });
    (this._tables || []).forEach(appTable => {
      if (appTable.invalid && appTable.editedRow && appTable.editedRow.validator) {
        AppFormUtils.logFormErrors(appTable.editedRow.validator, `"[root-editor-form] [${appTable.constructor.name.toLowerCase()}] `);
      }
    });
  }

  protected markForCheck() {
    // Should be override by subclasses, if change detection is Push
  }
}

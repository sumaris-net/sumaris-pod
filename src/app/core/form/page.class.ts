import { ViewChild, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params, NavigationEnd } from "@angular/router";
import { MatTabChangeEvent } from "@angular/material";
import { Entity, isNotNil } from '../services/model';
import { AlertController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { ToolbarComponent } from '../../shared/toolbar/toolbar';
import { AppTable } from '../table/table.class';
import { AppForm } from './form.class';
import { FormButtonsBarComponent } from './form-buttons-bar.component';
export abstract class AppTabPage<T extends Entity<T>, F = any> implements OnInit, OnDestroy {


    private _forms: AppForm<any>[];
    private _tables: AppTable<any, any>[];
    private _subscriptions: Subscription[];

    debug: boolean = false;
    data: T;
    selectedTabIndex: number = 0;
    submitted: boolean = false;
    error: string;
    loading: boolean = true;

    @ViewChild(ToolbarComponent) appToolbar: ToolbarComponent;
    @ViewChild(FormButtonsBarComponent) formButtonsBar: FormButtonsBarComponent;

    public get dirty(): boolean {
        return (this._forms && !!this._forms.find(form => form.dirty)) || (this._tables && !!this._tables.find(table => table.dirty));
    }

    public get valid(): boolean {
        return (!this._forms || !this._forms.find(form => !form.valid)) && (!this._tables || !this._tables.find(table => !table.valid));
    }

    public get invalid(): boolean {
        return (this._forms && !!this._forms.find(form => form.invalid)) || (this._tables && !!this._tables.find(table => !table.invalid));
    }

    constructor(
        protected route: ActivatedRoute,
        protected router: Router,
        protected alertCtrl: AlertController,
        protected translate: TranslateService
    ) {
        // Listen route parameters
        this.route.queryParams.subscribe(res => {
            const tabIndex = res["tab"];
            if (tabIndex !== undefined) {
                this.selectedTabIndex = parseInt(tabIndex);
            }
        });
    }

    ngOnInit() {
        // Catch back click events
        if (this.appToolbar) {
            this.registerSubscription(this.appToolbar.onBackClick.subscribe(event => this.onBackClick(event)));
        }
        if (this.formButtonsBar) {
            this.registerSubscription(this.formButtonsBar.onBack.subscribe(event => this.onBackClick(event)));
        }
    }

    ngOnDestroy() {
        if (this._subscriptions) {
            this._subscriptions.forEach(s => s.unsubscribe());
            this._subscriptions = undefined;
        }
    }

    abstract async load(id?: number, options?: F);

    abstract async save(event): Promise<any>;

    public registerForm(form: AppForm<any>): AppTabPage<T, F> {
        if (!form) throw 'Trying to register an invalid form';
        this._forms = this._forms || [];
        this._forms.push(form);
        return this;
    }

    public registerForms(forms: AppForm<any>[]): AppTabPage<T, F> {
        forms.forEach(form => this.registerForm(form));
        return this;
    }

    public registerTable(table: AppTable<any, any>): AppTabPage<T, F> {
        if (!table) throw 'Trying to register an invalid table';
        this._tables = this._tables || [];
        this._tables.push(table);
        return this;
    }

    public registerTables(tables: AppTable<any, any>[]): AppTabPage<T, F> {
        tables
            .filter(table => isNotNil(table)) // Skip not found tables
            .forEach(table => this.registerTable(table));
        return this;
    }

    public disable() {
        this._forms && this._forms.forEach(form => form.disable());
        this._tables && this._tables.forEach(table => table.disable());
    }

    public enable() {
        this._forms && this._forms.forEach(form => form.enable());
        this._tables && this._tables.forEach(table => table.enable());
    }

    public markAsPristine() {
        this.error = null;
        this.submitted = false;
        this._forms && this._forms.forEach(form => form.markAsPristine());
        this._tables && this._tables.forEach(table => table.markAsPristine());
    }

    public markAsUntouched() {
        this._forms && this._forms.forEach(form => form.markAsUntouched());
        this._tables && this._tables.forEach(table => table.markAsUntouched());
    }

    public markAsTouched() {
        this._forms && this._forms.forEach(form => form.markAsTouched());
        this._tables && this._tables.forEach(table => table.markAsTouched());
    }


    public onTabChange(event: MatTabChangeEvent) {
        const queryParams: Params = Object.assign({}, this.route.snapshot.queryParams);
        queryParams['tab'] = event.index;
        this.router.navigate(['.'], {
            relativeTo: this.route,
            queryParams: queryParams
        });
    }

    async cancel() {
        if (!this.dirty) return;
        await this.reload();
    };

    onBackClick(event: MouseEvent) {
        // Stop the go back event, to be able to overide it
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
                }
                else {
                    const translations = this.translate.instant(['COMMON.BTN_ABORT_CHANGES', 'COMMON.BTN_CANCEL', 'CONFIRM.CANCEL_CHANGES', 'CONFIRM.ALERT_HEADER']);

                    alert = await this.alertCtrl.create({
                        header: translations['CONFIRM.ALERT_HEADER'],
                        message: translations['CONFIRM.CANCEL_CHANGES'],
                        buttons: [
                            {
                                text: translations['COMMON.BTN_ABORT_CHANGES'],
                                role: 'cancel',
                                cssClass: 'secondary',
                                handler: () => {
                                    confirm = true; // update upper value
                                }
                            },
                            {
                                text: translations['COMMON.BTN_CANCEL'],
                                handler: () => { }
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
                }
                else if (this.dirty && this.data.id) {
                    this.doReload(); // async reload
                }

                // Execute the action
                this.appToolbar.goBack();
            }

        });
    }

    public async reload(confirm?: boolean) {
        const needConfirm = this.dirty;
        // if not confirm yet: ask confirmation
        if (!confirm && needConfirm) {
            const translations = this.translate.instant(['COMMON.YES', 'COMMON.NO', 'CONFIRM.CANCEL_CHANGES', 'CONFIRM.ALERT_HEADER']);
            const alert = await this.alertCtrl.create({
                header: translations['CONFIRM.ALERT_HEADER'],
                message: translations['CONFIRM.CANCEL_CHANGES'],
                buttons: [
                    {
                        text: translations['COMMON.NO'],
                        role: 'cancel',
                        cssClass: 'secondary',
                        handler: () => { }
                    },
                    {
                        text: translations['COMMON.YES'],
                        handler: () => {
                            confirm = true; // update upper value
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

    public async doReload() {
        this.loading = true;
        await this.load(this.data && this.data.id);
    }

    /* -- protected methods -- */

    protected async scrollToTop() {
        // TODO: FIXME (not working as the page is not the window)
        let scrollToTop = window.setInterval(() => {
            let pos = window.pageYOffset;
            if (pos > 0) {
                window.scrollTo(0, pos - 20); // how far to scroll on each step
            } else {
                window.clearInterval(scrollToTop);
            }
        }, 16);
    }

    protected isNewData(): boolean {
        return !this.data || this.data.id === undefined || this.data.id === null
    }

    protected registerSubscription(sub: Subscription) {
        this._subscriptions = this._subscriptions || [];
        this._subscriptions.push(sub);
    }

}

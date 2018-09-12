import { OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params, NavigationEnd } from "@angular/router";
import { MatTabChangeEvent } from "@angular/material";
import { AppForm, AppTable } from '../../core/core.module';

export abstract class AppTabPage<T, F = any>{


    private _forms: AppForm<any>[];
    private _tables: AppTable<any, any>[];

    data: T;
    selectedTabIndex: number = 0;
    submitted: boolean = false;
    error: string;
    loading: boolean = true;

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
        protected router: Router
    ) {
        // Listen route parameters
        this.route.queryParams.subscribe(res => {
            const tabIndex = res["tab"];
            if (tabIndex !== undefined) {
                this.selectedTabIndex = parseInt(tabIndex);
            }
        });
    }

    abstract async cancel();

    abstract async load(id?: number, options?: F);

    abstract async save(event): Promise<any>;

    public registerForm(form: AppForm<any>) {
        if (!form) throw 'Trying to register an invalid form';
        this._forms = this._forms || [];
        this._forms.push(form);
    }

    public registertable(table: AppTable<any, any>) {
        if (!table) throw 'Trying to register an invalid table';
        this._tables = this._tables || [];
        this._tables.push(table);
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

    onTabChange(event: MatTabChangeEvent) {
        const queryParams: Params = Object.assign({}, this.route.snapshot.queryParams);
        queryParams['tab'] = event.index;
        this.router.navigate(['.'], {
            relativeTo: this.route,
            queryParams: queryParams
        });
    }


}

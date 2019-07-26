import {Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {BehaviorSubject} from 'rxjs';
import {FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {ConfigOption, ConfigOptions, Configuration, Department, EntityUtils} from '../../core/services/model';
import {ConfigService} from "src/app/core/services/config.service";
import {AppForm, AppFormUtils, ConfigValidatorService, isNil, PlatformService} from "src/app/core/core.module";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {FormArrayHelper} from "../../core/form/form.utils";


@Component({
  moduleId: module.id.toString(),
  selector: 'app-remote-config-page',
  templateUrl: 'config.component.html',
  styleUrls: ['./config.component.scss']
})
export class RemoteConfigPage extends AppForm<Configuration> implements OnInit {

  private _propertyOptionsCache: { [index: number]: ConfigOption } = {};

  saving = false;
  loading = true;
  partners = new BehaviorSubject<Department[]>(null);
  data: Configuration;

  options = Object.getOwnPropertyNames(ConfigOptions).map(name => ConfigOptions[name]);
  optionMap: { [key: string]: ConfigOption };

  propertiesFormHelper: FormArrayHelper<{key: string; value: string}>;

  get propertiesForm(): FormArray {
    return this.form.get('properties') as FormArray;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected route: ActivatedRoute,
    protected router: Router,
    protected service: ConfigService,
    protected validator: ConfigValidatorService,
    protected formBuilder: FormBuilder,
    protected platform: PlatformService
      ) {
    super(dateAdapter, validator.getFormGroup());

    this.optionMap = {};
    this.options.forEach(o => {
      this.optionMap[o.key] = o;
    });
  };

  async ngOnInit() {

    this.propertiesFormHelper = new FormArrayHelper<{key: string; value: string}>(
      this.formBuilder,
      this.form,
      'properties',
      (value) => this.validator.getPropertyFormGroup(value),
      (v1, v2) => (!v1 && !v2) || v1.key === v2.key,
      (value) => isNil(value) || (isNil(value.key) && isNil(value.value))
    );

    // Wait plateform is ready
    await this.platform.ready();

    // Then, load
    this.load();
  }

  async load() {
    this.loading = true;
    let data;

    // Get data
    try {
      data = await this.service.load({fetchPolicy: "network-only"});
    }
    catch(err) {
      this.error = err && err.message || err;
      console.error(err);
      return;
    }

    // Update the UI
    this.updateView(data);
  }

  updateView(data: Configuration) {
    if (!data) return; //skip
    this.data = data;

    const json = data.asObject();

    // Transform properties map into array
    json.properties = EntityUtils.getObjectAsArray(data.properties || {});
    this.propertiesFormHelper.resize(Math.max(json.properties.length, 1));

    this.form.patchValue(json, {emitEvent: false});
    this.markAsPristine();

    this.partners.next(json.partners);
    this.loading = false;
  }

  async save($event: any, json?: any) {
    if (this.saving) return; // skip
    if (this.form.invalid) {
      AppFormUtils.logFormErrors(this.form);
      return;
    }
    console.debug("[config] Saving local settings...");

    this.saving = true;
    this.error = undefined;

    json = json || this.form.value;
    this.data.fromObject(json);

    this.disable();

    try {

      // Call save service
      const updatedData = await this.service.save(this.data);
      // Update the view
      this.updateView(updatedData);
      this.form.markAsUntouched();

    }
    catch(err) {
      this.error = err && err.message || err;
    }
    finally {
      this.enable();
      this.saving = false;
    }
  }

  getPropertyOption(index: number): ConfigOption {
    let option = this._propertyOptionsCache[index];
    if (!option) {
      option = this.updatePropertyOption(index);
      this._propertyOptionsCache[index] = option;
    }
    return option;
  }

  updatePropertyOption(index: number): ConfigOption {
    const optionKey = (this.propertiesForm.at(index) as FormGroup).controls.key.value;
    const option = optionKey && this.optionMap[optionKey] || null;
    return option;
  }

  removePartner(icon: String){
    console.log("remove Icon " + icon);
  }

  async cancel() {
    await this.load();
  }

}


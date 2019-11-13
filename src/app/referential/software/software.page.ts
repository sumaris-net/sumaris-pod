import {ChangeDetectionStrategy, Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {BehaviorSubject, Subject} from 'rxjs';
import {FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {ConfigOptions, Configuration, Department, EntityUtils, StatusIds, DefaultStatusList} from '../../core/services/model';
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {AppFormUtils, FormArrayHelper} from "../../core/form/form.utils";
import {FormFieldDefinition, FormFieldDefinitionMap, FormFieldValue} from "../../shared/form/field.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppForm, ConfigValidatorService, isNil, isNotNil} from "../../core/core.module";
import {ConfigService} from "../../core/services/config.service";
import {toInt, trimEmptyToNull} from "../../shared/functions";
import {TranslateService} from "@ngx-translate/core";


@Component({
  moduleId: module.id.toString(),
  selector: 'app-software-page',
  templateUrl: 'software.page.html',
  styleUrls: ['./software.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SoftwarePage extends AppForm<Configuration> implements OnInit {


  saving = false;
  loading = true;
  partners = new BehaviorSubject<Department[]>(null);
  data: Configuration;
  $title = new Subject<string>();
  statusList = DefaultStatusList;
  statusById;
  propertyDefinitions: FormFieldDefinition[] = Object.getOwnPropertyNames(ConfigOptions).map(name => ConfigOptions[name]);
  propertyDefinitionsByKey: FormFieldDefinitionMap = {};
  propertyDefinitionsByIndex: { [index: number]: FormFieldDefinition } = {};
  propertiesFormHelper: FormArrayHelper<FormFieldValue>;

  get propertiesForm(): FormArray {
    return this.form.get('properties') as FormArray;
  }

  get isNewData(): boolean {
    return !this.data || isNil(this.data.id);
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected route: ActivatedRoute,
    protected router: Router,
    protected service: ConfigService,
    protected validator: ConfigValidatorService,
    protected formBuilder: FormBuilder,
    protected platform: PlatformService,
    protected translate: TranslateService
      ) {
    super(dateAdapter, validator.getFormGroup());

    // Fill propertyDefinitionMap
    this.propertyDefinitions.forEach(o => this.propertyDefinitionsByKey[o.key] = o);

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
  }

  async ngOnInit() {

    this.propertiesFormHelper = new FormArrayHelper<FormFieldValue>(
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

    // Load by label is any
    const id = toInt(this.route.snapshot.params['id']);
    const label = trimEmptyToNull(this.route.snapshot.queryParams['label']);

    // Get data
    try {
      data = await this.service.load(label, {fetchPolicy: "network-only"});

      // Check if load [id + label] are those existing in the URL
      if (isNotNil(label) && data && data.id !== id) {
        throw new Error('Invalid configuration load. Expected id=' + id + ' but found ' + data.id);
      }
    }
    catch (err) {
      this.error = err && err.message || err;
      console.error(err);
      this.loading = false;
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

    this.setValue(json, {emitEvent: false});
    this.markAsPristine();

    this.computeTitle();

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
    catch (err) {
      this.error = err && err.message || err;
    }
    finally {
      this.enable();
      this.saving = false;
    }
  }

  getPropertyDefinition(index: number): FormFieldDefinition {
    let definition = this.propertyDefinitionsByIndex[index];
    if (!definition) {
      definition = this.updatePropertyDefinition(index);
      this.propertyDefinitionsByIndex[index] = definition;
    }
    return definition;
  }

  updatePropertyDefinition(index: number): FormFieldDefinition {
    const key = (this.propertiesForm.at(index) as FormGroup).controls.key.value;
    const definition = key && this.propertyDefinitionsByKey[key] || null;
    this.propertyDefinitionsByIndex[index] = definition; // update map by index
    return definition;
  }

  removePropertyAt(index: number) {
    this.propertiesFormHelper.removeAt(index);
    this.propertyDefinitionsByIndex = {}; // clear map by index
    this.markForCheck();
  }

  removePartner(icon: String){
    console.log("remove Icon " + icon);
  }

  async computeTitle() {
    if (this.isNewData) {
      this.$title.next(await this.translate.get('CONFIGURATION.NEW.TITLE').toPromise());
    }
    else {
      this.$title.next(await this.translate.get('CONFIGURATION.EDIT.TITLE', this.data).toPromise());
    }
  }

  async cancel() {
    await this.load();
  }

}


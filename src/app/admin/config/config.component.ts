import {Component, OnInit} from "@angular/core";

import {Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {BehaviorSubject} from 'rxjs';
import {FormArray, FormBuilder} from "@angular/forms";
import {Configuration, Department} from '../../core/services/model';
import {ConfigService} from "src/app/core/services/config.service";
import {AppForm, AppFormUtils, ConfigValidatorService, isNotNil} from "src/app/core/core.module";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {first} from "rxjs/operators";


export declare type ConfigOption = {
  key: string;
  label: string;
  defaultValue?: string;
  isTransient?: boolean;
}

const CONFIG_OPTIONS: Array<ConfigOption> = [
  {
    key: 'sumaris.logo',
    label: 'CONFIGURATION.OPTIONS.LOGO'
  },
  {
    key: 'sumaris.favicon',
    label: 'CONFIGURATION.OPTIONS.FAVICON'
  },
  {
    key: 'sumaris.defaultLocale',
    label: 'CONFIGURATION.OPTIONS.DEFAULT_LOCALE'
  },
  {
    key: 'sumaris.defaultLatLongFormat',
    label: 'CONFIGURATION.OPTIONS.DEFAULT_LATLONG_FORMAT'
  },
  {
    key: 'sumaris.logo.large',
    label: 'CONFIGURATION.OPTIONS.HOME.LOGO_LARGE'
  },
  {
    key: 'sumaris.partner.departments',
    label: 'CONFIGURATION.OPTIONS.HOME.PARTNER_DEPARTMENTS'
  },
  {
    key: 'sumaris.background.images',
    label: 'CONFIGURATION.OPTIONS.HOME.BACKGROUND_IMAGES'
  },
  {
    key: 'sumaris.color.primary',
    label: 'CONFIGURATION.OPTIONS.COLORS.PRIMARY'
  },
  {
    key: 'sumaris.color.secondary',
    label: 'CONFIGURATION.OPTIONS.COLORS.SECONDARY'
  },
  {
    key: 'sumaris.color.tertiary',
    label: 'CONFIGURATION.OPTIONS.COLORS.TERTIARY'
  }
];

@Component({
  moduleId: module.id.toString(),
  selector: 'page-config',
  templateUrl: 'config.component.html',
  styleUrls: ['./config.component.scss']
})
export class ConfigPage extends AppForm<Configuration> implements OnInit {

  partners = new BehaviorSubject<Department[]>(null);
  data: Configuration;
  loading: boolean = true;

  options = CONFIG_OPTIONS;

  get propertiesForm() : FormArray {
    return this.form.get('properties') as FormArray;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: Platform,
    protected route: ActivatedRoute,
    protected router: Router,
    protected service: ConfigService,
    protected validator: ConfigValidatorService,
    protected formBuilder: FormBuilder 
      ) {
    super(dateAdapter, platform, validator.getFormGroup());

  };

  ngOnInit() {

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
    this.data = data;
    this.form = this.validator.getFormGroup(data);
    if (data) this.partners.next(data.partners);
    this.loading = false;
  }

  async save($event: any, json?: any) {

    json = json || this.form.value;
    this.data.fromObject(json);

    this.disable();

    try {

      // Call save service
      const updatedData = await this.service.save(this.data);
      // Update the view
      this.updateView(updatedData);
      this.form.markAsUntouched();
      this.error = null;
    }
    catch(err) {
      this.error = err && err.message || err;
    }
    finally {
      this.enable();
    }
  }


  removePartner(icon: String){
    console.log("remove Icon " + icon);
  }

  async cancel() {
    await this.load();
  }

  addProperty(property?: {key: string; value: string}) {
    const control = this.propertiesForm;
    control.push(this.validator.getPropertyFormGroup(property));
  }

  removeProperty($event: MouseEvent, index: number) {
    const control = this.propertiesForm;

    // Do not remove if last item, but clear it
    if (control.length == 1) {
      control.at(0).setValue({key: null, value: null});
      return;
    }

    control.removeAt(index);

    this.form.markAsDirty();
  }
}


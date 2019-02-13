import { Component, OnInit, ViewChild } from "@angular/core";


import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Subscription, BehaviorSubject } from 'rxjs';

import { Location } from '@angular/common';
import { FormGroup, FormBuilder } from "@angular/forms";
import { environment } from '../../../environments/environment';
import { Configuration, Property, Department } from '../../core/services/model';
import { PodConfigService } from "src/app/core/services/podconfig.service";
import { AppFormUtils } from "src/app/core/core.module";
import { CarouselComponent } from "./carousel/carousel.component";

//Function that calculates de css to load
export function getBrandCSS(styles: string[]): string[] {
  alert("getBrandCSS " + environment.defaultProgram + "    " + styles);

  for (let i = 0; i < styles.length; i++) {
    //alert("getBrandCSS "+ environment.defaultProgram + "    " +styles);
    //console.log(environment.defaultProgram + "    " + styles[i]);
    styles[i] = environment.defaultProgram + '.' + styles[i];
  }
  return styles;
}
export function getFormValueFromEntityProperty(source: Configuration, form: FormGroup): { [key: string]: string } {
  const value = {};
  source.properties.forEach(prop => {
    let key = prop['name'];
    let val = prop['label'];
    console.log(key,val);
    if(form.controls[key])
      value[key] = val;
  });
  return value;
}


@Component({
  moduleId: module.id.toString(),
  selector: 'page-podconfig',
  templateUrl: 'podconfig.html',
  styleUrls: ['./podconfig.css']
})
export class PodConfigPage implements OnInit {
  departements = new BehaviorSubject<Department[]>(null);
  data: Configuration;
  form: FormGroup;
  loading: boolean = true;
  error: string;
  


  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected carousel: CarouselComponent,
    public modalCtrl: ModalController,
    protected configurationService: PodConfigService,
    protected formBuilder: FormBuilder 
      ) {
 
     this.form =  formBuilder.group({   
       'defaultProgram': [''],
       'baseUrl': [''],
       'remoteBaseUrl': [''],
       'version': [''],
       'defaultLatLongFormat': ['']
     });

     
     this.configurationService.getDepartments().then(de =>{
      this.departements.next(de);
      console.log("depService.logos " +  de .map(d=>d.logo) );
     } );
    

  };

  ngOnInit() {
    this.load();
  }

  async load() {

    const data = await this.configurationService.getConfs();
    console.dir(data);

    this.updateView(data);
  }

  removeIcon(icon: string){
    console.log(icon);
  }

  updateView(data: Configuration) {

    console.dir(data);
    this.data = data;

    const json = getFormValueFromEntityProperty(data, this.form);
    this.form.setValue(json);

    this.loading = false;
  }

 

  save($event: any) {

    const json = this.form.value;
    this.data.fromObject(json);

    this.form.disable();

    // Call service
    try {

      console.log(" Saving  ", this.form.value);
      //await this.configurationService.save(this.data);

      this.form.markAsUntouched();
    }
    catch(err) {
      this.error = err && err.message || err;
    }
    finally {
      this.form.enable();
    }

    
  }

  get dirty(): boolean {
    return this.form.dirty;
  }
}


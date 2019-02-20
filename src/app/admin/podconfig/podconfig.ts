import {Component, OnInit} from "@angular/core";

import {ModalController} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {BehaviorSubject} from 'rxjs';
import {FormBuilder, FormGroup} from "@angular/forms";
import {environment} from '../../../environments/environment';
import {Configuration, Department} from '../../core/services/model';
import {PodConfigService} from "src/app/core/services/podconfig.service";
import {AppFormUtils} from "src/app/core/core.module";
import {CarouselComponent} from "./carousel/carousel.component";

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
 

@Component({
  moduleId: module.id.toString(),
  selector: 'page-podconfig',
  templateUrl: 'podconfig.html',
  styleUrls: ['./podconfig.css']
})
export class PodConfigPage implements OnInit {
  partners = new BehaviorSubject<Department[]>(null);
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
       'name': [''], 
       'label': [''], 
     });

  };

  ngOnInit() {
    this.load();
  }

  async load() {

    // this.configurationService.getDepartments().then(de =>{
    //   this.departements.next(de);
    //   console.log("depService.logos " +  de .map(d=>d.logo) );
    // });

    const data = await this.configurationService.getConfs();
    console.dir(data);



    this.updateView(data);
  }

  removeIcon(icon: String){
    console.log("remove Icon " + icon);
  }

  // removeBG(bgImage: string){
  //   console.log("remove BackGround " + bgImage);
  //   this.configurationService.removeBackGround();
  // }

  updateView(data: Configuration) {

    console.dir(data);
    this.data = data;

    const json = AppFormUtils.getFormValueFromEntity(data, this.form);
    this.form.setValue(json);

    this.partners.next(data.partners);

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


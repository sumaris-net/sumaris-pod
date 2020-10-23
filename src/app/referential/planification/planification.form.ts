import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material/core";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {FormBuilder} from "@angular/forms";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {AppForm, ReferentialRef, IReferentialRef} from '../../core/core.module';
import {StatusIds} from "../../core/services/model/model.enum";
import {BehaviorSubject} from "rxjs";
import { Planification } from 'src/app/trip/services/model/planification.model';
import { PlanificationValidatorService } from 'src/app/trip/services/validator/planification.validator';
import { Program } from '../services/model/program.model';

@Component({
  selector: 'form-planification',
  templateUrl: './planification.form.html',
  styleUrls: ['./planification.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {provide: PlanificationValidatorService}
  ],
})
export class PlanificationForm extends AppForm<Planification> implements OnInit {

  protected formBuilder: FormBuilder;
  private _taxonNameSubject = new BehaviorSubject<IReferentialRef[]>(undefined);
  private _laboratoryubject = new BehaviorSubject<IReferentialRef[]>(undefined);
  private _fishingAreaSubject = new BehaviorSubject<IReferentialRef[]>(undefined);
  private _eotpSubject = new BehaviorSubject<IReferentialRef[]>(undefined);
  private _landingAreaSubject = new BehaviorSubject<IReferentialRef[]>(undefined);

  
  
  private eotpList: Array<{id,label: string, name: string, statusId : number, entityName: string}> = [
    {id: '1', label: 'P1', name: 'Projet 1', statusId:1,entityName:"Eotp"},
    {id: '2', label: 'P2', name: 'Projet 2', statusId:1,entityName:"Eotp"},
    {id: '3', label: 'P3', name: 'Projet 3',statusId:1,entityName:"Eotp"},
];

  mobile: boolean;
  enableTaxonNameFilter = false;
  canFilterTaxonName = true;

  @Input() program: Program;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: PlanificationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getFormGroup(), settings);
  }

  ngOnInit() {

    //set current year
    const currentYear = new Date ();
    this.form.get('year').setValue(currentYear);

    // taxonName combo
    this.registerAutocompleteField('taxonName', {
      /*suggestFn: (value, filter) => this.referentialRefService.suggest(value, {
        entityName: 'TaxonName',
        statusId: StatusIds.ENABLE
      }),*/
      items: this._taxonNameSubject,
      mobile: this.mobile
    });
    this.loadTaxonNames();


      // laboratory combo ------------------------------------------------------------
      this.registerAutocompleteField('laboratory', {
        items: this._laboratoryubject,
        mobile: this.mobile
      });

      this.loadDepartment();

      // fishingArea combo ------------------------------------------------------------
      this.registerAutocompleteField('fishingArea', {
        items: this._fishingAreaSubject,
        mobile: this.mobile
      });
      this.loadFishingAreas();
    
      // landingArea combo ------------------------------------------------------------
      this.registerAutocompleteField('landingArea', {
        items: this._landingAreaSubject,
        mobile: this.mobile
      });
      this.loadLandingAreas();

     // eotp combo -------------------------------------------------------------------
     this.registerAutocompleteField('eotp', {
      items: this._eotpSubject,
      mobile: this.mobile
    });
    this.loadEotps();


      
  }



  /*setValue(data: Test, opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    // Use label and name from metier.taxonGroup
    if (data && data.metier) {
      data.metier = data.metier.clone(); // Leave original object unchanged
      data.metier.label = data.metier.taxonGroup && data.metier.taxonGroup.label || data.metier.label;
      data.metier.name = data.metier.taxonGroup && data.metier.taxonGroup.name || data.metier.name;
    }
    super.setValue(data, opts);
  }*/

  // save buttonn
  save(){
    console.log("save work");

    console.log(this.form.get("comment"));

  /* console.log("comment : "+this.form.get("comment").value);*/
  }

  cancel(){
    console.log("cancel works");
  }

  add(){
    console.log("add works");
  }

  close(){
    console.log("close works");
  }
  
  toggleFilteredTaxonName() {
      this.enableTaxonNameFilter = !this.enableTaxonNameFilter;
      this.loadTaxonNames();
      console.log("enableTaxonNameFilter: " + this.enableTaxonNameFilter);
  }

  /* -- protected methods -- */

  protected async loadTaxonNames() {
    console.log("loadTaxonNames works");
    const taxonNameControl = this.form.get('taxonName');
    taxonNameControl.enable();
    // Refresh taxonNames
    if (this.enableTaxonNameFilter) {
      const taxonNames = await this.loadFilteredTaxonNamesMethod();
      this._taxonNameSubject.next(taxonNames);
    } else {
      const taxonNames = await this.loadTaxonNamesMethod();
      this._taxonNameSubject.next(taxonNames);
    }
  }

  // Load taxonName Service
  protected async loadTaxonNamesMethod(): Promise<ReferentialRef[]> {
    console.log("loadTaxonName works");
    const res = await this.referentialRefService.loadAll(0, 200, null, null, {entityName: "TaxonName"});
    return res.data;
  }

  // Load Filtered taxonName Service
  protected async loadFilteredTaxonNamesMethod(): Promise<ReferentialRef[]> {
    console.log("loadFilteredTaxonName works");
    // TODO replace with dataService.loadAlreadyFilledTaxonName(0, 200, null, null)
    const res = await this.referentialRefService.loadAll(2, 3, null, null, {entityName: "TaxonName"});
    return res.data;
  }


  //department-----------------------------------------------------------------------------------------------

     protected async loadDepartment() {
      const departmentControl = this.form.get('laboratory');
      departmentControl.enable();
        // Refresh departments
        const departments = await this.loadDepartmentsMethod();
        this._laboratoryubject.next(departments);
    }
  
    // Load department Service
    protected async loadDepartmentsMethod(): Promise<ReferentialRef[]> {
      const res = await this.referentialRefService.loadAll(0, 200, null,null, 
        {
          entityName: "Department"   
        });
  
        console.log("data departement :"+res.data);
      return res.data;
    }


  //fishing area ( zone en mer) ---------------------------------------------------------------------------------
  protected async loadFishingAreas() {
    const fishingAreaControl = this.form.get('fishingArea');
    fishingAreaControl.enable();
      // Refresh fishingAreas
      const fishingAreas = await this.loadFishingAreasMethod();
      this._fishingAreaSubject.next(fishingAreas);
  }

  // Load fishingAreas Service
  protected async loadFishingAreasMethod(): Promise<ReferentialRef[]> {
    const res = await this.referentialRefService.loadAll(0, 200, null,null, 
      {
        entityName: "Location",
        //statusId : 1,
        levelId : 2
      });

    return res.data;
  }    

  //landingArea  ( zone terrestre) ---------------------------------------------------------------------------------
  protected async loadLandingAreas() {
    const landingAreaControl = this.form.get('landingArea');
    landingAreaControl.enable();
      // Refresh landingArea
      const landingAreas = await this.loadLandingAreasMethod();
      this._landingAreaSubject.next(landingAreas);
  }

  // Load landingArea Service
  protected async loadLandingAreasMethod(): Promise<ReferentialRef[]> {
    const res = await this.referentialRefService.loadAll(0, 200, null,null, 
      {
        entityName: "Location",
        //statusId : 1,
        levelId : 3
      });

    return res.data;
  }    

  //Eotp en mode bouchon------------------------------------------------------------------------------------------------
  protected  loadEotps() {
    const eotpAreaControl = this.form.get('eotp');
    eotpAreaControl.enable();
      // Refresh fishingAreas
      const eotps =  this.eotpList; 
      this._eotpSubject.next(eotps);
  }
 //--------------------------------------------------------------------------------------------------------------------
   

}

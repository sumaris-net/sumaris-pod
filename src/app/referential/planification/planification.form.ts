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
  private _calcifiedTypeSubject = new BehaviorSubject<IReferentialRef[]>(undefined);



  private eotpList: Array<{id,label: string, name: string, statusId : number, entityName: string}> = [
    {id: '1', label: 'P1', name: 'Projet 1', statusId:1,entityName:"Eotp"},
    {id: '2', label: 'P2', name: 'Projet 2', statusId:1,entityName:"Eotp"},
    {id: '3', label: 'P3', name: 'Projet 3',statusId:1,entityName:"Eotp"},
  ];

  private FiltredEotpList: Array<{id,label: string, name: string, statusId : number, entityName: string}> = [
    {id: '4', label: 'P4', name: 'Projet 4', statusId:1,entityName:"Eotp"},
    {id: '5', label: 'P5', name: 'Projet 5', statusId:1,entityName:"Eotp"},
    {id: '6', label: 'P6', name: 'Projet 6',statusId:1,entityName:"Eotp"},
  ];

  mobile: boolean;
  enableTaxonNameFilter = true;
  canFilterTaxonName = true;

  enableEotpFilter = true;
  canFilterEotp = true;

  enableLaboratoryFilter = true;
  canFilterLaboratory = true;

  enableFishingAreaFilter = true;
  canFilterFishingArea = true;


  enableLandingAreaFilter = true;
  canFilterLandingArea = true;

  enableCalcifiedTypeFilter = true;
  canFilterCalcifiedType = true;


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

    // Calcified type combo ------------------------------------------------------------
      this.registerAutocompleteField('calcifiedType', {
        items: this._calcifiedTypeSubject,
        mobile: this.mobile
      });
      this.loadCalcifiedType();

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
  }

  /* -- protected methods -- */
 //Taxons -----------------------------------------------------------------------------------------------
  protected async loadTaxonNames() {
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
    // TODO replace with dataService.loadAlreadyFilledTaxonName(0, 200, null, null)
    const res = await this.referentialRefService.loadAll(2, 3, null, null, {entityName: "TaxonName"});
    return res.data;
  }


  //department(laboratory)---------------------------------------------------------------------------------------------
      toggleFilteredLaboratory(){
        this.enableLaboratoryFilter = !this.enableLaboratoryFilter;
        this.loadDepartment();
        console.log("enableLaboratoryFilter: " + this.enableLaboratoryFilter);
      }
     protected async loadDepartment() {
      const departmentControl = this.form.get('laboratory');
      departmentControl.enable();
        // Refresh filtred departments
        if (this.enableLaboratoryFilter) {
          const departments = await this.loadFiltredDepartmentsMethod();
          this._laboratoryubject.next(departments);
        } else {
          // Refresh filtred departments
          const departments = await this.loadDepartmentsMethod();
          this._laboratoryubject.next(departments);
        }
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

     //TODO : Load filtred department Service : another service to implement
     protected async loadFiltredDepartmentsMethod(): Promise<ReferentialRef[]> {
      const res = await this.referentialRefService.loadAll(0, 200, null,null,
        {
          entityName: "Department"
        });
        return res.data;
    }


  //fishing area ( zone en mer) ---------------------------------------------------------------------------------
  toggleFilteredFishingArea(){
    this.enableFishingAreaFilter = !this.enableFishingAreaFilter;
    this.loadFishingAreas();
    console.log("enableFishingAreaFilter: " + this.enableFishingAreaFilter);
  }
  protected async loadFishingAreas() {
    const fishingAreaControl = this.form.get('fishingArea');
    fishingAreaControl.enable();

      // Refresh fishingAreas
      if (this.enableFishingAreaFilter) {
        const fishingAreas = await this.loadFiltredFishingAreasMethod();
        this._fishingAreaSubject.next(fishingAreas);

      } else {
        // Refresh filtredfishingAreas
        const fishingAreas = await this.loadFishingAreasMethod();
        this._fishingAreaSubject.next(fishingAreas);
      }
  }

  // Load fishingAreas Service
  protected async loadFishingAreasMethod(): Promise<ReferentialRef[]> {
    const res = await this.referentialRefService.loadAll(0, 200, null,null,
      {
        entityName: "Location",
        //statusId : 1,
        levelId : 5
      });

    return res.data;
  }

   // TODO : Load fishingAreas Service : another service to implement
   protected async loadFiltredFishingAreasMethod(): Promise<ReferentialRef[]> {
    const res = await this.referentialRefService.loadAll(0, 200, null,null,
      {
        entityName: "Location",
        //statusId : 1,
        levelId : 5
      });

    return res.data;
  }

  //landingArea  ( zone terrestre) ---------------------------------------------------------------------------------
  toggleFilteredLandingArea(){
    this.enableLandingAreaFilter = !this.enableLandingAreaFilter;
    this.loadLandingAreas();
    console.log("enableLandingAreaFilter: " + this.enableLandingAreaFilter);
  }
  protected async loadLandingAreas() {
    const landingAreaControl = this.form.get('landingArea');
    landingAreaControl.enable();

  // Refresh landingArea
  if (this.enableLandingAreaFilter) {
    const landingAreas = await this.loadFiltredLandingAreasMethod();
      this._landingAreaSubject.next(landingAreas);

  } else {
     // Refresh filtred landingArea
     const landingAreas = await this.loadLandingAreasMethod();
     this._landingAreaSubject.next(landingAreas);
   }
  }

  // Load landingArea Service
  protected async loadLandingAreasMethod(): Promise<ReferentialRef[]> {
    const res = await this.referentialRefService.loadAll(0, 200, null,null,
      {
        entityName: "Location",
        //statusId : 1,
        levelId : 2
      });

    return res.data;
  }

 // TODO : Load filtred landing Service : another service to implement
  protected async loadFiltredLandingAreasMethod(): Promise<ReferentialRef[]> {
    const res = await this.referentialRefService.loadAll(0, 200, null,null,
      {
        entityName: "Location",
        //statusId : 1,
        levelId : 2
      });

    return res.data;
  }


  // Calcified Type ---------------------------------------------------------------------------------------------
        toggleFilteredCalcifiedType(){
          this.enableCalcifiedTypeFilter = !this.enableCalcifiedTypeFilter;
          this.loadCalcifiedType();
          console.log("enableCalcifiedTypeFilter: " + this.enableCalcifiedTypeFilter);
        }

        private calcifiedTypesList: Array<{id,label: string, name: string, statusId : number, entityName: string}> = [
          {id: '1', label: 'écaille', name: 'écaille', statusId:1,entityName:"calcifiedType"},
          {id: '2', label: 'illicium', name: 'illicium', statusId:1,entityName:"calcifiedType"},
          {id: '3', label: 'vertèbre', name: 'vertèbre',statusId:1,entityName:"calcifiedType"},
          {id: '4', label: 'otolithe', name: 'otolithe',statusId:1,entityName:"calcifiedType"}
        ];
      private filteredcalcifiedTypesList: Array<{id,label: string, name: string, statusId : number, entityName: string}> = [
          {id: '1', label: 'écaille', name: 'écaille', statusId:1,entityName:"calcifiedType"},
          {id: '2', label: 'illicium', name: 'illicium', statusId:1,entityName:"calcifiedType"}
        ];
       protected async loadCalcifiedType() {
        const calcifiedTypeControl = this.form.get('calcifiedType');


        calcifiedTypeControl.enable();
          // Refresh filtred departments
          if (this.enableCalcifiedTypeFilter) {
            //const calcifiedTypes = await this.loadFilteredCalcifiedTypesMethod();
            // Mocked data
           const calcifiedTypes = this.filteredcalcifiedTypesList;
           this._calcifiedTypeSubject.next(calcifiedTypes);
          } else {
            // Refresh filtred departments
            //const calcifiedTypes = await this.loadCalcifiedTypesMethod();
            // Mocked data
             const calcifiedTypes = this.calcifiedTypesList;
             this._calcifiedTypeSubject.next(calcifiedTypes);
          }
      }

    // Load CalcifiedTypes Service
    protected async loadCalcifiedTypesMethod(): Promise<ReferentialRef[]> {
      const res = await this.referentialRefService.loadAll(0, 200, null,null,
        {
          entityName: "CalcifiedTypes"
        });

        console.log("data CalcifiedTypes :"+res.data);
      return res.data;
    }

     //TODO : Load filtred CalcifiedTypes Service : another service to implement
     protected async loadFilteredCalcifiedTypesMethod(): Promise<ReferentialRef[]> {
      const res = await this.referentialRefService.loadAll(0, 200, null,null,
        {
          entityName: "CalcifiedTypes"
        });
        return res.data;
    }

  //Eotp en mode bouchon------------------------------------------------------------------------------------------------
  toggleFilteredEotp() {
    this.enableEotpFilter = !this.enableEotpFilter;
    //TODO :loadEotp()
    this.loadEotps();
    console.log("enableEotpFilter: " + this.enableEotpFilter);
   }

  protected  loadEotps() {
    const eotpAreaControl = this.form.get('eotp');
    eotpAreaControl.enable();

    if (this.enableEotpFilter) {
      // Refresh filtred eotp
      const eotps =  this.FiltredEotpList;
      this._eotpSubject.next(eotps);
    }
    else {
      // Refresh eotp
      const eotps =  this.eotpList;
      this._eotpSubject.next(eotps);
    }
  }
 //--------------------------------------------------------------------------------------------------------------------


}

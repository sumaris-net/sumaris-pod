import {ChangeDetectionStrategy, Component, Injector, OnInit} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {AcquisitionLevelCodes, LocationLevelIds, PmfmIds} from "../../referential/services/model/model.enum";
import {LandingPage} from "../landing/landing.page";
import {LandingValidatorService} from "../services/validator/landing.validator";
import {debounceTime, filter, map, mergeMap, startWith, switchMap, tap} from "rxjs/operators";
import {BehaviorSubject, Observable, Subscription} from "rxjs";
import {Landing} from "../services/model/landing.model";
import {AuctionControlValidators} from "../services/validator/auction-control.validators";
import {ModalController} from "@ionic/angular";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {fadeInOutAnimation, isNil, isNotEmptyArray, isNotNil} from "../../shared/shared.module";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {HistoryPageReference} from "../../core/services/model/settings.model";
import {ObservedLocation} from "../services/model/observed-location.model";
import {FormBuilder, FormGroup} from "@angular/forms";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {TaxonGroupRef} from "../../referential/services/model/taxon.model";
import {filterNotNil} from "../../shared/observables";
import {toNumber} from "../../shared/functions";
import {ExtractionHelpModal} from "../../extraction/help/help.modal";
import {AppHelpModal} from "../../shared/help/help.modal";

@Component({
  selector: 'app-auction-control',
  styleUrls: ['auction-control.page.scss'],
  templateUrl: './auction-control.page.html',
  providers: [
    {provide: ValidatorService, useExisting: LandingValidatorService}
  ],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuctionControlPage extends LandingPage implements OnInit {

  private _rowValidatorSubscription: Subscription;

  filterForm: FormGroup;

  $pmfms: Observable<PmfmStrategy[]>;
  $taxonGroupPmfm = new BehaviorSubject<PmfmStrategy>(null);
  $taxonGroups = new BehaviorSubject<TaxonGroupRef[]>(null);
  selectedTaxonGroup$: Observable<TaxonGroupRef>;
  showSamplesTable = false;
  helpContent: string;

  constructor(
    injector: Injector,
    protected referentialRefService: ReferentialRefService,
    protected formBuilder: FormBuilder,
    protected modalCtrl: ModalController
  ) {
    super(injector, {
      pathIdAttribute: 'controlId',
      tabCount: 2
    });

    this.filterForm = this.formBuilder.group({
      taxonGroup: [null]
    });
  }


  ngOnInit() {
    super.ngOnInit();

    // Default location levels ids
    this.landingForm.locationLevelIds = [LocationLevelIds.AUCTION];

    // Configure sample table
    this.samplesTable.inlineEdition = !this.mobile;

  }

  async ngAfterViewInit() {
    super.ngAfterViewInit();

    // Get program taxon groups
    this.registerSubscription(
      this.programSubject
        .pipe(
          filter(isNotNil),
          mergeMap((programLabel) => this.programService.loadTaxonGroups(programLabel))
        )
        .subscribe(taxonGroups => {
          console.debug("[control] Program taxonGroups: ", taxonGroups);
          this.$taxonGroups.next(taxonGroups);
        })
    );

    this.$pmfms = filterNotNil(this.$taxonGroups)
        .pipe(
          mergeMap(() => filterNotNil(this.landingForm.$pmfms)),
          map(pmfms => pmfms.map(pmfm => {
            // Controlled species PMFM
            if (pmfm.pmfmId === PmfmIds.CONTROLLED_SPECIES || pmfm.label === 'TAXON_GROUP') {
              console.debug(`[control] Replacing pmfm ${pmfm.label} qualitative values`);

              const taxonGroups = this.$taxonGroups.getValue();
              if (isNotEmptyArray(taxonGroups) && isNotEmptyArray(pmfm.qualitativeValues)) {
                pmfm = pmfm.clone(); // Clone (to keep unchanged the original pmfm)

                // Replace QV.name
                pmfm.qualitativeValues = pmfm.qualitativeValues.reduce((res, qv) => {
                  const taxonGroup = taxonGroups.find(tg => tg.label === qv.label);
                  // If not found in strategy's taxonGroups: ignore
                  if (!taxonGroup) {
                    console.warn(`Ignore invalid QualitativeValue {label: ${qv.label}} (not found in taxon groups of programe ${this.landingForm.program})`);
                    return res;
                  }
                  // Replace the QV name, using the taxon group name
                  qv.name = taxonGroup.name;
                  qv.entityName = taxonGroup.entityName || 'QualitativeValue';
                  return res.concat(qv);
                }, []);
              }
              else {
                console.debug(`[control] No qualitative values to replace, or no taxon groups in the strategy`);
              }

              this.$taxonGroupPmfm.next(pmfm);
            }

            // Force other Pmfm to optional (if in on field)
            else if (this.isOnFieldMode){
              pmfm = pmfm.clone(); // Skip original pmfm safe
              pmfm.required = false;
            }
            return pmfm;
          }))
        );

    // Get the taxon group control control
    this.selectedTaxonGroup$ = this.$taxonGroupPmfm
      .pipe(
        map(pmfm => pmfm && this.form.get( `measurementValues.${pmfm.pmfmId}`)),
        filter(isNotNil),
        switchMap(control => control.valueChanges
          .pipe(
            startWith<any, any>(control.value),
            debounceTime(250)
          )),
        // Update the help content
        tap(qv => {
          // TODO BLA load description, in the executeImport process
          console.log("TODO: update help modal with QV=", qv);
          this.helpContent = qv && qv.description || undefined;
        }),
        map(qv => {
          return ReferentialUtils.isNotEmpty(qv)
            && this.$taxonGroups.getValue().find(tg => tg.label === qv.label)
            || undefined;
        })
      );

    this.registerSubscription(
      this.selectedTaxonGroup$.pipe(
        filter(isNotNil),
        tap(taxonGroup => {
          console.debug('[control] Selected taxon group:', taxonGroup);
          this.samplesTable.defaultTaxonGroup = taxonGroup;
          this.samplesTable.showTaxonGroupColumn = ReferentialUtils.isEmpty(taxonGroup);
        }),
        mergeMap(taxonGroup => this.programService.loadProgramPmfms(this.programSubject.getValue(), {
          acquisitionLevel: AcquisitionLevelCodes.SAMPLE,
          taxonGroupId: toNumber(taxonGroup && taxonGroup.id, undefined)
        })),
        tap(async (pmfms) => {
          console.debug('[control] Applying taxon group PMFMs:', pmfms);
          if (this.samplesTable.dirty) {
            await this.samplesTable.save();
          }
          this.samplesTable.pmfms = pmfms;
          this.showSamplesTable = true;
        })
      ).subscribe());
  }

  onInitSampleForm({form, pmfms}) {
    // Remove previous subscription
    if (this._rowValidatorSubscription) {
      this._rowValidatorSubscription.unsubscribe();
    }

    // Add computation and validation
    this._rowValidatorSubscription = AuctionControlValidators.addSampleValidators(form, pmfms, {markForCheck: () => this.markForCheck()});
  }


  protected async onNewEntity(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {

    await super.onNewEntity(data, options);

    // Define default back link
    const observedLocationId = this.parent && this.parent.id || data && data.observedLocationId;
    this.defaultBackHref = `/observations/${observedLocationId}?tab=1`;
  }

  protected async onEntityLoaded(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onEntityLoaded(data, options);

    // Send landing date time to sample tables, but leave empty if FIELD mode (= will use current date)
    this.samplesTable.defaultSampleDate = this.isOnFieldMode ? undefined : data.dateTime;

    // Always open the second tab, when existing entity
    this.selectedTabIndex = 1;
    this.tabGroup.realignInkBar();

    // Define default back link
    const observedLocationId = this.parent && this.parent.id || data && data.observedLocationId;
    this.defaultBackHref = `/observations/${observedLocationId}?tab=1`;

    this.markForCheck();
  }

  updateView(data: Landing | null, opts?: {
    emitEvent?: boolean;
    openTabIndex?: number;
    updateRoute?: boolean;
  }) {
    // if vessel given in query params
    if (this.isNewData && this.route.snapshot.queryParams['vessel']) {
      // Open the second tab
      opts = {...opts, openTabIndex: 1};
    }

    super.updateView(data, opts);

    // Configure landing form
    this.landingForm.showLocation = false;
    this.landingForm.showDateTime = false;
    this.landingForm.showObservers = false;
  }

  async save(event?: Event, options?: any): Promise<boolean> {
    return super.save(event, options);
  }

  async openHelpModal(event?: UIEvent) {
    const modal = await this.modalCtrl.create({
      component: AppHelpModal,
      componentProps: {
        title: 'COMMON.BTN_SHOW_HELP',
        markdownContent: this.helpContent
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    await modal.onDidDismiss();
  }

  // protected async getValue(): Promise<Landing> {
  //   const data = await super.getValue();
  //
  //   // Make sure to set all samples attributes
  //   const generatedPrefix = this.samplesTable.acquisitionLevel + '#';
  //   console.log("Will update generate label");
  //   (data.samples || []).forEach(s => {
  //     // Always fill label
  //     if (isNilOrBlank(s.label)) {
  //       s.label = generatedPrefix + s.rankOrder;
  //     }
  //
  //     // Always use same taxon group
  //     s.taxonGroup = this.samplesTable.defaultTaxonGroup;
  //   });
  //
  //   return data;
  // }

  /* -- protected method -- */

  protected async setValue(data: Landing): Promise<void> {
    await super.setValue(data);
  }

  protected async computeTitle(data: Landing): Promise<string> {
    const titlePrefix = this.parent && this.parent instanceof ObservedLocation &&
      await this.translate.get('AUCTION_CONTROL.TITLE_PREFIX', {
        location: (this.parent.location && (this.parent.location.name || this.parent.location.label)),
        date: this.parent.startDateTime && this.dateFormat.transform(this.parent.startDateTime) as string || ''
      }).toPromise() || '';

    // new data
    if (!data || (isNil(data.id) && ReferentialUtils.isEmpty(data.vesselSnapshot))) {
      return titlePrefix + (await this.translate.get('AUCTION_CONTROL.NEW.TITLE').toPromise());
    }

    // Existing data
    return titlePrefix + (await this.translate.get('AUCTION_CONTROL.EDIT.TITLE', {
      vessel: data.vesselSnapshot && (data.vesselSnapshot.exteriorMarking || data.vesselSnapshot.name)
    }).toPromise());
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ... (await super.computePageHistory(title)),
      icon: 'flag'
    };
  }

  protected computePageUrl(id: number|'new') {
    const parentUrl = this.getParentPageUrl();
    return `${parentUrl}/control/${id}`;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.landingForm.invalid && !this.landingForm.measurementValuesForm.invalid ? 0 : (
      (this.samplesTable.invalid || this.landingForm.measurementValuesForm.invalid) ? 1 : -1);
  }
}

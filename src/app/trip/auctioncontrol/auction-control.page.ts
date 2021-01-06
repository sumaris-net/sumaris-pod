import {ChangeDetectionStrategy, Component, Injector, OnInit} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {LocationLevelIds, PmfmIds} from "../../referential/services/model/model.enum";
import {LandingPage} from "../landing/landing.page";
import {LandingValidatorService} from "../services/validator/landing.validator";
import {debounceTime, map, mergeMap, startWith} from "rxjs/operators";
import {BehaviorSubject, defer, forkJoin, Observable, Subscription} from "rxjs";
import {Landing} from "../services/model/landing.model";
import {AuctionControlValidators} from "../services/validator/auction-control.validators";
import {ModalController} from "@ionic/angular";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {HistoryPageReference} from "../../core/services/model/settings.model";
import {ObservedLocation} from "../services/model/observed-location.model";
import {FormBuilder, FormGroup} from "@angular/forms";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {fadeInOutAnimation} from "../../shared/material/material.animations";
import {isNil, isNotEmptyArray} from "../../shared/functions";

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

    this.registerAutocompleteField('taxonGroupFilter', {
      service: this.referentialRefService,
      filter: {
        entityName: 'TaxonGroup'
      }
    });
  }

  async ngAfterViewInit(): Promise<void> {
    await super.ngAfterViewInit();


    // Get program taxon groups
    const programTaxonGroups$ = defer(() => this.landingForm.ready())
      .pipe(
        map(() => this.landingForm.program),
        mergeMap(programLabel => this.programService.loadTaxonGroups(programLabel))
      );

    this.$pmfms =
      forkJoin([
        this.landingForm.$pmfms,
        programTaxonGroups$
      ])
      .pipe(
        map(([pmfms, taxonGroups]) => pmfms.map(pmfm => {

            // Controlled species PMFM
            if (pmfm.pmfmId === PmfmIds.CONTROLLED_SPECIES || pmfm.label === 'TAXON_GROUP') {
              console.debug(`[control] Replacing pmfm ${pmfm.label} qualitative values`);

              if (isNotEmptyArray(taxonGroups) && isNotEmptyArray(pmfm.qualitativeValues)) {
                pmfm = pmfm.clone(); // Clone (to keep unchanged the original pmfm)

                // Replace QV.name
                pmfm.qualitativeValues = pmfm.qualitativeValues.reduce((res, qv) => {
                  const tg = taxonGroups.find(tg => tg.label === qv.label);
                  // If not found in strategy's taxonGroups : ignore
                  if (!tg) {
                    console.warn(`Ignore invalid QualitativeValue {label: ${qv.label}} (not found in taxon groups of programe ${this.landingForm.program})`);
                    return res;
                  }
                  // Replace the QV name, using the taxon group name
                  qv.name = tg.name;
                  qv.entityName = tg.entityName || 'QualitativeValue';
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

    this.registerSubscription(
      forkJoin([
        this.$taxonGroupPmfm,
        programTaxonGroups$
      ])
        .pipe(
          mergeMap(([pmfm, taxonGroups]) => {
            const control = this.form.get( `measurementValues.${pmfm.pmfmId}`);
            return control.valueChanges.pipe(debounceTime(500))
              .pipe(
                startWith(control.value),
                map(qv => {
                  return ReferentialUtils.isNotEmpty(qv)
                    && taxonGroups.find(tg => tg.label === qv.label)
                    || undefined;
                })
              );
          })
        )
        .subscribe(taxonGroup => {
          console.debug('[control] Changing taxon group to:', taxonGroup);
          if (taxonGroup && !taxonGroup.entityName) {
            console.warn('[control] Settings manually entityName of taxon group:', taxonGroup);
            taxonGroup.entityName = 'TaxonGroup';
          }
          this.samplesTable.defaultTaxonGroup = taxonGroup;
          this.samplesTable.showTaxonGroupColumn = ReferentialUtils.isEmpty(taxonGroup);
          this.samplesTable.program = this.data.program && this.data.program.label;
          //this.samplesTable.markForCheck();
        }));
  }

  onStartSampleEditingForm({form, pmfms}) {
    // Remove previous subscription
    if (this._rowValidatorSubscription) {
      this._rowValidatorSubscription.unsubscribe();
    }

    // Add computation and validation
    this._rowValidatorSubscription = AuctionControlValidators.addSampleValidators(form, pmfms, {markForCheck: () => this.markForCheck()});
  }

  protected async onNewEntity(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {

    await super.onNewEntity(data, options);

    // if vessel given in query params
    if (this.route.snapshot.queryParams['vessel']) {

      // Open the second tab
      this.selectedTabIndex = 1;
      this.tabGroup.realignInkBar();
    }

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
    super.updateView(data, opts);

    // Configure landing form
    this.landingForm.showLocation = false;
    this.landingForm.showDateTime = false;
    this.landingForm.showObservers = false;
  }


  async save(event?: Event, options?: any): Promise<boolean> {
    return super.save(event, options);
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

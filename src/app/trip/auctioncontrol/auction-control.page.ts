import {ChangeDetectionStrategy, Component, Injector, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {LocationLevelIds, PmfmIds} from "../../referential/services/model/model.enum";
import {LandingPage} from "../landing/landing.page";
import {LandingValidatorService} from "../services/validator/landing.validator";
import {debounceTime, filter, map, mergeMap, startWith, switchMap} from "rxjs/operators";
import {from, Subscription} from "rxjs";
import {Landing} from "../services/model/landing.model";
import {AuctionControlValidators} from "../services/validator/auction-control.validators";
import {ModalController} from "@ionic/angular";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {fadeInOutAnimation, isNil, isNotNil} from "../../shared/shared.module";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {HistoryPageReference} from "../../core/services/model/settings.model";
import {ObservedLocation} from "../services/model/observed-location.model";
import {FormBuilder, FormGroup} from "@angular/forms";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";

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
    this.registerAutocompleteField('taxonGroupFilter', {
      service: referentialRefService,
      filter: {
        entityName: 'TaxonGroup'
      }
    })
  }

  ngOnInit() {
    super.ngOnInit();

    // Default location levels ids
    this.landingForm.locationLevelIds = [LocationLevelIds.AUCTION];

    // Configure sample table
    this.samplesTable.inlineEdition = !this.mobile;

    // Get the taxon group, by the PMFM 'CONTROLLED_SPECIES'
    this.registerSubscription(
      this.pmfms
        .pipe(
          map(pmfms => pmfms.find(p => p.pmfmId === PmfmIds.CONTROLLED_SPECIES || p.label === 'TAXON_GROUP')),
          filter(isNotNil),
          mergeMap(async (taxonGroupPmfm) => {
            await this.landingForm.ready();
            return taxonGroupPmfm;
          }),
          mergeMap((taxonGroupPmfm) => {
            // Load program taxon groups
            return from(this.programService.loadTaxonGroups(this.landingForm.program))
              .pipe(
                switchMap((taxonGroups) => {

                  // Update all qualitative values
                  taxonGroupPmfm.qualitativeValues = (taxonGroupPmfm.qualitativeValues || [])
                    .map(qv => {
                      const tg = taxonGroups.find(tg => tg.label === qv.label);
                      // If not found in strategies, remove the QV
                      if (!tg) return null;
                      // Replace the QV name, using the taxon group name
                      qv.name = tg.name;
                      qv.entityName = tg.entityName || 'QualitativeValue';
                      return qv;
                    })
                    .filter(isNotNil);

                  const control = this.form.get( 'measurementValues.' + taxonGroupPmfm.pmfmId);
                  // Listen every form value changes, to update default value
                  return control.valueChanges
                    .pipe(
                      debounceTime(500)
                    )
                    .pipe(
                      startWith(control.value),
                      map(qv => {
                        return ReferentialUtils.isNotEmpty(qv)
                        && taxonGroups.find(tg => tg.label === qv.label)
                        || undefined;
                      })
                    );
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
        })
    );
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

  protected addToPageHistory(page: HistoryPageReference) {
    super.addToPageHistory({ ...page, icon: 'flag'});
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

  protected computePageUrl(id: number|'new') {
    let parentUrl = this.getParentPageUrl();
    return `${parentUrl}/control/${id}`;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.landingForm.invalid && !this.landingForm.measurementValuesForm.invalid ? 0 : (
      (this.samplesTable.invalid || this.landingForm.measurementValuesForm.invalid) ? 1 : -1);
  }
}

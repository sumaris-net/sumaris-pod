import {ChangeDetectionStrategy, Component, Injector, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {EntityUtils, isNotNil, LocationLevelIds, PmfmIds} from "../../../referential/services/model";
import {LandingPage} from "../landing.page";
import {LandingValidatorService} from "../../services/landing.validator";
import {debounceTime, filter, map, mergeMap, switchMap} from "rxjs/operators";
import {Observable} from "rxjs";
import {AppFormUtils} from "../../../core/core.module";
import {Landing} from "../../services/model/landing.model";
import {isNilOrBlank} from "../../../shared/functions";

@Component({
  selector: 'app-landing-auction-control',
  templateUrl: './auction-control-landing.page.html',
  providers: [
    {provide: ValidatorService, useExisting: LandingValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AuctionControlLandingPage extends LandingPage implements OnInit {


  constructor(
    injector: Injector
  ) {
    super(injector);
  }

  ngOnInit() {
    super.ngOnInit();

    // Default location levels ids
    this.landingForm.locationLevelIds = [LocationLevelIds.AUCTION];

    // Get the taxon group, by the PMFM 'CONTROLLED_SPECIES'
    this.registerSubscription(
      this.pmfms
        .pipe(
          map(pmfms => pmfms.find(p => p.pmfmId === PmfmIds.CONTROLLED_SPECIES || p.label === 'TAXON_GROUP')),
          filter(isNotNil),
          mergeMap((taxonGroupPmfm) => {
            // Load program taxon groups
            return Observable.fromPromise(this.programService.loadTaxonGroups(this.landingForm.program))
              .pipe(
                switchMap(taxonGroups => {

                  // Update all qualitative values
                  taxonGroupPmfm.qualitativeValues = (taxonGroupPmfm.qualitativeValues || [])
                    .map(qv => {
                      const tg = taxonGroups.find(tg => tg.label === qv.label);
                      // If not found in strategies, remove the QV
                      if (!tg) return null;
                      // Replace the QV name, using the taxon group name
                      qv.name = tg.name;
                      return qv;
                    })
                    .filter(isNotNil);

                  const control = AppFormUtils.getControlFromPath(this.form, 'measurementValues.' + taxonGroupPmfm.pmfmId);
                  const actualQv = control.value;

                  // Update samples default taxon group
                  const actualTaxonGroup = EntityUtils.isNotEmpty(actualQv)
                    && taxonGroups.find(tg => tg.label === actualQv.label) || undefined;
                  this.samplesTable.defaultTaxonGroup = actualTaxonGroup;
                  this.samplesTable.showTaxonGroupColumn = EntityUtils.isEmpty(actualTaxonGroup);

                  // Listen every form value changes, to update default value
                  return control.valueChanges
                    .pipe(
                      debounceTime(500),
                      map(qv => EntityUtils.isNotEmpty(qv)
                        && taxonGroups.find(tg => tg.label === qv.label)
                        || undefined),
                    );
                })
              );
          })
        )
        .subscribe(actualTaxonGroup => {
          this.samplesTable.defaultTaxonGroup = actualTaxonGroup;
          this.samplesTable.showTaxonGroupColumn = EntityUtils.isEmpty(actualTaxonGroup);
        })
    );
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
}

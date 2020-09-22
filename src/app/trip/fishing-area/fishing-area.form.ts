import {ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {AppForm, isNotNil} from "../../core/core.module";
import {FishingArea} from "../services/model/fishing-area.model";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {FormBuilder} from "@angular/forms";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ModalController} from "@ionic/angular";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {NetworkService} from "../../core/services/network.service";
import {FishingAreaValidatorService} from "../services/validator/fishing-area.validator";
import {LocationLevelIds} from "../../referential/services/model/model.enum";
import {debounceTime, distinctUntilChanged, filter, pluck} from "rxjs/operators";
import {ReferentialUtils} from "../../core/services/model/referential.model";

@Component({
  selector: 'app-fishing-area-form',
  templateUrl: './fishing-area.form.html',
  styleUrls: ['./fishing-area.form.scss'],
})
export class FishingAreaForm extends AppForm<FishingArea> implements OnInit {

  mobile: boolean;

  @Input() required = true;
  @Input() showError = true;
  @Input() showDistanceToCoastGradient = true;
  @Input() showDepthGradient = true;
  @Input() showNearbySpecificArea = true;
  @Input() locationLevelIds = [LocationLevelIds.ICES_RECTANGLE];

  get empty(): boolean {
    const value = this.value;
    return (!value.location || !value.location.id)
      && (!value.distanceToCoastGradient || !value.distanceToCoastGradient.id)
      && (!value.depthGradient || !value.depthGradient.id)
      && (!value.nearbySpecificArea || !value.nearbySpecificArea.id)
  }

  get valid(): boolean {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected validatorService: FishingAreaValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    public network: NetworkService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getFormGroup(), settings);
    this.mobile = this.settings.mobile;
  }

  ngOnInit() {
    super.ngOnInit();

    // Set if required or not
    this.validatorService.updateFormGroup(this.form, {required: this.required});

    // Combo: fishing area
    this.registerAutocompleteField('location', {
      suggestFn: (value, filter) => this.referentialRefService.suggest(value, {
        entityName: 'Location',
        levelIds: this.locationLevelIds
      })
    });

    // Combo: distance to coast gradient
    this.registerAutocompleteField('distanceToCoastGradient', {
      suggestFn: (value, options) => this.suggest(value, options, 'DistanceToCoastGradient')
    });

    // Combo: depth gradient
    this.registerAutocompleteField('depthGradient', {
      suggestFn: (value, options) => this.suggest(value, options, 'DepthToCoastGradient')
    });

    // Combo: nearby specific area
    this.registerAutocompleteField('nearbySpecificArea', {
      suggestFn: (value, options) => this.suggest(value, options, 'NearbySpecificArea')
    });
  }

  private suggest(value: string, options: any, entityName: string) {
    return this.referentialRefService.suggest(value, {
        entityName: entityName,
        searchAttribute: options && options.searchAttribute
      },
      "rankOrder",
      "asc");
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }


}

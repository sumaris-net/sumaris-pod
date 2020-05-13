import {Component, Input, OnInit} from '@angular/core';
import {SaleValidatorService} from "../services/sale.validator";
import {Moment} from 'moment/moment';
import {AppForm, referentialToString, StatusIds} from '../../core/core.module';
import {DateAdapter} from "@angular/material/core";
import {LocationLevelIds, ReferentialRefService, VesselService} from '../../referential/referential.module';
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {Sale} from "../services/model/sale.model";

@Component({
  selector: 'form-sale',
  templateUrl: './sale.form.html',
  styleUrls: ['./sale.form.scss']
})
export class SaleForm extends AppForm<Sale> implements OnInit {

  @Input() required = true;
  @Input() showError = true;
  @Input() showVessel = true;
  @Input() showEndDateTime = true;
  @Input() showComment = true;
  @Input() showButtons = true;

  get empty(): any {
    const value = this.value;
    return (!value.saleLocation || !value.saleLocation.id)
      && (!value.startDateTime)
      && (!value.endDateTime)
      && (!value.saleType || !value.saleType.id)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected saleValidatorService: SaleValidatorService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter, saleValidatorService.getFormGroup(), settings);
  }

  ngOnInit() {
    super.ngOnInit();

    // Set if required or not
    this.saleValidatorService.updateFormGroup(this.form, {required: this.required});

    // Combo: vessels (if need)
    if (this.showVessel) {
      // Combo: vessels
      const vesselField = this.registerAutocompleteField('vesselSnapshot', {
        service: this.vesselSnapshotService,
        attributes: this.settings.getFieldDisplayAttributes('vesselSnapshot', ['exteriorMarking', 'name']),
        filter: {
          statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
        }
      });
      // Add base port location
      vesselField.attributes = vesselField.attributes.concat(this.settings.getFieldDisplayAttributes('location').map(key => 'basePortLocation.' + key));

    } else {
      this.form.get('vesselSnapshot').clearValidators();
    }

    // Combo: sale locations
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
      }
    });

    // Combo: sale types
    this.registerAutocompleteField('saleType', {
      service: this.referentialRefService,
      attributes: ['name'],
      filter: {
        entityName: 'SaleType'
      }
    });
  }

  referentialToString = referentialToString;
}

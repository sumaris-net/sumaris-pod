import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { isNil, isNotNil } from '@sumaris-net/ngx-components';
import { qualityFlagToColor } from '@app/data/services/model/model.utils';
import { Operation } from '@app/trip/services/model/trip.model';
import { QualityFlagIds } from '@app/referential/services/model/model.enum';
import { MatBadgeFill } from '@sumaris-net/ngx-components/src/app/shared/material/badge/badge-icon.directive';
import { AppColors } from '@app/shared/colors.utils';
import { QualityIonIcon } from '@app/data/quality/entity-quality-icon.component';

export declare type OperationMatSvgIcons = 'down-arrow' | 'rollback-arrow';
export declare type OperationIonIcon = 'navigate';


@Component({
  selector: 'app-operation-icon',
  templateUrl: 'operation-icon.component.html',
  styleUrls: ['./operation-icon.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationIconComponent {


  icon: OperationIonIcon = null;
  svgIcon: OperationMatSvgIcons = null;
  color: AppColors = null;
  badgeIcon: QualityIonIcon = null;
  badgeColor: AppColors = null;
  badgeFill: MatBadgeFill = 'clear';

  @Input() set value(value: Operation) {
    this.setValue(value);
  }

  get value(): Operation {
    return this._value;
  }

  @Input() set allowParentOperation(value: boolean) {
    if (this._allowParentOperation !== value) {
      this._allowParentOperation = value;
      if (this._value) this.setValue(this._value); // Recompute
    }
  }

  get allowParentOperation(): boolean {
    return this._allowParentOperation;
  }

  private _value: Operation;
  private _allowParentOperation: boolean;

  constructor(private cd: ChangeDetectorRef) {
  }

  setValue(value: Operation) {
    if (!value) {
      this.reset();
      return;
    }

    // DEBUG
    //console.debug('[operation-icon] Computing icon for', value);

    this._value = value;

    // Is child
    if (isNotNil(value.parentOperationId)) {
      this.svgIcon = 'rollback-arrow';
      this.icon = undefined;
    }
    // Is parent, and has a child
    else if (isNotNil(value.childOperationId) || value.qualityFlagId === QualityFlagIds.NOT_COMPLETED || this.allowParentOperation) {
      this.svgIcon = 'down-arrow';
      this.icon = undefined;
      if (isNil(value.childOperationId)) {
        this.badgeIcon = isNil(value.childOperationId) ? 'time' : undefined;
        this.badgeColor = 'accent';
        this.badgeFill = 'clear';
      }
    }
    // Other
    else {
      this.icon = 'navigate';
      this.svgIcon = undefined;
    }

    if (isNil(value.controlDate)) {
      this.badgeIcon = this.badgeIcon || undefined;
      this.color = this.color || 'secondary';
    }
    else if (isNil(value.qualificationDate)) {
      this.badgeIcon = 'checkmark';
      this.badgeColor = 'tertiary';
    }
    else if (isNil(value.qualityFlagId) || value.qualityFlagId === QualityFlagIds.NOT_QUALIFIED) {
        this.badgeIcon='checkmark-circle';
        this.badgeColor = 'tertiary';
    }
    else {
      if (value.qualityFlagId === QualityFlagIds.BAD) {
        this.badgeIcon = 'alert';
        this.badgeColor = 'danger';
        this.badgeFill = 'solid';
      }
      else {
        this.badgeIcon='flag';
        this.badgeColor = qualityFlagToColor(value.qualityFlagId);
      }
    }
    this.color = this.color || 'dark';

    this.cd.markForCheck();
  }

  private reset() {
    this.svgIcon = null;
    this.icon = null;
    this.badgeIcon = null;
    this.badgeFill = 'clear';
    this.badgeColor = null;
    this.cd.markForCheck();
  }
}

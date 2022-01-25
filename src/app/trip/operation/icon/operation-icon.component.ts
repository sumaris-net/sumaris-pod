import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, ViewEncapsulation} from '@angular/core';
import {isNil, isNotNil} from '@sumaris-net/ngx-components';
import {qualityFlagToColor} from '@app/data/services/model/model.utils';
import {Operation} from '@app/trip/services/model/trip.model';
import {QualityFlagIds} from '@app/referential/services/model/model.enum';
import {MatBadgeFill} from '@sumaris-net/ngx-components/src/app/shared/material/badge/badge-icon.directive';
import {AppColors} from '@app/shared/colors.utils';
import {QualityIonIcon} from '@app/data/quality/entity-quality-icon.component';

export declare type OperationMatSvgIcons = 'down-arrow' | 'rollback-arrow';
export declare type OperationIonIcon = 'navigate';


@Component({
  selector: 'app-operation-icon',
  templateUrl: 'operation-icon.component.html',
  styleUrls: ['./operation-icon.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None
})
export class OperationIconComponent {

  icon: OperationIonIcon = null;
  matSvgIcon: OperationMatSvgIcons = null;
  color: AppColors = null;
  badgeIcon: QualityIonIcon = null;
  badgeColor: AppColors = null;
  badgeFill: MatBadgeFill = 'clear';

  @Input() set error(error: any) {
    if (this._error !== error) {
      this._error = error;
      if (this._value) this.setValue(this._value); // Recompute
    }
  };

  get error(): any {
    return this._error;
  }

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
  private _error: any;

  constructor(private cd: ChangeDetectorRef) {
  }

  setValue(value: Operation) {
    this.reset();
    if (!value) {
      return;
    }

    // DEBUG
    //console.debug('[operation-icon] Computing icon for', value);

    this._value = value;

    // Is child
    if (isNotNil(value.parentOperationId)) {
      this.matSvgIcon = 'rollback-arrow';
      this.icon = undefined;
    }
    // Is parent, and has a child
    else if (isNotNil(value.childOperationId) || value.qualityFlagId === QualityFlagIds.NOT_COMPLETED || this.allowParentOperation) {
      this.matSvgIcon = 'down-arrow';
      this.icon = undefined;
      this.badgeIcon = isNil(value.childOperationId) ? 'time-outline' : undefined;
      this.badgeColor = this.badgeIcon && 'accent' || undefined;
    }
    // Other
    else {
      this.icon = 'navigate';
      this.matSvgIcon = undefined;
    }

    if (isNil(value.controlDate)) {
      this.color = this.color || 'secondary';
      if (this.error) {
        this.badgeIcon = 'alert';
        this.badgeColor = 'danger';
        this.badgeFill = 'solid';
      } else {
        this.badgeIcon = this.badgeIcon || undefined;
      }
    } else if (isNil(value.qualificationDate)) {
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
    this.icon = null;
    this.matSvgIcon = null;
    this.color = null;
    this.badgeIcon = null;
    this.badgeFill = 'clear';
    this.badgeColor = null;
    this.cd.markForCheck();
  }
}

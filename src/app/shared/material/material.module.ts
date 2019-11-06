import {NgModule} from "@angular/core";
import {
  MatAutocompleteModule, MatBadgeModule,
  MatButtonModule,
  MatButtonToggleModule,
  MatCardModule,
  MatCheckboxModule,
  MatDialogModule,
  MatExpansionModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatListModule,
  MatMenuModule,
  MatPaginatorModule,
  MatProgressBarModule,
  MatProgressSpinnerModule,
  MatRadioModule,
  MatSelectModule,
  MatSortModule,
  MatStepperModule,
  MatTableModule,
  MatTabsModule,
  MatToolbarModule,
  MatSlideToggleModule, MatRippleModule
} from "@angular/material";
import {CdkTableModule} from "@angular/cdk/table";
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatMomentDateModule} from '@angular/material-moment-adapter';

import {fadeInAnimation, slideInOutAnimation} from './material.animations';
import {InputElement} from './focusable';
import {A11yModule} from "@angular/cdk/a11y";
import {OverlayModule} from "@angular/cdk/overlay";
import {ScrollingModule} from "@angular/cdk/scrolling";
import {NgxMaterialTimepickerModule} from "ngx-material-timepicker";

export { fadeInAnimation, slideInOutAnimation, InputElement };

const modules: any[] = [
  MatTableModule,
  MatSortModule,
  MatAutocompleteModule,
  MatPaginatorModule,
  MatFormFieldModule,
  MatInputModule,
  CdkTableModule,
  MatDatepickerModule,
  MatMomentDateModule,
  MatCheckboxModule,
  MatExpansionModule,
  MatToolbarModule,
  MatDialogModule,
  MatIconModule,
  MatButtonModule,
  MatMenuModule,
  MatSelectModule,
  MatCardModule,
  MatTabsModule,
  MatListModule,
  MatStepperModule,
  MatButtonToggleModule,
  MatProgressSpinnerModule,
  MatProgressBarModule,
  MatRadioModule,
  MatBadgeModule,
  MatSlideToggleModule,
  MatDialogModule,
  A11yModule, // Used for focus trap
  OverlayModule,
  ScrollingModule,
  MatRippleModule,
  NgxMaterialTimepickerModule
];

@NgModule({
  imports: modules,
  exports: modules
})
export class MaterialModule {
}


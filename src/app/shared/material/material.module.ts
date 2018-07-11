import { NgModule } from "@angular/core";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import {
  MatPaginatorModule, MatTableModule, MatSortModule, MatFormFieldModule, MatInputModule,
  MatAutocompleteModule, MatCheckboxModule, MatExpansionModule, MatToolbarModule, MatDialogModule, MatIconModule,
  MatButtonModule, MatMenuModule, MatSelectModule, MatCardModule, MatTabsModule, MatListModule, MatStepperModule,
  MatButtonToggleModule, MatProgressSpinnerModule, MatProgressBarModule
} from "@angular/material";
import { CdkTableModule } from "@angular/cdk/table";
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatMomentDateModule } from '@angular/material-moment-adapter';

import { fadeInAnimation, slideInOutAnimation } from './material.animations';


const modules = [
  MatTableModule,
  MatSortModule,
  MatAutocompleteModule,
  MatPaginatorModule,
  BrowserAnimationsModule,
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
  MatProgressBarModule
];

@NgModule({
  imports: modules,
  exports: modules
})
export class MaterialModule {
}

export { fadeInAnimation, slideInOutAnimation }
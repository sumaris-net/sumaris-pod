import { NgModule } from '@angular/core';
import { CoreModule } from '@sumaris-net/ngx-components';
import { TranslateModule } from '@ngx-translate/core';
import { AppImageGalleryComponent } from '@app/image/gallery/image-gallery.component';


@NgModule({
  imports: [
    CoreModule,
    TranslateModule.forChild(),
  ],
  declarations: [
    // Components
    AppImageGalleryComponent
  ],
  exports: [
    // Components
    AppImageGalleryComponent
  ]
})
export class ImageModule {

}

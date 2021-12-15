import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { fadeInAnimation } from '@sumaris-net/ngx-components';

@Component({
  selector: 'app-image-gallery',
  templateUrl: './image-gallery.component.html',
  styleUrls: ['./image-gallery.component.scss'],
  animations: [fadeInAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppImageGalleryComponent
  //implements OnInit, OnDestroy
{

  constructor() {
  }

}

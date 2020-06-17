import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {Entity} from '../services/model/entity.model';
// import fade in animation
import {fadeInAnimation} from '../../shared/material/material.animations';
import {Moment} from "moment";

@Component({
  selector: 'app-entity-metadata',
  templateUrl: './entity-metadata.component.html',
  styleUrls: ['./entity-metadata.component.scss'],
  animations: [fadeInAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityMetadataComponent {

  @Input()
  value: Entity<any> & {creationDate?: Date | Moment; recorderDepartment?: any; recorderPerson?: any};

}

import { Injectable, Pipe, PipeTransform } from '@angular/core';
import { qualityFlagToColor } from '../model/model.utils';

@Pipe({
  name: 'qualityFlagToColor',
})
@Injectable({ providedIn: 'root' })
export class QualityFlagToColorPipe implements PipeTransform {
  transform(qualityFlagId: number): string {
    return qualityFlagToColor(qualityFlagId);
  }
}

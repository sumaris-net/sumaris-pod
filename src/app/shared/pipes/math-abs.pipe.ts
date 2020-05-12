import {Injectable, Pipe, PipeTransform} from '@angular/core';

@Pipe({
    name: 'mathAbs'
})
@Injectable({providedIn: 'root'})
export class MathAbsPipe implements PipeTransform {

    transform(val: number): any {
      if (val !== undefined && val !== null) {
        return Math.abs(val);
      } else {
        return val;
      }
    }
}

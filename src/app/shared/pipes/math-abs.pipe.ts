import {Injectable, Pipe, PipeTransform} from '@angular/core';

@Pipe({
    name: 'mathAbs'
})
@Injectable({providedIn: 'root'})
export class MathAbsPipe implements PipeTransform {

    transform(val: number): number | Promise<number> {
      // Format the output to display any way you want here.
      // For instance:
      if (val !== undefined && val !== null) {
        return Math.abs(val);
      } else {
        return val;
      }
    }
}

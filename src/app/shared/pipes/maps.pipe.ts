import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {isNotNil} from "../functions";

@Pipe({
    name: 'mapGet'
})
@Injectable({providedIn: 'root'})
export class MapGetPipe implements PipeTransform {

    transform(val: any, args: any): any {
      if (!val) return null;
      const key = args && (typeof args === 'string' ? args : args.key);
      if (!key) return null;
      return val[key];
    }
}

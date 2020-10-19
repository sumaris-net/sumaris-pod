import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {Referential, ReferentialRef, referentialToString} from "../../../core/services/model/referential.model";

@Pipe({
  name: 'referentialToString'
})
@Injectable({providedIn: 'root'})
export class ReferentialToStringPipe implements PipeTransform {

  constructor(
  ) {
  }

  transform(obj: Referential | ReferentialRef, args?: any): string {
    return referentialToString(obj, args);
  }
}

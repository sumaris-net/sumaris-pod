import {Injectable, Pipe, PipeTransform} from '@angular/core';
import {
  Referential,
  ReferentialRef,
  referentialsToString,
  referentialToString
} from "../../../core/services/model/referential.model";

@Pipe({
  name: 'referentialToString'
})
@Injectable({providedIn: 'root'})
export class ReferentialToStringPipe implements PipeTransform {

  constructor(
  ) {
  }

  transform(value: Referential | ReferentialRef | any, properties?: string[]): string {
    if (value instanceof Array) return referentialsToString(value, properties);
    return referentialToString(value, properties);
  }
}

import { Pipe, Injectable, PipeTransform } from '@angular/core';
import { Moment } from "moment/moment";
import { DateAdapter } from "@angular/material";
import { DATE_ISO_PATTERN } from '../constants';

@Pipe({
    name: 'numberFormat'
})
@Injectable()
export class NumberFormatPipe implements PipeTransform {

    transform(val: number): string | Promise<string> {
      // Format the output to display any way you want here.
      // For instance:
      if (val !== undefined && val !== null) {
        return val.toLocaleString(/*arguments you need*/);
      } else {
        return '';
      }
    }
}

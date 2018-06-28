import { Pipe, Injectable, PipeTransform } from '@angular/core';

@Pipe({
    name: 'highlight'
})
@Injectable()
export class HighlightPipe implements PipeTransform {

    transform(value: string | any, args?: any): string | Promise<string> {
        args = args || {};
        if (value && args && args.search && typeof args.search === 'string') {
            const regexp = new RegExp(args.search, 'gi');
            return value.replace(regexp, '<b>$&</b>');
        }
        return value;
    }
}
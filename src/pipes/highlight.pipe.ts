import { Pipe, Injectable, PipeTransform } from '@angular/core';

@Pipe({
    name: 'highlight'
})
@Injectable()
export class HighlightPipe implements PipeTransform {

    transform(value: string, args?: any) : string | Promise<string> {
        args = args || {};
        if (typeof value == "object") return value;
        if (args && args.search) {
            const regexp = new RegExp(args.search, 'gi');
            return value.replace(regexp, '<b>$&</b>');
        }
        return value;
    }
}
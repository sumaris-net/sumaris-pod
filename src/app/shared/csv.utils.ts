import { Observable } from 'rxjs';
import { FilesUtils, FileEvent, FileResponse, isNotNil } from '@sumaris-net/ngx-components';
import { filter, map } from 'rxjs/operators';
import { HttpEventType } from '@angular/common/http';

export class CsvUtils {
  static UTF8_BOM_CHAR = new Uint8Array([0xEF, 0xBB, 0xBF]); // UTF-8 BOM

  static exportToFile(rows: object[],
                      opts?: {
                        filename?: string;
                        headers?: string[];
                        separator?: string;
                        encoding?: string;
                      }) {
    if (!rows || !rows.length) return; // Skip if empty

    const filename = opts?.filename || 'export.csv';
    const charset = (opts?.encoding || 'utf-8').toLowerCase();
    const separator = opts?.separator || ',';
    const protectCellRegexp = new RegExp("/(\"|"+ separator +"|\n)/g");
    const keys = Object.keys(rows[0]);
    const headers = opts?.headers || keys;
    const csvContent =
      // Header row
      headers.join(separator) + '\n' +
      // Data rows
      rows.map(row => {
        return keys.map(k => {
          const cell = row[k] === null || row[k] === undefined ? '' : row[k];
          let cellStr: string = cell instanceof Date
            ? cell.toLocaleString()
            : cell.toString().replace(/"/g, '""');
          if (protectCellRegexp.test(cellStr)) {
            cellStr = `"${cellStr}"`;
          }
          return cellStr;
        }).join(separator);
      }).join('\n');


    const blob = new Blob((charset === 'utf-8')
      // Add UTF-8 BOM character
      ? [CsvUtils.UTF8_BOM_CHAR, csvContent]
      // Non UTF-8 charset
      : [csvContent],
      { type: `text/csv;charset=${charset};` });
    if (navigator.msSaveBlob) { // IE 10+
      navigator.msSaveBlob(blob, filename);
    } else {
      const link = document.createElement('a');
      if (link.download !== undefined) {
        // Browsers that support HTML5 download attribute
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', filename);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }
    }
  }

  static parseFile(file: File, opts?: {separator?: string; encoding?: string;}): Observable<FileEvent<string[][]>> {
    return FilesUtils.readAsText(file, opts?.encoding)
      .pipe(
        map(e => {
          if (e.type === HttpEventType.UploadProgress) {
            const loaded = Math.round(e.loaded * 0.8);
            return {...e, loaded};
          }
          else if (e instanceof FileResponse){
            const body = e.body;
            const data = CsvUtils.parseCSV(body, opts);
            return new FileResponse<string[][]>({body: data});
          }
        }),
        filter(isNotNil)
      );
  }

  static parseCSV(body: string, opts?: {separator?: string}): string[][] {
    const separator = opts?.separator || ',';

    // Protect special characters (quote and /n)
    body = body.replace('""', '<quote>') // Protect double quote
      .replace(/("[^\n"]+)\n\r?/gm, '"$1<br>') // Protect \n inside a quoted expression
      .replace(/\n\r/gm, '\n') // Windows CR

    const headerAndRows = body.split('\n', 2);
    const headers = headerAndRows[0].split(separator);

    return body.split('\n') // split into rows
      .map(row => {
        const cells = row.split(separator, headers.length) // split into cells
          .map(cell => cell.replace(/^"([^"]*)"$/, '$1')) // Clean trailing quotes
          .map(cell => cell.replace('<quote>', '"')) // restore protected quote
          .map(cell => cell.replace('<br>', '\n')) // restore protected br
        ;
        return cells;
      });
  }
}

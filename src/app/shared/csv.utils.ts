export class CsvUtils {
  static exportToFile(rows: object[],
                      options?: {
                        filename?: string;
                        headers?: string[];
                        separator?: string;
                      }) {
    if (!rows || !rows.length) {
      return;
    }
    const filename = options?.filename || 'export.csv';
    const separator = options?.separator || ',';
    const protectCellRegexp = new RegExp("/(\"|"+ separator +"|\n)/g");
    const keys = Object.keys(rows[0]);
    const headers = options?.headers || keys;
    const csvContent =
      // Header row
      headers.join(separator) + '\n' +
      // Data rows
      rows.map(row => {
        return keys.map(k => {
          let cell = row[k] === null || row[k] === undefined ? '' : row[k];
          const cellStr: string = cell instanceof Date
            ? cell.toLocaleString()
            : cell.toString().replace(/"/g, '""');
          if (protectCellRegexp.test(cellStr)) {
            cell = `"${cell}"`;
          }
          return cell;
        }).join(separator);
      }).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
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
}

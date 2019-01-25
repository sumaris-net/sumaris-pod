
/* -- Extraction -- */

export declare class ExtractionType {
  label: string;
  category: string;
  name?: string;
}

export declare class ExtractionResult {
  columns: ExtractionColumn[];
  rows: ExtractionRow[];
  total: number;
}


export declare class ExtractionColumn {
  index?: number;
  name: string;
  type: string;
  description?: String;
  rankOrder: number;
}

export class ExtractionRow extends Array<any> {
  constructor(
  ) {
    super()
  }
}

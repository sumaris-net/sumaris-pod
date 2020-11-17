
export declare type TextWithMaskPattern = 'SampleRowCode' | 'UndefinedPattern';
export const TEXT_WITH_MASK_PATTERNS: TextWithMaskPattern[] = ['SampleRowCode'];

export declare class TextWithMaskFormatOptions {
  pattern: TextWithMaskPattern;
  maxDecimals?: number;
  placeholderChar?: string;
}
export declare type TextWithMaskFormatFn = (value: number | null, opts?: TextWithMaskFormatOptions) => string;
export const DEFAULT_PLACEHOLDER_CHAR = '\u005F';
export const SPACE_PLACEHOLDER_CHAR = '\u2000';
export const DEFAULT_MAX_DECIMALS = 3;

const DEFAULT_OPTIONS = <TextWithMaskFormatOptions>{
  pattern: 'SampleRowCode',
  maxDecimals: DEFAULT_MAX_DECIMALS,
  placeholderChar: DEFAULT_PLACEHOLDER_CHAR
};
const DEFAULT_SAMPLE_ROW_CODE_OPTIONS: TextWithMaskFormatOptions = {
  ...DEFAULT_OPTIONS
};

export  function formatSampleRowCode(value: number | null, opts?: TextWithMaskFormatOptions): string {
  if (value === undefined || value === null) return null;

  switch(opts.pattern) {
    case 'SampleRowCode':
    default:
      return formatToSampleRowCode(value, { ...DEFAULT_SAMPLE_ROW_CODE_OPTIONS, ...opts });
  }
}


function formatToSampleRowCode(value: number, opts: TextWithMaskFormatOptions): string {
  // opts.pattern === SampleRowCode

  return '2020-BIO-1234';
}

export function parseTextWithMask(input: string, pattern: string, maxDecimals?: number, placeholderChar?: string): string | null {

  return '2020---BIO----1234';
}



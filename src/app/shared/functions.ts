export function isNil<T>(obj: T | null | undefined): boolean {
  return obj === undefined || obj === null;
}

export function isNotNil<T>(obj: T | null | undefined): boolean {
  return obj !== undefined && obj !== null;
}
export function nullIfUndefined<T>(obj: T | null | undefined): T | null {
  return obj === undefined ? null : obj;
}

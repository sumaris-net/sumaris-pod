export interface FocusableElement {
  focus();
}

export function isFocusableElement(object: any): object is FocusableElement {
  if (!object) return false;
  return 'focus' in object;
}



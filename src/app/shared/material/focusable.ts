import {ElementRef} from "@angular/core";

export declare interface FocusableElement {
  focus();
}
export declare interface InputElement {
  focus();
  tabindex?: number;
  tabIndex?: number;
  value: any;
}

export function isFocusableElement(object: any): object is FocusableElement {
  if (!object) return false;
  return 'focus' in object;
}

export function isInputElement(object: any): object is InputElement {
  if (!object) return false;
  return  (('focus' in object)
    && ('value' in object
      // has value is not always set (neither tabindex) check on 2 properties with a logical OR
      || ('tabindex' in object || 'tabIndex' in object)));
}

export function asInputElement(object: ElementRef): InputElement|undefined {
  if (object) {
    if (isInputElement(object)) return object;
    if (object.nativeElement && isInputElement(object.nativeElement)) return object.nativeElement;
  }
  return undefined;
}

export function tabindexComparator(a, b) {
  const valueA = a.tabindex || a.tabIndex;
  const valueB = b.tabindex || b.tabIndex;
  return valueA === valueB ? 0 : (valueA > valueB ? 1 : -1);
}

export declare interface FocusableElement {
  focus();
}

export function setTabIndex(input: any, tabindex: number) {
  if (input instanceof HTMLInputElement) {
    input.setAttribute('tabindex', tabindex.toString());
  }
  else if (isInputElement(input)) {
    input.tabindex = tabindex;
  }
  else if (isInputElement(input.nativeElement)) {
    setTabIndex(input.nativeElement, tabindex); // loop
  }
  else {
    console.warn("Trying to change tabindex on this element:", input);
  }
}

import {asInputElement, isInputElement} from "./material/focusable";
import {ElementRef} from "@angular/core";

// Copy from functions.ts (to avoid a circular reference)

function isNilOrBlank<T>(obj: T | null | undefined): boolean {
  return obj === undefined || obj === null || (typeof obj === 'string' && obj.trim() === "");
}
function isNotNil<T>(obj: T | null | undefined): boolean {
  return obj !== undefined && obj !== null;
}

export function selectInputContent(event: UIEvent) {
  if (event.defaultPrevented) return false;
  const input = (event.target as any);
  if (input && typeof input.select === "function") {

    // Nothing to select
    if (isNilOrBlank(input.value)) return false;

    try {
      input.select();
    } catch (err) {
      console.error("Could not select input content", err);
      return false;
    }
  }
  return true;
}

export function selectInputRange(input: any, startIndex: number, endIndex?: number) {
  if (input && typeof input.setSelectionRange === "function") {

    // No content
    if (isNilOrBlank(input.value)) return false;

    try {
      input.setSelectionRange(startIndex, isNotNil(endIndex) ? endIndex : startIndex);
    } catch (err) {
      console.error("Could not select input range", err);
      return false;
    }
  }
  return true;
}

export function getCaretPosition(input: any): number {
  if (input && input.selectionStart) {
    return input.selectionDirection ?
      (input.selectionDirection === 'backward' ? input.selectionStart : input.selectionEnd) : input.selectionStart;
  }
  return -1;
}

export function moveInputCaretToSeparator(event: KeyboardEvent, separator: string, forward?: boolean) {
  if (event.defaultPrevented || !separator) return false;
  const input = (event.target as any);
  if (!input) return true;

  const caretPosition = getCaretPosition(input);

  // DEBUG
  console.debug("caretPosition=", caretPosition);

  if (caretPosition == -1) return true; // Caret pos not found: skip

  // Get input value
  const value = input.value as string;

  // No content: skip
  if (isNilOrBlank(value)) return false;

  try {


    if (value && caretPosition <= value.length) {
      // DEBUG
      //console.debug("Input text value: ", value);
      //console.debug("Cursor at: ", caretPosition);
      //console.debug("Text after cursor: ", value.substr(caretPosition));
      //console.debug("Next separator at: ", value.indexOf(separator, caretPosition));

      forward = forward !== false;

      const separatorIndex = (forward ? value.indexOf(separator, caretPosition) : value.lastIndexOf(separator, caretPosition));
      if (separatorIndex !== -1 && ((forward && separatorIndex + 1 < value.length)
        || (!forward && separatorIndex > 0))) {
        if (input.setSelectionRange) {
          // Move after the next separator
          if (selectInputRange(input, separatorIndex + (forward ? 1 : -1))) {
            // Stop the keyboard event
            event.preventDefault();
            event.stopPropagation();
          }
        }
      }
    }
  } catch (err) {
    console.error("Could not move caret to next separator", err);
    return false;
  }
  return true;
}

export function filterNumberInput(event: KeyboardEvent, allowDecimals: boolean, decimalSeparator?: string) {
  //input number entered or one of the 4 direction up, down, left and right
  if ((event.which >= 48 && event.which <= 57) || (event.which >= 37 && event.which <= 40)) {
    //console.debug('input number entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
    // OK
  }
  // Decimal separator
  else if (allowDecimals && ((!decimalSeparator && (event.key === '.' || event.key === ','))
    || (decimalSeparator && event.key === decimalSeparator))) {
    //console.debug('input decimal separator entered :' + event.code);
    // OK
  } else {
    //input command entered of delete, backspace or one of the 4 direction up, down, left and right, or negative sign
    if ((event.keyCode >= 37 && event.keyCode <= 40) || event.keyCode == 46 || event.which == 8 || event.keyCode == 9 || event.keyCode == 45) {
      //console.debug('input command entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
      // OK
    }
    // Cancel other keyboard events
    else {
      //console.debug('input not number entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode + ' ' + event.code );
      event.preventDefault();
    }
  }
}

export function focusInput(element: ElementRef) {
  const inputElement = asInputElement(element);
  if (inputElement)
    inputElement.focus();
  else {
    console.warn("Trying to focus on this element:", element);
  }
}
export function setTabIndex(element: ElementRef, tabIndex: number) {
  if(isInputElement(element)) {
    element.tabindex = tabIndex;
  }
  else if (element && isInputElement(element.nativeElement)) {
    element.nativeElement.tabIndex = tabIndex;
  }
  else {
    console.warn("Trying to change tabindex on this element:", element);
  }
}

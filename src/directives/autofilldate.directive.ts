// Import the core angular services.
import { AfterContentInit } from "@angular/core";
import { Directive } from "@angular/core";
import { ElementRef } from "@angular/core";
import { OnChanges } from "@angular/core";
import { OnDestroy } from "@angular/core";
import { SimpleChanges } from "@angular/core";

// ----------------------------------------------------------------------------------- //
// ----------------------------------------------------------------------------------- //

var BASE_DATE_PATTERN = "L";

@Directive({
  selector: "[autoFillDate]",
  inputs: [
    "datePattern: datePattern"
  ]
})
export class AutoFillDateDirective implements OnChanges {

  public datePattern: string;
  private elementRef: ElementRef;

  // I initialize the directive.
  constructor(elementRef: ElementRef) {

    this.elementRef = elementRef;
    this.datePattern = BASE_DATE_PATTERN;

  }

  /*public ngAfterContentInit(): void {}*/

  public ngOnChanges(changes: SimpleChanges): void {

    console.log(changes);
  }

  /*public ngOnDestroy(): void {}*/

}

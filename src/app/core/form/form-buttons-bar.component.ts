import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
    selector: 'form-buttons-bar',
    templateUrl: './form-buttons-bar.component.html',
    host: { '(window:keydown)': 'hotkeys($event)' },
    styleUrls: ['./form-buttons-bar.component.scss']
})
export class FormButtonsBarComponent {

    @Input()
    disabled: boolean = false;

    @Input()
    disabledCancel: boolean = false;

    @Output()
    onCancel: EventEmitter<Event> = new EventEmitter<Event>();

    @Output()
    onSave: EventEmitter<Event> = new EventEmitter<Event>();

    @Output()
    onNext: EventEmitter<Event> = new EventEmitter<Event>();

    @Output()
    onBack: EventEmitter<Event> = new EventEmitter<Event>();

    hotkeys(event) {

      if (event.repeat) return;

        // Ctrl+S
        if (event.key == 's' && event.ctrlKey) {
            if (!this.disabled) this.onSave.emit(event);
            event.preventDefault();
        }
        // Ctrl+Z 
        if (event.key == 'z' && event.ctrlKey) {
            if (!this.disabled && !this.disabledCancel) this.onCancel.emit(event);
            event.preventDefault();
        }
        // esc
        if (event.key == 'Escape' && !FormButtonsBarComponent.isEscapeAlreadyBind(event.target)) {
          this.onBack.emit(event);
          event.preventDefault();
        }

    }

    static isEscapeAlreadyBind(target: any): boolean {
      return target && target.type && (target.type == 'textarea') || false
    }
}

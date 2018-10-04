import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Entity } from '../services/model';

@Component({
    selector: 'form-buttons-bar',
    templateUrl: './form-buttons-bar.component.html',
    styleUrls: ['./form-buttons-bar.component.scss']
})
export class FormButtonsBarComponent {

    @Input()
    disabled: boolean = false;

    @Input()
    disabledCancel: boolean = false;

    @Output()
    onCancel: EventEmitter<any> = new EventEmitter<any>();

    @Output()
    onSave: EventEmitter<any> = new EventEmitter<any>();

    @Output()
    onNext: EventEmitter<any> = new EventEmitter<any>();

}
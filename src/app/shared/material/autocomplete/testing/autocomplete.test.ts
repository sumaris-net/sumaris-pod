import {Component, OnInit} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {SharedValidators} from "../../../validator/validators";
import {MatAutocompleteConfigHolder} from "../material.autocomplete";
import {isNotNil, suggestFromArray} from "../../../functions";
import {BehaviorSubject} from "rxjs";
import {LoadResult} from "../../../services/entity-service.class";
import Timer = NodeJS.Timer;

export class Entity {
  id: number;
  label: string;
  name: string;
}

const FAKE_ENTITIES= [
  {id: 1, label: 'AAA', name: 'Item A', description: 'Very long description A', comments: 'Very very long comments... again for A'},
  {id: 2, label: 'BBB', name: 'Item B', description: 'Very long description B', comments: 'Very very long comments... again for B'},
  {id: 3, label: 'CCC', name: 'Item C', description: 'Very long description C', comments: 'Very very long comments... again for C'}
];

function deepCopy(values?: Entity[]): Entity[] {
  return (values || FAKE_ENTITIES).map(entity => Object.assign({}, entity));
}

@Component({
  selector: 'app-autocomplete-test',
  templateUrl: './autocomplete.test.html'
})
export class AutocompleteTestPage implements OnInit {

  private _items = deepCopy(FAKE_ENTITIES);
  private _$items = new BehaviorSubject<Entity[]>(undefined);

  form: FormGroup;
  autocompleteFields = new MatAutocompleteConfigHolder();
  memoryHide = false;
  memoryAutocompleteFieldName = 'entity-$items';
  memoryTimer: Timer;

  constructor(
    protected formBuilder: FormBuilder
  ) {
    this.form = formBuilder.group({
      entity: [null, SharedValidators.entity],
      missingEntity: [null, SharedValidators.entity],
      disableEntity: [null, SharedValidators.entity],
    });

    this.form.get('disableEntity').disable();
  }

  ngOnInit() {

    // From suggest
    this.autocompleteFields.add('entity-suggestFn', {
      suggestFn: (value, filter) => this.suggest(value, filter),
      attributes: ['label', 'name'],
      displayWith: this.entityToString
    });

    // From items
    this.autocompleteFields.add('entity-items', {
      items: FAKE_ENTITIES.slice(),
      attributes: ['label', 'name'],
      displayWith: this.entityToString
    });

    // From items
    this.autocompleteFields.add('entity-$items', {
      items: this._$items,
      attributes: ['label', 'name'],
      displayWith: this.entityToString
    });

    // From items
    this.autocompleteFields.add('entity-items-large', {
      items: FAKE_ENTITIES.slice(),
      attributes: ['label', 'name', 'description', 'comments'],
      displayWith: this.entityToString
    });

    this.loadData();
    //setTimeout(() => this.loadData(), 1500);

    //
    setTimeout(() => this.loadItems(), 1000);
  }

  // Load the form with data
  async loadData() {
    const data = {
      entity: deepCopy(FAKE_ENTITIES)[1], // Select the second

      // THis item is NOT in the items list => i should be displayed anyway
      missingEntity: {
        id: -1, label: '??', name: 'Missing item'
      },

      disableEntity: deepCopy(FAKE_ENTITIES)[2]
    };

    this.form.setValue(data);
  }

  async loadItems() {
    this._$items.next(deepCopy(FAKE_ENTITIES));
  }

  entityToString(item: any) {
    return [item && item.label || undefined, item && item.name || undefined].filter(isNotNil).join(' - ');
  }

  async suggest(value: any, filter?: any): Promise<LoadResult<any>> {
    return suggestFromArray(this._items, value, {
      ...filter,
      searchAttributes: ['label', 'name']
    });
  }

  doSubmit(event) {

    console.debug("Validate form: ", this.form.value);
  }

  compareWithFn(o1: Entity, o2: Entity): boolean {
    return o1 && o2 && o1.id === o2.id;
  }

  startMemoryTimer() {
    this.memoryTimer = setInterval(() => {
      this.memoryHide = !this.memoryHide;
    }, 50);
  }

  stopMemoryTimer() {
    clearInterval(this.memoryTimer);
    this.memoryTimer = null;
    this.memoryHide = false;
  }

  /* -- protected methods -- */

  stringify(value: any) {
    return JSON.stringify(value);
  }
}


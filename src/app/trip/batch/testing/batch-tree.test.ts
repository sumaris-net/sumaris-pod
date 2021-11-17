import {Component, OnInit, ViewChild} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {BehaviorSubject} from "rxjs";
import {Batch, BatchUtils} from "../../services/model/batch.model";
import {ReferentialRefService} from "../../../referential/services/referential-ref.service";
import {ProgramService} from "../../../referential/services/program.service";
import {mergeMap} from "rxjs/operators";
import {BatchTreeComponent} from "../batch-tree.component";
import {MatAutocompleteConfigHolder} from "@sumaris-net/ngx-components";
import {SharedValidators} from "@sumaris-net/ngx-components";
import {PmfmIds} from "../../../referential/services/model/model.enum";
import {isEmptyArray, isNotNil, toNumber} from "@sumaris-net/ngx-components";
import {EntityUtils}  from "@sumaris-net/ngx-components";
import {EntitiesStorage}  from "@sumaris-net/ngx-components";
import {ProgramRefService} from "../../../referential/services/program-ref.service";

function getSortingMeasValues(opts?: {
  weight?: number;
  discardOrLanding: 'LAN'|'DIS';
}) {
  opts = {
    discardOrLanding: 'LAN',
    ...opts
  }
  const res = {};

  res[PmfmIds.DISCARD_OR_LANDING] = opts.discardOrLanding === 'LAN' ? 190 : 191;
  if (isNotNil(opts.weight)) {
    res[PmfmIds.BATCH_MEASURED_WEIGHT] = opts.weight;
  }
  return res;
}

function getIndivMeasValues(opts?: {
  length?: number;
  discardOrLanding: 'LAN'|'DIS';
}) {
  opts = {
    discardOrLanding: 'LAN',
    ...opts
  }
  const res = {};

  res[PmfmIds.DISCARD_OR_LANDING] = opts.discardOrLanding === 'LAN' ? 190 : 191;
  if (isNotNil(opts.length)) {
    res[PmfmIds.LENGTH_TOTAL_CM] = opts.length;
  }
  return res;
}
const TREE_EXAMPLES: {[key: string]: any} = {
  'default':
    {
      label: 'CATCH_BATCH', rankOrder: 1, children: [
        {
          label: 'SORTING_BATCH#1',
          rankOrder: 1,
          taxonGroup: {id: 1122, label: 'MNZ', name: 'Baudroie nca'},
          children: [
            {
              label: 'SORTING_BATCH#1.LAN', rankOrder: 1,
              measurementValues: getSortingMeasValues({discardOrLanding: 'LAN', weight: 100}),
              children: [
                {
                  label: 'SORTING_BATCH#1.LAN.%',
                  rankOrder: 1,
                  samplingRatio: 0.5,
                  samplingRatioText: '50%',
                  children: [
                    {
                      label: 'SORTING_BATCH_INDIVIDUAL#1',
                      rankOrder: 1,
                      taxonName: {id: 1033, label: 'MON', name: 'Lophius piscatorius'},
                      measurementValues: getIndivMeasValues({discardOrLanding: 'LAN', length: 11}),
                      individualCount: 1
                    },
                    {
                      label: 'SORTING_BATCH_INDIVIDUAL#3',
                      rankOrder: 3,
                      taxonName: {id: 1034, label: 'ANK', name: 'Lophius budegassa'},
                      measurementValues: getIndivMeasValues({discardOrLanding: 'LAN', length: 33}),
                      individualCount: 1
                    }
                  ]
                }
              ]
            },
            {
              label: 'SORTING_BATCH#1.DIS', rankOrder: 2,
              measurementValues: getSortingMeasValues({discardOrLanding: 'DIS', weight: 0}),
              children: [
                {
                  label: 'SORTING_BATCH#1.DIS.%',
                  rankOrder: 1,
                  samplingRatio: 0.5,
                  samplingRatioText: '50%',
                  children: [
                    {
                      label: 'SORTING_BATCH_INDIVIDUAL#2',
                      rankOrder: 2,
                      taxonName: {id: 1034, label: 'ANK', name: 'Lophius budegassa'},
                      measurementValues: getIndivMeasValues({discardOrLanding: 'DIS', length: 22}),
                      individualCount: 1
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    },
  'empty': {id: 100, label: 'CATCH_BATCH', rankOrder: 1}
};

@Component({
  selector: 'app-batch-tree-test',
  templateUrl: './batch-tree.test.html'
})
export class BatchTreeTestPage implements OnInit {


  $programLabel = new BehaviorSubject<string>(undefined);
  $gearId = new BehaviorSubject<number>(undefined);
  form: FormGroup;
  autocomplete = new MatAutocompleteConfigHolder();

  outputs: {
    [key: string]: string;
  } = {};

  @ViewChild('mobileBatchTree', { static: true }) mobileBatchTree: BatchTreeComponent;
  @ViewChild('desktopBatchTree', { static: true }) desktopBatchTree: BatchTreeComponent;

  constructor(
    formBuilder: FormBuilder,
    protected referentialRefService: ReferentialRefService,
    protected programRefService: ProgramRefService,
    private entities: EntitiesStorage
  ) {

    this.form = formBuilder.group({
      program: [{id:10, label: 'ADAP-MER'}, Validators.compose([Validators.required, SharedValidators.entity])],
      gear: [null, Validators.compose([Validators.required, SharedValidators.entity])],
      example: [null, Validators.required]
    });
  }

  ngOnInit() {

    // Program
    this.autocomplete.add('program', {
      suggestFn: (value, filter) => this.referentialRefService.suggest(value, {
        ...filter,
        entityName: 'Program'
      }),
      attributes: ['label', 'name']
    });
    this.form.get('program').valueChanges
      //.pipe(debounceTime(450))
      .subscribe(p => {
        const label = p && p.label;
        if (label) {
          this.$programLabel.next(label);
        }
      });

    // Gears (from program)
    this.autocomplete.add('gear', {
      items: this.$programLabel.pipe(
        mergeMap((programLabel) => {
          if (!programLabel) return Promise.resolve([]);
          return this.programRefService.loadGears(programLabel);
        })
      ),
      attributes: ['label', 'name']
    });
    this.form.get('gear').valueChanges
      //.pipe(debounceTime(450))
      .subscribe(g => this.$gearId.next(toNumber(g && g.id, null)));


    // Input example
    this.autocomplete.add('example', {
      items: Object.keys(TREE_EXAMPLES).map((label, index) => ({id: index+1, label})),
      attributes: ['label']
    });
    this.form.get('example').valueChanges
      //.pipe(debounceTime(450))
      .subscribe(example => {
        if (example && typeof example.label == 'string') {
          const json = TREE_EXAMPLES[example.label];
          this.dumpCatchBatch(Batch.fromObject(json), 'example');
        }
      });


    this.form.patchValue({
      program: {id: 1, label: 'SUMARiS' },
      gear: {id: 6, label: 'OTB'},
      example: {id: 1, label: 'default'}
    });

    this.applyExample();
  }


  // Load data into components
  async updateView(data: Batch) {

    this.mobileBatchTree.value = data.clone();
    this.desktopBatchTree.value = data.clone();

    this.mobileBatchTree.enable();
    this.desktopBatchTree.enable();

    setTimeout(() => {
      this.mobileBatchTree.waitIdle().then( () => this.mobileBatchTree.autoFill());
      this.desktopBatchTree.waitIdle().then( () => this.desktopBatchTree.autoFill())
    });
  }

  doSubmit(event?: UIEvent) {
    // Nothing to do
  }


  async getExampleTree(key?: string): Promise<Batch> {

    if (!key) {
      const example = this.form.get('example').value;
      key = example && example.label || 'default';
    }

    // Load example
    const json = TREE_EXAMPLES[key];

    // Convert to array (as Pod should sent) with:
    // - a local ID
    // - only the parentId, and NOT the parent
    const batches = EntityUtils.treeToArray(json) || [];
    await EntityUtils.fillLocalIds(batches, (_, count) => this.entities.nextValues('BatchVO', count));
    batches.forEach(b => {
      b.parentId = b.parent && b.parent.id;
      delete b.parent;
    });

    // Convert into Batch tree
    const catchBatch = Batch.fromObjectArrayAsTree(batches)
    BatchUtils.computeIndividualCount(catchBatch);

    return catchBatch;
  }

  // Load data into components
  async applyExample(key?: string) {
    const catchBatch = await this.getExampleTree(key);
    await this.updateView(catchBatch);
  }

  async dumpExample(key?: string) {
    const catchBatch = await this.getExampleTree(key);
     this.dumpCatchBatch(catchBatch, 'example');
  }

  async dumpBatchTree(batchTree: BatchTreeComponent, outputName?: string) {

    await batchTree.save();
    const catchBatch = batchTree.value;

    this.dumpCatchBatch(catchBatch, outputName);

    if (batchTree.mobile) {
      let html = "<br/>Sub batches :<br/>";
      const subBatches = await batchTree.getSubBatches();
      if (isEmptyArray(subBatches)) {
        html += '&nbsp;No result';
      }
      else {
        let html = "<ul>";
        subBatches.forEach(b => {
          BatchUtils.logTree(b, {
            showAll: false,
            println: (m) => {
              html += "<li>" + m + "</li>";
            }
          });
        });
        html += "</ul>"
      }

      // Append to result
      this.outputs[outputName] += html;
    }

  }


  dumpCatchBatch(catchBatch: Batch, outputName?: string) {
    let html = "";
    if (catchBatch) {
      BatchUtils.logTree(catchBatch, {
        showAll: false,
        println: (m) => {
          html += "<br/>" + m
        }
      });
      html = html.replace(/\t/g, '&nbsp;&nbsp;');

      this.outputs[outputName] = html;
    }
    else {
      this.outputs[outputName] = '&nbsp;No result';
    }

    console.debug(html);
  }

  async copyBatchTree(source: BatchTreeComponent, target: BatchTreeComponent) {
    await source.save();

    source.disable();
    target.disable();
    try {
      await target.setValue(source.value);
    }
    finally {
      source.enable();
      target.enable();
    }
  }

  /* -- protected methods -- */

  stringify(value: any) {
    return JSON.stringify(value);
  }
}


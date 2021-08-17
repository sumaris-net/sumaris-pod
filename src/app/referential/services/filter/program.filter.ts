import {BaseReferentialFilter} from './referential.filter';
import {Program} from '../model/program.model';
import {EntityClass} from '@sumaris-net/ngx-components';

@EntityClass({typename: 'ProgramFilterVO'})
export class ProgramFilter extends BaseReferentialFilter<ProgramFilter, Program> {

  static ENTITY_NAME = 'Program';
  static fromObject: (source: any, opts?: any) => ProgramFilter;

  constructor() {
    super();
    this.entityName = ProgramFilter.ENTITY_NAME;
  }

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.entityName = source.entityName || ProgramFilter.ENTITY_NAME;
  }
}

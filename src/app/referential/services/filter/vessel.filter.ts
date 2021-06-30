import {VesselSnapshot} from '../model/vessel-snapshot.model';
import {Moment} from 'moment';
import {EntityAsObjectOptions, EntityClass, EntityFilter, EntityUtils, FilterFn, fromDateISOString, isNotNil, isNotNilOrBlank, ReferentialRef, toDateISOString} from '@sumaris-net/ngx-components';
import {SynchronizationStatus} from '../../../data/services/model/root-data-entity.model';
import {NOT_MINIFY_OPTIONS} from '@app/core/services/model/referential.model';

@EntityClass({typename: 'VesselFilterVO'})
export class VesselSnapshotFilter extends EntityFilter<VesselSnapshotFilter, VesselSnapshot> {

  static fromObject: (source: any, opts?: any) => VesselSnapshotFilter;

  program: ReferentialRef;
  date: Moment;
  vesselId: number;
  searchText: string;
  statusId: number;
  statusIds: number[];
  synchronizationStatus: SynchronizationStatus;

  fromObject(source: any, opts?: any) {
    super.fromObject(source, opts);
    this.program = ReferentialRef.fromObject(source.program) ||
      isNotNilOrBlank(source.programLabel) && ReferentialRef.fromObject({label: source.programLabel});
    this.date = fromDateISOString(source.date);
    this.vesselId = source.vesselId;
    this.searchText = source.searchText;
    this.statusId = source.statusId;
    this.statusIds = source.statusIds;
    this.synchronizationStatus = source.synchronizationStatus;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target = super.asObject(opts);
    if (opts && opts.minify) {
      target.programLabel = this.program && this.program.label;

      // NOT in pod
      // target.synchronizationStatus =
    }
    else {
      target.program = this.program && this.program.asObject({...opts, ...NOT_MINIFY_OPTIONS});
      target.synchronizationStatus = this.synchronizationStatus;
    }
    target.vesselId = this.vesselId;
    target.statusIds = isNotNil(this.statusId) ? [this.statusId] : this.statusIds;
    target.date = toDateISOString(this.date);
    return target;
  }

  buildFilter(): FilterFn<VesselSnapshot>[] {
    const filterFns = super.buildFilter();

    // Program
    if (this.program) {
      const programId = this.program.id;
      const programLabel = this.program.label;
      if (isNotNil(programId)) {
        filterFns.push(t => (t.program && t.program.id === programId));
      }
      else if (isNotNilOrBlank(programLabel)) {
        filterFns.push(t => (t.program && t.program.label === programLabel));
      }
    }

    // Vessel id
    if (isNotNil(this.vesselId)) {
      const vesselId = this.vesselId;
      filterFns.push(t => t.id === vesselId);
    }

    // Status
    const statusIds = isNotNil(this.statusId) ? [this.statusId] : this.statusIds;
    if (statusIds) {
      filterFns.push(t => statusIds.includes(t.vesselStatusId));
    }

    // Search text
    const searchTextFilter = EntityUtils.searchTextFilter(['name', 'exteriorMarking', 'registrationCode'], this.searchText);
    if (searchTextFilter) filterFns.push(searchTextFilter);

    // Synchronization status
    if (this.synchronizationStatus) {
      if (this.synchronizationStatus === 'SYNC') {
        filterFns.push(EntityUtils.isRemote);
      }
      else {
        filterFns.push(EntityUtils.isLocal);
      }
    }

    return filterFns;
  }
}

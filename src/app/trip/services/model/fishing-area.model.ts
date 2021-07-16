import {DataEntity, DataEntityAsObjectOptions} from '../../../data/services/model/data-entity.model';
import {ReferentialRef} from '@sumaris-net/ngx-components';
import {NOT_MINIFY_OPTIONS} from '@app/core/services/model/referential.model';

export class FishingArea extends DataEntity<FishingArea> {

  static TYPENAME = 'FishingAreaVO';

  static fromObject(source: any): FishingArea {
    const res = new FishingArea();
    res.fromObject(source);
    return res;
  }

  location: ReferentialRef;

  distanceToCoastGradient: ReferentialRef;
  depthGradient: ReferentialRef;
  nearbySpecificArea: ReferentialRef;

  // operationId: number;

  constructor() {
    super();
    this.__typename = FishingArea.TYPENAME;
    this.location = null;
    this.distanceToCoastGradient = null;
    this.depthGradient = null;
    this.nearbySpecificArea = null;
    // this.operationId = null;
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.location = this.location && this.location.asObject({...options, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.distanceToCoastGradient = this.distanceToCoastGradient && this.distanceToCoastGradient.asObject({...options, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.depthGradient = this.depthGradient && this.depthGradient.asObject({...options, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.nearbySpecificArea = this.nearbySpecificArea && this.nearbySpecificArea.asObject({...options, ...NOT_MINIFY_OPTIONS}) || undefined;
    return target;
  }

  fromObject(source: any): FishingArea {
    super.fromObject(source);
    this.location = source.location && ReferentialRef.fromObject(source.location);
    this.distanceToCoastGradient = source.distanceToCoastGradient && ReferentialRef.fromObject(source.distanceToCoastGradient);
    this.depthGradient = source.depthGradient && ReferentialRef.fromObject(source.depthGradient);
    this.nearbySpecificArea = source.nearbySpecificArea && ReferentialRef.fromObject(source.nearbySpecificArea);
    // this.operationId = source.operationId;
    return this;
  }

  }

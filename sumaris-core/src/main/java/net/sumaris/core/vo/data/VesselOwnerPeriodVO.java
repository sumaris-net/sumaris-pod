package net.sumaris.core.vo.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.IValueObject;
import net.sumaris.core.model.annotation.Comment;
import net.sumaris.core.model.data.VesselOwnerPeriodId;
import net.sumaris.core.vo.data.vessel.VesselOwnerVO;
import net.sumaris.core.vo.referential.LocationVO;

import java.util.Date;

@Data
@FieldNameConstants
@EqualsAndHashCode
@Comment("Vessel owner period")
public class VesselOwnerPeriodVO implements IEntity<VesselOwnerPeriodId>,
        IValueObject<VesselOwnerPeriodId> {

    @EqualsAndHashCode.Exclude
    private VesselOwnerPeriodId id;

    private Date startDate;
    private Date endDate;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VesselVO vessel;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VesselOwnerVO vesselOwner;


    public Integer getVesselId() {
        if (this.id != null) return this.id.getVesselId();
        return null;
    }

    public void setVesselId(Integer vesselId) {
        if (this.id == null) {
            this.id = new VesselOwnerPeriodId();
        }
        this.id.setVesselId(vesselId);
    }

    public Integer getVesselOwnerId() {
        if (this.id == null) return null;
        return this.id.getVesselOwnerId();
    }

    public void setVesselOwnerId(Integer vesselOwnerId) {
        if (this.id == null) {
            this.id = new VesselOwnerPeriodId();
        }
        this.id.setVesselOwnerId(vesselOwnerId);
    }
}

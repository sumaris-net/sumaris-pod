package net.sumaris.core.model.data;

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

import lombok.*;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class VesselOwnerPeriodId implements Serializable {

    @Column(name = "vessel_fk")
    private Integer vesselId;

    @Column(name = "vessel_owner_fk")
    private Integer vesselOwnerId;

    @Column(name = "start_date")
    @Temporal(TemporalType.DATE)
    private Date startDate;

    public VesselOwnerPeriodId copy() {
        return new VesselOwnerPeriodId(this.getVesselId(), this.getVesselOwnerId(), this.getStartDate());
    }
}

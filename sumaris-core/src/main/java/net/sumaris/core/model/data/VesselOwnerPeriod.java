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

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@FieldNameConstants
@Entity
@Table(name = "vessel_owner_period")
@IdClass(VesselOwnerPeriodId.class)
public class VesselOwnerPeriod implements IEntity<VesselOwnerPeriodId> {
    public static interface Fields extends IEntity.Fields {
        String VESSEL = "vessel";
        String VESSEL_OWNER = "vesselOwner";
        String START_DATE = "startDate";
        String END_DATE = "endDate";
    }

    @Transient
    private VesselOwnerPeriodId id;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vessel_fk", nullable = false)
    private Vessel vessel;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vessel_owner_fk", nullable = false)
    private VesselOwner vesselOwner;

    @Id
    @Column(name = "start_date")
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.DATE)
    private Date endDate;
}

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

import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.location.Location;
import org.hibernate.annotations.Formula;

import javax.persistence.*;
import java.util.Date;

@Data
@FieldNameConstants
@Entity
@Table(name = "vessel_registration_period")
public class VesselRegistrationPeriod implements IWithVesselEntity<Integer, Vessel> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VESSEL_REGISTRATION_PERIOD_SEQ")
    @SequenceGenerator(name = "VESSEL_REGISTRATION_PERIOD_SEQ", sequenceName="VESSEL_REGISTRATION_PERIOD_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "start_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date startDate;

    @Column(name = "end_date")
    @Temporal(TemporalType.DATE)
    private Date endDate;

    @Formula("coalesce(end_date, date'2100-01-01')")
    @Column(name = "nvl_end_date", insertable = false, updatable = false)
    private Date nvlEndDate;

    @Column(name = "registration_code", length = 40)
    private String registrationCode;

    @Column(name = "int_registration_code", length = 40)
    private String intRegistrationCode;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vessel_fk", nullable = false)
    @ToString.Exclude
    private Vessel vessel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_location_fk", nullable = false)
    private Location registrationLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

}

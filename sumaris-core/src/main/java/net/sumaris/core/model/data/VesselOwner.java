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
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.referential.location.Location;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "vessel_owner",
        indexes =
        @Index(name="ix_vessel_owner_reg_code", columnList = "registration_code")
)
public class VesselOwner implements IEntity<Integer>, IWithProgramEntity<Integer, Program> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VESSEL_OWNER_SEQ")
    @SequenceGenerator(name = "VESSEL_OWNER_SEQ", sequenceName="VESSEL_OWNER_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "registration_code", length = 40, nullable = false)
    private String registrationCode;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "street")
    private String street;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "city")
    private String city;

    @Column(name = "date_of_birth")
    private Date dateOfBirth;

    @Column(name = "retirement_date")
    private Date retirementDate;

    @Column(name = "activity_start_date")
    private Date activityStartDate;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "mobile_number", length = 50)
    private String mobileNumber;

    @Column(name = "fax_number", length = 50)
    private String faxNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_location_fk")
    private Location countryLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

}

/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.model.technical.optimization.vessel;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.VesselType;
import net.sumaris.core.model.referential.location.Location;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Document(indexName = VesselSnapshot.INDEX)
public class VesselSnapshot {

    public static final String INDEX = "vessel_snapshot";

    private Integer id;
    private String name;

    private String exteriorMarking;

    @Column(name = "registration_code", length = 40)
    private String registrationCode;

    @Column(name = "int_registration_code", length = 40)
    private String intRegistrationCode;

    @Column(name = "administrative_power")
    private Integer administrativePower;

    @Column(name = "length_over_all")
    private Double lengthOverAll;

    @Column(name = "gross_tonnage_grt")
    private Double grossTonnageGrt;

    @Column(name = "gross_tonnage_gt")
    private Double grossTonnageGt;

    @Column(name = "construction_year")
    private Integer constructionYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="base_port_location_fk", nullable = false)
    private Location basePortLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_location_fk", nullable = false)
    private Location registrationLocation;

    private String comments;

    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "creation_date")
    private Date creationDate;

    @Column(name = "update_date")
    private Date updateDate;

    @Column(name = "control_date")
    private Date controlDate;

    @Column(name = "validation_date")
    private Date validationDate;

    @Column(name = "qualification_date")
    private Date qualificationDate;

    @Column(name = "qualification_comments", length = IDataEntity.LENGTH_COMMENTS)
    private String qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_flag_fk")
    private QualityFlag qualityFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk")
    private Department recorderDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_person_fk")
    private Person recorderPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = VesselType.class)
    @JoinColumn(name = "vessel_type_fk", nullable = false)
    private VesselType vesselType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;
}

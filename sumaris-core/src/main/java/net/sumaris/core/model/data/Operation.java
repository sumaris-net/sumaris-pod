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
import net.sumaris.core.model.administration.programStrategy.Program;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.QualityFlag;
import net.sumaris.core.model.referential.metier.Metier;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@FieldNameConstants
@Entity
@Table(name = "operation")
@NamedQueries({
    @NamedQuery(name = "Operation.updateUndefinedOperationDates",
        query = "UPDATE Operation o " +
            "SET " +
            "  o.startDateTime = :startDateTime, " +
            "  o.fishingStartDateTime = :startDateTime, " +
            "  o.endDateTime = :endDateTime, " +
            "  o.fishingEndDateTime = :endDateTime " +
            "WHERE o.id IN ( " +
            "   SELECT o2.id " +
            "   FROM Operation o2 " +
            "   INNER JOIN o2.trip ft " +
            "   WHERE ft.id = :tripId " +
            "   AND o2.startDateTime = ft.departureDateTime " +
            "   AND o2.endDateTime = ft.returnDateTime " +
            "   AND (o2.startDateTime != :startDateTime " +
            "       OR o2.fishingStartDateTime != :startDateTime " +
            "       OR o2.endDateTime != :endDateTime " +
            "       OR o2.fishingEndDateTime != :endDateTime) " +
            ")"),
        @NamedQuery(name = "Operation.countByTripId",
                query = "SELECT COUNT(*) " +
                        "FROM Operation o " +
                        "WHERE o.trip.id = :tripId")
})
public class Operation implements IDataEntity<Integer>,
    IWithSamplesEntity<Integer, Sample>,
    IWithBatchesEntity<Integer, Batch>,
    IWithProductsEntity<Integer, Product> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "OPERATION_SEQ")
    @SequenceGenerator(name = "OPERATION_SEQ", sequenceName = "OPERATION_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(name = "control_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date controlDate;

    @Column(name = "qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name = "qualification_comments", length = LENGTH_COMMENTS)
    private String qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = QualityFlag.class)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Trip.class )
    @JoinColumn(name = "trip_fk", nullable = false)
    @ToString.Exclude
    private Trip trip;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = Operation.class )
    @JoinColumn(name = "operation_fk")
    @ToString.Exclude
    private Operation parentOperation;

    @Column(name = "start_date_time", nullable = false)
    private Date startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private Date endDateTime;

    @Column(name = "fishing_start_date_time")
    private Date fishingStartDateTime;

    @Column(name = "fishing_end_date_time")
    private Date fishingEndDateTime;

    @Column(name = "rank_order_on_period")
    private Integer rankOrderOnPeriod;

    @Column(name = "has_catch")
    private Boolean hasCatch;

    @ManyToOne(fetch = FetchType.LAZY)
    // /!\ metier is nullable !
    @JoinColumn(name = "metier_fk")
    private Metier metier;

    @ManyToOne(fetch = FetchType.LAZY)
    // /!\ physicalGear is nullable (need by ObsDeb)
    @JoinColumn(name = "physical_gear_fk")
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    private PhysicalGear physicalGear;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselPosition.class, mappedBy = VesselPosition.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselPosition> positions = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = VesselUseMeasurement.class, mappedBy = VesselUseMeasurement.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<VesselUseMeasurement> vesselUseMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = GearUseMeasurement.class, mappedBy = GearUseMeasurement.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<GearUseMeasurement> gearUseMeasurements = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Sample.class, mappedBy = Sample.Fields.OPERATION)
    @Cascade({org.hibernate.annotations.CascadeType.DELETE})
    private List<Sample> samples = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Batch.class, mappedBy = Batch.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Batch> batches;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Product.class, mappedBy = Product.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Product> products = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = FishingArea.class, mappedBy = FishingArea.Fields.OPERATION)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<FishingArea> fishingAreas = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, targetEntity = Operation.class, mappedBy = Fields.PARENT_OPERATION)
    private Operation childOperation;


}

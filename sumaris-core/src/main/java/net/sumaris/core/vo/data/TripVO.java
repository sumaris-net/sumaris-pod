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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithObserversEntity;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.MetierVO;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@FieldNameConstants
@EqualsAndHashCode
public class TripVO implements IRootDataVO<Integer>,
        IWithObserversEntity<Integer, PersonVO>,
        IWithVesselSnapshotEntity<Integer, VesselSnapshotVO> {

    public static final String TYPENAME = "TripVO";

    @EqualsAndHashCode.Exclude
    private Integer id;
    private String comments;
    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Date qualificationDate;
    private String qualificationComments;
    private Integer qualityFlagId;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private VesselSnapshotVO vesselSnapshot;

    private Date departureDateTime;
    private Date returnDateTime;
    private LocationVO departureLocation;
    private LocationVO returnLocation;
    private ProgramVO program;
    private Set<PersonVO> observers;
    private List<PhysicalGearVO> gears;

    @EqualsAndHashCode.Exclude
    private Boolean hasSales; // Optimization: allow to NOT fetch expected sale (fix #IMAGINE-651)
    @EqualsAndHashCode.Exclude
    private List<SaleVO> sales;
    @EqualsAndHashCode.Exclude
    private SaleVO sale; // shortcut when only one sale

    @EqualsAndHashCode.Exclude
    private Boolean hasExpectedSales; // Optimization: allow to NOT fetch expected sale (fix #IMAGINE-651)
    @EqualsAndHashCode.Exclude
    private List<ExpectedSaleVO> expectedSales;
    @EqualsAndHashCode.Exclude
    private ExpectedSaleVO expectedSale; // shortcut when only one expected sale

    @EqualsAndHashCode.Exclude
    private List<MetierVO> metiers;

    @EqualsAndHashCode.Exclude
    private List<OperationVO> operations;

    @EqualsAndHashCode.Exclude
    private List<OperationGroupVO> operationGroups;

    @EqualsAndHashCode.Exclude
    private List<FishingAreaVO> fishingAreas;

    @EqualsAndHashCode.Exclude
    private List<MeasurementVO> measurements; // vessel_use_measurement
    private Map<Integer, String> measurementValues; // vessel_use_measurement

    // Parent
    @ToString.Exclude
    private LandingVO landing;
    private Integer landingId;

    private Integer observedLocationId;
    @ToString.Exclude
    private ObservedLocationVO observedLocation;

    @Override
    @JsonIgnore
    public Date getVesselDateTime() {
        return departureDateTime;
    }

}

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
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.referential.LocationVO;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@FieldNameConstants
public class LandingVO implements IRootDataVO<Integer>,
        IWithVesselSnapshotEntity<Integer, VesselSnapshotVO>,
        IWithObserversEntity<Integer, PersonVO>,
        IWithMeasurementValues{

    private Integer id;
    private String comments;
    private Date creationDate;
    private Date updateDate;
    private Date controlDate;
    private Date validationDate;
    private Integer qualityFlagId;
    private Date qualificationDate;
    private String qualificationComments;
    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Integer vesselId;
    @ToString.Exclude
    private VesselSnapshotVO vesselSnapshot;

    private Date dateTime;
    private LocationVO location;
    private Integer rankOrder;

    private Set<PersonVO> observers;

    @EqualsAndHashCode.Exclude
    private Boolean hasSamples; // Optimization: allow to NOT fetch samples
    private List<SampleVO> samples;
    private Integer samplesCount;

    @EqualsAndHashCode.Exclude
    private Boolean hasSales; // Optimization: allow to NOT fetch sales
    private List<SaleVO> sales;
    private List<Integer> saleIds;

    // Not used in the App
    // and the association 'Landing.expectedSales' has been comment out (see issue sumaris-pod #24)
    //private List<ExpectedSaleVO> expectedSales;

    private ProgramVO program;

    private List<MeasurementVO> measurements;
    private Map<Integer, String> measurementValues;

    @ToString.Exclude
    private ObservedLocationVO observedLocation;
    private Integer observedLocationId;

    @ToString.Exclude
    private TripVO trip;
    private Integer tripId;

    @Override
    @JsonIgnore
    public Date getVesselDateTime() {
        return dateTime;
    }
}

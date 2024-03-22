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

import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.model.data.IWithVesselSnapshotEntity;
import net.sumaris.core.vo.administration.programStrategy.ProgramVO;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.referential.LocationVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@FieldNameConstants
public class SaleVO implements IRootDataVO<Integer>,
        IWithRecorderPersonEntity<Integer, PersonVO>,
        IWithVesselSnapshotEntity<Integer, VesselSnapshotVO> {

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
    private ProgramVO program;

    private Integer vesselId;
    @ToString.Exclude
    private VesselSnapshotVO vesselSnapshot;

    private Date startDateTime;
    private Date endDateTime;
    private LocationVO saleLocation;
    private ReferentialVO saleType;

    private Set<PersonVO> observers;

    @ToString.Exclude
    private TripVO trip;
    private Integer tripId;

    @ToString.Exclude
    private LandingVO landing;
    private Integer landingId;

    private List<MeasurementVO> measurements; // sale_measurement
    private Map<Integer, String> measurementValues; // sale_measurement

    private List<ProductVO> products;

    // FIXME to remove - not used by ObsVente
    private List<SampleVO> samples;

    private BatchVO catchBatch;
    private List<BatchVO> batches;
    @Override
    public Date getVesselDateTime() {
        return startDateTime;
    }
}

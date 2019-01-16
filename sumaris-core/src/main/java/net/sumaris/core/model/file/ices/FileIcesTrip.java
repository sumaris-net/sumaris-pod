package net.sumaris.core.model.file.ices;

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
import net.sumaris.core.dao.technical.model.IEntityBean;
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "file_ices_trip")
public class FileIcesTrip implements Serializable, IEntityBean<Integer> {

    public static final DatabaseTableEnum TABLE = DatabaseTableEnum.FILE_ICES_TRIP;

    public static final String COLUMN_SAMPLING_TYPE = "sampling_type";
    public static final String COLUMN_VESSEL_FLAG_COUNTRY = "vessel_flag_country";
    public static final String COLUMN_LANDING_COUNTRY = "landing_country";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_PROJECT = "project";
    public static final String COLUMN_TRIP_CODE = "trip_code";
    public static final String COLUMN_VESSEL_LENGTH = "vessel_length";
    public static final String COLUMN_VESSEL_POWER = "vessel_power";
    public static final String COLUMN_VESSEL_SIZE = "vessel_size";
    public static final String COLUMN_VESSEL_TYPE = "vessel_type";
    public static final String COLUMN_HARBOUR = "harbour";
    public static final String COLUMN_OPERATION_COUNT = "number_hauls";
    public static final String COLUMN_DAYS_AT_SEA = "days_at_sea";
    public static final String COLUMN_VESSEL_IDENTIFIER = "vessel_identifier";
    public static final String COLUMN_SAMPLING_COUNTRY= "sampling_country";
    public static final String COLUMN_SAMPLING_METHOD = "sampling_method";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "file_ices_trip_seq")
    @SequenceGenerator(name = "file_ices_trip_seq", sequenceName="file_ices_trip_seq")
    private Integer id;

    @Column(nullable = false, length = 2, name = COLUMN_SAMPLING_TYPE)
    private String samplingType;

    @Column(nullable = false, length = 3, name = COLUMN_LANDING_COUNTRY)
    private String landingCountry;

    @Column(nullable = false, length = 3, name = COLUMN_VESSEL_FLAG_COUNTRY)
    private String vesselFlagCountry;

    @Column(nullable = false, scale = 4, name = COLUMN_YEAR)
    private Integer year;

    @Column(nullable = false, name = COLUMN_PROJECT)
    private String project;

    @Column(nullable = false, name = COLUMN_TRIP_CODE, length = 50)
    private String tripCode;

    @Column(scale= 6, name = COLUMN_VESSEL_LENGTH)
    private Integer vesselLength;

    @Column(scale = 6, name = COLUMN_VESSEL_POWER)
    private Integer vesselPower;

    @Column(scale = 6, name = COLUMN_VESSEL_SIZE)
    private Integer vesselSize;

    @Column(nullable = false, scale = 3, name = COLUMN_VESSEL_TYPE)
    private Integer vesselType;

    @Column(name = COLUMN_HARBOUR, length = 50)
    private String harbour;

    @Column(scale = 5, name = COLUMN_OPERATION_COUNT)
    private Integer operationCount;

    @Column(scale = 5,name = COLUMN_DAYS_AT_SEA)
    private Integer daysAtSea;

    @Column(name = COLUMN_VESSEL_IDENTIFIER)
    private Integer vesselIdentifier;

    @Column(nullable = false, length = 3, name = COLUMN_SAMPLING_COUNTRY)
    private String samplingCountry;

    @Column(length = 50, name = COLUMN_SAMPLING_METHOD) // nullable = false (removed for GBR)
    private String samplingMethod;


}

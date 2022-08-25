package net.sumaris.core.model.technical.extraction.rdb;

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
import net.sumaris.core.dao.technical.schema.DatabaseTableEnum;

import javax.persistence.*;
import java.io.Serializable;

@Data
@FieldNameConstants
@Entity
@Table(name = "p01_rdb_station")
public class ProductRdbStation implements Serializable, IEntity<Integer> {

    public static final DatabaseTableEnum TABLE = DatabaseTableEnum.P01_RDB_STATION;
    public static final String SHEET_NAME = "HH";

    public static final String COLUMN_SAMPLING_TYPE = ProductRdbTrip.COLUMN_SAMPLING_TYPE;
    public static final String COLUMN_VESSEL_FLAG_COUNTRY = ProductRdbTrip.COLUMN_VESSEL_FLAG_COUNTRY;
    public static final String COLUMN_LANDING_COUNTRY = ProductRdbTrip.COLUMN_LANDING_COUNTRY;
    public static final String COLUMN_YEAR = ProductRdbTrip.COLUMN_YEAR;
    public static final String COLUMN_PROJECT = ProductRdbTrip.COLUMN_PROJECT;
    public static final String COLUMN_TRIP_CODE = ProductRdbTrip.COLUMN_TRIP_CODE;

    public static final String COLUMN_STATION_NUMBER = "station_number";
    public static final String COLUMN_FISHING_VALIDITY = "fishing_validity";
    public static final String COLUMN_AGGREGATION_LEVEL = "aggregation_level";
    public static final String COLUMN_CATCH_REGISTRATION = "catch_registration";
    public static final String COLUMN_SPECIES_REGISTRATION = "species_registration";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_FISHING_TIME  = "fishing_time";
    public static final String COLUMN_POS_START_LAT = "pos_start_lat";
    public static final String COLUMN_POS_START_LON = "pos_start_lon";
    public static final String COLUMN_POS_END_LAT = "pos_end_lat";
    public static final String COLUMN_POS_END_LON = "pos_end_lon";
    public static final String COLUMN_AREA = "area";
    public static final String COLUMN_STATISTICAL_RECTANGLE = "statistical_rectangle";
    public static final String COLUMN_SUB_POLYGON = "sub_polygon";
    public static final String COLUMN_MAIN_FISHING_DEPTH = "main_fishing_depth";
    public static final String COLUMN_MAIN_WATER_DEPTH = "main_water_depth";
    public static final String COLUMN_NATIONAL_METIER = "national_metier";
    public static final String COLUMN_EU_METIER_LEVEL5 = "eu_metier_level5";
    public static final String COLUMN_EU_METIER_LEVEL6 = "eu_metier_level6";
    public static final String COLUMN_GEAR_TYPE = "gear_type";
    public static final String COLUMN_MESH_SIZE = "mesh_size";
    public static final String COLUMN_SELECTION_DEVICE = "selection_device";
    public static final String COLUMN_MESH_SIZE_SELECTION_DEVICE  = "mesh_size_selection_device";
    public static final String COLUMN_VESSEL_LENGTH_CATEGORY = "vessel_length_category";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "p01_rdb_station_seq")
    @SequenceGenerator(name = "p01_rdb_station_seq", sequenceName="p01_rdb_station_seq", allocationSize = 1)
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

    @Column(name = COLUMN_STATION_NUMBER, scale=6)
    private Integer stationNumber;

    @Column(name = COLUMN_FISHING_VALIDITY, length=1)
    private String fishingValidity;

    @Column(name = COLUMN_AGGREGATION_LEVEL, length=1)
    private String aggregationLevel;

    @Column(nullable = false, length= 3, name = COLUMN_CATCH_REGISTRATION)
    private String catchRegistration;

    @Column(name = COLUMN_SPECIES_REGISTRATION, length = 25)
    private String speciesRegistration;

    @Column(name = COLUMN_DATE, length = 10)
    private String date;

    @Column(name = COLUMN_TIME, length = 8)
    private String time;

    @Column(name = COLUMN_FISHING_TIME, scale=6)
    private Integer fishingTime; // in minutes

    @Column(scale=9, precision=7, name = COLUMN_POS_START_LAT)
    private Double positionStartLatitude;

    @Column(scale=10, precision=7, name = COLUMN_POS_START_LON)
    private Double positionStartLongitude;

    @Column(scale=9, precision=7, name = COLUMN_POS_END_LAT)
    private Double positionEndLatitude;

    @Column(scale=10, precision=7, name = COLUMN_POS_END_LON)
    private Double positionEndLongitude;

    @Column(nullable = false, length = 25, name = COLUMN_AREA)
    private String area;

    @Column(length = 25, name = COLUMN_STATISTICAL_RECTANGLE)
    private String statisticalRectangle;

    @Column(length = 25, name = COLUMN_SUB_POLYGON)
    private String subPolygon;

    @Column(scale = 5, name = COLUMN_MAIN_FISHING_DEPTH)
    private Integer mainFishingDepth;

    @Column(scale = 5, name = COLUMN_MAIN_WATER_DEPTH)
    private Integer mainWaterDepth;

    @Column(length = 25, name = COLUMN_NATIONAL_METIER)
    private String nationalMetier;

    @Column(length = 25, name = COLUMN_EU_METIER_LEVEL5)
    private String metierLevel5;

    @Column(nullable = false, length = 25, name = COLUMN_EU_METIER_LEVEL6)
    private String metierLevel6;

    @Column(nullable = false, length = 5, name = COLUMN_GEAR_TYPE)
    private String gearType;

    @Column(scale = 5, name = COLUMN_MESH_SIZE)
    private Integer meshSize;

    @Column(scale = 5, name = COLUMN_SELECTION_DEVICE)
    private Integer selectionDevice;

    @Column(scale = 5, name = COLUMN_MESH_SIZE_SELECTION_DEVICE)
    private Integer meshSizeSelectionDevice; // in mm
}

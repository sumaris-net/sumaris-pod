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
@Table(name = "file_ices_landing")
public class FileIcesLandingStatistics implements Serializable, IEntityBean<Integer> {

    public static final DatabaseTableEnum TABLE = DatabaseTableEnum.FILE_ICES_LANDING;

    public static final String COLUMN_VESSEL_FLAG_COUNTRY = FileIcesTrip.COLUMN_VESSEL_FLAG_COUNTRY;
    public static final String COLUMN_LANDING_COUNTRY = FileIcesTrip.COLUMN_LANDING_COUNTRY;
    public static final String COLUMN_YEAR = FileIcesTrip.COLUMN_YEAR;
    public static final String COLUMN_QUARTER = "quarter";
    public static final String COLUMN_MONTH = "month";
    public static final String COLUMN_AREA = FileIcesStation.COLUMN_AREA;
    public static final String COLUMN_STATISTICAL_RECTANGLE = FileIcesStation.COLUMN_STATISTICAL_RECTANGLE;
    public static final String COLUMN_SUB_POLYGON = FileIcesStation.COLUMN_SUB_POLYGON;
    public static final String COLUMN_SPECIES = "species";
    public static final String COLUMN_LANDING_CATEGORY = FileIcesSpeciesList.COLUMN_LANDING_CATEGORY;
    public static final String COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE = FileIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE;
    public static final String COLUMN_COMMERCIAL_SIZE_CATEGORY = FileIcesSpeciesList.COLUMN_COMMERCIAL_SIZE_CATEGORY;
    public static final String COLUMN_NATIONAL_METIER = FileIcesStation.COLUMN_NATIONAL_METIER;
    public static final String COLUMN_EU_METIER_LEVEL5 = FileIcesStation.COLUMN_EU_METIER_LEVEL5;
    public static final String COLUMN_EU_METIER_LEVEL6 = FileIcesStation.COLUMN_EU_METIER_LEVEL6;
    public static final String COLUMN_HARBOUR = FileIcesTrip.COLUMN_HARBOUR;
    public static final String COLUMN_VESSEL_LENGTH_CATEGORY = "vessel_length_cat";
    public static final String COLUMN_UNALLOCATED_CATCH_WEIGHT = "unallocated_catch_weight";
    public static final String COLUMN_AREA_MISREPORTED_CATCH_WEIGHT = "area_misreported_catch_weight";
    public static final String COLUMN_OFFICIAL_LANDINGS_WEIGHT = "official_landings_weight";
    public static final String COLUMN_LANDINGS_MULTIPLIER = "landings_multiplier";
    public static final String COLUMN_OFFICIAL_LANDINGS_VALUE = "official_landings_value";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "file_ices_landing_seq")
    @SequenceGenerator(name = "file_ices_landing_seq", sequenceName="file_ices_landing_seq")
    private Integer id;

    @Column(nullable = false, length = 3, name = COLUMN_LANDING_COUNTRY)
    private String landingCountry;

    @Column(nullable = false, length = 3, name = COLUMN_VESSEL_FLAG_COUNTRY)
    private String vesselFlagCountry;

    @Column(nullable = false, scale = 4, name = COLUMN_YEAR)
    private Integer year;

    @Column(nullable = false, scale = 1, name = COLUMN_QUARTER)
    private Integer quarter;

    @Column(scale = 2, name = COLUMN_MONTH)
    private Integer month;

    @Column(nullable = false, length = 25, name = COLUMN_AREA)
    private String area;

    @Column(length = 25, name = COLUMN_STATISTICAL_RECTANGLE)
    private String statisticalRectangle;

    @Column(length = 25, name = COLUMN_SUB_POLYGON)
    private String subPolygon;

    @Column(nullable = false, length = 25, name = COLUMN_SPECIES)
    private String species;

    @Column(nullable = false, length = 25, name = COLUMN_LANDING_CATEGORY)
    private String landingCategory;

    @Column(length = 25, name = COLUMN_COMMERCIAL_SIZE_CATEGORY_SCALE)
    private String commercialCategoryScale;

    @Column(length = 25, name = COLUMN_COMMERCIAL_SIZE_CATEGORY)
    private String commercialCategory;

    @Column(length = 25, name = COLUMN_NATIONAL_METIER)
    private String nationalMetier;

    @Column(length = 25, name = COLUMN_EU_METIER_LEVEL5)
    private String metierLevel5;

    @Column(nullable = false, length = 25, name = COLUMN_EU_METIER_LEVEL6)
    private String metierLevel6;

    @Column(length = 25, name = COLUMN_HARBOUR)
    private String harbour;

    @Column(length = 25, name = COLUMN_VESSEL_LENGTH_CATEGORY)
    private String vesselLengthCategory;

    @Column(nullable = false, name = COLUMN_UNALLOCATED_CATCH_WEIGHT, scale = 12, precision = 2)
    private Double unallocatedCatchWeight;

    @Column(nullable = false, name = COLUMN_AREA_MISREPORTED_CATCH_WEIGHT, scale = 12, precision = 2)
    private Double misReportedCatchWeight;

    @Column(nullable = false, name = COLUMN_OFFICIAL_LANDINGS_WEIGHT, scale = 12, precision = 2)
    private Double landingsWeight;

    @Column(name = COLUMN_LANDINGS_MULTIPLIER, scale = 4, precision = 3)
    private Double landingsMultiplier;

    @Column(name = COLUMN_OFFICIAL_LANDINGS_VALUE, scale = 12, precision = 2)
    private Double landingsValue;


}

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

package net.sumaris.core.model.referential.spatial;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "denormalized_spatial_item")
@IdClass(DenormalizedSpatialItemId.class)
public class DenormalizedSpatialItem implements Serializable {

    @Id
    @Column(name = "spatial_item_type_fk", nullable = false)
    @EqualsAndHashCode.Include
    private Long spatialItemTypeId;

    @Column(name = "spatial_item_type_label", nullable = false)
    private String spatialItemTypeLabel;

    @Column(name = "spatial_item_type_name", nullable = false)
    private String spatialItemTypeName;

    @Column(name = "spatial_item_type_status", nullable = false)
    private String spatialItemTypeStatus;

    @Column(name = "spatial_item_type_update_date", nullable = false)
    private Date spatialItemTypeUpdateDate;

    @Column(name = "spatial_item_update_date", nullable = false)
    private Date spatialItemUpdateDate;

    @Column(name = "object_table", nullable = false)
    private String objectTable;

    @Column(name = "object_table_filter")
    private String objectTableFilter;

    @Id
    @Column(name = "object_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long objectId;

    @Column(name = "object_label")
    private String objectLabel;

    @Column(name = "object_name")
    private String objectName;

    @Column(name = "object_status")
    private String objectStatus;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "localized_name")
    private String localizedName;

    @Column(name = "localized_location_fk")
    private Long localizedLocationId;

    @Column(name = "localized_location_level_fk")
    private String localizedLocationLevelFk;

    @Column(name = "localized_loc_class_level_fk")
    private String localizedLocClassLevelFk;

    @Column(name = "localized_location_label")
    private String localizedLocationLabel;

    @Column(name = "localized_location_name")
    private String localizedLocationName;

    @Column(name = "localized_location_status")
    private String localizedLocationStatus;
}

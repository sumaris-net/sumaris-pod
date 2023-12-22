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

package net.sumaris.core.model.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.programStrategy.AcquisitionLevel;
import net.sumaris.core.model.administration.programStrategy.Program;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "gear_use_features_origin")
public class GearUseFeaturesOrigin implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk", nullable = false)
    @EqualsAndHashCode.Include
    private Program program;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gear_use_features_fk", nullable = false)
    @EqualsAndHashCode.Include
    private GearUseFeatures gearUseFeatures;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquisition_level_fk")
    private AcquisitionLevel acquisitionLevel;
}
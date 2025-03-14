package net.sumaris.core.model.data;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2024 SUMARiS Consortium
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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.persistence.*;
import java.io.Serializable;


@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "operation_vessel_association")
public class OperationVesselAssociation implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Operation.class)
    @JoinColumn(name = "operation_fk", nullable = false)
    @EqualsAndHashCode.Include
    private Operation operation;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Vessel.class)
    @JoinColumn(name = "vessel_fk", nullable = false)
    @EqualsAndHashCode.Include
    private Vessel vessel;

    @Column(name = "is_catch_on_operation_vessel", nullable = false)
    private Boolean isCatchOnOperationVessel;

}

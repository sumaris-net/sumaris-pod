package net.sumaris.core.model.administration.programStrategy;

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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.location.Location;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "applied_strategy")
public class AppliedStrategy implements IEntity<Integer> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "APPLIED_STRATEGY_SEQ")
    @SequenceGenerator(name = "APPLIED_STRATEGY_SEQ", sequenceName="APPLIED_STRATEGY_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_fk", nullable = false)
    
    private Strategy strategy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_fk", nullable = false)
    
    private Location location;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = AppliedPeriod.Fields.APPLIED_STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<AppliedPeriod> appliedPeriods = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = PmfmAppliedStrategy.Fields.APPLIED_STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<PmfmAppliedStrategy> pmfmStrategies = new ArrayList<>();

}

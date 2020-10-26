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

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@FieldNameConstants
@Entity
@Table(name = "strategy")
public class Strategy implements IItemReferentialEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "STRATEGY_SEQ")
    @SequenceGenerator(name = "STRATEGY_SEQ", sequenceName="STRATEGY_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_fk", nullable = false)
    private Status status;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(length = 2000)
    private String comments;

    private String analyticReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = AppliedStrategy.class, mappedBy = AppliedStrategy.Fields.STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<AppliedStrategy> appliedStrategies = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = PmfmStrategy.class, mappedBy = PmfmStrategy.Fields.STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<PmfmStrategy> pmfmStrategies = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = StrategyDepartment.class, mappedBy = StrategyDepartment.Fields.STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<StrategyDepartment> strategyDepartments = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(name = "strategy2gear", joinColumns = {
            @JoinColumn(name = "strategy_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "gear_fk", nullable = false, updatable = false) })
    private Set<Gear> gears = Sets.newHashSet();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ReferenceTaxonStrategy.class, mappedBy = ReferenceTaxonStrategy.Fields.STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ReferenceTaxonStrategy> referenceTaxons = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = TaxonGroupStrategy.class, mappedBy = TaxonGroupStrategy.Fields.STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<TaxonGroupStrategy> taxonGroups = new ArrayList<>();

    public void addPmfmStrategy(PmfmStrategy pmfmStrategy, boolean setReverse) {
        if (pmfmStrategy != null) {
            getPmfmStrategies().add(pmfmStrategy);
            if (setReverse) {
                pmfmStrategy.setStrategy(this);
            }
        }
    }

    public String toString() {
        return String.format("Strategy{id=%s, label=%s, programId=%s}",
                id,
                label,
                program.getId());
    }
}

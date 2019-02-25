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
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.service.referential.PmfmService;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
@Entity
public class Strategy implements IItemReferentialEntity {

    public static final String PROPERTY_PROGRAM = "program";
    public static final String PROPERTY_GEARS = "gears";

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = PmfmStrategy.class, mappedBy = PmfmStrategy.PROPERTY_STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<PmfmStrategy> pmfmStrategies = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "strategy2gear", joinColumns = {
            @JoinColumn(name = "strategy_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "gear_fk", nullable = false, updatable = false) })
    private Set<Gear> gears = Sets.newHashSet();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ReferenceTaxonStrategy.class, mappedBy = ReferenceTaxonStrategy.PROPERTY_STRATEGY)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ReferenceTaxonStrategy> referenceTaxons = new ArrayList<>();
}

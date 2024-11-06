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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationClassification;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import net.sumaris.core.model.technical.configuration.SoftwareProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Cacheable
public class Program implements IItemReferentialEntity<Integer> {

    public static final String ENTITY_NAME = "Program";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PROGRAM_SEQ")
    @SequenceGenerator(name = "PROGRAM_SEQ", sequenceName="PROGRAM_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    @ToString.Include
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

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_LABEL)
    @ToString.Include
    private String label;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_NAME)
    private String name;

    private String description;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Strategy.class, mappedBy = Strategy.Fields.PROGRAM)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Strategy> strategies = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = SoftwareProperty.class)
    @JoinColumn(name = "object_id")
    @Where(clause = "object_type_fk = (select ot.id from object_type ot where ot.label = 'PROGRAM')")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<SoftwareProperty> properties = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = TaxonGroupType.class)
    @JoinColumn(name = "taxon_group_type_fk", nullable = false)
    private TaxonGroupType taxonGroupType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gear_classification_fk", nullable = false)
    private GearClassification gearClassification;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "program2location_classif", joinColumns = {
            @JoinColumn(name = "program_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "location_classification_fk", nullable = false, updatable = false) })
    private Set<LocationClassification> locationClassifications = Sets.newHashSet();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(name = "program2location", joinColumns = {
            @JoinColumn(name = "program_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "location_fk", nullable = false, updatable = false) })
    private Set<Location> locations = Sets.newHashSet();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ProgramDepartment.class, mappedBy = ProgramDepartment.Fields.PROGRAM)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ProgramDepartment> departments = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, targetEntity = ProgramPerson.class, mappedBy = ProgramPerson.Fields.PROGRAM)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ProgramPerson> persons = new ArrayList<>();

    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Program that = (Program) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }
}

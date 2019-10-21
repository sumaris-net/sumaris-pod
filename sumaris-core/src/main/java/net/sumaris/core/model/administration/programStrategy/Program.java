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

import lombok.Data;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.gear.GearClassification;
import net.sumaris.core.model.referential.taxon.TaxonGroupType;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Cacheable
public class Program implements IItemReferentialEntity {

    public static final String PROPERTY_PROPERTIES = "properties";
    public static final String PROPERTY_TAXON_GROUP_TYPE = "taxonGroupType";
    public static final String PROPERTY_GEAR_CLASSIFICATION = "gearClassification";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PROGRAM_SEQ")
    @SequenceGenerator(name = "PROGRAM_SEQ", sequenceName="PROGRAM_SEQ")
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
    private String label;

    @Column(nullable = false, length = IItemReferentialEntity.LENGTH_NAME)
    private String name;

    private String description;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = Strategy.class, mappedBy = Strategy.PROPERTY_PROGRAM)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<Strategy> strategies = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER, targetEntity = ProgramProperty.class, mappedBy = ProgramProperty.PROPERTY_PROGRAM)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<ProgramProperty> properties = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = TaxonGroupType.class)
    @JoinColumn(name = "taxon_group_type_fk", nullable = false)
    private TaxonGroupType taxonGroupType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gear_classification_fk", nullable = false)
    private GearClassification gearClassification;

    public int hashCode() {
        return Objects.hash(label);
    }
}

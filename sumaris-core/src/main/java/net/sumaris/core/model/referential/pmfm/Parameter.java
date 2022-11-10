package net.sumaris.core.model.referential.pmfm;

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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Quadruplet paramètre/support/méthode/fraction. Ce quadruplet est systématiquement associé aux résultats et décrit précisemment les conditions de mesure/d'analyse de ceux-ci.
 */
@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Cacheable
public class Parameter implements IItemReferentialEntity<Integer> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "PARAMETER_SEQ")
    @SequenceGenerator(name = "PARAMETER_SEQ", sequenceName="PARAMETER_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)

    @EqualsAndHashCode.Include
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

    @Column(nullable = false, length = LENGTH_LABEL, unique = true)

    private String label;

    @Column(nullable = false, length = LENGTH_NAME)
    private String name;

    private String description;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(name = "is_qualitative", nullable = false)
    private Boolean isQualitative;

    @Column(name = "is_alphanumeric", nullable = false)
    private Boolean isAlphanumeric;

    @Column(name = "is_boolean")
    private Boolean isBoolean;

    @Column(name = "is_date")
    private Boolean isDate;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = QualitativeValue.class, mappedBy = QualitativeValue.Fields.PARAMETER)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<QualitativeValue> qualitativeValues = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_group_fk", nullable = false)
    private ParameterGroup parameterGroup;
}

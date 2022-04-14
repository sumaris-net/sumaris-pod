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

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.Status;
import org.hibernate.annotations.Formula;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "pmfm",
        uniqueConstraints = {
            @UniqueConstraint(name="pmfm_unique_c", columnNames = {"parameter_fk", "matrix_fk", "fraction_fk", "method_fk", "unit_fk"})
        }
)
@Cacheable
public class Pmfm implements IItemReferentialEntity, IReferentialWithStatusEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "PMFM_SEQ")
    @SequenceGenerator(name = "PMFM_SEQ", sequenceName="PMFM_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

    @Column(length = IItemReferentialEntity.LENGTH_LABEL, unique = true)
    @ToString.Include
    private String label;

    @Formula("(select p.name from parameter p where p.id = parameter_fk)")
    private String name;

    /**
     * Valeur mimimale autorisée par défaut (peut etre redéfini dans les stratégies).
     */
    @Column(name = "min_value")
    private Double minValue;

    /**
     * Valeur maximale autorisée par défaut (peut etre redéfini dans les stratégies).
     */
    @Column(name = "max_value")
    private Double maxValue;

    /**
     * Valeur par défaut (peut être redéfini dans les stratégies).
     */
    @Column(name = "default_value")
    private Double defaultValue;

    /**
     * Nombre de décimales significatives pour le résultat mesuré/analysé suivant le quadruplet lié.
     */
    @Column(name = "maximum_number_decimals")
    private Integer maximumNumberDecimals;

    /**
     * Nombre de chiffres significatifs en tout du résultat pour le quadruplet concerné.
     */
    @Column(name = "signif_figures_number")
    private Integer signifFiguresNumber;

    /**
     * Seuil de détection des instruments de mesure et de la méthode associée.
     */
    @Column(name = "detection_threshold")
    private Double detectionThreshold;

    /**
     * Précision de la mesure et de la méthode associée.
     */
    @Column(name = "precision")
    private Double precision;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Parameter.class)
    @JoinColumn(name = "parameter_fk", nullable = false)
    private Parameter parameter;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Matrix.class)
    @JoinColumn(name = "matrix_fk")
    private Matrix matrix;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Fraction.class)
    @JoinColumn(name = "fraction_fk")
    private Fraction fraction;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Method.class)
    @JoinColumn(name = "method_fk")
    private Method method;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Unit.class)
    @JoinColumn(name = "unit_fk", nullable = false)
    private Unit unit;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(name = "pmfm2qualitative_value", joinColumns = {
            @JoinColumn(name = "pmfm_fk", nullable = false, updatable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name = "qualitative_value_fk", nullable = false, updatable = false) })
    private Set<QualitativeValue> qualitativeValues = Sets.newHashSet();

    public boolean equals(Object other) {
        if (this == other) return true;
        if ( !(other instanceof Pmfm) ) return false;

        final Pmfm bean = (Pmfm) other;

        if ( !Objects.equals(bean.getId(), getId() ) ) return false;
        if ( !Objects.equals(bean.getParameter(), getParameter() ) ) return false;
        if ( !Objects.equals(bean.getMatrix(), getMatrix() ) ) return false;
        if ( !Objects.equals(bean.getFraction(), getFraction() ) ) return false;
        if ( !Objects.equals(bean.getUnit(), getUnit() ) ) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = getParameter().hashCode();
        result = 29 * result + getId();
        return result;
    }
}

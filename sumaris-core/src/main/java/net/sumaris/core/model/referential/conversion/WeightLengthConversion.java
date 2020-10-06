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

package net.sumaris.core.model.referential.conversion;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.OriginItemType;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.Parameter;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.pmfm.Unit;
import net.sumaris.core.model.referential.taxon.ReferenceTaxon;
import net.sumaris.core.model.referential.taxon.TaxonGroup;

import javax.persistence.*;
import java.util.Date;

/**
 * </p>Relation Taille poids :<br/>
 * <p>
 * Le calcul se fait par multiplication de la taille d’un poisson par un coefficient multiplicateur (convCoeff).
 * Ce coefficient est obtenu à partir des caractéristiques suivantes
 * - Taxon mesuré (=espèces scientifiques) : ReferenceTaxon
 * - Période (ex : du mois de "décembre" au mois de "juillet", indépendant de l'année): startMonth,
 *   endMonth
 * - Lieu ( Lieu père si inexistant).
 * - Sexe. Cette caractéristique n’étant pas renseignée systématiquement dans la table de correspondance,
 *   la recherche du coefficient doit d’abord s’effectuer sur les coefficients indépendant du sexe (Sex=null ).
 *   Si cette recherche est infructueuse, et si le sexe du lot est connu (critère de classement ‘sexe’ renseigné)
 *   une nouvelle recherche doit être lancée sur les coefficients ayant le sexe du lot considéré
 * </p>
 */
@Data
@FieldNameConstants
@Entity
@Table(name = "weight_length_conversion")
@Cacheable
public class WeightLengthConversion implements IReferentialWithStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "WEIGHT_LENGTH_CONVERSION_SEQ")
    @SequenceGenerator(name = "WEIGHT_LENGTH_CONVERSION_SEQ", sequenceName="WEIGHT_LENGTH_CONVERSION_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

    @Column(name = "conversion_coefficient_a", nullable = false)
    private Double conversionCoefficientA;

    @Column(name = "conversion_coefficient_b", nullable = false)
    private Double conversionCoefficientB;

    @Column(name = "start_month", nullable = false)
    private Integer startMonth;

    @Column(name = "end_month", nullable = false)
    private Integer endMonth;

    @Column(name = "year")
    private Integer year;

    private String description;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_item_type_fk")
    private OriginItemType originItemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sex_qualitative_value_fk")
    private QualitativeValue sex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "length_parameter_fk", nullable = false)
    private Parameter lengthParameter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "length_unit_fk", nullable = false)
    private Unit lengthUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_taxon_fk", nullable = false)
    private ReferenceTaxon referenceTaxon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_fk", nullable = false)
    private Location location;


    public String toString() {
        return new StringBuilder().append(super.toString())
                .append(",id=").append(this.id)
                .append(",referenceTaxon=").append(this.referenceTaxon.getId())
                .append(",conversionCoefficientA=").append(this.conversionCoefficientA)
                .append(",conversionCoefficientB=").append(this.conversionCoefficientB)
                .append(",sex=").append(this.sex != null ? this.sex.getLabel() : null)
                .append(",lengthParameter=").append(this.lengthParameter.getLabel())
                .append(",lengthUnit=").append(this.lengthUnit.getLabel())
                .append(",startMonth=").append(this.startMonth)
                .append(",endMonth=").append(this.endMonth)
                .append(",endMonth=").append(this.endMonth)
                .append(",year=").append(this.year)
                .append(",location=").append(this.location.getLabel())
                .toString();
    }
}

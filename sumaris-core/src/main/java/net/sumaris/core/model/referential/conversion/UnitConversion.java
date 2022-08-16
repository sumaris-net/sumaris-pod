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
import net.sumaris.core.model.referential.pmfm.Unit;

import javax.persistence.*;
import java.io.Serializable;
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
@Table(name = "unit_conversion")
@Cacheable
public class UnitConversion implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_unit_fk")
    private Unit fromUnit;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_unit_fk")
    private Unit toUnit;

    @Column(name = "conversion_coefficient", nullable = false)
    private Double conversionCoefficient;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;
}

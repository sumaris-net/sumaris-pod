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

import lombok.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.OriginItemType;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.pmfm.QualitativeValue;
import net.sumaris.core.model.referential.taxon.TaxonGroup;

import javax.persistence.*;
import java.util.Date;

/**
 * </p>Equivalent poids vif :<br/>
 * Le calcul se fait par multiplication du poids total du lot par un coefficient multiplicateur (convCoeff) . Ce coefficient est obtenu à partir des caractéristiques suivantes :
 * </p>
 * <li>- espèce commerciale : TaxonGroup
 *   <ul>- présentation du poisson: fishPresentation (ex : "Entier", "Vidé", "Étêté, vidé, équeuté", "Décortiqué", ...).</ul>
 *   <ul>- Etat du poisson : fishState (ex : "frais", "congelé", "salé", "séché"...).</ul>
 *   <ul>- Pays dans lequel s’est effectuée la capture.</ul>
 * </li>
 *
 * <p>
 * Definition FAO : "Round Weigth" : the weight of the whole fish before processing or removal of any part.
 * FAO (1998): Guidelines for the routine collection of capture fishery data. FAO Fish. Tech. Pap, 382: 113 p.
 * </p>
 */
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "round_weight_conversion")
@Cacheable
@NamedQueries({
        @NamedQuery(name = "RoundWeightConversion.dressingByTaxonGroupId", query = "SELECT\n" +
                "        DISTINCT c.dressing\n" +
                "      FROM RoundWeightConversion c\n" +
                "      WHERE c.taxonGroup.id IN (\n" +
                "        SELECT tgh.id.parentTaxonGroup.id\n" +
                "        FROM TaxonGroupHierarchy tgh\n" +
                "        WHERE tgh.id.childTaxonGroup.id = :taxonGroupId)\n" +
                "      AND c.location.id = :locationId\n" +
                "       AND c.dressing.status.id != 0\n" +
                "      AND NOT(\n" +
                "         c.startDate > :endDate\n" +
                "         OR coalesce(c.endDate, :startDate) < :startDate\n" +
                "        )"),
        @NamedQuery(name = "RoundWeightConversion.preservingByTaxonGroupId", query = "SELECT\n" +
                "        DISTINCT c.preserving\n" +
                "      FROM RoundWeightConversion c\n" +
                "      WHERE c.taxonGroup.id IN (\n" +
                "        SELECT tgh.id.parentTaxonGroup.id\n" +
                "        FROM TaxonGroupHierarchy tgh\n" +
                "        WHERE tgh.id.childTaxonGroup.id = :taxonGroupId)\n" +
                "      AND c.location.id = :locationId\n" +
                "      AND c.preserving.status.id != 0\n" +
                "      AND NOT(\n" +
                "         c.startDate > :endDate\n" +
                "         OR coalesce(c.endDate, :startDate) < :startDate\n" +
                "        )")
})
public class RoundWeightConversion implements IReferentialWithStatusEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ROUND_WEIGHT_CONVERSION_SEQ")
    @SequenceGenerator(name = "ROUND_WEIGHT_CONVERSION_SEQ", sequenceName="ROUND_WEIGHT_CONVERSION_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_item_type_fk")
    private OriginItemType originItemType;

    @Column(name = "conversion_coefficient", nullable = false)

    private Double conversionCoefficient;

    private String description;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxon_group_fk", nullable = false)

    private TaxonGroup taxonGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_fk", nullable = false)

    private Location location;

    /**
     * Etat du poisson (Entier, Eteté, etc.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preserving_fk", nullable = false)

    private QualitativeValue preserving;

    /**
     *  Présentation du poisson (Frais, Congelé, etc.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dressing_fk", nullable = false)

    private QualitativeValue dressing;

    @Column(name = "start_date", nullable = false)

    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

}

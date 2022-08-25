package net.sumaris.core.model.referential.taxon;

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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.IReferentialEntity;

import javax.persistence.*;
import java.util.Date;

/**
 *  Permet de définir la date de début d'appartenance du taxon (de référence) au groupe de taxon.
 *  Cette date est renseignée automatiquemnt par le système, lors d'une modification d'affectation.
 *
 */
@Data
@FieldNameConstants
@Entity
@Table(name = "taxon_group_historical_record")
public class TaxonGroupHistoricalRecord implements IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TAXON_GROUP_HISTORICAL_REC_SEQ")
    @SequenceGenerator(name = "TAXON_GROUP_HISTORICAL_REC_SEQ", sequenceName="TAXON_GROUP_HISTORICAL_REC_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(length = IItemReferentialEntity.LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = TaxonGroup.class)
    @JoinColumn(name = "taxon_group_fk", nullable = false)
    private TaxonGroup taxonGroup;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ReferenceTaxon.class)
    @JoinColumn(name = "reference_taxon_fk", nullable = false)
    private ReferenceTaxon referenceTaxon;
}

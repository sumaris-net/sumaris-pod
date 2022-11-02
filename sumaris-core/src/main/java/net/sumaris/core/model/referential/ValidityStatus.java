package net.sumaris.core.model.referential;

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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;
import net.sumaris.core.dao.technical.model.annotation.OntologyEntity;
import net.sumaris.core.model.ModelVocabularies;

import javax.persistence.*;
import java.util.Date;

/**
 * Etat de validation d'une donnée du référentiel. Utile pour les responsables de référentiel.<br/>
 * Validity status of a referential data.
 */
@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Cacheable
@Table(name="validity_status")
@OntologyEntity(vocab = ModelVocabularies.COMMON)
public class ValidityStatus implements IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "VALIDITY_STATUS_SEQ")
    @SequenceGenerator(name = "VALIDITY_STATUS_SEQ", sequenceName="VALIDITY_STATUS_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    @ToString.Include
    private Integer id;

    @Column(nullable = false)
    @ToString.Include
    private String label;

    @Column(nullable = false)
    private String name;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

}

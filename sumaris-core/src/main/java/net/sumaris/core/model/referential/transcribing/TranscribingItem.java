package net.sumaris.core.model.referential.transcribing;

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
import net.sumaris.core.model.ModelVocabularies;
import net.sumaris.core.model.annotation.OntologyEntity;
import net.sumaris.core.model.referential.IItemReferentialEntity;
import net.sumaris.core.model.referential.Status;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "transcribing_item")
@OntologyEntity(vocab = ModelVocabularies.TRANSCRIBING)
public class TranscribingItem implements IItemReferentialEntity<Integer>  {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "TRANSCRIBING_ITEM_SEQ")
    @SequenceGenerator(name = "TRANSCRIBING_ITEM_SEQ", sequenceName="TRANSCRIBING_ITEM_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
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

    @Column(name = "object_id")
    private Integer objectId;

    @Column(nullable = false) // length = 255
    private String label;

    @Column(length = LENGTH_NAME) // Not mandatory in Adagio DB
    private String name;

    private String description;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcribing_item_type_fk", nullable = false)
    private TranscribingItemType type;
}

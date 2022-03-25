package net.sumaris.core.model.social;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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
import net.sumaris.core.dao.technical.model.ISignedEntity;
import net.sumaris.core.model.data.IDataEntity;
import net.sumaris.core.model.referential.ObjectType;

import javax.persistence.*;
import java.util.Date;


@Data
@ToString(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "user_interaction")
@Cacheable
public class UserInteraction implements ISignedEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "USER_INTERACTION_SEQ")
    @SequenceGenerator(name = "USER_INTERACTION_SEQ", sequenceName="USER_INTERACTION_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(name = "issuer", nullable = false, length = CRYPTO_PUBKEY_LENGTH)
    @ToString.Include
    private String issuer;

    @Column(name = "interaction_type", nullable = false, length = 30)
    @ToString.Include(rank = 1)
    private String interactionType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ObjectType.class)
    @JoinColumn(name = "object_type_fk", nullable = false)
    private ObjectType objectType;

    @Column(name = "object_id", nullable = false)
    private Integer objectId;

    @Column(length = IDataEntity.LENGTH_COMMENTS)
    private String comment;

    @Column(name = "hash", length = CRYPTO_HASH_LENGTH)
    @ToString.Include
    private String hash;

    @Column(name = "signature", length = CRYPTO_SIGNATURE_LENGTH)
    private String signature;

}

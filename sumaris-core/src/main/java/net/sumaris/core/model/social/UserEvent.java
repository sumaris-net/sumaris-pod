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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IUpdateDateEntityBean;
import net.sumaris.core.model.data.IDataEntity;

import javax.persistence.*;
import java.util.Date;


@Data
@FieldNameConstants
@Entity
@Table(name = "user_event")
@Cacheable
public class UserEvent implements IUpdateDateEntityBean<Integer, Date> {

    static final int CRYPTO_SIGNATURE_LENGTH = 100;
    static final int CRYPTO_HASH_LENGTH = 100;
    static final int CRYPTO_PUBKEY_LENGTH = 100;

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "USER_EVENT_SEQ")
    @SequenceGenerator(name = "USER_EVENT_SEQ", sequenceName="USER_EVENT_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @Column(name = "issuer", nullable = false, length = CRYPTO_PUBKEY_LENGTH)
    private String issuer;

    @Column(name = "recipient", nullable = false, length = CRYPTO_PUBKEY_LENGTH)
    private String recipient;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Lob
    @Column(length=20971520)
    private String content;

    @Column(name = "hash", length = CRYPTO_HASH_LENGTH)
    private String hash;

    @Column(name = "signature", length = CRYPTO_SIGNATURE_LENGTH)
    private String signature;

    @Column(name = "read_signature", length = CRYPTO_SIGNATURE_LENGTH)
    private String readSignature;

    public String toString() {
        return new StringBuilder().append(super.toString())
                .append(",eventType=").append(this.eventType)
                .append(",issuer=").append(this.issuer)
                .append(",hash=").append(this.hash)
                .toString();
    }
}

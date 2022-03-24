package net.sumaris.core.model.administration.user;

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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.dao.technical.model.IUpdateDateEntity;
import net.sumaris.core.model.referential.IReferentialEntity;

import javax.persistence.*;
import java.util.Date;

@Data
@FieldNameConstants
@Entity
@Table(name = "user_settings")
public class UserSettings implements IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "USER_SETTINGS_SEQ")
    @SequenceGenerator(name = "USER_SETTINGS_SEQ", sequenceName="USER_SETTINGS_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "update_date")
    private Date updateDate;

    @Column(name="issuer", nullable = false, unique = true)
    private String issuer;

    @Column(name="locale", nullable = false, length = 10)
    private String locale;

    @Column(name="lat_long_format", length = 6)
    private String latLongFormat;

    @Column(name="nonce")
    private String nonce;

    @Column(name="content", length = 5000)
    private String content;


}

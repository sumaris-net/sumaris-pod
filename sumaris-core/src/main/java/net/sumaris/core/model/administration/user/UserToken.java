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

import lombok.*;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IEntity;
import net.sumaris.core.model.referential.IReferentialEntity;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "user_token")
@Cacheable
public class UserToken implements IEntity<Integer> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "USER_TOKEN_SEQ")
    @SequenceGenerator(name = "USER_TOKEN_SEQ", sequenceName="USER_TOKEN_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "creation_date", nullable = false)
    private Date creationDate;

    @Column(name="pubkey", nullable = false)
    private String pubkey;

    @Column(name="token", nullable = false, unique = true)
    private String token;

    public int hashCode() {
        return Objects.hash(token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserToken userToken = (UserToken) o;
        return token.equals(userToken.token);
    }
}

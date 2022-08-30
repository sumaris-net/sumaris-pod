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

import lombok.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.user.Person;
import org.apache.commons.lang3.builder.EqualsBuilder;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "user_profile")
public class UserProfile implements IItemReferentialEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_PROFILE_SEQ")
    @SequenceGenerator(name = "USER_PROFILE_SEQ", sequenceName="USER_PROFILE_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
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

    @Column(nullable = false, length = 50)
    
    private String label;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "userProfiles", targetEntity = Person.class)
    private Set<Person> users = new HashSet<>();

    public int hashCode() {
        return Objects.hash(label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UserProfile that = (UserProfile) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }
}

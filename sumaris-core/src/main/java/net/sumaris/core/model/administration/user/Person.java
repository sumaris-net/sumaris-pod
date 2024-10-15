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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.UserProfile;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cascade;

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
@Table(name = "person")
@Cacheable
@NamedQueries({
        @NamedQuery(name = "Person.count", query = "SELECT\n" +
                "        count(distinct t.id)\n" +
                "      FROM\n" +
                "        Person t\n" +
                "        inner join t.status s\n" +
                "        left outer join t.userProfiles up\n" +
                "      WHERE\n" +
                "        (:userProfileId is null OR up.id = :userProfileId)\n" +
                "        AND (:statusIds is null OR s.id IN (:statusIds))\n" +
                "        AND (:email is null OR upper(t.email) = upper(:email))\n" +
                "        AND (:pubkey is null OR t.pubkey = :pubkey)\n" +
                "        AND (:firstName is null OR upper(t.firstName) = upper(:firstName))\n" +
                "        AND (:lastName is null OR upper(t.lastName) = upper(:lastName))"
        )
})
public class Person implements IReferentialWithStatusEntity<Integer> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "PERSON_SEQ")
    @SequenceGenerator(name = "PERSON_SEQ", sequenceName="PERSON_SEQ", allocationSize = SEQUENCE_ALLOCATION_SIZE)
    
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

    @Column(name="first_name", nullable = false)
    private String firstName;

    @Column(name="last_name", nullable = false)
    private String lastName;

    @Column(name="email", nullable = false, unique = true)
    private String email;

    @Column(name="email_md5", unique = true)
    private String emailMD5;

    @Column(name="pubkey", unique = true)
    private String pubkey;

    @Column(name="username", length = 40)
    private String username;

    @Column(name="username_extranet", length = 40)
    private String usernameExtranet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_fk", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    private Department department;

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = UserProfile.class)
    @Cascade(org.hibernate.annotations.CascadeType.DETACH)
    @JoinTable(name = "person2user_profile",
            joinColumns = {
                @JoinColumn(name = "person_fk", nullable = false, updatable = false)
            },
            inverseJoinColumns = {
                @JoinColumn(name = "user_profile_fk", nullable = false, updatable = false)
            })
    private Set<UserProfile> userProfiles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_fk")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private ImageAttachment avatar;

    public int hashCode() {
        return Objects.hash(id, pubkey, email);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Person that = (Person) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .isEquals();
    }
}

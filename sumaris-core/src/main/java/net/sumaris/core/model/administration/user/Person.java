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

import com.google.common.collect.Sets;
import lombok.Data;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.UserProfile;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@Entity
@Table(name = "person")
@Cacheable
public class Person implements IReferentialEntity {

    public static final String PROPERTY_PUBKEY = "pubkey";
    public static final String PROPERTY_LAST_NAME= "lastName";
    public static final String PROPERTY_FIRST_NAME = "firstName";
    public static final String PROPERTY_EMAIL = "email";
    public static final String PROPERTY_EMAIL_MD5 = "emailMD5";
    public static final String PROPERTY_USER_PROFILES = "userProfiles";
    public static final String PROPERTY_STATUS = "status";

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
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

    public String toString() {
        return new StringBuilder().append(super.toString()).append(",email=").append(this.email).toString();
    }

    public int hashCode() {
        return Objects.hash(id, pubkey, email);
    }
}

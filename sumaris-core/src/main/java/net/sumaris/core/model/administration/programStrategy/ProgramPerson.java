package net.sumaris.core.model.administration.programStrategy;

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
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.model.referential.location.Location;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Entity
@Table(name = "program2person")
@NamedQueries({
        @NamedQuery(name = "ProgramPerson.count", query = "SELECT\n" +
                "   count(distinct t.id)\n" +
                "      FROM\n" +
                "        ProgramPerson t\n" +
                "      WHERE\n" +
                "        (:programId is null OR t.program.id = :programId)\n" +
                "        AND (:personId is null OR t.person.id = :personId)\n" +
                "        AND (:privilegeId is null OR  t.privilege.id = :privilegeId)"
        ),
    @NamedQuery(name = "ProgramPerson.privilegeIds", query = "SELECT\n" +
        "   distinct t.privilege.id\n" +
        "      FROM\n" +
        "        ProgramPerson t\n" +
        "        INNER JOIN t.privilege p" +
        "      WHERE\n" +
        "        (:programId is null OR t.program.id = :programId)\n" +
        "        AND (:personId is null OR t.person.id = :personId)\n" +
        "        AND (:privilegeId is null OR  t.privilege.id = :privilegeId)"
    )
})
public class ProgramPerson implements IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "PROGRAM2PERSON_SEQ")
    @SequenceGenerator(name = "PROGRAM2PERSON_SEQ", sequenceName="PROGRAM2PERSON_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_fk", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_fk")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_privilege_fk", nullable = false)
    private ProgramPrivilege privilege;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_fk", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_person_fk")
    private Person referencePerson;

}

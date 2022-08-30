package net.sumaris.core.model.file;

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
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.IUpdateDateEntity;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.IReferentialEntity;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter

@FieldNameConstants
@Entity
@Table(name = "file")
public class File implements Serializable, IUpdateDateEntity<Integer, Date> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FILE_SEQ")
    @SequenceGenerator(name = "FILE_SEQ", sequenceName="FILE_SEQ", allocationSize = IReferentialEntity.SEQUENCE_ALLOCATION_SIZE)
    
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false)
    
    private String name;

    @Column(nullable = false, name = "content_type")
    private String contentType;

    @Column(length = 2000)
    private String comments;

    /* -- Quality insurance -- */

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_person_fk")
    private Person recorderPerson;

    /* -- Tree link -- */

    @OneToMany(fetch = FetchType.LAZY, targetEntity = FileLine.class, mappedBy = FileLine.Fields.FILE)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    private List<FileLine> lines = new ArrayList<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_status_fk", nullable = false)
    private FileStatus status;
}

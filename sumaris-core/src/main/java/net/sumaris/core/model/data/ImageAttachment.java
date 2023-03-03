package net.sumaris.core.model.data;

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
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.model.administration.user.Department;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.model.referential.ObjectType;
import net.sumaris.core.model.referential.ProcessingStatus;
import net.sumaris.core.model.referential.QualityFlag;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="image_attachment",
    indexes = @Index(name="image_attachment_object_idx", columnList = "object_type_fk,object_id")
)
public class ImageAttachment implements IDataEntity<Integer>,
        IWithRecorderPersonEntity<Integer, Person>,
        IWithRecorderDepartmentEntity<Integer, Department> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IMAGE_ATTACHMENT_SEQ")
    @SequenceGenerator(name = "IMAGE_ATTACHMENT_SEQ", sequenceName="IMAGE_ATTACHMENT_SEQ", allocationSize = IDataEntity.SEQUENCE_ALLOCATION_SIZE)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "date_time")
    private Date dateTime;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(length=20971520)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    // TODO: find a way to avoid selection of this field
    private String content;

    @Column()
    private String path;

    @Column(length = LENGTH_COMMENTS)
    private String comments;

    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "update_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_person_fk")
    private Person recorderPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_department_fk", nullable = false)
    private Department recorderDepartment;

    @Column(name="control_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date controlDate;

    @Column(name="validation_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date validationDate;

    @Column(name="qualification_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qualificationDate;

    @Column(name="qualification_comments", length = LENGTH_COMMENTS)
    private String qualificationComments;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = QualityFlag.class)
    @JoinColumn(name = "quality_flag_fk", nullable = false)
    private QualityFlag qualityFlag;


    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ObjectType.class)
    @JoinColumn(name = "object_type_fk", nullable = true) // Nullable for SUMARiS compatibility (e.g. for Department logo or Person avatar)
    private ObjectType objectType;

    @Column(name = "object_id", nullable = true) // Nullable for SUMARiS compatibility (e.g. for Department logo or Person avatar)
    private Integer objectId;
}
